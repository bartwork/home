import Foundation
import CocoaMQTT

/// MQTT-клиент к Wiren Board (как Android HomeMqttClient).
@Observable
final class MqttService {
    private(set) var connected = false
    private(set) var values: [String: String] = [:]
    private(set) var lastError: String?

    private var client: CocoaMQTT?
    private let bridge = DelegateBridge()

    init() {
        bridge.owner = self
    }

    func connect(host: String, port: Int) {
        disconnect()
        lastError = nil

        let cleanHost = host.trimmingCharacters(in: .whitespacesAndNewlines)
        let mqttPort = UInt16(clamping: max(1, port))
        let clientId = "ios-homepanel-\(UUID().uuidString.prefix(8))"
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
        guard let client, connected else {
            lastError = "нет MQTT"
            return
        }
        _ = client.publish(Topics.commandTopic(stateTopic), withString: payload, qos: .qos0, retained: false)
    }

    func isOn(_ topic: String) -> Bool {
        let v = values[topic]?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() ?? "0"
        return v != "0" && !v.isEmpty && v != "false" && v != "off"
    }

    func dimmerLevel(_ channel: String) -> Double {
        Double(values[channel] ?? "0") ?? 0
    }

    func toggleSwitch(_ topic: String) {
        publish(stateTopic: topic, payload: isOn(topic) ? "0" : "1")
    }

    func toggleDimmer(power: String, channel: String) {
        if isOn(power) {
            publish(stateTopic: power, payload: "0")
        } else {
            let level = max(dimmerLevel(channel), 50)
            publish(stateTopic: channel, payload: String(Int(level.rounded())))
            publish(stateTopic: power, payload: "1")
        }
    }

    func setDimmerLevel(channel: String, power: String, level: Double) {
        let clamped = Int(min(100, max(0, level)).rounded())
        publish(stateTopic: channel, payload: String(clamped))
        if clamped > 0, !isOn(power) {
            publish(stateTopic: power, payload: "1")
        } else if clamped == 0, isOn(power) {
            publish(stateTopic: power, payload: "0")
        }
    }

    // MARK: - Climate / LED state

    func climateState(_ device: Topics.ClimateDevice) -> ClimateState {
        ClimateState(
            on: values[device.active] == "1",
            target: intValue(device.target),
            indoor: doubleValue(device.indoor),
            mode: intValue(device.mode),
            fan: intValue(device.fan),
            swing: intValue(device.swing),
            ecoOn: values[device.eco] == "1",
            sleepOn: values[device.sleep] == "1"
        )
    }

    func ledRgbState() -> LedRgbState {
        LedRgbState(
            on: values[Topics.Led.strip] == "1",
            brightness: intValue(Topics.Led.brightness) ?? 0,
            hue: intValue(Topics.Led.hue) ?? 0,
            saturation: intValue(Topics.Led.saturation) ?? 100,
            hueChanging: values[Topics.Led.hueChanging] == "1"
        )
    }

    func ledWhiteState() -> LedWhiteState {
        LedWhiteState(
            on: values[Topics.Led.white] == "1",
            brightness: intValue(Topics.Led.whiteBrightness) ?? 0
        )
    }

    func setLedColor(hue: Int, saturation: Int, brightness: Int) {
        let bri = max(1, min(100, brightness == 0 ? 80 : brightness))
        if values[Topics.Led.hueChanging] == "1" {
            publish(stateTopic: Topics.Led.hueChanging, payload: "0")
        }
        publish(stateTopic: Topics.Led.strip, payload: "1")
        publish(stateTopic: Topics.Led.hue, payload: "\(min(360, max(0, hue)))")
        publish(stateTopic: Topics.Led.saturation, payload: "\(min(100, max(0, saturation)))")
        publish(stateTopic: Topics.Led.brightness, payload: "\(bri)")
    }

    func setLedBrightness(_ n: Int) {
        let next = min(100, max(0, n))
        publish(stateTopic: Topics.Led.brightness, payload: "\(next)")
        if next > 0 {
            if values[Topics.Led.hueChanging] == "1" {
                publish(stateTopic: Topics.Led.hueChanging, payload: "0")
            }
            if values[Topics.Led.strip] != "1" {
                publish(stateTopic: Topics.Led.strip, payload: "1")
            }
        } else if values[Topics.Led.strip] == "1" {
            publish(stateTopic: Topics.Led.strip, payload: "0")
        }
    }

