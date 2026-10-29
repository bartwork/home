import SwiftUI

/// Планшетный пульт: Свет · Лента · Климат (как kitchen/room panel).
struct LightsView: View {
    @Bindable var mqtt: MqttService
    @State private var tab = 0
    @Environment(\.horizontalSizeClass) private var sizeClass

    private var sideBySide: Bool { sizeClass == .regular }

    var body: some View {
        NavigationStack {
            VStack(spacing: 12) {
                header
                Group {
                    switch tab {
                    case 1: LedPanel(mqtt: mqtt, sideBySide: sideBySide)
                    case 2: ClimateHub(mqtt: mqtt, sideBySide: sideBySide)
                    default: LightsPanel(mqtt: mqtt)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .padding(.horizontal, sideBySide ? 28 : 16)
            .padding(.top, 8)
            .padding(.bottom, 12)
            .background(Color(.systemGroupedBackground))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .principal) {
                    Text("Дом").font(.headline)
                }
            }
        }
    }

    private var header: some View {
        VStack(spacing: 10) {
            SegmentBar(titles: ["Свет", "Лента", "Климат"], selection: $tab)
            StatusDot(
                kind: mqtt.connected ? .online : .offline,
                title: mqtt.connected ? "MQTT online" : "MQTT offline",
                detail: mqtt.connected ? nil : mqtt.lastError
            )
            .padding(.horizontal, 4)
        }
    }
}

// MARK: - Lights

private struct LightsPanel: View {
    @Bindable var mqtt: MqttService

    var body: some View {
        ScrollView {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 280), spacing: 12)], spacing: 12) {
                ForEach(Topics.sections) { section in
                    PanelCard {
                        VStack(alignment: .leading, spacing: 12) {
                            SectionHeaderLabel(title: section.title, systemImage: section.systemImage)
                            ForEach(section.dimmers) { DimmerRow(mqtt: mqtt, dimmer: $0) }
                            ForEach(section.switches) { SwitchRow(mqtt: mqtt, light: $0) }
                        }
                    }
                }
            }
            .padding(.bottom, 8)
        }
    }
}

private struct DimmerRow: View {
    @Bindable var mqtt: MqttService
    let dimmer: Topics.Dimmer

    private var on: Bool { mqtt.isOn(dimmer.power) }
    private var level: Double { mqtt.dimmerLevel(dimmer.channel) }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Label {
                    Text(dimmer.title).font(.body.weight(.medium))
                } icon: {
                    Image(systemName: on ? "lightbulb.fill" : "lightbulb")
                        .foregroundStyle(on ? HomeTheme.warning : .secondary)
                }
                Spacer()
                Toggle(
                    "",
                    isOn: Binding(
                        get: { on },
                        set: { _ in mqtt.toggleDimmer(power: dimmer.power, channel: dimmer.channel) }
                    )
                )
                .labelsHidden()
                .tint(HomeTheme.accent)
                .disabled(!mqtt.connected)
            }
            HStack(spacing: 12) {
                Slider(
                    value: Binding(
                        get: { level },
                        set: { mqtt.setDimmerLevel(channel: dimmer.channel, power: dimmer.power, level: $0) }
                    ),
                    in: 0...100,
                    step: 1
                )
                .tint(HomeTheme.accent)
                Text("\(Int(level))%")
                    .font(.caption.monospacedDigit().weight(.medium))
                    .foregroundStyle(.secondary)
                    .frame(width: 40, alignment: .trailing)
            }
            .disabled(!mqtt.connected)
            .opacity(mqtt.connected ? 1 : 0.45)
        }
        .padding(.vertical, 2)
    }
}

private struct SwitchRow: View {
    @Bindable var mqtt: MqttService
    let light: Topics.LightSwitch

    private var on: Bool { mqtt.isOn(light.topic) }

    var body: some View {
        HStack {
            Label {
                Text(light.title).font(.body.weight(.medium))
            } icon: {
                Image(systemName: on ? "power.circle.fill" : "power.circle")
                    .foregroundStyle(on ? HomeTheme.accent : .secondary)
            }
            Spacer()
            Toggle(
                "",
                isOn: Binding(
                    get: { on },
                    set: { mqtt.publish(stateTopic: light.topic, payload: $0 ? "1" : "0") }
                )
            )
            .labelsHidden()
            .tint(HomeTheme.accent)
            .disabled(!mqtt.connected)
        }
        .padding(.vertical, 2)
    }
}

// MARK: - LED

private struct LedPanel: View {
    @Bindable var mqtt: MqttService
    var sideBySide: Bool

