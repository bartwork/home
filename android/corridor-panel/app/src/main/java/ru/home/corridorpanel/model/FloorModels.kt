package ru.home.corridorpanel.model

enum class FloorMode(val mqtt: String, val label: String) {
    Off("off", "Выкл"),
    Heat("heat", "Нагрев"),
    Auto("auto", "Авто"),
    ;

    companion object {
        fun parse(raw: String?): FloorMode =
            when ((raw ?: "").trim().lowercase()) {
                "off" -> Off
                "heat" -> Heat
                else -> Auto
            }
    }
}

object FloorModels {
    const val SETPOINT_MIN = 15.0
    const val SETPOINT_MAX = 35.0
    const val SETPOINT_DEFAULT = 26.0

    fun clampSetpoint(n: Double): Double {
        val stepped = kotlin.math.round(n * 2.0) / 2.0
        return stepped.coerceIn(SETPOINT_MIN, SETPOINT_MAX)
    }

    fun formatTemp(n: Double?): String {
        if (n == null) return "—"
        return if (n % 1.0 == 0.0) n.toInt().toString() else String.format("%.1f", n)
    }
}

data class FloorState(
    val on: Boolean = false,
    val temp: Double? = null,
    val setpoint: Double = FloorModels.SETPOINT_DEFAULT,
    val mode: FloorMode = FloorMode.Auto,
) {
    val heating: Boolean get() = mode != FloorMode.Off && on
}

fun Map<String, String>.floorState(zone: Topics.FloorZone): FloorState {
    val sp = this[zone.setpoint]?.trim()?.toDoubleOrNull()
    return FloorState(
        on = this[zone.relay] == "1",
        temp = this[zone.sensor]?.trim()?.toDoubleOrNull(),
        setpoint = sp ?: FloorModels.SETPOINT_DEFAULT,
        mode = FloorMode.parse(this[zone.mode]),
    )
}
