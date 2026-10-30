package ru.home.corridorpanel.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.DoorFront
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.home.corridorpanel.CorridorViewModel
import ru.home.corridorpanel.UiState
import ru.home.corridorpanel.model.Topics
import ru.home.corridorpanel.sip.SipRegistration
import ru.home.corridorpanel.ui.theme.Bg
import ru.home.corridorpanel.ui.theme.Surface
import ru.home.corridorpanel.ui.theme.Surface2
import ru.home.corridorpanel.ui.theme.Teal
import ru.home.corridorpanel.ui.theme.TextMuted
import ru.home.corridorpanel.ui.theme.TextPrimary
import ru.home.corridorpanel.ui.theme.ThemeMode

private enum class HomeTab(
    val title: String,
    val icon: ImageVector,
) {
    Room("Зал", Icons.Outlined.Chair),
    Kitchen("Кухня", Icons.Outlined.Kitchen),
    Hall("Прихожая", Icons.Outlined.MeetingRoom),
    Wet("Санузел", Icons.Outlined.WaterDrop),
    Climate("Климат", Icons.Outlined.Thermostat),
}

/**
 * Центр «Мой дом»: вертикальный nav + дверь + контент зоны.
 */
@Composable
fun CorridorScreen(
    state: UiState,
    vm: CorridorViewModel,
    onBlankScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var settings by remember { mutableStateOf(false) }
    var cameraExpanded by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(HomeTab.Room) }
    val usageCounts by vm.usage.counts.collectAsStateWithLifecycle()
    val usageTop = remember(usageCounts) { vm.usage.top(15) }
    val usageTotal = remember(usageCounts) { vm.usage.totalActions() }
    val errorEntries by vm.errors.entries.collectAsStateWithLifecycle()

    LaunchedEffect(state.sip.isInCall) {
        if (state.sip.isInCall) cameraExpanded = false
    }

    PanelFit(modifier) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Bg,
                                if (state.themeMode == ThemeMode.Dark) Color(0xFF0C1420) else Color(0xFFEAF0F7),
                            ),
                        ),
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HeaderBar(
                    mqttConnected = state.connected,
                    mqttError = state.lastError,
                    sip = state.sip.registration,
                    onOpenSettings = { settings = true },
                    onBlankScreen = {
                        settings = false
                        onBlankScreen()
                    },
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ZoneRail(
                        selected = tab,
                        onSelect = { tab = it },
                        modifier = Modifier
                            .width(92.dp)
                            .fillMaxHeight(),
                    )

                    PanelCard(
                        title = null,
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight(),
                    ) {
                        DoorPanel(
                            sip = state.sip,
                            camera = vm.doorCamera,
                            onCall = { vm.sip.callDoor() },
                            cameraExpanded = cameraExpanded,
                            onExpandCamera = { cameraExpanded = true },
                            values = state.values,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    PanelCard(
                        title = tab.title,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        HomeZoneContent(
                            tab = tab,
                            state = state,
                            vm = vm,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            if (cameraExpanded && !state.sip.isInCall) {
                DoorCameraFullscreen(
                    camera = vm.doorCamera,
                    onClose = { cameraExpanded = false },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (state.sip.isInCall) {
                ActiveCallOverlay(
                    sip = state.sip,
                    camera = vm.doorCamera,
                    onAccept = { vm.sip.accept() },
                    onHangup = { vm.sip.hangup() },
                    onToggleMic = { vm.sip.toggleMicMute() },
                    onToggleSpeaker = { vm.sip.toggleSpeakerMute() },
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f),
                )
            }

            PanelSettingsOverlay(
                visible = settings && !state.sip.isInCall,
                state = state,
                usageTop = usageTop,
                usageTotal = usageTotal,
                errorEntries = errorEntries,
                onDismiss = { settings = false },
                onAutoBrightness = vm::setAutoBrightness,
                onManualBrightness = vm::setManualBrightness,
                onClickSound = vm::setClickSound,
                onThemeMode = vm::setThemeMode,
                onBlankScreen = {
                    settings = false
                    onBlankScreen()
                },
                onSaveBroker = vm::saveBroker,
                onReconnect = vm::reconnect,
                onSaveSip = vm::saveSip,
                onClearUsage = vm::clearUsageStats,
                onClearErrors = vm::clearErrorLog,
            )
        }
    }
}

@Composable
private fun HomeZoneContent(
    tab: HomeTab,
    state: UiState,
    vm: CorridorViewModel,
    modifier: Modifier = Modifier,
) {
    when (tab) {
        HomeTab.Room -> LightsPanel(
            values = state.values,
            dimmers = Topics.roomDimmers,
            switches = emptyList(),
            onToggleDimmer = vm::toggleDimmer,
            onLevel = vm::setDimmerLevel,
            onToggleSwitch = vm::toggleSwitch,
            onPublish = vm::publish,
            showLed = true,
            modifier = modifier,
        )
        HomeTab.Kitchen -> LightsPanel(
            values = state.values,
            dimmers = Topics.kitchenDimmers,
            switches = Topics.kitchenSwitches,
            onToggleDimmer = vm::toggleDimmer,
            onLevel = vm::setDimmerLevel,
            onToggleSwitch = vm::toggleSwitch,
            onPublish = vm::publish,
            groupLabel = "Выключатели",
            modifier = modifier,
        )
        HomeTab.Hall -> LightsPanel(
            values = state.values,
            dimmers = Topics.hallDimmers,
            masterSwitches = Topics.masterSwitches,
            switches = Topics.storageSwitches,
            onToggleDimmer = vm::toggleDimmer,
            onLevel = vm::setDimmerLevel,
            onToggleSwitch = vm::toggleSwitch,
            onPublish = vm::publish,
            groupLabel = "Кладовка",
            modifier = modifier,
        )
        HomeTab.Wet -> WetRoomsPanel(
            values = state.values,
            onToggleSwitch = vm::toggleSwitch,
            modifier = modifier,
        )
        HomeTab.Climate -> Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Кондиционеры", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            HomeClimatePanel(
                values = state.values,
                publish = vm::publish,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Тёплый пол",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp),
            )
            FloorPanel(
                values = state.values,
                publish = vm::publish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
            )
        }
    }
}

@Composable
private fun ZoneRail(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val click = LocalClickSound.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Teal.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.DoorFront, null, tint = Teal, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(4.dp))
        HomeTab.entries.forEach { t ->
            val on = t == selected
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (on) Teal.copy(alpha = 0.22f) else Color.Transparent)
                    .border(
                        width = if (on) 1.dp else 0.dp,
                        color = if (on) Teal.copy(alpha = 0.45f) else Color.Transparent,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable {
                        click.play()
                        onSelect(t)
                    }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    t.icon,
                    contentDescription = t.title,
                    tint = if (on) Teal else TextMuted,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = t.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (on) TextPrimary else TextMuted,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun PanelCard(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .border(1.dp, TextMuted.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            content = content,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeaderBar(
    mqttConnected: Boolean,
    mqttError: String?,
    sip: SipRegistration,
    onOpenSettings: () -> Unit,
    onBlankScreen: () -> Unit,
) {
    val click = LocalClickSound.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .border(1.dp, TextMuted.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { click.play() },
                onDoubleClick = {
                    click.play()
                    onOpenSettings()
                },
                onLongClick = {
                    click.play()
                    onBlankScreen()
                },
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = Topics.DISPLAY_NAME,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            Text(
                text = "двойной тап — настройки · удержание — экран",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            StatusDot(
                online = sip == SipRegistration.Ok,
                warn = sip == SipRegistration.Progress,
                label = sip.title,
            )
            StatusDot(
                online = mqttConnected,
                label = if (mqttConnected) "MQTT" else (mqttError?.take(18) ?: "MQTT"),
            )
        }
    }
}

@Composable
private fun StatusDot(
    online: Boolean,
    label: String,
    warn: Boolean = false,
) {
    val color = when {
        online -> Teal
        warn -> Color(0xFFFFB74D)
        else -> Color(0xFFE57373)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
