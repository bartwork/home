import SwiftUI

struct ClimateColumn: View {
    @Bindable var mqtt: MqttService

    private var air: AirSnapshot { AirSnapshot(values: mqtt.values) }

    var body: some View {
        VStack(spacing: 10) {
            AirStrip(air: air)
            ForEach(Topics.climates) { unit in
                CompactClimateCard(mqtt: mqtt, unit: unit)
            }
        }
    }
}

struct AirSnapshot {
    let temp: Double?
    let humidity: Double?
    let co2: Int?
    let voc: Int?

    init(values: [String: String]) {
        temp = Double(values[Topics.Air.temperature] ?? "")
        humidity = Double(values[Topics.Air.humidity] ?? "")
        co2 = Double(values[Topics.Air.co2] ?? "").map { Int($0) }
        voc = Double(values[Topics.Air.voc] ?? "").map { Int($0) }
    }

    var co2Hint: String {
        guard let co2 else { return "" }
        if co2 < 800 { return "норма" }
        if co2 < 1200 { return "душновато" }
        return "проветри"
    }
}

struct AirStrip: View {
    let air: AirSnapshot

    var body: some View {
        HStack {
            metric(air.temp.map { String(format: "%.1f", $0) } ?? "—", "°C")
            Spacer()
            metric(air.humidity.map { String(format: "%.0f", $0) } ?? "—", "%")
            Spacer()
            VStack(spacing: 2) {
                Text(air.co2.map(String.init) ?? "—").font(.title3.weight(.semibold))
                Text("CO₂").font(.caption2).foregroundStyle(.secondary)
                if !air.co2Hint.isEmpty {
                    Text(air.co2Hint).font(.caption2).foregroundStyle(PanelTheme.accent)
                }
            }
            Spacer()
            metric(air.voc.map(String.init) ?? "—", "VOC")
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(Color.primary.opacity(0.04), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func metric(_ value: String, _ label: String) -> some View {
        VStack(spacing: 2) {
            Text(value).font(.title3.weight(.semibold))
            Text(label).font(.caption2).foregroundStyle(.secondary)
        }
    }
}

struct CompactClimateCard: View {
    @Bindable var mqtt: MqttService
    let unit: Topics.ClimateUnit

    private var on: Bool { mqtt.values[unit.active] == "1" }
    private var target: Int { Int(Double(mqtt.values[unit.target] ?? "") ?? 24) }
    private var indoor: Double? { Double(mqtt.values[unit.indoor] ?? "") }
    private var mode: Int? { Double(mqtt.values[unit.mode] ?? "").map { Int($0) } }
    private var fan: Int? { Double(mqtt.values[unit.fan] ?? "").map { Int($0) } }
    private var swing: Int? { Double(mqtt.values[unit.swing] ?? "").map { Int($0) } }

    private let modes: [(Int, String)] = [
        (1, "Нагрев"), (2, "Охл."), (3, "Авто"), (4, "Сушка"), (5, "Вент."),
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Button {
                    mqtt.publish(stateTopic: unit.active, payload: on ? "0" : "1")
                } label: {
                    PowerGlyph(on: on)
                }
                .buttonStyle(.plain)
                Text(unit.title).font(.headline)
                Spacer()
                Text(indoor.map { String(format: "%.0f°", $0) } ?? "—")
                    .foregroundStyle(on ? PanelTheme.accent : .secondary)
            }

            HStack {
                Button("-") { setTarget(target - 1) }.buttonStyle(.bordered)
                Text("\(target)°").font(.title2.monospacedDigit()).frame(maxWidth: .infinity)
                Button("+") { setTarget(target + 1) }.buttonStyle(.bordered)
            }

            HStack(spacing: 6) {
                ForEach(modes, id: \.0) { m in
                    Button(m.1) {
                        mqtt.publish(stateTopic: unit.mode, payload: "\(m.0)")
                    }
                    .buttonStyle(.bordered)
                    .tint(mode == m.0 ? PanelTheme.accent : nil)
                }
            }

            HStack(spacing: 6) {
                Button(fan == 1 ? "Тихий ✓" : "Тихий") {
                    mqtt.publish(stateTopic: unit.fan, payload: fan == 1 ? "0" : "1")
                }
                .buttonStyle(.bordered)
                Button(fanLabel) {
                    mqtt.publish(stateTopic: unit.fan, payload: "\(nextFan(fan))")
                }
                .buttonStyle(.bordered)
                Button(fan == 5 ? "Турбо ✓" : "Турбо") {
                    mqtt.publish(stateTopic: unit.fan, payload: fan == 5 ? "0" : "5")
                }
                .buttonStyle(.bordered)
                Button(swingLabel) {
                    mqtt.publish(stateTopic: unit.swing, payload: "\(((swing ?? 0) + 1) % 4)")
                }
                .buttonStyle(.bordered)
            }
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(Color.primary.opacity(0.04))
                .overlay {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .strokeBorder(on ? PanelTheme.accent.opacity(0.35) : .clear, lineWidth: 1)
                }
        )
    }

    private var fanLabel: String {
        switch fan {
        case 0: return "Авто"
        case 2: return "1"
        case 3: return "2"
        case 4: return "3"
        default: return "Вент."
        }
    }

    private var swingLabel: String {
        switch swing {
        case 1: return "Оба"
        case 2: return "Верт."
        case 3: return "Гор."
        default: return "Стоп"
        }
    }

    private func setTarget(_ n: Int) {
        mqtt.publish(stateTopic: unit.target, payload: "\(min(30, max(16, n)))")
    }

    private func nextFan(_ current: Int?) -> Int {
        let cycle = [0, 1, 2, 3, 4, 5]
        guard let current, let i = cycle.firstIndex(of: current) else { return 0 }
        return cycle[(i + 1) % cycle.count]
    }
}
