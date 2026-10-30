package ru.home.corridorpanel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.home.corridorpanel.model.DEFAULT_LED_PALETTE
import ru.home.corridorpanel.model.Topics
import ru.home.corridorpanel.model.hsvToColor
import ru.home.corridorpanel.model.ledState
import ru.home.corridorpanel.ui.components.RoundBtn
import ru.home.corridorpanel.ui.theme.Surface
import ru.home.corridorpanel.ui.theme.TextMuted

/**
 * Карниз RGB + белый — как на квадратных планшетах (room-panel LedPanel).
 */
@Composable
fun LedStripBlock(
    values: Map<String, String>,
    onPublish: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = values.ledState()
    var pickerOpen by remember { mutableStateOf(false) }

    fun ensureOn() {
        if (!state.on) onPublish(Topics.Led.strip, "1")
    }

    fun stopHue() {
        if (state.hueChanging) onPublish(Topics.Led.hueChanging, "0")
    }

    fun applyColor(h: Int, s: Int, v: Int) {
        stopHue()
        val bri = v.coerceIn(0, 100).let { if (it <= 0) 80 else it }
        onPublish(Topics.Led.strip, "1")
        onPublish(Topics.Led.hue, h.coerceIn(0, 360).toString())
        onPublish(Topics.Led.saturation, s.coerceIn(0, 100).toString())
        onPublish(Topics.Led.brightness, bri.toString())
    }

    fun setBri(n: Int) {
        val next = n.coerceIn(0, 100)
        onPublish(Topics.Led.brightness, next.toString())
        when {
            next > 0 -> {
                stopHue()
                ensureOn()
            }
            state.on -> onPublish(Topics.Led.strip, "0")
        }
    }

    fun setHue(n: Int) {
        stopHue()
        onPublish(Topics.Led.hue, n.coerceIn(0, 360).toString())
        ensureOn()
    }

    fun setSat(n: Int) {
        stopHue()
        onPublish(Topics.Led.saturation, n.coerceIn(0, 100).toString())
        ensureOn()
    }

    fun setWhiteBri(n: Int) {
        val next = n.coerceIn(0, 100)
        onPublish(Topics.Led.whiteBrightness, next.toString())
        when {
            next > 0 && !state.whiteOn -> onPublish(Topics.Led.white, "1")
            next == 0 && state.whiteOn -> onPublish(Topics.Led.white, "0")
        }
    }

    fun toggleWhite() {
        if (state.whiteOn) {
            onPublish(Topics.Led.white, "0")
        } else {
            onPublish(Topics.Led.white, "1")
            if (state.whiteBrightness <= 0) onPublish(Topics.Led.whiteBrightness, "100")
        }
    }

    val preview = Color(
        hsvToColor(
            state.hue,
            state.saturation,
            if (state.on) maxOf(state.brightness, 18) else 12,
        ),
    )

    if (pickerOpen) {
        LedColorPickerDialog(
            hue = state.hue,
            saturation = state.saturation,
            brightness = state.brightness.coerceAtLeast(1),
            onChange = { h, s, v -> applyColor(h, s, v) },
            onDismiss = { pickerOpen = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Карниз",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            RoundBtn(active = state.on, onClick = {
                onPublish(Topics.Led.strip, if (state.on) "0" else "1")
            }, size = 36.dp) {
                Icon(Icons.Outlined.PowerSettingsNew, null, Modifier.size(18.dp))
            }
            RoundBtn(active = state.hueChanging, onClick = {
                onPublish(Topics.Led.hueChanging, if (state.hueChanging) "0" else "1")
                if (!state.hueChanging) ensureOn()
            }, size = 36.dp) {
                Icon(Icons.Outlined.Autorenew, null, Modifier.size(18.dp))
            }
            RoundBtn(active = state.whiteOn, onClick = ::toggleWhite, size = 36.dp) {
                Text("W", style = MaterialTheme.typography.labelLarge)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(preview)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .clickable { pickerOpen = true },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Цвет",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            DEFAULT_LED_PALETTE.forEach { sw ->
                val c = Color(hsvToColor(sw.hue, sw.saturation, 90))
                val selected =
                    state.hue == sw.hue && state.saturation == sw.saturation && state.on
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) Color.White else Color(0x2EE8EEF6),
                            shape = CircleShape,
                        )
                        .clickable {
                            applyColor(sw.hue, sw.saturation, sw.brightness)
                        },
                )
            }
        }

        LedSlider(
            "Яркость",
            state.brightness,
            100,
            Brush.horizontalGradient(listOf(Color.DarkGray, preview)),
            ::setBri,
        )
        LedSlider(
            "Оттенок",
            state.hue,
            360,
            Brush.horizontalGradient(
                listOf(
                    Color.Red, Color.Yellow, Color.Green,
                    Color.Cyan, Color.Blue, Color.Magenta, Color.Red,
                ),
            ),
            ::setHue,
        )
        LedSlider(
            "Насыщ.",
            state.saturation,
            100,
            Brush.horizontalGradient(
                listOf(Color.White, Color(hsvToColor(state.hue, 100, 100))),
            ),
            ::setSat,
        )
        LedSlider(
            "Белый",
            state.whiteBrightness,
            100,
            Brush.horizontalGradient(listOf(Color.DarkGray, Color.White)),
            ::setWhiteBri,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LedSlider(
    label: String,
    value: Int,
    max: Int,
    track: Brush,
    onChange: (Int) -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    var local by remember { mutableFloatStateOf(value.toFloat()) }
    LaunchedEffect(value) {
        if (!dragging) local = value.toFloat()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = Modifier.size(width = 56.dp, height = 16.dp),
        )
        Box(Modifier.weight(1f)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(4.dp))
                    .background(track),
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
                        onChange(local.toInt())
                    },
                    valueRange = 0f..max.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent,
                    ),
                )
            }
        }
        Text(
            "${local.toInt()}",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.size(width = 28.dp, height = 14.dp),
        )
    }
}
