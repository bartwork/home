import Foundation

/// Каталог MQTT — синхронно с Android kitchen/room panel.
enum Topics {
    struct Dimmer: Identifiable, Hashable {
        let id: String
        let title: String
        let channel: String
        let power: String
    }

    /// Реле вкл/выкл (не SwiftUI.Switch).
    struct LightSwitch: Identifiable, Hashable {
        let id: String
        let title: String
        let topic: String
    }

    struct Section: Identifiable {
        let id: String
        let title: String
        let systemImage: String
        let dimmers: [Dimmer]
        let switches: [LightSwitch]
    }

    struct ClimateDevice: Identifiable, Hashable {
        let id: String
        let title: String
        let active: String
        let mode: String
        let target: String
        let indoor: String
        let fan: String
        let swing: String
        let eco: String
        let sleep: String

        var all: [String] {
            [active, mode, target, indoor, fan, swing, eco, sleep]
        }

        /// ONOKOM-AIR-HR-1-MB-B_{suffix}
        static func onokom(id: String, title: String, suffix: String) -> ClimateDevice {
            let base = "/devices/ONOKOM-AIR-HR-1-MB-B_\(suffix)/controls"
            return ClimateDevice(
                id: id,
                title: title,
                active: "\(base)/Active",
                mode: "\(base)/Mode",
                target: "\(base)/Target temperature",
                indoor: "\(base)/Indoor air temperature",
                fan: "\(base)/Smart fan speed",
                swing: "\(base)/Vanes swing",
                eco: "\(base)/Eco mode",
                sleep: "\(base)/Sleep mode"
            )
        }
    }

    /// WB-LED карниз: RGB-лента и отдельно белый Channel 4.
    enum Led {
        static let strip = "/devices/wb-led_221/controls/RGB Strip"
        static let brightness = "/devices/wb-led_221/controls/RGB Strip Brightness"
        static let hue = "/devices/wb-led_221/controls/RGB Strip Hue"
        static let saturation = "/devices/wb-led_221/controls/RGB Strip Saturation"
        static let hueChanging = "/devices/wb-led_221/controls/Hue Changing"
        /// Белый канал (4-й) — отдельное устройство, не часть RGB.
        static let white = "/devices/wb-led_221/controls/Channel 4"
        static let whiteBrightness = "/devices/wb-led_221/controls/Channel 4 Brightness"

        static var all: [String] {
            [strip, brightness, hue, saturation, hueChanging, white, whiteBrightness]
        }
    }

    // MARK: - Кухня

    static let kitchenWindow = Dimmer(
        id: "kitchen_window",
        title: "У окна",
        channel: "/devices/wb-mdm3_218/controls/Channel 1",
        power: "/devices/wb-mdm3_218/controls/K1"
    )
    static let kitchenLamp = Dimmer(
        id: "kitchen_lamp",
        title: "Лампа",
        channel: "/devices/wb-mdm3_218/controls/Channel 2",
        power: "/devices/wb-mdm3_218/controls/K2"
    )
    static let kitchenDoor = Dimmer(
        id: "kitchen_door",
        title: "У двери",
        channel: "/devices/wb-mdm3_218/controls/Channel 3",
        power: "/devices/wb-mdm3_218/controls/K3"
    )
    static let kitchenBra = LightSwitch(
        id: "kitchen_bra",
        title: "Бра",
        topic: "/devices/wb-mr6cv3_239/controls/K1"
    )
    static let kitchenUnder = LightSwitch(
        id: "kitchen_under",
        title: "Подсветка",
        topic: "/devices/wb-mr6cv3_239/controls/K2"
    )
    static let kitchenHood = LightSwitch(
        id: "kitchen_hood",
        title: "Вытяжка",
        topic: "/devices/wb-mr6cv3_239/controls/K3"
    )

    // MARK: - Комната

    static let roomMain = Dimmer(
        id: "room_main",
        title: "Свет",
        channel: "/devices/wb-mdm3_75/controls/Channel 1",
        power: "/devices/wb-mdm3_75/controls/K1"
    )
    static let roomExtra = Dimmer(
        id: "room_extra",
        title: "Доп. свет",
        channel: "/devices/wb-mdm3_75/controls/Channel 2",
        power: "/devices/wb-mdm3_75/controls/K2"
    )
    static let loggia = Dimmer(
        id: "loggia",
        title: "Лоджия",
        channel: "/devices/wb-mdm3_75/controls/Channel 3",
        power: "/devices/wb-mdm3_75/controls/K3"
    )
    static let braKladovka = Dimmer(
        id: "bra_kladovka",
        title: "Бра левое",
        channel: "/devices/wb-mdm3_76/controls/Channel 2",
        power: "/devices/wb-mdm3_76/controls/K2"
    )
    static let braLoggia = Dimmer(
        id: "bra_loggia",
        title: "Бра правое",
        channel: "/devices/wb-mdm3_76/controls/Channel 3",
        power: "/devices/wb-mdm3_76/controls/K3"
    )
    /// Кладовка: лента (230В) и обычный свет — только как реле.
    static let kladovkaStrip = LightSwitch(
        id: "kladovka_strip",
        title: "Лента",
        topic: "/devices/wb-mr6cv3_239/controls/K4"
    )
    static let kladovkaLight = LightSwitch(
        id: "kladovka_light",
        title: "Свет",
        topic: "/devices/wb-mr6cv3_239/controls/K5"
    )

