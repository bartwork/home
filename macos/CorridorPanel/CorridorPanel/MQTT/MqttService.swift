import Foundation
import CocoaMQTT

/// MQTT к Wiren Board (как Android / iOS HomePanel).
@Observable
final class MqttService {
    private(set) var connected = false
    private(set) var values: [String: String] = [:]
    private(set) var lastError: String?

    private var client: CocoaMQTT?
    private let bridge = DelegateBridge()
    private var lastDimmerLevels: [String: Int] = [:]
    /// Колбэк после успешной постановки команды (статистика использования).
    var onPublish: ((String) -> Void)?
    /// Ошибки MQTT → журнал.
    var onError: ((String) -> Void)?

    init() {
        bridge.owner = self
    }

    func connect(host: String, port: Int) {
        disconnect()
        lastError = nil
        let cleanHost = host.trimmingCharacters(in: .whitespacesAndNewlines)
        let mqttPort = UInt16(clamping: max(1, port))
        let clientId = "mac-corridor-\(UUID().uuidString.prefix(8))"
        let mqtt = CocoaMQTT(clientID: clientId, host: cleanHost, port: mqttPort)
        mqtt.keepAlive = 30
        mqtt.autoReconnect = true
        mqtt.autoReconnectTimeInterval = 5
        mqtt.cleanSession = true
        mqtt.delegate = bridge
        mqtt.logLevel = .warning
        client = mqtt
        _ = mqtt.connect()
    }

    func disconnect() {
        client?.autoReconnect = false
        client?.disconnect()
        client = nil
        connected = false
    }

    func publish(stateTopic: String, payload: String) {
        values[stateTopic] = payload
        onPublish?(stateTopic)
        guard let client, connected else {
            reportError("нет MQTT")
            return
        }
        _ = client.publish(Topics.commandTopic(stateTopic), withString: payload, qos: .qos0, retained: false)
    }

    private func reportError(_ message: String) {
        lastError = message
        onError?(message)
    }

    func isOn(_ topic: String) -> Bool {
        values[topic] == "1"
    }

    func dimmerLevel(_ channel: String) -> Double {
        Double(values[channel] ?? "0") ?? 0
    }

    func intValue(_ topic: String) -> Int? {
        guard let raw = values[topic]?.trimmingCharacters(in: .whitespacesAndNewlines),
              let d = Double(raw) else { return nil }
        return Int(d)
    }

    func toggleSwitch(_ topic: String) {
        publish(stateTopic: topic, payload: isOn(topic) ? "0" : "1")
    }

    func toggleDimmer(_ d: Topics.Dimmer) {
        let level = Int(dimmerLevel(d.channel))
        let powerOn: Bool = {
            switch values[d.power] {
            case "1": return true
            case "0": return false
            default: return level > 0
            }
        }()
        if powerOn {
            if level > 0 { lastDimmerLevels[d.id] = level }
            publish(stateTopic: d.power, payload: "0")
        } else {
            let recall = level > 0 ? level : (lastDimmerLevels[d.id] ?? 80)
            publish(stateTopic: d.power, payload: "1")
            if level <= 0 {
                publish(stateTopic: d.channel, payload: "\(recall)")
            }
        }
    }

    func setDimmerLevel(_ d: Topics.Dimmer, level: Double) {
        let next = Int(min(100, max(0, level)).rounded())
        if next > 0 { lastDimmerLevels[d.id] = next }
        publish(stateTopic: d.channel, payload: "\(next)")
        if next > 0, !isOn(d.power) {
            publish(stateTopic: d.power, payload: "1")
        } else if next == 0, isOn(d.power) {
            publish(stateTopic: d.power, payload: "0")
        }
    }

    // MARK: - Bridge

    private func onMain(_ body: @escaping () -> Void) {
        if Thread.isMainThread { body() } else { DispatchQueue.main.async(execute: body) }
    }

    fileprivate func handleConnectAck(_ ack: CocoaMQTTConnAck, mqtt: CocoaMQTT) {
        onMain {
            if ack == .accept {
                self.connected = true
                self.lastError = nil
                for topic in Topics.allStateTopics() {
                    _ = mqtt.subscribe(topic, qos: .qos0)
                }
            } else {
                self.connected = false
                self.reportError("MQTT ack \(ack)")
            }
        }
    }

    fileprivate func handleMessage(_ message: CocoaMQTTMessage) {
        let payload = message.string ?? String(data: Data(message.payload), encoding: .utf8) ?? ""
        onMain {
            self.values[message.topic] = payload
        }
    }

    fileprivate func handleDisconnect(_ err: Error?) {
        onMain {
            self.connected = false
            if let err { self.reportError(err.localizedDescription) }
        }
    }

    fileprivate func handleState(_ state: CocoaMQTTConnState) {
        onMain {
            self.connected = state == .connected
            if state == .connected { self.lastError = nil }
        }
    }
}

private final class DelegateBridge: NSObject, CocoaMQTTDelegate {
    weak var owner: MqttService?

    func mqtt(_ mqtt: CocoaMQTT, didConnectAck ack: CocoaMQTTConnAck) {
        owner?.handleConnectAck(ack, mqtt: mqtt)
    }

    func mqtt(_ mqtt: CocoaMQTT, didReceiveMessage message: CocoaMQTTMessage, id: UInt16) {
        owner?.handleMessage(message)
    }

    func mqttDidDisconnect(_ mqtt: CocoaMQTT, withError err: Error?) {
        owner?.handleDisconnect(err)
    }

    func mqtt(_ mqtt: CocoaMQTT, didStateChangeTo state: CocoaMQTTConnState) {
        owner?.handleState(state)
    }

    func mqtt(_ mqtt: CocoaMQTT, didSubscribeTopics success: NSDictionary, failed: [String]) {}
    func mqttDidPing(_ mqtt: CocoaMQTT) {}
    func mqttDidReceivePong(_ mqtt: CocoaMQTT) {}
    func mqtt(_ mqtt: CocoaMQTT, didPublishMessage message: CocoaMQTTMessage, id: UInt16) {}
    func mqtt(_ mqtt: CocoaMQTT, didPublishAck id: UInt16) {}
    func mqtt(_ mqtt: CocoaMQTT, didUnsubscribeTopics topics: [String]) {}
    func mqtt(_ mqtt: CocoaMQTT, didReceive trust: SecTrust, completionHandler: @escaping (Bool) -> Void) {
        completionHandler(true)
    }
}
