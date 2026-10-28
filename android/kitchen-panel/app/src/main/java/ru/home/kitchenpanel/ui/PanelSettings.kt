package ru.home.kitchenpanel.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.home.kitchenpanel.AmbientBrightness
import ru.home.kitchenpanel.UiState
import ru.home.kitchenpanel.ui.theme.Bg
import ru.home.kitchenpanel.ui.theme.Surface
import ru.home.kitchenpanel.ui.theme.Surface2
import ru.home.kitchenpanel.ui.theme.Teal
import ru.home.kitchenpanel.ui.theme.TextMuted
import ru.home.kitchenpanel.ui.theme.TextPrimary

@Composable
fun PanelSettingsOverlay(
    visible: Boolean,
    state: UiState,
    onDismiss: () -> Unit,
    onAutoBrightness: (Boolean) -> Unit,
    onManualBrightness: (Float) -> Unit,
    onClickSound: (Boolean) -> Unit,
    onSaveBroker: (String, Int) -> Unit,
    onReconnect: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 6 },
        exit = fadeOut() + slideOutVertically { it / 6 },
    ) {
        PanelSettingsScreen(
            state = state,
            onDismiss = onDismiss,
            onAutoBrightness = onAutoBrightness,
            onManualBrightness = onManualBrightness,
            onClickSound = onClickSound,
            onSaveBroker = onSaveBroker,
            onReconnect = onReconnect,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PanelSettingsScreen(
    state: UiState,
    onDismiss: () -> Unit,
    onAutoBrightness: (Boolean) -> Unit,
    onManualBrightness: (Float) -> Unit,
    onClickSound: (Boolean) -> Unit,
    onSaveBroker: (String, Int) -> Unit,
    onReconnect: () -> Unit,
) {
    val click = LocalClickSound.current
    var hostText by remember(state.brokerHost) { mutableStateOf(state.brokerHost) }
    var portText by remember(state.brokerPort) { mutableStateOf(state.brokerPort.toString()) }
    val briPct = ((state.manualBrightness - AmbientBrightness.MIN_BRIGHTNESS) /
        (AmbientBrightness.MAX_BRIGHTNESS - AmbientBrightness.MIN_BRIGHTNESS) * 100f)
        .toInt()
        .coerceIn(0, 100)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0E1622), Color(0xFF0A1018), Color(0xFF0C1420)),
                ),
            ),
    ) {
        // шапка на всю ширину
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141C28))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Настройки", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text(
                    "Двойной тап по активной вкладке",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Surface2)
                    .clickable {
                        click.play()
                        onDismiss()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Close, "Закрыть", tint = TextPrimary, modifier = Modifier.size(20.dp))
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SettingsSection(icon = Icons.Outlined.Brightness6, title = "Экран") {
                SettingsToggleRow(
                    title = "Автояркость",
                    subtitle = if (state.lightSensorAvailable) "По датчику освещённости"
                    else "Датчик недоступен",
                    checked = state.autoBrightness && state.lightSensorAvailable,
                    enabled = state.lightSensorAvailable,
                    onCheckedChange = {
                        click.play()
                        onAutoBrightness(it)
                    },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (state.autoBrightness) "Ручная яркость (авто вкл.)" else "Яркость · $briPct%",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (state.autoBrightness) TextMuted.copy(alpha = 0.55f) else TextMuted,
                )
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    Slider(
                        value = state.manualBrightness,
                        onValueChange = onManualBrightness,
                        valueRange = AmbientBrightness.MIN_BRIGHTNESS..AmbientBrightness.MAX_BRIGHTNESS,
                        enabled = !state.autoBrightness,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Teal,
                            activeTrackColor = Teal,
                            inactiveTrackColor = TextMuted.copy(alpha = 0.22f),
                            disabledThumbColor = Teal.copy(alpha = 0.35f),
                            disabledActiveTrackColor = Teal.copy(alpha = 0.25f),
                            disabledInactiveTrackColor = TextMuted.copy(alpha = 0.12f),
                        ),
                    )
                }
            }

            SettingsSection(icon = Icons.AutoMirrored.Outlined.VolumeUp, title = "Звук") {
                SettingsToggleRow(
                    title = "Звук нажатий",
                    subtitle = "Мягкий клик при тапах",
                    checked = state.clickSound,
                    enabled = true,
                    onCheckedChange = { onClickSound(it) },
                )
                if (state.clickSound) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Teal.copy(alpha = 0.16f))
                            .border(1.dp, Teal.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .clickable { click.play() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Проверить звук",
                            style = MaterialTheme.typography.labelLarge,
                            color = Teal,
                        )
                    }
                }
            }

            SettingsSection(icon = Icons.Outlined.Wifi, title = "MQTT") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    StatusDot(ok = state.connected)
                    Text(
                        text = if (state.connected) "Подключено" else (state.lastError ?: "Нет связи"),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (state.connected) Teal else Color(0xFFE57373),
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "Снова",
                        style = MaterialTheme.typography.labelLarge,
                        color = Teal,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                click.play()
                                onReconnect()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Teal,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedLabelColor = Teal,
                    unfocusedLabelColor = TextMuted,
                    cursorColor = Teal,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                )
                OutlinedTextField(
                    value = hostText,
                    onValueChange = { hostText = it },
                    label = { Text("Хост") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter(Char::isDigit).take(5) },
                    label = { Text("Порт") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Teal)
                        .clickable {
                            click.play()
                            onSaveBroker(hostText, portText.toIntOrNull() ?: state.brokerPort)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Сохранить брокер",
                        style = MaterialTheme.typography.labelLarge,
                        color = Bg,
                    )
                }
            }

            if (state.lightSensorAvailable) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                ) {
                    Icon(Icons.Outlined.Sensors, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Text(
                        "Датчик освещённости активен",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Teal.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = Teal, modifier = Modifier.size(16.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        }
        content()
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Bg,
                checkedTrackColor = Teal,
                uncheckedThumbColor = TextPrimary,
                uncheckedTrackColor = Surface2,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun StatusDot(ok: Boolean) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(if (ok) Teal else Color(0xFFE57373)),
    )
}
