package ru.home.corridorpanel.model

/** Подписи топиков (отдельно от gen Topics.kt). См. shared/corridor-panel/topic_registry.md */
object TopicLabels {
    private val groupTitles = mapOf(
        "hall" to "Прихожая",
        "room" to "Зал",
        "kitchen" to "Кухня",
        "storage" to "Кладовка",
        "bath" to "Ванна",
        "toilet" to "Туалет",
        "master" to "Дом",
    )

    private val titleByTopic: Map<String, String> by lazy {
        buildMap {
            Topics.dimmers.forEach { d ->
                val g = groupTitles[d.group] ?: d.group
                put(d.channel, "$g · ${d.title}")
                put(d.power, "$g · ${d.title}")
            }
            Topics.allSwitches.forEach { s ->
                val g = groupTitles[s.group] ?: s.group
                put(s.topic, "$g · ${s.title}")
            }
            put(Topics.Led.strip, "Лента · RGB")
            put(Topics.Led.brightness, "Лента · яркость")
            put(Topics.Led.hue, "Лента · оттенок")
            put(Topics.Led.saturation, "Лента · насыщ.")
            put(Topics.Led.hueChanging, "Лента · цикл цвета")
            put(Topics.Led.white, "Лента · белый")
            put(Topics.Led.whiteBrightness, "Лента · белый яркость")
            put(Topics.Air.temperature, "Воздух · температура")
            put(Topics.Air.humidity, "Воздух · влажность")
            put(Topics.Air.co2, "Воздух · CO₂")
            put(Topics.Air.voc, "Воздух · VOC")
            put(Topics.Air.illuminance, "Воздух · освещённость")
            put(Topics.Air.sound, "Воздух · шум")
            put(Topics.Air.motion, "Воздух · движение")
            put(Topics.Air.maxMotion, "Воздух · max движение")
            put(Topics.Weather.temperature, "Улица · температура")
            put(Topics.Weather.humidity, "Улица · влажность")
            put(Topics.Weather.battery, "Улица · батарея")
            put(Topics.Weather.linkquality, "Улица · связь")
            Topics.climates.forEach { c ->
                put(c.active, "Кондей ${c.title} · вкл")
                put(c.mode, "Кондей ${c.title} · режим")
                put(c.target, "Кондей ${c.title} · цель")
                put(c.indoor, "Кондей ${c.title} · внутри")
                put(c.fan, "Кондей ${c.title} · вентилятор")
                put(c.swing, "Кондей ${c.title} · жалюзи")
                put(c.horizontal, "Кондей ${c.title} · гориз.")
                put(c.vertical, "Кондей ${c.title} · верт.")
                put(c.eco, "Кондей ${c.title} · eco")
                put(c.sleep, "Кондей ${c.title} · sleep")
            }
            Topics.floors.forEach { z ->
                put(z.relay, "Тёплый пол ${z.title} · реле")
                put(z.sensor, "Тёплый пол ${z.title} · датчик")
                put(z.setpoint, "Тёплый пол ${z.title} · цель")
                put(z.mode, "Тёплый пол ${z.title} · режим")
            }
        }
    }

    fun displayTitle(topic: String): String =
        titleByTopic[topic] ?: topic.substringAfterLast('/').ifEmpty { topic }
}
