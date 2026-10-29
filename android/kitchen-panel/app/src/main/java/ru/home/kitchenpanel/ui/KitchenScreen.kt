package ru.home.kitchenpanel.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.dp
import ru.home.kitchenpanel.KitchenViewModel
import ru.home.kitchenpanel.UiState
import ru.home.kitchenpanel.model.climateState
import ru.home.kitchenpanel.model.floorState
import ru.home.kitchenpanel.ui.theme.Bg
import ru.home.kitchenpanel.ui.theme.Surface
import ru.home.kitchenpanel.ui.theme.Teal
import ru.home.kitchenpanel.ui.theme.TextMuted
import ru.home.kitchenpanel.ui.theme.tabActive
import ru.home.kitchenpanel.ui.theme.tabIdle

private val tabs = listOf("Свет", "Пол", "Климат")

@Composable
fun KitchenScreen(
    state: UiState,
    vm: KitchenViewModel,
    onBlankScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableIntStateOf(0) }
    var settings by remember { mutableStateOf(false) }

    PanelFit(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
        ) {
            SegmentTabs(
                tabs = tabs,
                selected = tab,
                connected = state.connected,
                onSelect = { tab = it },
                onOpenSettings = { settings = true },
                onBlankScreen = {
                    settings = false
                    onBlankScreen()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            )

            if (!state.connected) {
                Text(
                    text = state.lastError?.let { "MQTT: $it" }
                        ?: "Нет MQTT · двойной тап по активной вкладке",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE57373),
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp),
                    maxLines = 1,
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            ) {
                when (tab) {
                    0 -> LightsPanel(
                        values = state.values,
                        onToggleDimmer = vm::toggleDimmer,
                        onLevel = vm::setDimmerLevel,
                        onToggleSwitch = vm::toggleSwitch,
                        modifier = Modifier.fillMaxSize(),
                    )
                    1 -> FloorPanel(
                        state = state.values.floorState(),
                        publish = vm::publish,
                        modifier = Modifier.fillMaxSize(),
                    )
                    else -> ClimatePanel(
                        state = state.values.climateState(),
                        publish = vm::publish,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        PanelSettingsOverlay(
            visible = settings,
            state = state,
            onDismiss = { settings = false },
            onAutoBrightness = vm::setAutoBrightness,
            onManualBrightness = vm::setManualBrightness,
            onClickSound = vm::setClickSound,
            onBlankScreen = {
                settings = false
                onBlankScreen()
            },
            onSaveBroker = vm::saveBroker,
            onReconnect = vm::reconnect,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SegmentTabs(
    tabs: List<String>,
    selected: Int,
    connected: Boolean,
    onSelect: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onBlankScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val click = LocalClickSound.current
    val baseConfig = LocalViewConfiguration.current
    val longPressConfig = remember(baseConfig) {
        object : ViewConfiguration by baseConfig {
            override val longPressTimeoutMillis: Long
                get() = baseConfig.longPressTimeoutMillis * 2
        }
    }
    CompositionLocalProvider(LocalViewConfiguration provides longPressConfig) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
                .padding(4.dp),
        ) {
            tabs.forEachIndexed { i, title ->
                val active = selected == i
                val bg by animateColorAsState(
                    targetValue = when {
                        active && connected -> Teal
                        active -> Teal.copy(alpha = 0.45f)
                        else -> Color.Transparent
                    },
                    label = "tabBg",
                )
                val fg by animateColorAsState(
                    if (active) Bg else TextMuted,
                    label = "tabFg",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .then(
                            if (active) {
                                // тап · двойной → настройки · удержание → экран выкл
                                Modifier.combinedClickable(
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
                            } else {
                                Modifier.combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        click.play()
                                        onSelect(i)
                                    },
                                    onLongClick = {
                                        click.play()
                                        onBlankScreen()
                                    },
                                )
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = title,
                        style = if (active) MaterialTheme.typography.tabActive
                        else MaterialTheme.typography.tabIdle,
                        color = fg,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
