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
import ru.home.kitchenpanel.model.FloorMode
import ru.home.kitchenpanel.model.FloorModels
import ru.home.kitchenpanel.model.FloorState
import ru.home.kitchenpanel.model.Topics
import ru.home.kitchenpanel.ui.components.RoundBtn
import ru.home.kitchenpanel.ui.theme.Surface
import ru.home.kitchenpanel.ui.theme.Teal
import ru.home.kitchenpanel.ui.theme.TextMuted
import ru.home.kitchenpanel.ui.theme.TextPrimary

@Composable
fun FloorPanel(
    state: FloorState,
    publish: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun setMode(mode: FloorMode) {
        publish(Topics.Floor.mode, mode.mqtt)
    }

    fun setSetpoint(n: Double) {
        publish(Topics.Floor.setpoint, FloorModels.clampSetpoint(n).toString())
    }

    val tempLabel = FloorModels.formatTemp(state.temp)
    val setLabel = FloorModels.formatTemp(state.setpoint)

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .padding(14.dp),
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
                    setMode(if (state.mode == FloorMode.Off) FloorMode.Auto else FloorMode.Off)
                },
                size = 44.dp,
            ) {
                Icon(Icons.Outlined.PowerSettingsNew, null, Modifier.size(22.dp))
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
                onClick = { setSetpoint(state.setpoint - 0.5) },
                size = 48.dp,
            ) {
                Icon(Icons.Outlined.Remove, null, Modifier.size(26.dp))
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
                onClick = { setSetpoint(state.setpoint + 0.5) },
                size = 48.dp,
            ) {
                Icon(Icons.Outlined.Add, null, Modifier.size(26.dp))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FloorMode.entries.forEach { m ->
                val selected = state.mode == m
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) Color(0x525EC8D8) else Color(0x14E8EEF6))
                        .border(
                            1.dp,
                            if (selected) Color(0x8C5EC8D8) else Color.Transparent,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable { setMode(m) },
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