    var body: some View {
        ScrollView {
            AdaptivePanelStack(sideBySide: sideBySide) {
                RgbStripCard(mqtt: mqtt)
                WhiteChannelCard(mqtt: mqtt)
            }
            .frame(maxWidth: 900)
            .frame(maxWidth: .infinity)
        }
        .disabled(!mqtt.connected)
        .opacity(mqtt.connected ? 1 : 0.55)
    }
}

private struct RgbStripCard: View {
    @Bindable var mqtt: MqttService
    @State private var pickerOpen = false

    private var state: LedRgbState { mqtt.ledRgbState() }

    var body: some View {
        PanelCard(padding: 16) {
            VStack(alignment: .leading, spacing: 14) {
                SectionHeaderLabel(title: "Лента карниза", systemImage: "light.strip.2.fill")

                HStack(spacing: 10) {
                    RoundIconButton(systemImage: "power", active: state.on, size: 46) {
                        mqtt.publish(stateTopic: Topics.Led.strip, payload: state.on ? "0" : "1")
                    }
                    RoundIconButton(
                        systemImage: "arrow.triangle.2.circlepath",
                        active: state.hueChanging,
                        size: 46
                    ) {
                        mqtt.publish(
                            stateTopic: Topics.Led.hueChanging,
                            payload: state.hueChanging ? "0" : "1"
                        )
                        if !state.hueChanging {
                            mqtt.publish(stateTopic: Topics.Led.strip, payload: "1")
                        }
                    }
                    Button { pickerOpen = true } label: { colorPreview }
                        .buttonStyle(.plain)
                        .accessibilityLabel("Выбрать цвет")
                }

                paletteRow

                VStack(spacing: 12) {
                    LedSliderRow(title: "Яркость", value: state.brightness, max: 100) {
                        mqtt.setLedBrightness($0)
                    }
                    LedSliderRow(title: "Оттенок", value: state.hue, max: 360) {
                        mqtt.setLedHue($0)
                    }
                    LedSliderRow(title: "Насыщ.", value: state.saturation, max: 100) {
                        mqtt.setLedSaturation($0)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity)
        .sheet(isPresented: $pickerOpen) {
            LedColorPickerSheet(
                hue: state.hue,
                saturation: state.saturation,
                brightness: max(state.brightness, 1)
            ) { h, s, v in
                mqtt.setLedColor(hue: h, saturation: s, brightness: v)
            }
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
        }
    }

    private var colorPreview: some View {
        RoundedRectangle(cornerRadius: 12, style: .continuous)
            .fill(Color.ledHSV(
                h: state.hue,
                s: state.saturation,
                v: state.on ? max(state.brightness, 18) : 12
            ))
            .frame(maxWidth: .infinity)
            .frame(height: 46)
            .overlay {
                Label("Цвет", systemImage: "paintpalette.fill")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.white.opacity(0.95))
            }
            .overlay {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(.white.opacity(0.28), lineWidth: 1)
            }
    }

    private var paletteRow: some View {
        HStack(spacing: 10) {
            ForEach(LedPalette.defaults, id: \.self) { sw in
                let selected = state.on && state.hue == sw.hue && state.saturation == sw.saturation
                Button {
                    mqtt.setLedColor(hue: sw.hue, saturation: sw.saturation, brightness: sw.brightness)
                } label: {
                    Circle()
                        .fill(Color.ledHSV(h: sw.hue, s: sw.saturation, v: 90))
                        .frame(width: 34, height: 34)
                        .overlay {
                            Circle().strokeBorder(
                                selected ? Color.white : Color.primary.opacity(0.12),
                                lineWidth: selected ? 2.5 : 1
                            )
                        }
                }
                .buttonStyle(.plain)
            }
        }
        .frame(maxWidth: .infinity)
    }
}

private struct WhiteChannelCard: View {
    @Bindable var mqtt: MqttService

    private var state: LedWhiteState { mqtt.ledWhiteState() }

    var body: some View {
        PanelCard(padding: 16) {
            VStack(alignment: .leading, spacing: 14) {
                SectionHeaderLabel(title: "Белый карниз", systemImage: "sun.max.fill")

                HStack(spacing: 12) {
                    RoundIconButton(systemImage: "power", active: state.on, size: 46) {
                        mqtt.toggleWhiteChannel()
                    }
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Channel 4").font(.subheadline.weight(.medium))
                        Text("отдельный белый канал WB-LED")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                    Spacer(minLength: 0)
                }

                LedSliderRow(title: "Яркость", value: state.brightness, max: 100) {
                    mqtt.setWhiteBrightness($0)
                }
            }
        }
        .frame(maxWidth: .infinity)
    }
}

/// Цветовой круг: hue по углу, saturation по радиусу (жест за кругом — clamp).
private struct LedColorPickerSheet: View {
    let hue: Int
    let saturation: Int
    let brightness: Int
    let onChange: (Int, Int, Int) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var localHue = 0
    @State private var localSat = 100

    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                colorWheel
                    .frame(maxWidth: 320)
                    .aspectRatio(1, contentMode: .fit)
                    .padding(.horizontal, 24)

                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(Color.ledHSV(h: localHue, s: localSat, v: brightness))
                    .frame(height: 44)
                    .padding(.horizontal, 28)
                    .overlay {
                        Text("H \(localHue) · S \(localSat)")
                            .font(.caption.monospacedDigit().weight(.medium))
                            .foregroundStyle(.white)
                    }

                Spacer(minLength: 0)
            }
            .padding(.top, 12)
            .navigationTitle("Цвет ленты")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Готово") { dismiss() }
                }
            }
        }
        .onAppear {
            localHue = hue
            localSat = saturation
        }
    }

    private var colorWheel: some View {
        GeometryReader { geo in
            let side = min(geo.size.width, geo.size.height)
            let center = CGPoint(x: side / 2, y: side / 2)
            let radius = side / 2

            ZStack {
                AngularGradient(
                    colors: [
                        .red, .yellow, .green, .cyan, .blue,
                        Color(red: 1, green: 0, blue: 1), .red,
                    ],
                    center: .center
                )
                .mask(Circle())

                RadialGradient(
                    colors: [.white, .white.opacity(0)],
                    center: .center,
                    startRadius: 0,
                    endRadius: radius
                )
                .mask(Circle())

                Circle()
                    .strokeBorder(.white.opacity(0.35), lineWidth: 2)
                    .frame(width: side, height: side)

                Circle()
                    .fill(.white)
                    .frame(width: 22, height: 22)
                    .shadow(radius: 2)
                    .overlay(Circle().strokeBorder(.black.opacity(0.25), lineWidth: 1))
                    .position(thumbPosition(center: center, radius: radius * 0.92))
            }
            .frame(width: side, height: side)
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { update(from: $0.location, center: center, radius: radius) }
            )
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    private func thumbPosition(center: CGPoint, radius: CGFloat) -> CGPoint {
        let angle = Double(localHue) * .pi / 180
        let r = radius * CGFloat(localSat) / 100
        return CGPoint(
            x: center.x + CGFloat(cos(angle)) * r,
            y: center.y + CGFloat(sin(angle)) * r
        )
    }

    private func update(from point: CGPoint, center: CGPoint, radius: CGFloat) {
        let dx = point.x - center.x
        let dy = point.y - center.y
        let dist = hypot(dx, dy)
        guard radius > 0 else { return }
        var deg = atan2(dy, dx) * 180 / .pi
        if deg < 0 { deg += 360 }
        let sat = Int(min(100, max(0, (min(dist, radius) / radius) * 100)).rounded())
        localHue = Int(deg.rounded())
        localSat = sat
        onChange(localHue, localSat, brightness)
    }
}

