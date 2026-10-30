import SwiftUI

/// Zigbee на наружном блоке кондея — погода на улице.
struct OutdoorWeatherRow: View {
    @Bindable var mqtt: MqttService

    private var temp: Double? {
        Double(mqtt.values[Topics.Weather.temperature] ?? "")
    }
    private var humidity: Double? {
        Double(mqtt.values[Topics.Weather.humidity] ?? "")
    }

    var body: some View {
        HStack(spacing: 10) {
            Text("Улица")
                .font(.caption.weight(.medium))
                .foregroundStyle(.secondary)

            Image(systemName: "thermometer.medium")
                .foregroundStyle(PanelTheme.accent)
            Text(temp.map { String(format: "%.1f°", $0) } ?? "—")
                .font(.title3.weight(.semibold).monospacedDigit())

            Image(systemName: "humidity.fill")
                .foregroundStyle(.secondary)
            Text(humidity.map { "\(Int($0.rounded()))%" } ?? "—")
                .font(.body.monospacedDigit())

            Spacer(minLength: 0)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(Color.primary.opacity(0.04))
                .overlay {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .strokeBorder(
                            temp != nil ? PanelTheme.accent.opacity(0.28) : .clear,
                            lineWidth: 1
                        )
                }
        )
    }
}
