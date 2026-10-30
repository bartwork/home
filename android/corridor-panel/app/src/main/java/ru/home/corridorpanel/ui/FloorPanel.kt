package ru.home.corridorpanel.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.home.corridorpanel.model.FloorMode
import ru.home.corridorpanel.model.FloorModels
import ru.home.corridorpanel.model.FloorState
import ru.home.corridorpanel.model.Topics
import ru.home.corridorpanel.model.floorState
import ru.home.corridorpanel.ui.components.RoundBtn
import ru.home.corridorpanel.ui.theme.Surface2
import ru.home.corridorpanel.ui.theme.Teal
import ru.home.corridorpanel.ui.theme.TextMuted
import ru.home.corridorpanel.ui.theme.TextPrimary

/** Зоны тёплого пола (прихожая / ванна / туалет / кухня). */
@Composable
fun FloorPanel(
    values: Map<String, String>,
    publish: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    zones: List<Topics.FloorZone> = Topics.floors,
) {
    if (zones.isEmpty()) return
    var selectedId by remember { mutableStateOf(zones.first().id) }
    val zone = zones.firstOrNull { it.id == selectedId } ?: zones.first()
    val state = values.floorState(zone)

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FloorZoneTabs(
            zones = zones,
            selectedId = zone.id,
            values = values,
            onSelect = { selectedId = it },
        )
        FloorZoneControls(
            state = state,
            onMode = { publish(zone.mode, it.mqtt) },
            onSetpoint = { publish(zone.setpoint, FloorModels.clampSetpoint(it).toString()) },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun FloorZoneTabs(
    zones: List<Topics.FloorZone>,
    selectedId: String,
    values: Map<String, String>,
    onSelect: (String) -> Unit,
) {
    val click = LocalClickSound.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        zones.forEach { z ->
            val selected = z.id == selectedId
            val heating = values.floorState(z).heating
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            selected -> Teal.copy(alpha = 0.32f)
                            else -> Surface2
                        },
                    )
                    .border(
                        1.dp,
                        when {
                            selected -> Teal.copy(alpha = 0.55f)
                            heating -> Teal.copy(alpha = 0.28f)
                            else -> Color.Transparent
                        },
                        RoundedCornerShape(10.dp),
                    )
                    .clickable {
                        click.play()
                        onSelect(z.id)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = z.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        selected -> Teal
                        heating -> Teal.copy(alpha = 0.85f)
                        else -> TextMuted
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun FloorZoneControls(
    state: FloorState,
    onMode: (FloorMode) -> Unit,
    onSetpoint: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val click = LocalClickSound.current
    val tempLabel = FloorModels.formatTemp(state.temp)
    val setLabel = FloorModels.formatTemp(state.setpoint)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundBtn(
                active = state.heating,
                onClick = {
                    onMode(if (state.mode == FloorMode.Off) FloorMode.Auto else FloorMode.Off)
                },
                size = 40.dp,
            ) {
                Icon(Icons.Outlined.PowerSettingsNew, null, Modifier.size(20.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("температура пола", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "$tempLabel°",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (state.heating) Teal else TextPrimary,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundBtn(
                active = false,
                onClick = { onSetpoint(state.setpoint - 0.5) },
                size = 44.dp,
            ) {
                Icon(Icons.Outlined.Remove, null, Modifier.size(24.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("уставка", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "$setLabel°",
                    style = MaterialTheme.typography.displayLarge,
                    color = if (state.mode == FloorMode.Auto || state.heating) Teal else TextPrimary,
                )
            }
            RoundBtn(
                active = false,
                onClick = { onSetpoint(state.setpoint + 0.5) },
                size = 44.dp,
            ) {
                Icon(Icons.Outlined.Add, null, Modifier.size(24.dp))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FloorMode.entries.forEach { m ->
                val selected = state.mode == m
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) Teal.copy(alpha = 0.32f) else Surface2)
                        .border(
                            1.dp,
                            if (selected) Teal.copy(alpha = 0.55f) else Color.Transparent,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable {
                            click.play()
                            onMode(m)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = m.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) Teal else TextMuted,
                    )
                }
            }
        }
    }
}
