package ru.home.corridorpanel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.home.corridorpanel.model.AirState
import ru.home.corridorpanel.model.ClimateModels
import ru.home.corridorpanel.model.ClimateState
import ru.home.corridorpanel.model.Topics
import ru.home.corridorpanel.model.airState
import ru.home.corridorpanel.model.climateState
import ru.home.corridorpanel.ui.components.ChipBtn
import ru.home.corridorpanel.ui.components.RoundBtn
import ru.home.corridorpanel.ui.theme.Surface2
import ru.home.corridorpanel.ui.theme.Teal
import ru.home.corridorpanel.ui.theme.TextMuted
import ru.home.corridorpanel.ui.theme.TextPrimary

private val CardShape = RoundedCornerShape(14.dp)

@Composable
fun HomeClimatePanel(
    values: Map<String, String>,
    publish: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val air = values.airState()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AirStrip(air = air)
        Topics.climates.forEach { unit ->
            CompactClimateCard(
                title = unit.title,
                state = values.climateState(unit),
                publish = publish,
                unit = unit,
            )
        }
    }
}

@Composable
fun AirStrip(air: AirState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Surface2)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AirMetric("°C", air.temp?.let { String.format("%.1f", it) } ?: "—")
        AirMetric("%", air.humidity?.let { String.format("%.0f", it) } ?: "—")
        AirMetric(
            "CO₂",
            air.co2?.toString() ?: "—",
            hint = air.co2?.let { air.co2Hint },
        )
        AirMetric("VOC", air.voc?.toString() ?: "—")
    }
}

@Composable
private fun AirMetric(label: String, value: String, hint: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.labelSmall, color = Teal)
        }
    }
}

@Composable
private fun CompactClimateCard(
    title: String,
    state: ClimateState,
    unit: Topics.ClimateUnit,
    publish: (String, String) -> Unit,
) {
    fun setTarget(n: Int) {
        publish(unit.target, ClimateModels.clampTarget(n).toString())
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Surface2)
            .border(
                1.dp,
                if (state.on) Teal.copy(alpha = 0.35f) else Color.Transparent,
                CardShape,
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            RoundBtn(active = state.on, onClick = {
                publish(unit.active, if (state.on) "0" else "1")
            }, size = 36.dp) {
                Icon(Icons.Outlined.PowerSettingsNew, null, Modifier.size(18.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
            Text(
                text = state.indoor?.let { String.format("%.0f°", it) } ?: "—",
                style = MaterialTheme.typography.titleMedium,
                color = if (state.on) Teal else TextMuted,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            RoundBtn(active = false, onClick = { setTarget((state.target ?: 24) - 1) }, size = 32.dp) {
                Icon(Icons.Outlined.Remove, null, Modifier.size(16.dp))
            }
            Text(
                text = "${state.target ?: "—"}°",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            RoundBtn(active = false, onClick = { setTarget((state.target ?: 24) + 1) }, size = 32.dp) {
                Icon(Icons.Outlined.Add, null, Modifier.size(16.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            ClimateModels.modes.forEach { m ->
                ChipBtn(
                    label = m.label,
                    active = state.mode == m.value,
                    onClick = { publish(unit.mode, m.value.toString()) },
                    modifier = Modifier.weight(1f),
                    compact = true,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            ChipBtn(
                label = if (state.quietOn) "Тихий ✓" else "Тихий",
                active = state.quietOn,
                onClick = {
                    publish(
                        unit.fan,
                        ClimateModels.toggleFanPreset(state.fan, ClimateModels.FAN_QUIET).toString(),
                    )
                },
                modifier = Modifier.weight(1f),
                compact = true,
            )
            ChipBtn(
                label = ClimateModels.labelByValue(ClimateModels.fans, state.fan) ?: "Вент.",
                active = state.fan != null && state.fan > 0 && !state.quietOn && !state.turboOn,
                onClick = {
                    publish(unit.fan, ClimateModels.nextInCycle(ClimateModels.fans, state.fan).toString())
                },
                modifier = Modifier.weight(1f),
                compact = true,
            )
            ChipBtn(
                label = if (state.turboOn) "Турбо ✓" else "Турбо",
                active = state.turboOn,
                onClick = {
                    publish(
                        unit.fan,
                        ClimateModels.toggleFanPreset(state.fan, ClimateModels.FAN_TURBO).toString(),
                    )
                },
                modifier = Modifier.weight(1f),
                compact = true,
            )
            ChipBtn(
                label = ClimateModels.labelByValue(ClimateModels.swings, state.swing) ?: "Кач.",
                active = (state.swing ?: 0) > 0,
                onClick = {
                    publish(unit.swing, ClimateModels.nextInCycle(ClimateModels.swings, state.swing).toString())
                },
                modifier = Modifier.weight(1f),
                compact = true,
            )
        }
    }
}
