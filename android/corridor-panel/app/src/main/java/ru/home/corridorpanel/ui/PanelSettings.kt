package ru.home.corridorpanel.ui

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
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DoorFront
import androidx.compose.material.icons.outlined.ErrorOutline
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.home.corridorpanel.AmbientBrightness
import ru.home.corridorpanel.ErrorEntry
import ru.home.corridorpanel.ErrorLog
import ru.home.corridorpanel.UiState
import ru.home.corridorpanel.UsageEntry
import ru.home.corridorpanel.sip.SipConfig
import ru.home.corridorpanel.sip.SipRegistration
import ru.home.corridorpanel.ui.theme.Bg
import ru.home.corridorpanel.ui.theme.Surface
import ru.home.corridorpanel.ui.theme.Surface2
import ru.home.corridorpanel.ui.theme.Teal
import ru.home.corridorpanel.ui.theme.TextMuted
import ru.home.corridorpanel.ui.theme.TextPrimary

@Composable
fun PanelSettingsOverlay(
    visible: Boolean,
    state: UiState,
    usageTop: List<UsageEntry>,
    usageTotal: Int,
    errorEntries: List<ErrorEntry>,
    onDismiss: () -> Unit,
    onAutoBrightness: (Boolean) -> Unit,
    onManualBrightness: (Float) -> Unit,
    onClickSound: (Boolean) -> Unit,
    onThemeMode: (ru.home.corridorpanel.ui.theme.ThemeMode) -> Unit,
    onBlankScreen: () -> Unit,
    onSaveBroker: (String, Int) -> Unit,
    onReconnect: () -> Unit,
    onSaveSip: (SipConfig) -> Unit,
    onClearUsage: () -> Unit,
    onClearErrors: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 6 },
        exit = fadeOut() + slideOutVertically { it / 6 },
    ) {
        PanelSettingsScreen(
            state = state,
            usageTop = usageTop,
            usageTotal = usageTotal,
            errorEntries = errorEntries,
            onDismiss = onDismiss,
            onAutoBrightness = onAutoBrightness,
            onManualBrightness = onManualBrightness,
            onClickSound = onClickSound,
            onThemeMode = onThemeMode,
            onBlankScreen = onBlankScreen,
            onSaveBroker = onSaveBroker,
            onReconnect = onReconnect,
            onClearUsage = onClearUsage,
            onClearErrors = onClearErrors,
            onSaveSip = onSaveSip,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PanelSettingsScreen(
    state: UiState,
    usageTop: List<UsageEntry>,
    usageTotal: Int,
    errorEntries: List<ErrorEntry>,
    onDismiss: () -> Unit,
    onAutoBrightness: (Boolean) -> Unit,
    onManualBrightness: (Float) -> Unit,
    onClickSound: (Boolean) -> Unit,
    onThemeMode: (ru.home.corridorpanel.ui.theme.ThemeMode) -> Unit,
    onBlankScreen: () -> Unit,
    onSaveBroker: (String, Int) -> Unit,
    onReconnect: () -> Unit,
    onSaveSip: (SipConfig) -> Unit,
    onClearUsage: () -> Unit,
    onClearErrors: () -> Unit,
) {
    val click = LocalClickSound.current
    var hostText by remember(state.brokerHost) { mutableStateOf(state.brokerHost) }
    var portText by remember(state.brokerPort) { mutableStateOf(state.brokerPort.toString()) }
    val sipCfg = state.sip.config
    var sipDomain by remember(sipCfg.domain) { mutableStateOf(sipCfg.domain) }
    var sipUser by remember(sipCfg.username) { mutableStateOf(sipCfg.username) }
    var sipPass by remember(sipCfg.password) { mutableStateOf(sipCfg.password) }
    var doorExt by remember(sipCfg.doorExtension) { mutableStateOf(sipCfg.doorExtension) }
    var camHost by remember(sipCfg.cameraHost) { mutableStateOf(sipCfg.cameraHost) }
    var camUser by remember(sipCfg.cameraUser) { mutableStateOf(sipCfg.cameraUser) }
    var camPass by remember(sipCfg.cameraPassword) { mutableStateOf(sipCfg.cameraPassword) }
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
            SettingsSection(icon = Icons.Outlined.Brightness6, title = "Оформление") {
                Text("Тема", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ThemeChip(
                        label = "Тёмная",
                        selected = state.themeMode == ru.home.corridorpanel.ui.theme.ThemeMode.Dark,
                        onClick = {
                            click.play()
                            onThemeMode(ru.home.corridorpanel.ui.theme.ThemeMode.Dark)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    ThemeChip(
                        label = "Светлая",
                        selected = state.themeMode == ru.home.corridorpanel.ui.theme.ThemeMode.Light,
                        onClick = {
                            click.play()
                            onThemeMode(ru.home.corridorpanel.ui.theme.ThemeMode.Light)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

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
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface2)
                        .border(1.dp, TextMuted.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                        .clickable {
                            click.play()
                            onBlankScreen()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Выключить экран",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                    )
                }
                Text(
                    "Или удержание вкладки сверху · касание включит снова",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 6.dp),
                )
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

            SettingsSection(icon = Icons.Outlined.BarChart, title = "Частое использование") {
                Text(
                    "Команды MQTT (антиспам слайдеров). Потом по топу можно собрать главный экран. Всего: $usageTotal.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
                Spacer(Modifier.height(8.dp))
                if (usageTop.isEmpty()) {
                    Text(
                        "Пока пусто — нажимайте выключатели и свет.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                } else {
                    usageTop.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    entry.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                )
                                Text(
                                    entry.topic,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    maxLines = 1,
                                )
                            }
                            Text(
                                "${entry.count}×",
                                style = MaterialTheme.typography.titleSmall,
                                color = Teal,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (usageTotal > 0) Color(0x33E57373) else Surface2)
                        .clickable(enabled = usageTotal > 0) {
                            click.play()
                            onClearUsage()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Очистить статистику",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (usageTotal > 0) Color(0xFFE57373) else TextMuted,
                    )
                }
            }

            SettingsSection(icon = Icons.Outlined.ErrorOutline, title = "Журнал ошибок") {
                Text(
                    "MQTT, SIP и камера. Повтор одной ошибки не чаще раза в 30 с. До 100 записей.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
                Spacer(Modifier.height(8.dp))
                if (errorEntries.isEmpty()) {
                    Text(
                        "Пока пусто.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                } else {
                    errorEntries.take(40).forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                ErrorLog.formatTime(entry.atMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                modifier = Modifier.width(88.dp),
                            )
                            Text(
                                entry.kind.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = Teal,
                                modifier = Modifier.width(64.dp),
                            )
                            Text(
                                entry.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f),
                                maxLines = 3,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (errorEntries.isNotEmpty()) Color(0x33E57373) else Surface2)
                        .clickable(enabled = errorEntries.isNotEmpty()) {
                            click.play()
                            onClearErrors()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Очистить журнал",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (errorEntries.isNotEmpty()) Color(0xFFE57373) else TextMuted,
                    )
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

            SettingsSection(icon = Icons.Outlined.DoorFront, title = "Домофон SIP") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    StatusDot(ok = state.sip.registration == SipRegistration.Ok)
                    Text(
                        text = state.sip.registration.title +
                            (state.sip.registrationMessage.takeIf { it.isNotBlank() && state.sip.registration != SipRegistration.Ok }
                                ?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.labelMedium,
                        color = when (state.sip.registration) {
                            SipRegistration.Ok -> Teal
                            SipRegistration.Progress -> Color(0xFFFFB74D)
                            else -> Color(0xFFE57373)
                        },
                        maxLines = 2,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                val sipColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Teal,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedLabelColor = Teal,
                    unfocusedLabelColor = TextMuted,
                    cursorColor = Teal,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                )
                OutlinedTextField(
                    value = sipDomain,
                    onValueChange = { sipDomain = it },
                    label = { Text("SIP domain (Asterisk)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = sipColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sipUser,
                        onValueChange = { sipUser = it },
                        label = { Text("User") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = sipColors,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = sipPass,
                        onValueChange = { sipPass = it },
                        label = { Text("Пароль") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = sipColors,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = doorExt,
                    onValueChange = { doorExt = it },
                    label = { Text("Ext двери") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = sipColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = camHost,
                    onValueChange = { camHost = it },
                    label = { Text("Камера ISAPI host") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = sipColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = camUser,
                        onValueChange = { camUser = it },
                        label = { Text("Cam user") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = sipColors,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = camPass,
                        onValueChange = { camPass = it },
                        label = { Text("Cam pass") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = sipColors,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Teal)
                        .clickable {
                            click.play()
                            onSaveSip(
                                SipConfig(
                                    domain = sipDomain,
                                    username = sipUser,
                                    password = sipPass,
                                    doorExtension = doorExt,
                                    cameraHost = camHost,
                                    cameraUser = camUser,
                                    cameraPassword = camPass,
                                ),
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Сохранить SIP / камеру",
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
private fun ThemeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Teal.copy(alpha = 0.22f) else Surface2)
            .border(
                1.dp,
                if (selected) Teal.copy(alpha = 0.5f) else TextMuted.copy(alpha = 0.18f),
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) TextPrimary else TextMuted,
        )
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
            .border(1.dp, TextMuted.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
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
