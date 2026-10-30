import SwiftUI

/// Карниз: RGB + белый (wb-led_221) — как на квадратных планшетах.
struct LedStripBlock: View {
    @Bindable var mqtt: MqttService
    @State private var pickerOpen = false

    private var state: LedRgbState { mqtt.ledRgbState() }
    private var white: LedWhiteState { mqtt.ledWhiteState() }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Карниз")
                .font(.caption.weight(.medium))
                .foregroundStyle(.secondary)

            HStack(spacing: 8) {
                Button {
                    mqtt.publish(stateTopic: Topics.Led.strip, payload: state.on ? "0" : "1")
                } label: {
                    PowerGlyph(on: state.on)
                }
                .buttonStyle(.plain)

                Button {
                    mqtt.publish(
                        stateTopic: Topics.Led.hueChanging,
                        payload: state.hueChanging ? "0" : "1"
                    )
                    if !state.hueChanging {
                        mqtt.publish(stateTopic: Topics.Led.strip, payload: "1")
                    }
                } label: {
                    Image(systemName: "arrow.triangle.2.circlepath")
                        .font(.body.weight(.semibold))
                        .foregroundStyle(state.hueChanging ? PanelTheme.accent : .secondary)
                        .frame(width: 34, height: 34)
                        .background(
                            Circle().fill(
                                state.hueChanging
                                    ? PanelTheme.accentSoft
                                    : Color.primary.opacity(0.06)
                            )
                        )
                }
                .buttonStyle(.plain)

                Button { mqtt.toggleWhiteChannel() } label: {
                    Text("W")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(white.on ? PanelTheme.accent : .secondary)
                        .frame(width: 34, height: 34)
                        .background(
                            Circle().fill(
                                white.on ? PanelTheme.accentSoft : Color.primary.opacity(0.06)
                            )
                        )
                }
                .buttonStyle(.plain)

                Button { pickerOpen = true } label: {
                    colorPreview
                }
                .buttonStyle(.plain)
            }

            paletteRow

            LedSliderRow(title: "Яркость", value: state.brightness, max: 100) {
                mqtt.setLedBrightness($0)
            }
            LedSliderRow(title: "Оттенок", value: state.hue, max: 360) {
                mqtt.setLedHue($0)
            }
            LedSliderRow(title: "Насыщ.", value: state.saturation, max: 100) {
                mqtt.setLedSaturation($0)
            }
            LedSliderRow(title: "Белый", value: white.brightness, max: 100) {
                mqtt.setWhiteBrightness($0)
            }
        }
        .padding(10)
        .background(rowBg())
        .sheet(isPresented: $pickerOpen) {
            LedColorPickerSheet(
                hue: state.hue,
                saturation: state.saturation,
                brightness: max(state.brightness, 1)
            ) { h, s, v in
                mqtt.setLedColor(hue: h, saturation: s, brightness: v)
            }
        }
    }

    private var colorPreview: some View {
        RoundedRectangle(cornerRadius: 10, style: .continuous)
            .fill(Color.ledHSV(
                h: state.hue,
                s: state.saturation,
                v: state.on ? max(state.brightness, 18) : 12
            ))
            .frame(maxWidth: .infinity)
            .frame(height: 34)
            .overlay {
                Text("Цвет")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.white.opacity(0.95))
            }
            .overlay {
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .strokeBorder(.white.opacity(0.28), lineWidth: 1)
            }
    }

    private var paletteRow: some View {
        HStack(spacing: 8) {
            ForEach(LedPalette.defaults, id: \.self) { sw in
                let selected = state.on && state.hue == sw.hue && state.saturation == sw.saturation
                Button {
                    mqtt.setLedColor(hue: sw.hue, saturation: sw.saturation, brightness: sw.brightness)
                } label: {
                    Circle()
                        .fill(Color.ledHSV(h: sw.hue, s: sw.saturation, v: 90))
                        .frame(width: 28, height: 28)
                        .overlay {
                            Circle().strokeBorder(
                                selected ? Color.white : Color.primary.opacity(0.12),
                                lineWidth: selected ? 2 : 1
                            )
                        }
                }
                .buttonStyle(.plain)
            }
        }
        .frame(maxWidth: .infinity)
    }

    private func rowBg() -> some View {
        RoundedRectangle(cornerRadius: 12, style: .continuous)
            .fill(Color.primary.opacity(0.04))
    }
}

// MARK: - Models / MQTT helpers

