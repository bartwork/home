package ru.home.roompanel.model

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

data class LedState(
    val on: Boolean = false,
    val brightness: Int = 0,
    val hue: Int = 0,
    val saturation: Int = 100,
    val hueChanging: Boolean = false,
    val whiteOn: Boolean = false,
    val whiteBrightness: Int = 0,
) {
    val lit: Boolean get() = on || whiteOn
}

fun Map<String, String>.ledState(): LedState {
    fun int(topic: String, default: Int = 0): Int =
        this[topic]?.trim()?.toDoubleOrNull()?.toInt() ?: default
    return LedState(
        on = this[Topics.Led.strip] == "1",
        brightness = int(Topics.Led.brightness),
        hue = int(Topics.Led.hue),
        saturation = int(Topics.Led.saturation, 100),
        hueChanging = this[Topics.Led.hueChanging] == "1",
        whiteOn = this[Topics.Led.white] == "1",
        whiteBrightness = int(Topics.Led.whiteBrightness),
    )
}

data class ColorSwatch(val hue: Int, val saturation: Int, val brightness: Int)

val DEFAULT_LED_PALETTE = listOf(
    ColorSwatch(28, 85, 100),
    ColorSwatch(48, 70, 100),
    ColorSwatch(0, 95, 100),
    ColorSwatch(330, 75, 100),
    ColorSwatch(280, 80, 100),
    ColorSwatch(210, 85, 100),
    ColorSwatch(175, 70, 100),
    ColorSwatch(130, 70, 100),
)

/** HSV → ARGB (H 0–360, S/V 0–100). */
fun hsvToColor(h: Int, s: Int, v: Int): Long {
    val hh = ((h % 360) + 360) % 360
    val ss = s.coerceIn(0, 100) / 100.0
    val vv = v.coerceIn(0, 100) / 100.0
    val c = vv * ss
    val x = c * (1 - kotlin.math.abs((hh / 60.0) % 2 - 1))
    val m = vv - c
    val (r1, g1, b1) = when {
        hh < 60 -> Triple(c, x, 0.0)
        hh < 120 -> Triple(x, c, 0.0)
        hh < 180 -> Triple(0.0, c, x)
        hh < 240 -> Triple(0.0, x, c)
        hh < 300 -> Triple(x, 0.0, c)
        else -> Triple(c, 0.0, x)
    }
    val r = ((r1 + m) * 255).toInt().coerceIn(0, 255)
    val g = ((g1 + m) * 255).toInt().coerceIn(0, 255)
    val b = ((b1 + m) * 255).toInt().coerceIn(0, 255)
    return 0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
}
