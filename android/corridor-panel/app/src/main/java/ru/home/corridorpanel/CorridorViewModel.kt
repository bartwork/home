package ru.home.corridorpanel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ru.home.corridorpanel.model.Topics
import ru.home.corridorpanel.mqtt.HomeMqttClient
import ru.home.corridorpanel.sip.DoorCameraFeed
import ru.home.corridorpanel.sip.SipConfig
import ru.home.corridorpanel.sip.SipService
import ru.home.corridorpanel.sip.SipUiState
import ru.home.corridorpanel.ui.theme.ThemeMode

data class UiState(
    val connected: Boolean = false,
    val values: Map<String, String> = emptyMap(),
    val lastError: String? = null,
    val brokerHost: String = Topics.DEFAULT_BROKER_HOST,
    val brokerPort: Int = Topics.DEFAULT_BROKER_PORT,
    val autoBrightness: Boolean = true,
    /** Ручная яркость 0…1 (когда авто выкл). */
    val manualBrightness: Float = 0.55f,
    val clickSound: Boolean = true,
    val lightSensorAvailable: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.Dark,
    val sip: SipUiState = SipUiState(),
)

class CorridorViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val mqtt = HomeMqttClient()
    val usage = UsageStats(app.applicationContext)
    val errors = ErrorLog(app.applicationContext)
    val sip = SipService(app.applicationContext)
    val doorCamera = DoorCameraFeed()

    private val _brokerHost = MutableStateFlow(
        prefs.getString(KEY_HOST, Topics.DEFAULT_BROKER_HOST) ?: Topics.DEFAULT_BROKER_HOST,
    )
    private val _brokerPort = MutableStateFlow(
        prefs.getInt(KEY_PORT, Topics.DEFAULT_BROKER_PORT),
    )
    private val _autoBrightness = MutableStateFlow(prefs.getBoolean(KEY_AUTO_BRI, true))
    private val _manualBrightness = MutableStateFlow(
        prefs.getFloat(KEY_MANUAL_BRI, 0.55f).coerceIn(
            AmbientBrightness.MIN_BRIGHTNESS,
            AmbientBrightness.MAX_BRIGHTNESS,
        ),
    )
    private val _clickSound = MutableStateFlow(prefs.getBoolean(KEY_CLICK_SOUND, true))
    private val _lightSensorAvailable = MutableStateFlow(true)
    private val _themeMode = MutableStateFlow(
        when (prefs.getString(KEY_THEME, ThemeMode.Dark.name)) {
            ThemeMode.Light.name -> ThemeMode.Light
            else -> ThemeMode.Dark
        },
    )

    private val lastDimmerLevels = mutableMapOf<String, Int>()

    val uiState: StateFlow<UiState> = combine(
        combine(mqtt.connected, mqtt.values, mqtt.lastError) { c, v, e ->
            MqttSnap(c, v, e)
        },
        combine(_brokerHost, _brokerPort, ::Pair),
        combine(_autoBrightness, _manualBrightness, _clickSound, _lightSensorAvailable) { a, b, c, d ->
            Quad(a, b, c, d)
        },
        combine(_themeMode, sip.state, ::Pair),
    ) { mqttSnap, broker, panel, themeSip ->
        UiState(
            connected = mqttSnap.connected,
            values = mqttSnap.values,
            lastError = mqttSnap.error,
            brokerHost = broker.first,
            brokerPort = broker.second,
            autoBrightness = panel.a,
            manualBrightness = panel.b,
            clickSound = panel.c,
            lightSensorAvailable = panel.d,
            themeMode = themeSip.first,
            sip = themeSip.second,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    private data class MqttSnap(
        val connected: Boolean,
        val values: Map<String, String>,
        val error: String?,
    )

    init {
        mqtt.onError = { errors.record(ErrorKind.Mqtt, it) }
        sip.onError = { errors.record(ErrorKind.Sip, it) }
        doorCamera.onError = { errors.record(ErrorKind.Camera, it) }
        reconnect()
    }

    /** Вызывать после выдачи RECORD_AUDIO / CAMERA (Linphone probe). */
    fun startSip() {
        val cfg = loadSipConfig()
        sip.start(cfg)
        doorCamera.start(cfg)
    }

    fun setLightSensorAvailable(available: Boolean) {
        _lightSensorAvailable.value = available
    }

    fun setAutoBrightness(enabled: Boolean) {
        _autoBrightness.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_BRI, enabled).apply()
    }

    fun setManualBrightness(value: Float) {
        val v = value.coerceIn(AmbientBrightness.MIN_BRIGHTNESS, AmbientBrightness.MAX_BRIGHTNESS)
        if (_manualBrightness.value == v) return
        _manualBrightness.value = v
        prefs.edit().putFloat(KEY_MANUAL_BRI, v).apply()
    }

    fun setClickSound(enabled: Boolean) {
        _clickSound.value = enabled
        prefs.edit().putBoolean(KEY_CLICK_SOUND, enabled).apply()
    }

    fun setThemeMode(mode: ThemeMode) {
        if (_themeMode.value == mode) return
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME, mode.name).apply()
    }

    fun reconnect() {
        mqtt.connect(_brokerHost.value, _brokerPort.value)
    }

    fun saveBroker(host: String, port: Int) {
        val h = host.trim().ifEmpty { Topics.DEFAULT_BROKER_HOST }
        val p = port.coerceIn(1, 65535)
        prefs.edit().putString(KEY_HOST, h).putInt(KEY_PORT, p).apply()
        _brokerHost.value = h
        _brokerPort.value = p
        mqtt.connect(h, p)
    }

    fun saveSip(config: SipConfig) {
        val next = config.normalized()
        prefs.edit()
            .putString(KEY_SIP_DOMAIN, next.domain)
            .putString(KEY_SIP_USER, next.username)
            .putString(KEY_SIP_PASS, next.password)
            .putString(KEY_SIP_TRANSPORT, next.transport)
            .putString(KEY_DOOR_EXT, next.doorExtension)
            .putString(KEY_CAM_HOST, next.cameraHost)
            .putString(KEY_CAM_USER, next.cameraUser)
            .putString(KEY_CAM_PASS, next.cameraPassword)
            .apply()
        sip.start(next)
        doorCamera.start(next)
    }

    fun publish(topic: String, payload: String) {
        usage.record(topic)
        mqtt.publish(topic, payload)
    }

    fun clearUsageStats() = usage.clear()

    fun clearErrorLog() = errors.clear()

    fun toggleSwitch(topic: String) {
        val on = uiState.value.values[topic] == "1"
        publish(topic, if (on) "0" else "1")
    }

    fun toggleDimmer(d: Topics.Dimmer) {
        val values = uiState.value.values
        val level = values[d.channel]?.toDoubleOrNull()?.toInt()
        val powerOn = when (values[d.power]) {
            "1" -> true
            "0" -> false
            else -> (level ?: 0) > 0
        }
        if (powerOn) {
            if (level != null && level > 0) lastDimmerLevels[d.id] = level
            publish(d.power, "0")
        } else {
            val recall = when {
                level != null && level > 0 -> level
                else -> lastDimmerLevels[d.id] ?: 80
            }
            publish(d.power, "1")
            if ((level ?: 0) <= 0) publish(d.channel, recall.toString())
        }
    }

    fun setDimmerLevel(d: Topics.Dimmer, level: Int) {
        val next = level.coerceIn(0, 100)
        if (next > 0) lastDimmerLevels[d.id] = next
        publish(d.channel, next.toString())
        val power = uiState.value.values[d.power]
        if (next > 0 && power != "1") publish(d.power, "1")
        if (next == 0 && power != "0") publish(d.power, "0")
    }

    override fun onCleared() {
        doorCamera.shutdown()
        sip.stop()
        mqtt.shutdown()
        super.onCleared()
    }

    private fun loadSipConfig(): SipConfig = SipConfig(
        domain = prefs.getString(KEY_SIP_DOMAIN, SipConfig.DEFAULT_DOMAIN) ?: SipConfig.DEFAULT_DOMAIN,
        username = prefs.getString(KEY_SIP_USER, SipConfig.DEFAULT_USERNAME) ?: SipConfig.DEFAULT_USERNAME,
        password = prefs.getString(KEY_SIP_PASS, SipConfig.DEFAULT_PASSWORD) ?: SipConfig.DEFAULT_PASSWORD,
        transport = prefs.getString(KEY_SIP_TRANSPORT, SipConfig.DEFAULT_TRANSPORT)
            ?: SipConfig.DEFAULT_TRANSPORT,
        doorExtension = prefs.getString(KEY_DOOR_EXT, SipConfig.DEFAULT_DOOR_EXT)
            ?: SipConfig.DEFAULT_DOOR_EXT,
        cameraHost = prefs.getString(KEY_CAM_HOST, SipConfig.DEFAULT_CAMERA_HOST)
            ?: SipConfig.DEFAULT_CAMERA_HOST,
        cameraUser = prefs.getString(KEY_CAM_USER, SipConfig.DEFAULT_CAMERA_USER)
            ?: SipConfig.DEFAULT_CAMERA_USER,
        cameraPassword = prefs.getString(KEY_CAM_PASS, SipConfig.DEFAULT_CAMERA_PASSWORD)
            ?: SipConfig.DEFAULT_CAMERA_PASSWORD,
    )

    companion object {
        private const val PREFS = "corridor_panel"
        private const val KEY_HOST = "broker_host"
        private const val KEY_PORT = "broker_port"
        private const val KEY_AUTO_BRI = "auto_brightness"
        private const val KEY_MANUAL_BRI = "manual_brightness"
        private const val KEY_CLICK_SOUND = "click_sound"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_SIP_DOMAIN = "sip_domain"
        private const val KEY_SIP_USER = "sip_user"
        private const val KEY_SIP_PASS = "sip_pass"
        private const val KEY_SIP_TRANSPORT = "sip_transport"
        private const val KEY_DOOR_EXT = "door_ext"
        private const val KEY_CAM_HOST = "cam_host"
        private const val KEY_CAM_USER = "cam_user"
        private const val KEY_CAM_PASS = "cam_pass"
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