    func setLedHue(_ n: Int) {
        setLedColor(
            hue: n,
            saturation: intValue(Topics.Led.saturation) ?? 100,
            brightness: max(intValue(Topics.Led.brightness) ?? 80, 1)
        )
    }

    func setLedSaturation(_ n: Int) {
        setLedColor(
            hue: intValue(Topics.Led.hue) ?? 0,
            saturation: n,
            brightness: max(intValue(Topics.Led.brightness) ?? 80, 1)
        )
    }

    func setWhiteBrightness(_ n: Int) {
        let next = min(100, max(0, n))
        publish(stateTopic: Topics.Led.whiteBrightness, payload: "\(next)")
        let on = values[Topics.Led.white] == "1"
        if next > 0, !on {
            publish(stateTopic: Topics.Led.white, payload: "1")
        } else if next == 0, on {
            publish(stateTopic: Topics.Led.white, payload: "0")
        }
    }

    func toggleWhiteChannel() {
        if values[Topics.Led.white] == "1" {
            publish(stateTopic: Topics.Led.white, payload: "0")
        } else {
            publish(stateTopic: Topics.Led.white, payload: "1")
            if (intValue(Topics.Led.whiteBrightness) ?? 0) <= 0 {
                publish(stateTopic: Topics.Led.whiteBrightness, payload: "100")
            }
        }
    }

    private func intValue(_ topic: String) -> Int? {
        guard let raw = values[topic]?.trimmingCharacters(in: .whitespacesAndNewlines),
              let d = Double(raw) else { return nil }
        return Int(d)
    }

    private func doubleValue(_ topic: String) -> Double? {
        guard let raw = values[topic]?.trimmingCharacters(in: .whitespacesAndNewlines) else { return nil }
        return Double(raw)
    }

    // MARK: - Bridge (CocoaMQTTDelegate не должен быть на @Observable классе)

    private func onMain(_ body: @escaping () -> Void) {
        if Thread.isMainThread {
            body()
        } else {
            DispatchQueue.main.async(execute: body)
        }
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
                self.lastError = "MQTT ack \(ack)"
            }
        }
    }

    fileprivate func handleMessage(_ message: CocoaMQTTMessage) {
        let payload = message.string ?? String(data: Data(message.payload), encoding: .utf8) ?? ""
        onMain {
            self.values[message.topic] = payload
        }
    }

    fileprivate func handleSubscribeFailed(_ failed: [String]) {
        guard !failed.isEmpty else { return }
        onMain {
            self.lastError = "subscribe: \(failed.joined(separator: ", "))"
        }
    }

    fileprivate func handleDisconnect(_ err: Error?) {
        onMain {
            self.connected = false
            if let err {
                self.lastError = err.localizedDescription
            }
        }
    }

    fileprivate func handleState(_ state: CocoaMQTTConnState) {
        onMain {
            self.connected = state == .connected
            if state == .connected {
                self.lastError = nil
            }
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

    func mqtt(_ mqtt: CocoaMQTT, didSubscribeTopics success: NSDictionary, failed: [String]) {
        owner?.handleSubscribeFailed(failed)
    }

    func mqttDidDisconnect(_ mqtt: CocoaMQTT, withError err: Error?) {
        owner?.handleDisconnect(err)
    }

    func mqtt(_ mqtt: CocoaMQTT, didStateChangeTo state: CocoaMQTTConnState) {
        owner?.handleState(state)
    }

    func mqttDidPing(_ mqtt: CocoaMQTT) {}
    func mqttDidReceivePong(_ mqtt: CocoaMQTT) {}
    func mqtt(_ mqtt: CocoaMQTT, didPublishMessage message: CocoaMQTTMessage, id: UInt16) {}
    func mqtt(_ mqtt: CocoaMQTT, didPublishAck id: UInt16) {}
    func mqtt(_ mqtt: CocoaMQTT, didUnsubscribeTopics topics: [String]) {}
    func mqtt(_ mqtt: CocoaMQTT, didReceive trust: SecTrust, completionHandler: @escaping (Bool) -> Void) {
        completionHandler(true)
    }
}
