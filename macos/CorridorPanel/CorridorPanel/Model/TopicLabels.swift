import Foundation

/// Подписи топиков (не в gen Topics.swift). См. shared/corridor-panel/topic_registry.md
enum TopicLabels {
    private static let groupTitles: [String: String] = [
        "hall": "Прихожая",
        "room": "Зал",
        "kitchen": "Кухня",
        "storage": "Кладовка",
        "bath": "Ванна",
        "toilet": "Туалет",
        "master": "Дом",
    ]

    private static let titleByTopic: [String: String] = {
        var map: [String: String] = [:]
        for d in Topics.dimmers {
            let g = groupTitles[d.group] ?? d.group
            map[d.channel] = "\(g) · \(d.title)"
            map[d.power] = "\(g) · \(d.title)"
        }
        for s in Topics.allSwitches {
            let g = groupTitles[s.group] ?? s.group
            map[s.topic] = "\(g) · \(s.title)"
        }
        map[Topics.Led.strip] = "Лента · RGB"
        map[Topics.Led.brightness] = "Лента · яркость"
        map[Topics.Led.hue] = "Лента · оттенок"
        map[Topics.Led.saturation] = "Лента · насыщ."
        map[Topics.Led.hueChanging] = "Лента · цикл цвета"
        map[Topics.Led.white] = "Лента · белый"
        map[Topics.Led.whiteBrightness] = "Лента · белый яркость"
        map[Topics.Air.temperature] = "Воздух · температура"
        map[Topics.Air.humidity] = "Воздух · влажность"
        map[Topics.Air.co2] = "Воздух · CO₂"
        map[Topics.Air.voc] = "Воздух · VOC"
        map[Topics.Air.illuminance] = "Воздух · освещённость"
        map[Topics.Air.sound] = "Воздух · шум"
        map[Topics.Air.motion] = "Воздух · движение"
        map[Topics.Air.maxMotion] = "Воздух · max движение"
        map[Topics.Weather.temperature] = "Улица · температура"
        map[Topics.Weather.humidity] = "Улица · влажность"
        map[Topics.Weather.battery] = "Улица · батарея"
        map[Topics.Weather.linkquality] = "Улица · связь"
        for c in Topics.climates {
            map[c.active] = "Кондей \(c.title) · вкл"
            map[c.mode] = "Кондей \(c.title) · режим"
            map[c.target] = "Кондей \(c.title) · цель"
            map[c.indoor] = "Кондей \(c.title) · внутри"
            map[c.fan] = "Кондей \(c.title) · вентилятор"
            map[c.swing] = "Кондей \(c.title) · жалюзи"
            map[c.horizontal] = "Кондей \(c.title) · гориз."
            map[c.vertical] = "Кондей \(c.title) · верт."
            map[c.eco] = "Кондей \(c.title) · eco"
            map[c.sleep] = "Кондей \(c.title) · sleep"
        }
        for z in Topics.floors {
            map[z.relay] = "Тёплый пол \(z.title) · реле"
            map[z.sensor] = "Тёплый пол \(z.title) · датчик"
            map[z.setpoint] = "Тёплый пол \(z.title) · цель"
            map[z.mode] = "Тёплый пол \(z.title) · режим"
        }
        return map
    }()

    static func displayTitle(for topic: String) -> String {
        if let t = titleByTopic[topic] { return t }
        if let last = topic.split(separator: "/").last { return String(last) }
        return topic
    }
}
