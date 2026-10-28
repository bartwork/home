package ru.home.kitchenpanel.mqtt

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import ru.home.kitchenpanel.model.Topics
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class HomeMqttClient {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "home-mqtt").apply { isDaemon = true }
    }
    private val clientRef = AtomicReference<MqttClient?>(null)
    private val subscribed = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _values = MutableStateFlow<Map<String, String>>(emptyMap())
    val values: StateFlow<Map<String, String>> = _values.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun connect(host: String, port: Int) {
        if (closed.get()) return
        executor.execute {
            if (closed.get()) return@execute
            disconnectInternal()
            val serverUri = "tcp://${host.trim()}:$port"
            try {
                val client = MqttClient(
                    serverUri,
                    "kitchen-panel-${UUID.randomUUID().toString().take(8)}",
                    MemoryPersistence(),
                )
                client.setTimeToWait(15_000)
                client.setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        if (clientRef.get() !== client) return
                        _connected.value = true
                        _lastError.value = null
                        executor.execute {
                            if (clientRef.get() === client) subscribeAll(client)
                        }
                    }

                    override fun connectionLost(cause: Throwable?) {
                        if (clientRef.get() !== client) return
                        _connected.value = false
                        subscribed.set(false)
                        _lastError.value = cause?.message ?: "соединение потеряно"
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        if (topic.isNullOrBlank() || message == null) return
                        val payload = String(message.payload, Charsets.UTF_8)
                        _values.update { cur ->
                            if (cur[topic] == payload) cur else cur + (topic to payload)
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
                })

                val opts = MqttConnectOptions().apply {
                    isAutomaticReconnect = true
                    isCleanSession = true
                    connectionTimeout = 10
                    keepAliveInterval = 30
                    maxReconnectDelay = 15_000
                }
                client.connect(opts)
                clientRef.set(client)
                if (client.isConnected) {
                    _connected.value = true
                    _lastError.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "connect failed", e)
                _connected.value = false
                _lastError.value = e.message ?: e.javaClass.simpleName
            }
        }
    }

    fun publish(stateTopic: String, payload: String) {
        _values.update { cur ->
            if (cur[stateTopic] == payload) cur else cur + (stateTopic to payload)
        }
        if (closed.get()) return
        executor.execute {
            val client = clientRef.get()
            if (client == null || !client.isConnected) {
                _connected.value = false
                _lastError.value = "нет MQTT"
                return@execute
            }
            try {
                val topic = Topics.commandTopic(stateTopic)
                val msg = MqttMessage(payload.toByteArray(Charsets.UTF_8)).apply {
                    qos = 0
                    isRetained = false
                }
                client.publish(topic, msg)
            } catch (e: MqttException) {
                Log.e(TAG, "publish failed $stateTopic", e)
                _lastError.value = e.message
            }
        }
    }

    fun disconnect() {
        if (closed.get()) return
        executor.execute { disconnectInternal() }
    }

    /** Остановить клиент и пул потоков (ViewModel.onCleared). */
    fun shutdown() {
        if (!closed.compareAndSet(false, true)) return
        try {
            executor.execute { disconnectInternal() }
            executor.shutdown()
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (_: Exception) {
            executor.shutdownNow()
        }
    }

    private fun disconnectInternal() {
        subscribed.set(false)
        val client = clientRef.getAndSet(null) ?: return
        try {
            client.setCallback(null)
        } catch (_: Exception) {
        }
        try {
            if (client.isConnected) client.disconnect(500)
        } catch (_: Exception) {
        }
        try {
            client.close()
        } catch (_: Exception) {
        }
        _connected.value = false
    }

    private fun subscribeAll(client: MqttClient) {
        if (!client.isConnected) return
        if (!subscribed.compareAndSet(false, true)) return
        val topics = Topics.allStateTopics().toTypedArray()
        val qos = IntArray(topics.size) { 0 }
        try {
            client.subscribe(topics, qos)
            Log.i(TAG, "subscribed ${topics.size} topics")
        } catch (e: MqttException) {
            subscribed.set(false)
            Log.e(TAG, "subscribe failed", e)
            _lastError.value = e.message
            var ok = 0
            for (t in topics) {
                try {
                    client.subscribe(t, 0)
                    ok++
                } catch (ex: MqttException) {
                    Log.w(TAG, "subscribe $t: ${ex.message}")
                }
            }
            if (ok > 0) {
                subscribed.set(true)
                if (ok == topics.size) _lastError.value = null
            }
        }
    }

    companion object {
        private const val TAG = "HomeMqtt"
    }
}
