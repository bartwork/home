package ru.home.corridorpanel.ui

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import ru.home.corridorpanel.model.Topics
import ru.home.corridorpanel.ui.components.RoundBtn
import ru.home.corridorpanel.ui.theme.Surface2
import ru.home.corridorpanel.ui.theme.Teal
import ru.home.corridorpanel.ui.theme.TextMuted
import ru.home.corridorpanel.ui.theme.TextPrimary
import ru.home.corridorpanel.ui.theme.rowTitle
import ru.home.corridorpanel.ui.theme.valueTeal

private val RowShape = RoundedCornerShape(14.dp)

@Composable
fun LightsPanel(
    values: Map<String, String>,
    dimmers: List<Topics.Dimmer>,
    switches: List<Topics.Switch>,
    onToggleDimmer: (Topics.Dimmer) -> Unit,
    onLevel: (Topics.Dimmer, Int) -> Unit,
    onToggleSwitch: (String) -> Unit,
    onPublish: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    groupLabel: String? = null,
    masterSwitches: List<Topics.Switch> = emptyList(),
    showLed: Boolean = false,
) {
    // Скролл, когда рядов много (кухня/зал/прихожая), иначе weight на санузле.
    val dense = showLed || masterSwitches.isNotEmpty() ||
        dimmers.size + switches.size >= 4
    Column(
        modifier = modifier
            .fillMaxSize()
            .then(if (dense) Modifier.verticalScroll(rememberScrollState()) else Modifier),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val rowMod = if (dense) {
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
        } else {
            Modifier
                .weight(1f)
                .fillMaxWidth()
        }
        if (masterSwitches.isNotEmpty()) {
            masterSwitches.forEach { sw ->
                SwitchRow(
                    title = sw.title,
                    on = values[sw.topic] == "1",
                    onToggle = { onToggleSwitch(sw.topic) },
                    modifier = rowMod,
                )
            }
        }
        dimmers.forEach { d ->
            DimmerRow(
                title = d.title,
                channel = values[d.channel],
                power = values[d.power],
                onToggle = { onToggleDimmer(d) },
                onLevel = { onLevel(d, it) },
                modifier = rowMod,
            )
        }
        if (showLed) {
            LedStripBlock(
                values = values,
                onPublish = onPublish,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (groupLabel != null && switches.isNotEmpty()) {
            Text(
                text = groupLabel,
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                modifier = Modifier.padding(start = 4.dp, top = if (dimmers.isEmpty()) 0.dp else 2.dp),
            )
        }
        switches.forEach { sw ->
            SwitchRow(
                title = sw.title,
                on = values[sw.topic] == "1",
                onToggle = { onToggleSwitch(sw.topic) },
                modifier = rowMod,
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
            .clip(RowShape)
            .background(Surface2)
            .border(
                width = 1.dp,
                color = if (on) Teal.copy(alpha = 0.35f) else Color.Transparent,
                shape = RowShape,
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
                .weight(0.34f)
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
                    .weight(0.66f)
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
            .clip(RowShape)
            .background(Surface2)
            .border(
                width = 1.dp,
                color = if (on) Teal.copy(alpha = 0.35f) else Color.Transparent,
                shape = RowShape,
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
            color = TextPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
    }
}
