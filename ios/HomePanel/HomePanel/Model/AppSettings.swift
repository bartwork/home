import Foundation

/// Настройки MQTT и SIP (UserDefaults).
@Observable
final class AppSettings {
    enum Defaults {
        static let mqttHost = "192.168.1.1"
        static let mqttPort = 1883
        static let sipDomain = "192.168.1.9"
        static let sipUsername = "102"
        static let sipPassword = "soft102test"
        static let sipTransport = "udp"
        static let doorExtension = "100"
        static let doorCameraHost = "192.168.1.253"
        static let doorCameraUser = "admin"
        static let doorCameraPassword = "258258zzz"
    }

    private enum Key {
        static let mqttHost = "mqtt.host"
        static let mqttPort = "mqtt.port"
        static let sipDomain = "sip.domain"
        static let sipUsername = "sip.username"
        static let sipPassword = "sip.password"
        static let sipTransport = "sip.transport"
        static let doorExtension = "sip.doorExt"
        static let doorCameraHost = "door.cameraHost"
        static let doorCameraUser = "door.cameraUser"
        static let doorCameraPassword = "door.cameraPassword"
    }

    var mqttHost: String {
        didSet { UserDefaults.standard.set(mqttHost, forKey: Key.mqttHost) }
    }

    var mqttPort: Int {
        didSet { UserDefaults.standard.set(mqttPort, forKey: Key.mqttPort) }
    }

    var sipDomain: String {
        didSet { UserDefaults.standard.set(sipDomain, forKey: Key.sipDomain) }
    }

    var sipUsername: String {
        didSet { UserDefaults.standard.set(sipUsername, forKey: Key.sipUsername) }
    }

    var sipPassword: String {
        didSet { UserDefaults.standard.set(sipPassword, forKey: Key.sipPassword) }
    }

    /// udp | tcp | tls
    var sipTransport: String {
        didSet { UserDefaults.standard.set(sipTransport, forKey: Key.sipTransport) }
    }

    var doorExtension: String {
        didSet { UserDefaults.standard.set(doorExtension, forKey: Key.doorExtension) }
    }

    /// IP панели для ISAPI-снимков (видеозвонок без SIP H.264).
    var doorCameraHost: String {
        didSet { UserDefaults.standard.set(doorCameraHost, forKey: Key.doorCameraHost) }
    }

    var doorCameraUser: String {
        didSet { UserDefaults.standard.set(doorCameraUser, forKey: Key.doorCameraUser) }
    }

    var doorCameraPassword: String {
        didSet { UserDefaults.standard.set(doorCameraPassword, forKey: Key.doorCameraPassword) }
    }

    var sipIdentity: String {
        "sip:\(sipUsername)@\(sipDomain)"
    }

    init() {
        let d = UserDefaults.standard
        mqttHost = d.string(forKey: Key.mqttHost) ?? Defaults.mqttHost
        mqttPort = (d.object(forKey: Key.mqttPort) as? Int) ?? Defaults.mqttPort
        sipDomain = d.string(forKey: Key.sipDomain) ?? Defaults.sipDomain
        sipUsername = d.string(forKey: Key.sipUsername) ?? Defaults.sipUsername
        sipPassword = d.string(forKey: Key.sipPassword) ?? Defaults.sipPassword
        sipTransport = d.string(forKey: Key.sipTransport) ?? Defaults.sipTransport
        doorExtension = d.string(forKey: Key.doorExtension) ?? Defaults.doorExtension
        doorCameraHost = d.string(forKey: Key.doorCameraHost) ?? Defaults.doorCameraHost
        doorCameraUser = d.string(forKey: Key.doorCameraUser) ?? Defaults.doorCameraUser
        doorCameraPassword = d.string(forKey: Key.doorCameraPassword) ?? Defaults.doorCameraPassword
    }
}
