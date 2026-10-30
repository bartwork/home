import Foundation

enum FloorMode: String, CaseIterable, Identifiable {
    case off, heat, auto

    var id: String { rawValue }

    var mqtt: String { rawValue }

    var label: String {
        switch self {
        case .off: return "Выкл"
        case .heat: return "Нагрев"
        case .auto: return "Авто"
        }
    }

    static func parse(_ raw: String?) -> FloorMode {
        switch (raw ?? "").trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case "off": return .off
        case "heat": return .heat
        default: return .auto
        }
    }
}

enum FloorModels {
    static let setpointMin = 15.0
    static let setpointMax = 35.0
    static let setpointDefault = 26.0

    static func clampSetpoint(_ n: Double) -> Double {
        let stepped = (n * 2).rounded() / 2
        return min(setpointMax, max(setpointMin, stepped))
    }

    static func formatTemp(_ n: Double?) -> String {
        guard let n else { return "—" }
        if n.rounded() == n { return String(Int(n)) }
        return String(format: "%.1f", n)
    }
}

struct FloorState {
    var on = false
    var temp: Double?
    var setpoint = FloorModels.setpointDefault
    var mode: FloorMode = .auto

    var heating: Bool { mode != .off && on }
}

extension Dictionary where Key == String, Value == String {
    func floorState(_ zone: Topics.FloorZone) -> FloorState {
        let sp = Double((self[zone.setpoint] ?? "").trimmingCharacters(in: .whitespacesAndNewlines))
        let temp = Double((self[zone.sensor] ?? "").trimmingCharacters(in: .whitespacesAndNewlines))
        return FloorState(
            on: self[zone.relay] == "1",
            temp: temp,
            setpoint: sp ?? FloorModels.setpointDefault,
            mode: FloorMode.parse(self[zone.mode])
        )
    }
}