private struct LedSliderRow: View {
    let title: String
    let value: Int
    let max: Int
    let onChange: (Int) -> Void

    @State private var local: Double = 0
    @State private var dragging = false

    var body: some View {
        HStack(spacing: 10) {
            Text(title)
                .font(.caption.weight(.medium))
                .foregroundStyle(.secondary)
                .frame(width: 64, alignment: .leading)
            Slider(
                value: $local,
                in: 0...Double(max),
                step: 1,
                onEditingChanged: { editing in
                    dragging = editing
                    if !editing { onChange(Int(local.rounded())) }
                }
            )
            .tint(HomeTheme.accent)
            Text("\(Int(local))")
                .font(.caption.monospacedDigit().weight(.medium))
                .frame(width: 36, alignment: .trailing)
        }
        .onAppear { local = Double(value) }
        .onChange(of: value) { _, new in
            if !dragging { local = Double(new) }
        }
    }
}

// MARK: - Climate

private struct ClimateHub: View {
    @Bindable var mqtt: MqttService
    var sideBySide: Bool

    var body: some View {
        Group {
            if sideBySide {
                AdaptivePanelStack(sideBySide: true) {
                    ForEach(Topics.climates) { ClimatePanel(mqtt: mqtt, device: $0) }
                }
            } else {
                ScrollView {
                    AdaptivePanelStack(sideBySide: false) {
                        ForEach(Topics.climates) { ClimatePanel(mqtt: mqtt, device: $0) }
                    }
                }
            }
        }
    }
}

private struct ClimatePanel: View {
    @Bindable var mqtt: MqttService
    let device: Topics.ClimateDevice

    private var state: ClimateState { mqtt.climateState(device) }

