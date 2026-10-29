import Foundation

/// Единая сессия приложения: настройки + MQTT + SIP.
@Observable
final class HomeSession {
    let settings = AppSettings()
    let mqtt = MqttService()
    let sip = SipService()

    private(set) var started = false

    func start() {
        if started {
            // После фона — поднять упавшие соединения.
            ensureConnected()
            return
        }
        started = true
        reconnect()
    }

    func stop() {
        guard started else { return }
        started = false
        mqtt.disconnect()
        sip.stop()
    }

    func reconnect() {
        mqtt.connect(host: settings.mqttHost, port: settings.mqttPort)
        sip.start(settings: settings)
    }

    private func ensureConnected() {
        if !mqtt.connected {
            mqtt.connect(host: settings.mqttHost, port: settings.mqttPort)
        }
        if sip.registration != .ok && sip.registration != .progress {
            sip.start(settings: settings)
        }
    }
}
