import SwiftUI

struct FloorColumn: View {
    @Bindable var mqtt: MqttService
    @State private var selectedId = Topics.floors.first?.id ?? ""

    private var zone: Topics.FloorZone {
        Topics.floors.first(where: { $0.id == selectedId })
            ?? Topics.floors.first
            ?? Topics.floor_hall
    }

    private var state: FloorState {
        mqtt.values.floorState(zone)
    }

    var body: some View {
        VStack(spacing: 10) {
            Picker("Зона", selection: $selectedId) {
                ForEach(Topics.floors) { z in
                    Text(z.title).tag(z.id)
                }
            }
            .pickerStyle(.segmented)

            HStack {
                Button {
                    let next: FloorMode = state.mode == .off ? .auto : .off
                    mqtt.publish(stateTopic: zone.mode, payload: next.mqtt)
                } label: {
                    Image(systemName: "power")
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(state.heating ? PanelTheme.accent : .secondary)
                        .frame(width: 40, height: 40)
                        .background(
                            Circle().fill(state.heating ? PanelTheme.accentSoft : Color.primary.opacity(0.06))
                        )
                }
                .buttonStyle(.plain)
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("температура пола")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text("\(FloorModels.formatTemp(state.temp))°")
                        .font(.title2.weight(.semibold))
                        .foregroundStyle(state.heating ? PanelTheme.accent : .primary)
                }
            }

            HStack {
                Button {
                    mqtt.publish(
                        stateTopic: zone.setpoint,
                        payload: String(FloorModels.clampSetpoint(state.setpoint - 0.5))
                    )
                } label: {
                    Image(systemName: "minus")
                        .frame(width: 44, height: 44)
                        .background(Circle().fill(Color.primary.opacity(0.06)))
                }
                .buttonStyle(.plain)

                VStack(spacing: 2) {
                    Text("уставка")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text("\(FloorModels.formatTemp(state.setpoint))°")
                        .font(.system(size: 42, weight: .semibold, design: .rounded))
                        .foregroundStyle(
                            state.mode == .auto || state.heating ? PanelTheme.accent : .primary
                        )
                }
                .frame(maxWidth: .infinity)

                Button {
                    mqtt.publish(
                        stateTopic: zone.setpoint,
                        payload: String(FloorModels.clampSetpoint(state.setpoint + 0.5))
                    )
                } label: {
                    Image(systemName: "plus")
                        .frame(width: 44, height: 44)
                        .background(Circle().fill(Color.primary.opacity(0.06)))
                }
                .buttonStyle(.plain)
            }

            HStack(spacing: 6) {
                ForEach(FloorMode.allCases) { m in
                    let selected = state.mode == m
                    Button {
                        mqtt.publish(stateTopic: zone.mode, payload: m.mqtt)
                    } label: {
                        Text(m.label)
                            .font(.caption.weight(.semibold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(
                                RoundedRectangle(cornerRadius: 10, style: .continuous)
                                    .fill(selected ? PanelTheme.accentSoft : Color.primary.opacity(0.05))
                            )
                            .overlay {
                                RoundedRectangle(cornerRadius: 10, style: .continuous)
                                    .strokeBorder(selected ? PanelTheme.accent.opacity(0.5) : .clear, lineWidth: 1)
                            }
                            .foregroundStyle(selected ? PanelTheme.accent : .secondary)
                    }
                    .buttonStyle(.plain)
                }
            }

            Spacer(minLength: 0)
        }
    }
}