    var body: some View {
        PanelCard(padding: 16) {
            VStack(spacing: 16) {
                header
                temperatureRow
                modeRow
                chipRows
            }
        }
        .frame(maxWidth: .infinity)
        .disabled(!mqtt.connected)
        .opacity(mqtt.connected ? 1 : 0.55)
    }

    private var header: some View {
        HStack {
            RoundIconButton(systemImage: "power", active: state.on, size: 48) {
                mqtt.publish(stateTopic: device.active, payload: state.on ? "0" : "1")
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(device.title).font(.headline)
                Text("кондиционер").font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                Text("в комнате").font(.caption).foregroundStyle(.secondary)
                Text(indoorLabel + "°")
                    .font(.title2.weight(.semibold))
                    .foregroundStyle(state.on ? HomeTheme.accent : .primary)
            }
        }
    }

    private var temperatureRow: some View {
        HStack {
            RoundIconButton(systemImage: "minus", size: 52) {
                setTarget((state.target ?? 24) - 1)
            }
            Spacer()
            Text((state.target.map(String.init) ?? "—") + "°")
                .font(.system(size: 56, weight: .light, design: .rounded))
                .foregroundStyle(state.on ? HomeTheme.accent : .primary)
            Spacer()
            RoundIconButton(systemImage: "plus", size: 52) {
                setTarget((state.target ?? 24) + 1)
            }
        }
    }

    private var modeRow: some View {
        HStack(spacing: 6) {
            ForEach(ClimateModels.modes, id: \.value) { m in
                let selected = state.mode == m.value
                Button {
                    mqtt.publish(stateTopic: device.mode, payload: "\(m.value)")
                } label: {
                    Image(systemName: modeIcon(m.value))
                        .font(.body.weight(.semibold))
                        .foregroundStyle(selected ? HomeTheme.accent : .secondary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .fill(selected ? HomeTheme.accentSoft : Color.primary.opacity(0.06))
                        )
                        .overlay {
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .strokeBorder(selected ? HomeTheme.accent.opacity(0.4) : .clear, lineWidth: 1)
                        }
                }
                .buttonStyle(.plain)
                .accessibilityLabel(m.label)
            }
        }
    }

    private var chipRows: some View {
        VStack(spacing: 8) {
            HStack(spacing: 6) {
                PanelChip(title: "Тихий", active: state.quietOn) {
                    mqtt.publish(
                        stateTopic: device.fan,
                        payload: "\(ClimateModels.toggleFanPreset(current: state.fan, preset: ClimateModels.fanQuiet))"
                    )
                }
                PanelChip(title: "Турбо", active: state.turboOn) {
                    mqtt.publish(
                        stateTopic: device.fan,
                        payload: "\(ClimateModels.toggleFanPreset(current: state.fan, preset: ClimateModels.fanTurbo))"
                    )
                }
                PanelChip(
                    title: ClimateModels.label(in: ClimateModels.fans, state.fan) ?? "Вент.",
                    active: (state.fan ?? 0) > 0 && !state.quietOn && !state.turboOn
                ) {
                    mqtt.publish(
                        stateTopic: device.fan,
                        payload: "\(ClimateModels.nextInCycle(ClimateModels.fans, current: state.fan))"
                    )
                }
            }
            HStack(spacing: 6) {
                PanelChip(
                    title: ClimateModels.label(in: ClimateModels.swings, state.swing) ?? "Качание",
                    active: (state.swing ?? 0) > 0,
                    compact: true
                ) {
                    mqtt.publish(
                        stateTopic: device.swing,
                        payload: "\(ClimateModels.nextInCycle(ClimateModels.swings, current: state.swing))"
                    )
                }
                PanelChip(title: "Eco", active: state.ecoOn) {
                    mqtt.publish(stateTopic: device.eco, payload: state.ecoOn ? "0" : "1")
                }
                PanelChip(title: "Сон", active: state.sleepOn) {
                    mqtt.publish(stateTopic: device.sleep, payload: state.sleepOn ? "0" : "1")
                }
            }
        }
    }

    private var indoorLabel: String {
        guard let indoor = state.indoor else { return "—" }
        if indoor.rounded() == indoor { return String(Int(indoor)) }
        return String(format: "%.1f", indoor)
    }

    private func setTarget(_ n: Int) {
        mqtt.publish(stateTopic: device.target, payload: "\(ClimateModels.clampTarget(n))")
    }

    private func modeIcon(_ value: Int) -> String {
        switch value {
        case 1: return "flame"
        case 2: return "snowflake"
        case 3: return "arrow.triangle.2.circlepath"
        case 4: return "drop"
        case 5: return "wind"
        default: return "circle"
        }
    }
}
