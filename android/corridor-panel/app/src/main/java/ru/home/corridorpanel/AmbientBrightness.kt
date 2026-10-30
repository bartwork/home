package ru.home.corridorpanel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import java.lang.ref.WeakReference
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow

/**
 * Яркость окна: авто по датчику или ручная.
 * Activity только через WeakReference — без утечки при recreate.
 */
class AmbientBrightness(activity: Activity) : SensorEventListener {
    private val activityRef = WeakReference(activity)
    private val appContext = activity.applicationContext
    private val sensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val light: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    private var smoothedLux = 40f
    private var lastApplied = -1f
    private var listening = false
    private var autoEnabled = true
    private var manualBrightness = 0.55f
    private var blanked = false

    private var savedTimeout: Int? = null
    private var savedSysBrightness: Int? = null
    private var savedSysMode: Int? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    /** После long-press палец ещё на экране — sleep чуть позже, иначе таймаут сбрасывается. */
    private val sleepAfterLift = Runnable { tryGoToSleep(activityRef.get()) }

    val available: Boolean get() = light != null
    val isBlanked: Boolean get() = blanked

    fun start() {
        if (listening) return
        listening = true
        if (blanked) {
            applyRaw(0f, force = true)
            return
        }
        if (autoEnabled && light != null) {
            sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_UI)
            Log.i(TAG, "light sensor on (${light.name})")
        } else {
            apply(manualBrightness, force = true)
        }
    }

    fun stop() {
        if (!listening) return
        listening = false
        sensorManager.unregisterListener(this)
    }

    /** Полная остановка (onDestroy). */
    fun release() {
        mainHandler.removeCallbacks(sleepAfterLift)
        if (blanked) {
            // не оставляем урезанный screen-off timeout после kill
            blanked = false
            restoreSystemDisplayLocked()
        }
        stop()
        activityRef.clear()
    }

    /**
     * Выключить подсветку: яркость 0 сразу + короткий screen-off timeout
     * (нужен WRITE_SETTINGS) + goToSleep после отпускания пальца.
     */
    fun setBlanked(value: Boolean) {
        if (blanked == value) return
        blanked = value
        val activity = activityRef.get()
        mainHandler.removeCallbacks(sleepAfterLift)
        if (value) {
            sensorManager.unregisterListener(this)
            applyRaw(0f, force = true)
            saveAndDimSystem(activity)
            // палец ещё держит вкладку → sleep через короткую паузу
            mainHandler.postDelayed(sleepAfterLift, 180)
        } else {
            restoreSystemDisplayLocked()
            if (listening) {
                if (autoEnabled && light != null) {
                    sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_UI)
                } else {
                    apply(manualBrightness, force = true)
                }
            } else {
                apply(manualBrightness, force = true)
            }
        }
    }

    fun ensureWriteSettings(activity: Activity): Boolean {
        if (Settings.System.canWrite(activity)) return true
        try {
            activity.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:${activity.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
            Log.w(TAG, "WRITE_SETTINGS intent failed", e)
        }
        return false
    }

    fun setAutoEnabled(enabled: Boolean) {
        if (autoEnabled == enabled) return
        autoEnabled = enabled
        if (!listening || blanked) return
        sensorManager.unregisterListener(this)
        if (enabled && light != null) {
            sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_UI)
        } else {
            apply(manualBrightness, force = true)
        }
    }

    fun setManualBrightness(value: Float) {
        manualBrightness = value.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
        if (!autoEnabled && !blanked) apply(manualBrightness, force = true)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!autoEnabled || blanked) return
        val lux = event?.values?.getOrNull(0) ?: return
        smoothedLux = smoothedLux * 0.82f + lux.coerceAtLeast(0f) * 0.18f
        val target = luxToBrightness(smoothedLux)
        if (lastApplied < 0f || kotlin.math.abs(target - lastApplied) >= 0.025f) {
            apply(target)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun apply(brightness: Float, force: Boolean = false) {
        applyRaw(brightness.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS), force)
    }

    private fun applyRaw(brightness: Float, force: Boolean = false) {
        val b = brightness.coerceIn(0f, MAX_BRIGHTNESS)
        if (!force && lastApplied >= 0f && kotlin.math.abs(b - lastApplied) < 0.012f) return
        lastApplied = b
        val activity = activityRef.get() ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        activity.runOnUiThread {
            val a = activityRef.get() ?: return@runOnUiThread
            if (a.isFinishing || a.isDestroyed) return@runOnUiThread
            val lp = a.window.attributes
            lp.screenBrightness = b
            a.window.attributes = lp
        }
    }

    private fun saveAndDimSystem(activity: Activity?) {
        val a = activity ?: return
        if (!Settings.System.canWrite(a)) {
            Log.i(TAG, "no WRITE_SETTINGS — только window brightness=0")
            return
        }
        val cr = a.contentResolver
        try {
            if (savedTimeout == null) {
                savedTimeout = Settings.System.getInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, 60_000)
            }
            if (savedSysBrightness == null) {
                savedSysBrightness = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 128)
            }
            if (savedSysMode == null) {
                savedSysMode = Settings.System.getInt(
                    cr,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
                )
            }
            Settings.System.putInt(
                cr,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
            )
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, 0)
            // минимум, который обычно принимает система — подсветка гаснет почти сразу после lift
            Settings.System.putInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, 100)
        } catch (e: Exception) {
            Log.w(TAG, "system dim failed", e)
        }
    }

    private fun restoreSystemDisplayLocked() {
        val activity = activityRef.get() ?: return
        if (!Settings.System.canWrite(activity)) return
        val cr = activity.contentResolver
        try {
            savedTimeout?.let {
                Settings.System.putInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, it)
            }
            savedSysMode?.let {
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, it)
            }
            savedSysBrightness?.let {
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, it)
            }
        } catch (e: Exception) {
            Log.w(TAG, "system restore failed", e)
        }
        savedTimeout = null
        savedSysBrightness = null
        savedSysMode = null
    }

    private fun tryGoToSleep(activity: Activity?) {
        val a = activity ?: return
        try {
            val pm = a.getSystemService(Context.POWER_SERVICE) as PowerManager
            val m = PowerManager::class.java.getMethod("goToSleep", Long::class.javaPrimitiveType)
            m.invoke(pm, SystemClock.uptimeMillis())
            Log.i(TAG, "goToSleep ok")
        } catch (_: Exception) {
            // обычно нет DEVICE_POWER — полагаемся на timeout
        }
    }

    companion object {
        private const val TAG = "AmbientBrightness"
        /** Ниже прежнего 0.06 — ночью можно почти погасить. */
        const val MIN_BRIGHTNESS = 0.015f
        const val MAX_BRIGHTNESS = 1.0f

        fun luxToBrightness(lux: Float): Float {
            val x = ln(1.0 + max(lux, 0f).toDouble()).toFloat()
            val t = ((x - 0.4f) / 6.2f).coerceIn(0f, 1f)
            val curved = t.toDouble().pow(0.85).toFloat()
            return MIN_BRIGHTNESS + (MAX_BRIGHTNESS - MIN_BRIGHTNESS) * curved
        }
    }
}
