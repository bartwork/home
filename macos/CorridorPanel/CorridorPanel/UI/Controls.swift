import SwiftUI

struct StatusPill: View {
    let online: Bool
    var warn: Bool = false
    let label: String

    var body: some View {
        let color: Color = online ? PanelTheme.accent : (warn ? PanelTheme.warning : PanelTheme.danger)
        HStack(spacing: 8) {
            Circle().fill(color).frame(width: 8, height: 8)
            Text(label)
                .font(.caption.weight(.medium))
                .foregroundStyle(color)
                .lineLimit(1)
        }
        .padding(.trailing, 8)
    }
}

struct ColumnCard<Content: View>: View {
    var title: String? = nil
    var weight: CGFloat = 1
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            if let title {
                Text(title)
                    .font(.title3.weight(.semibold))
            }
            content()
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        }
        .padding(12)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(
            RoundedRectangle(cornerRadius: PanelTheme.cardRadius, style: .continuous)
                .fill(PanelTheme.surface)
                .overlay {
                    RoundedRectangle(cornerRadius: PanelTheme.cardRadius, style: .continuous)
                        .strokeBorder(PanelTheme.hairline, lineWidth: 1)
                }
        )
        .layoutPriority(weight)
    }
}

struct PowerGlyph: View {
    let on: Bool
    var size: CGFloat = 34

    var body: some View {
        Image(systemName: "power")
            .font(.body.weight(.semibold))
            .foregroundStyle(on ? PanelTheme.accent : .secondary)
            .frame(width: size, height: size)
            .background(
                Circle().fill(on ? PanelTheme.accentSoft : Color.primary.opacity(0.06))
            )
    }
}

struct DimmerRow: View {
    @Bindable var mqtt: MqttService
    let dimmer: Topics.Dimmer

    private var level: Double { mqtt.dimmerLevel(dimmer.channel) }
    private var on: Bool {
        switch mqtt.values[dimmer.power] {
        case "1": return true
        case "0": return false
        default: return level > 0
        }
    }
    var body: some View {
        HStack(spacing: 10) {
            Button { mqtt.toggleDimmer(dimmer) } label: {
                PowerGlyph(on: on)
            }
            .buttonStyle(.plain)

            Text(dimmer.title)
                .font(.body.weight(.medium))
                .lineLimit(1)
                .frame(width: 72, alignment: .leading)

            Slider(
                value: Binding(
                    get: { level },
                    set: { mqtt.setDimmerLevel(dimmer, level: $0) }
                ),
                in: 0...100
            )
            .tint(PanelTheme.accent)

            Text("\(Int(level))")
                .font(.caption.monospacedDigit())
                .foregroundStyle(on ? PanelTheme.accent : .secondary)
                .frame(width: 28, alignment: .trailing)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background(rowBackground(on: on))
    }
}

struct SwitchRow: View {
    @Bindable var mqtt: MqttService
    let item: Topics.LightSwitch

    private var on: Bool { mqtt.isOn(item.topic) }

    var body: some View {
        Button { mqtt.toggleSwitch(item.topic) } label: {
            HStack(spacing: 10) {
                PowerGlyph(on: on)
                Text(item.title)
                    .font(.body.weight(.medium))
                    .foregroundStyle(.primary)
                Spacer()
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 10)
            .background(rowBackground(on: on))
        }
        .buttonStyle(.plain)
    }
}

private func rowBackground(on: Bool) -> some View {
    RoundedRectangle(cornerRadius: 12, style: .continuous)
        .fill(Color.primary.opacity(0.04))
        .overlay {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(
                    on ? PanelTheme.accent.opacity(0.35) : .clear,
                    lineWidth: 1
                )
        }
}
