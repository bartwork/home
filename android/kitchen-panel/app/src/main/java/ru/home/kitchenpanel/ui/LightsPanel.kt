package ru.home.kitchenpanel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ru.home.kitchenpanel.model.Topics
import ru.home.kitchenpanel.ui.LocalClickSound
import ru.home.kitchenpanel.ui.components.RoundBtn
import ru.home.kitchenpanel.ui.theme.Surface
import ru.home.kitchenpanel.ui.theme.Teal
import ru.home.kitchenpanel.ui.theme.TextMuted
import ru.home.kitchenpanel.ui.theme.rowTitle
import ru.home.kitchenpanel.ui.theme.valueTeal

@Composable
fun LightsPanel(
    values: Map<String, String>,
    onToggleDimmer: (Topics.Dimmer) -> Unit,
    onLevel: (Topics.Dimmer, Int) -> Unit,
    onToggleSwitch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Topics.dimmers.forEach { d ->
            DimmerRow(
                title = d.title,
                channel = values[d.channel],
                power = values[d.power],
                onToggle = { onToggleDimmer(d) },
                onLevel = { onLevel(d, it) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
        Topics.switches.forEach { sw ->
            SwitchRow(
                title = sw.title,
                on = values[sw.topic] == "1",
                onToggle = { onToggleSwitch(sw.topic) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DimmerRow(
    title: String,
    channel: String?,
    power: String?,
    onToggle: () -> Unit,
    onLevel: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val remote = channel?.toDoubleOrNull()?.toInt()?.coerceIn(0, 100) ?: 0
    val on = when (power) {
        "1" -> true
        "0" -> false
        else -> remote > 0
    }
    var dragging by remember { mutableStateOf(false) }
    var local by remember { mutableFloatStateOf(remote.toFloat()) }
    LaunchedEffect(remote) {
        if (!dragging) local = remote.toFloat()
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(
                width = 1.dp,
                color = if (on) Teal.copy(alpha = 0.35f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val click = LocalClickSound.current
        RoundBtn(
            active = on,
            onClick = onToggle,
            size = 36.dp,
            modifier = Modifier.zIndex(1f),
        ) {
            Icon(Icons.Outlined.PowerSettingsNew, null, Modifier.size(18.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.rowTitle,
            maxLines = 1,
            modifier = Modifier
                .weight(0.38f)
                .zIndex(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        click.play()
                        onToggle()
                    },
                ),
        )
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            Slider(
                value = local,
                onValueChange = {
                    dragging = true
                    local = it
                },
                onValueChangeFinished = {
                    dragging = false
                    onLevel(local.toInt())
                },
                valueRange = 0f..100f,
                modifier = Modifier
                    .weight(0.62f)
                    .height(28.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Teal,
                    activeTrackColor = Teal,
                    inactiveTrackColor = TextMuted.copy(alpha = 0.22f),
                ),
            )
        }
        Text(
            text = "${local.toInt()}",
            style = if (on) MaterialTheme.typography.valueTeal
            else MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    on: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val click = LocalClickSound.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(
                width = 1.dp,
                color = if (on) Teal.copy(alpha = 0.35f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    click.play()
                    onToggle()
                },
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RoundBtn(active = on, onClick = onToggle, size = 36.dp) {
            Icon(Icons.Outlined.PowerSettingsNew, null, Modifier.size(18.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.rowTitle,
        )
    }
}
