package ru.home.roompanel

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
    private val sensorManager =
        activity.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val light: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    private var smoothedLux = 40f
    private var lastApplied = -1f
    private var listening = false
    private var autoEnabled = true
    private var manualBrightness = 0.55f

    val available: Boolean get() = light != null

    fun start() {
        if (listening) return
        listening = true
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
        stop()
        activityRef.clear()
    }

    fun setAutoEnabled(enabled: Boolean) {
        if (autoEnabled == enabled) return
        autoEnabled = enabled
        if (!listening) return
        sensorManager.unregisterListener(this)
        if (enabled && light != null) {
            sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_UI)
        } else {
            apply(manualBrightness, force = true)
        }
    }

    fun setManualBrightness(value: Float) {
        manualBrightness = value.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
        if (!autoEnabled) apply(manualBrightness, force = true)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!autoEnabled) return
        val lux = event?.values?.getOrNull(0) ?: return
        smoothedLux = smoothedLux * 0.82f + lux.coerceAtLeast(0f) * 0.18f
        val target = luxToBrightness(smoothedLux)
        if (lastApplied < 0f || kotlin.math.abs(target - lastApplied) >= 0.025f) {
            apply(target)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun apply(brightness: Float, force: Boolean = false) {
        val b = brightness.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
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

    companion object {
        private const val TAG = "AmbientBrightness"
        const val MIN_BRIGHTNESS = 0.06f
        const val MAX_BRIGHTNESS = 1.0f

        fun luxToBrightness(lux: Float): Float {
            val x = ln(1.0 + max(lux, 0f).toDouble()).toFloat()
            val t = ((x - 0.4f) / 6.2f).coerceIn(0f, 1f)
            val curved = t.toDouble().pow(0.85).toFloat()
            return MIN_BRIGHTNESS + (MAX_BRIGHTNESS - MIN_BRIGHTNESS) * curved
        }
    }
}
