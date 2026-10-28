package ru.home.roompanel.model

/** Каталог топиков комнаты — синхронно с docs/mqtt_topics.md. */
object Topics {
    const val DEFAULT_BROKER_HOST = "192.168.1.1"
    const val DEFAULT_BROKER_PORT = 1883

    data class Dimmer(
        val id: String,
        val title: String,
        val channel: String,
        val power: String,
    )

    data class Switch(
        val id: String,
        val title: String,
        val topic: String,
    )

    val roomMain = Dimmer(
        id = "room_main",
        title = "Свет",
        channel = "/devices/wb-mdm3_75/controls/Channel 1",
        power = "/devices/wb-mdm3_75/controls/K1",
    )
    val roomExtra = Dimmer(
        id = "room_extra",
        title = "Доп. свет",
        channel = "/devices/wb-mdm3_75/controls/Channel 2",
        power = "/devices/wb-mdm3_75/controls/K2",
    )
    val loggia = Dimmer(
        id = "loggia",
        title = "Лоджия",
        channel = "/devices/wb-mdm3_75/controls/Channel 3",
        power = "/devices/wb-mdm3_75/controls/K3",
    )
    val braKladovka = Dimmer(
        id = "bra_kladovka",
        title = "Бра левое",
        channel = "/devices/wb-mdm3_76/controls/Channel 2",
        power = "/devices/wb-mdm3_76/controls/K2",
    )
    val braLoggia = Dimmer(
        id = "bra_loggia",
        title = "Бра правое",
        channel = "/devices/wb-mdm3_76/controls/Channel 3",
        power = "/devices/wb-mdm3_76/controls/K3",
    )

    val dimmers = listOf(roomMain, roomExtra, loggia, braKladovka, braLoggia)

    val kladovkaLight = Switch(
        id = "kladovka",
        title = "Кладовка",
        topic = "/devices/wb-mr6cv3_239/controls/K5",
    )

    val switches = listOf(kladovkaLight)

    object Led {
        const val strip = "/devices/wb-led_221/controls/RGB Strip"
        const val brightness = "/devices/wb-led_221/controls/RGB Strip Brightness"
        const val hue = "/devices/wb-led_221/controls/RGB Strip Hue"
        const val saturation = "/devices/wb-led_221/controls/RGB Strip Saturation"
        const val hueChanging = "/devices/wb-led_221/controls/Hue Changing"
        const val white = "/devices/wb-led_221/controls/Channel 4"
        const val whiteBrightness = "/devices/wb-led_221/controls/Channel 4 Brightness"

        val all = listOf(strip, brightness, hue, saturation, hueChanging, white, whiteBrightness)
    }

    object Climate {
        private const val base = "/devices/ONOKOM-AIR-HR-1-MB-B_2/controls"
        const val active = "$base/Active"
        const val mode = "$base/Mode"
        const val target = "$base/Target temperature"
        const val indoor = "$base/Indoor air temperature"
        const val fan = "$base/Smart fan speed"
        const val swing = "$base/Vanes swing"
        const val horizontal = "$base/Horizontal vanes"
        const val vertical = "$base/Vertical vanes"
        const val eco = "$base/Eco mode"
        const val sleep = "$base/Sleep mode"

        val all = listOf(
            active, mode, target, indoor, fan, swing, horizontal, vertical, eco, sleep,
        )
    }

    fun allStateTopics(): List<String> =
        buildList {
            dimmers.forEach {
                add(it.channel)
                add(it.power)
            }
            switches.forEach { add(it.topic) }
            addAll(Led.all)
            addAll(Climate.all)
        }

    /** Wiren Board: команда пишется в …/on. */
    fun commandTopic(stateTopic: String): String = stateTopic.trimEnd('/') + "/on"
}
