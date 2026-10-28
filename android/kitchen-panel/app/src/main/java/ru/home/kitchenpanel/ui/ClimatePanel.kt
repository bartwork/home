package ru.home.kitchenpanel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ru.home.kitchenpanel.model.ClimateModels
import ru.home.kitchenpanel.model.ClimateState
import ru.home.kitchenpanel.model.Topics
import ru.home.kitchenpanel.ui.components.ChipBtn
import ru.home.kitchenpanel.ui.components.RoundBtn
import ru.home.kitchenpanel.ui.theme.Surface
import ru.home.kitchenpanel.ui.theme.Teal
import ru.home.kitchenpanel.ui.theme.TextMuted
import ru.home.kitchenpanel.ui.theme.TextPrimary

private val modeIcons: Map<Int, ImageVector> = mapOf(
    1 to Icons.Outlined.Whatshot,
    2 to Icons.Outlined.AcUnit,
    3 to Icons.Outlined.Sync,
    4 to Icons.Outlined.WaterDrop,
    5 to Icons.Outlined.Air,
)

@Composable
fun ClimatePanel(
    state: ClimateState,
    publish: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun setTarget(n: Int) {
        publish(Topics.Climate.target, ClimateModels.clampTarget(n).toString())
    }

    val indoorLabel = state.indoor?.let { formatTemp(it) } ?: "—"
    val targetLabel = state.target?.toString() ?: "—"
    val fanLabel = ClimateModels.labelByValue(ClimateModels.fans, state.fan) ?: "Вентилятор"
    val swingLabel = ClimateModels.labelByValue(ClimateModels.swings, state.swing) ?: "Качание"
    val fanActive = state.fan != null && state.fan > 0 && !state.quietOn && !state.turboOn
    val swingActive = (state.swing ?: 0) > 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        ClimateHeader(
            on = state.on,
            indoorLabel = indoorLabel,
            onPower = { publish(Topics.Climate.active, if (state.on) "0" else "1") },
        )

        TemperatureRow(
            on = state.on,
            targetLabel = targetLabel,
            onMinus = { state.target?.let { setTarget(it - 1) } },
            onPlus = { setTarget((state.target ?: 24) + 1) },
        )

        ModeRow(
            selected = state.mode,
            onSelect = { publish(Topics.Climate.mode, it.toString()) },
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ChipBtn(
                    label = "Тихий",
                    active = state.quietOn,
                    onClick = {
                        publish(
                            Topics.Climate.fan,
                            ClimateModels.toggleFanPreset(state.fan, ClimateModels.FAN_QUIET).toString(),
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                )
                ChipBtn(
                    label = "Турбо",
                    active = state.turboOn,
                    onClick = {
                        publish(
                            Topics.Climate.fan,
                            ClimateModels.toggleFanPreset(state.fan, ClimateModels.FAN_TURBO).toString(),
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                )
                ChipBtn(
                    label = fanLabel,
                    active = fanActive,
                    onClick = {
                        publish(
                            Topics.Climate.fan,
                            ClimateModels.nextInCycle(ClimateModels.fans, state.fan).toString(),
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ChipBtn(
                    label = swingLabel,
                    active = swingActive,
                    onClick = {
                        publish(
                            Topics.Climate.swing,
                            ClimateModels.nextInCycle(ClimateModels.swings, state.swing).toString(),
                        )
                    },
                    compact = true,
                    modifier = Modifier
                        .weight(1.6f)
                        .height(40.dp),
                )
                ChipBtn(
                    label = "Eco",
                    active = state.ecoOn,
                    onClick = { publish(Topics.Climate.eco, if (state.ecoOn) "0" else "1") },
                    modifier = Modifier
                        .weight(0.85f)
                        .height(40.dp),
                )
                ChipBtn(
                    label = "Сон",
                    active = state.sleepOn,
                    onClick = { publish(Topics.Climate.sleep, if (state.sleepOn) "0" else "1") },
                    modifier = Modifier
                        .weight(0.85f)
                        .height(40.dp),
                )
            }
        }
    }
}

@Composable
private fun ClimateHeader(
    on: Boolean,
    indoorLabel: String,
    onPower: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundBtn(active = on, onClick = onPower, size = 44.dp) {
            Icon(Icons.Outlined.PowerSettingsNew, null, Modifier.size(22.dp))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("в комнате", style = MaterialTheme.typography.labelMedium)
            Text(
                text = "$indoorLabel°",
                style = MaterialTheme.typography.titleLarge,
                color = if (on) Teal else TextPrimary,
            )
        }
    }
}

@Composable
private fun TemperatureRow(
    on: Boolean,
    targetLabel: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundBtn(active = false, onClick = onMinus, size = 48.dp) {
            Icon(Icons.Outlined.Remove, null, Modifier.size(26.dp))
        }
        Text(
            text = "$targetLabel°",
            style = MaterialTheme.typography.displayLarge,
            color = if (on) Teal else TextPrimary,
        )
        RoundBtn(active = false, onClick = onPlus, size = 48.dp) {
            Icon(Icons.Outlined.Add, null, Modifier.size(26.dp))
        }
    }
}

@Composable
private fun ModeRow(
    selected: Int?,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ClimateModels.modes.forEach { m ->
            val isSelected = selected == m.value
            val icon = modeIcons[m.value] ?: Icons.Outlined.Sync
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Color(0x525EC8D8) else Color(0x14E8EEF6))
                    .border(
                        1.dp,
                        if (isSelected) Color(0x8C5EC8D8) else Color.Transparent,
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { onSelect(m.value) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = m.label,
                    tint = if (isSelected) Teal else TextMuted,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

private fun formatTemp(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value)
