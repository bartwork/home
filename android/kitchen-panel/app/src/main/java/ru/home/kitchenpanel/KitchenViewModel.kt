package ru.home.kitchenpanel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ru.home.kitchenpanel.model.Topics
import ru.home.kitchenpanel.mqtt.HomeMqttClient

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
)

class KitchenViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val mqtt = HomeMqttClient()

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

    private val lastDimmerLevels = mutableMapOf<String, Int>()

    val uiState: StateFlow<UiState> = combine(
        combine(mqtt.connected, mqtt.values, mqtt.lastError, ::Triple),
        combine(_brokerHost, _brokerPort, ::Pair),
        combine(_autoBrightness, _manualBrightness, _clickSound, _lightSensorAvailable) { a, b, c, d ->
            Quad(a, b, c, d)
        },
    ) { mqttTriple, broker, panel ->
        UiState(
            connected = mqttTriple.first,
            values = mqttTriple.second,
            lastError = mqttTriple.third,
            brokerHost = broker.first,
            brokerPort = broker.second,
            autoBrightness = panel.a,
            manualBrightness = panel.b,
            clickSound = panel.c,
            lightSensorAvailable = panel.d,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    init {
        reconnect()
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

    fun publish(topic: String, payload: String) = mqtt.publish(topic, payload)

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
        mqtt.shutdown()
        super.onCleared()
    }

    companion object {
        private const val PREFS = "kitchen_panel"
        private const val KEY_HOST = "broker_host"
        private const val KEY_PORT = "broker_port"
        private const val KEY_AUTO_BRI = "auto_brightness"
        private const val KEY_MANUAL_BRI = "manual_brightness"
        private const val KEY_CLICK_SOUND = "click_sound"
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
