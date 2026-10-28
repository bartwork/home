package ru.home.kitchenpanel.model

/** Каталог топиков кухни — синхронно с docs/mqtt_topics.md. */
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

    val lightWindow = Dimmer(
        id = "kitchen_window",
        title = "У окна",
        channel = "/devices/wb-mdm3_218/controls/Channel 1",
        power = "/devices/wb-mdm3_218/controls/K1",
    )
    val lightLamp = Dimmer(
        id = "kitchen_lamp",
        title = "Лампа",
        channel = "/devices/wb-mdm3_218/controls/Channel 2",
        power = "/devices/wb-mdm3_218/controls/K2",
    )
    val lightDoor = Dimmer(
        id = "kitchen_door",
        title = "У двери",
        channel = "/devices/wb-mdm3_218/controls/Channel 3",
        power = "/devices/wb-mdm3_218/controls/K3",
    )

    val dimmers = listOf(lightWindow, lightLamp, lightDoor)

    val bra = Switch(
        id = "kitchen_bra",
        title = "Бра",
        topic = "/devices/wb-mr6cv3_239/controls/K1",
    )
    val underCabinet = Switch(
        id = "kitchen_under",
        title = "Подсветка",
        topic = "/devices/wb-mr6cv3_239/controls/K2",
    )
    val hood = Switch(
        id = "kitchen_hood",
        title = "Вытяжка",
        topic = "/devices/wb-mr6cv3_239/controls/K3",
    )

    val switches = listOf(bra, underCabinet, hood)

    /**
     * Тёплый пол кухни (реле K5).
     * deviceId — id в БД home (не номер кабеля из docs/devices.md).
     */
    object Floor {
        const val deviceId = 18
        const val relay = "/devices/wb-mr6cv3_225/controls/K5"
        const val sensor = "/devices/wb-m1w2_82/controls/External Sensor 2"
        const val setpoint = "/devices/home/controls/floor_${deviceId}_setpoint"
        const val mode = "/devices/home/controls/floor_${deviceId}_mode"

        val all = listOf(relay, sensor, setpoint, mode)
    }

    object Climate {
        private const val base = "/devices/ONOKOM-AIR-HR-1-MB-B_1/controls"
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
            addAll(Floor.all)
            addAll(Climate.all)
        }

    fun commandTopic(stateTopic: String): String = stateTopic.trimEnd('/') + "/on"
}