    static let sections: [Section] = [
        Section(
            id: "kitchen",
            title: "Кухня",
            systemImage: "fork.knife",
            dimmers: [kitchenWindow, kitchenLamp, kitchenDoor],
            switches: [kitchenBra, kitchenUnder, kitchenHood]
        ),
        Section(
            id: "room",
            title: "Комната",
            systemImage: "sofa.fill",
            dimmers: [roomMain, roomExtra, loggia, braKladovka, braLoggia],
            switches: []
        ),
        Section(
            id: "kladovka",
            title: "Кладовка",
            systemImage: "archivebox.fill",
            dimmers: [],
            switches: [kladovkaStrip, kladovkaLight]
        ),
    ]

    // MARK: - Климат (ONOKOM)

    static let climateKitchen = ClimateDevice.onokom(id: "kitchen_ac", title: "Кухня", suffix: "1")
    static let climateRoom = ClimateDevice.onokom(id: "room_ac", title: "Комната", suffix: "2")
    static let climates: [ClimateDevice] = [climateKitchen, climateRoom]

    static func allStateTopics() -> [String] {
        var topics: [String] = []
        for section in sections {
            for d in section.dimmers {
                topics.append(d.channel)
                topics.append(d.power)
            }
            topics.append(contentsOf: section.switches.map(\.topic))
        }
        topics.append(contentsOf: Led.all)
        for ac in climates {
            topics.append(contentsOf: ac.all)
        }
        return topics
    }

    /// Wiren Board: команда пишется в …/on.
    static func commandTopic(_ stateTopic: String) -> String {
        let trimmed = stateTopic.hasSuffix("/") ? String(stateTopic.dropLast()) : stateTopic
        return trimmed + "/on"
    }
}

// MARK: - Climate models

struct ClimateLabeledValue: Hashable {
    let value: Int
    let label: String
}

enum ClimateModels {
    static let targetMin = 16
    static let targetMax = 30
    static let fanQuiet = 1
    static let fanTurbo = 5

    static let modes: [ClimateLabeledValue] = [
        .init(value: 1, label: "Нагрев"),
        .init(value: 2, label: "Охлаждение"),
        .init(value: 3, label: "Авто"),
        .init(value: 4, label: "Сушка"),
        .init(value: 5, label: "Вентиляция"),
    ]

    static let fans: [ClimateLabeledValue] = [
        .init(value: 0, label: "Авто"),
        .init(value: 1, label: "Тихий"),
        .init(value: 2, label: "1"),
        .init(value: 3, label: "2"),
        .init(value: 4, label: "3"),
        .init(value: 5, label: "Турбо"),
    ]

    static let swings: [ClimateLabeledValue] = [
        .init(value: 0, label: "Качание стоп"),
        .init(value: 1, label: "Качание оба"),
        .init(value: 2, label: "Качание верт."),
        .init(value: 3, label: "Качание гор."),
    ]

    static func clampTarget(_ n: Int) -> Int {
        min(targetMax, max(targetMin, n))
    }

    static func nextInCycle(_ list: [ClimateLabeledValue], current: Int?) -> Int {
        guard !list.isEmpty else { return 0 }
        guard let current, let i = list.firstIndex(where: { $0.value == current }) else {
            return list[0].value
        }
        return list[(i + 1) % list.count].value
    }

    static func toggleFanPreset(current: Int?, preset: Int) -> Int {
        current == preset ? 0 : preset
    }

    static func label(in list: [ClimateLabeledValue], _ v: Int?) -> String? {
        guard let v else { return nil }
        return list.first(where: { $0.value == v })?.label
    }
}

struct ClimateState {
    var on = false
    var target: Int?
    var indoor: Double?
    var mode: Int?
    var fan: Int?
    var swing: Int?
    var ecoOn = false
    var sleepOn = false

    var quietOn: Bool { fan == ClimateModels.fanQuiet }
    var turboOn: Bool { fan == ClimateModels.fanTurbo }
}

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

    /// HSV (H 0–360, S/V 0–100) → RGB 0…1.
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
