import Foundation

@Observable
final class PanelSession {
    let settings = AppSettings()
    let usage = UsageStats()
    let errors = ErrorLog()
    let mqtt = MqttService()
    let sip = SipService()
    let doorCamera = DoorCameraFeed()

    private(set) var started = false

    init() {
        mqtt.onPublish = { [weak self] topic in
            self?.usage.record(topic: topic)
        }
        mqtt.onError = { [weak self] message in
            self?.errors.record(kind: .mqtt, message: message)
        }
        sip.onError = { [weak self] message in
            self?.errors.record(kind: .sip, message: message)
        }
        doorCamera.onError = { [weak self] message in
            self?.errors.record(kind: .camera, message: message)
        }
    }

    func start() {
        if started {
            ensureConnected()
            return
        }
        started = true
        reconnect()
        retainCamera()
    }

    func stop() {
        guard started else { return }
        started = false
        doorCamera.release()
        mqtt.disconnect()
        sip.stop()
    }

    func retainCamera() {
        doorCamera.retain(
            host: settings.doorCameraHost,
            username: settings.doorCameraUser,
            password: settings.doorCameraPassword
        )
    }

    func reconnect() {
        mqtt.connect(host: settings.mqttHost, port: settings.mqttPort)
        sip.restart(settings: settings)
        doorCamera.release()
        retainCamera()
    }

    /// Мягкий resume (scene active): MQTT + SIP без полного teardown, если уже ок.
    private func ensureConnected() {
        if !mqtt.connected {
            mqtt.connect(host: settings.mqttHost, port: settings.mqttPort)
        }
        switch sip.registration {
        case .ok, .progress:
            break
        case .none, .failed, .cleared:
            sip.start(settings: settings)
        }
    }
}