struct LedRgbState {
    var on = false
    var brightness = 0
    var hue = 0
    var saturation = 100
    var hueChanging = false
}

struct LedWhiteState {
    var on = false
    var brightness = 0
}

struct LedSwatch: Hashable {
    let hue: Int
    let saturation: Int
    let brightness: Int
}

enum LedPalette {
    static let defaults: [LedSwatch] = [
        .init(hue: 28, saturation: 85, brightness: 100),
        .init(hue: 48, saturation: 70, brightness: 100),
        .init(hue: 0, saturation: 95, brightness: 100),
        .init(hue: 330, saturation: 75, brightness: 100),
        .init(hue: 280, saturation: 80, brightness: 100),
        .init(hue: 210, saturation: 85, brightness: 100),
        .init(hue: 175, saturation: 70, brightness: 100),
        .init(hue: 130, saturation: 70, brightness: 100),
    ]

    static func rgb(h: Int, s: Int, v: Int) -> (Double, Double, Double) {
        let hh = ((h % 360) + 360) % 360
        let ss = Double(min(100, max(0, s))) / 100
        let vv = Double(min(100, max(0, v))) / 100
        let c = vv * ss
        let x = c * (1 - abs((Double(hh) / 60).truncatingRemainder(dividingBy: 2) - 1))
        let m = vv - c
        let (r1, g1, b1): (Double, Double, Double)
        switch hh {
        case ..<60: (r1, g1, b1) = (c, x, 0)
        case ..<120: (r1, g1, b1) = (x, c, 0)
        case ..<180: (r1, g1, b1) = (0, c, x)
        case ..<240: (r1, g1, b1) = (0, x, c)
        case ..<300: (r1, g1, b1) = (x, 0, c)
        default: (r1, g1, b1) = (c, 0, x)
        }
        return (r1 + m, g1 + m, b1 + m)
    }
}

extension Color {
    static func ledHSV(h: Int, s: Int, v: Int) -> Color {
        let rgb = LedPalette.rgb(h: h, s: s, v: v)
        return Color(red: rgb.0, green: rgb.1, blue: rgb.2)
    }
}

extension MqttService {
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
        let bri = max(1, min(100, brightness))
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
}

private struct LedSliderRow: View {
    let title: String
    let value: Int
    let max: Int
    let onChange: (Int) -> Void

    @State private var local: Double = 0
    @State private var dragging = false

    var body: some View {
        HStack(spacing: 8) {
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(width: 56, alignment: .leading)
            Slider(
                value: Binding(
                    get: { dragging ? local : Double(value) },
                    set: {
                        dragging = true
                        local = $0
                    }
                ),
                in: 0...Double(max),
                onEditingChanged: { editing in
                    if !editing {
                        dragging = false
                        onChange(Int(local.rounded()))
                    }
                }
            )
            .tint(PanelTheme.accent)
            Text("\(dragging ? Int(local.rounded()) : value)")
                .font(.caption.monospacedDigit())
                .foregroundStyle(.secondary)
                .frame(width: 28, alignment: .trailing)
        }
        .onAppear { local = Double(value) }
        .onChange(of: value) { _, new in
            if !dragging { local = Double(new) }
        }
    }
}

/// Цветовой круг: hue по углу, saturation по радиусу.
private struct LedColorPickerSheet: View {
    let hue: Int
    let saturation: Int
    let brightness: Int
    let onChange: (Int, Int, Int) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var localHue = 0
    @State private var localSat = 100

    var body: some View {
        VStack(spacing: 16) {
            HStack {
                Text("Цвет ленты")
                    .font(.headline)
                Spacer()
                Button("Готово") { dismiss() }
                    .keyboardShortcut(.defaultAction)
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)

            colorWheel
                .frame(width: 280, height: 280)

            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(Color.ledHSV(h: localHue, s: localSat, v: brightness))
                .frame(height: 40)
                .overlay {
                    Text("H \(localHue) · S \(localSat)")
                        .font(.caption.monospacedDigit().weight(.medium))
                        .foregroundStyle(.white)
                }
                .padding(.horizontal, 24)

            Spacer(minLength: 8)
        }
        .frame(width: 360, height: 420)
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
                    .frame(width: 20, height: 20)
                    .shadow(radius: 2)
                    .overlay(Circle().strokeBorder(.black.opacity(0.25), lineWidth: 1))
                    .position(thumbPosition(center: center, radius: radius * 0.92))
            }
            .frame(width: side, height: side)
            .contentShape(Circle())
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
