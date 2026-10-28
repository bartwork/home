package ru.home.kitchenpanel.model

data class LabeledValue(val value: Int, val label: String)

object ClimateModels {
    const val TARGET_MIN = 16
    const val TARGET_MAX = 30
    const val FAN_QUIET = 1
    const val FAN_TURBO = 5

    val modes = listOf(
        LabeledValue(1, "Нагрев"),
        LabeledValue(2, "Охлаждение"),
        LabeledValue(3, "Авто"),
        LabeledValue(4, "Сушка"),
        LabeledValue(5, "Вентиляция"),
    )

    val fans = listOf(
        LabeledValue(0, "Авто"),
        LabeledValue(1, "Тихий"),
        LabeledValue(2, "1"),
        LabeledValue(3, "2"),
        LabeledValue(4, "3"),
        LabeledValue(5, "Турбо"),
    )

    val swings = listOf(
        LabeledValue(0, "Качание стоп"),
        LabeledValue(1, "Качание оба"),
        LabeledValue(2, "Качание вертикальное"),
        LabeledValue(3, "Качание горизонтальное"),
    )

    fun clampTarget(n: Int): Int = n.coerceIn(TARGET_MIN, TARGET_MAX)

    fun nextInCycle(list: List<LabeledValue>, current: Int?): Int {
        if (list.isEmpty()) return 0
        if (current == null) return list.first().value
        val i = list.indexOfFirst { it.value == current }
        if (i < 0) return list.first().value
        return list[(i + 1) % list.size].value
    }

    fun toggleFanPreset(current: Int?, preset: Int): Int =
        if (current == preset) 0 else preset

    fun labelByValue(list: List<LabeledValue>, v: Int?): String? =
        v?.let { list.find { item -> item.value == it }?.label }
}

data class ClimateState(
    val on: Boolean = false,
    val target: Int? = null,
    val indoor: Double? = null,
    val mode: Int? = null,
    val fan: Int? = null,
    val swing: Int? = null,
    val ecoOn: Boolean = false,
    val sleepOn: Boolean = false,
) {
    val quietOn: Boolean get() = fan == ClimateModels.FAN_QUIET
    val turboOn: Boolean get() = fan == ClimateModels.FAN_TURBO
}

fun Map<String, String>.climateState(): ClimateState {
    fun num(topic: String): Double? = this[topic]?.trim()?.toDoubleOrNull()
    fun int(topic: String): Int? = num(topic)?.toInt()
    val fan = int(Topics.Climate.fan)
    return ClimateState(
        on = this[Topics.Climate.active] == "1",
        target = int(Topics.Climate.target),
        indoor = num(Topics.Climate.indoor),
        mode = int(Topics.Climate.mode),
        fan = fan,
        swing = int(Topics.Climate.swing),
        ecoOn = this[Topics.Climate.eco] == "1",
        sleepOn = this[Topics.Climate.sleep] == "1",
    )
}
