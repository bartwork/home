package ru.home.corridorpanel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.home.corridorpanel.model.Topics

/** Санузел: две колонки (ванна | туалет) под landscape SM10P. */
@Composable
fun WetRoomsPanel(
    values: Map<String, String>,
    onToggleSwitch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LightsPanel(
            values = values,
            dimmers = emptyList(),
            switches = Topics.bathSwitches,
            onToggleDimmer = {},
            onLevel = { _, _ -> },
            onToggleSwitch = onToggleSwitch,
            groupLabel = "Ванна",
            modifier = Modifier.weight(1f),
        )
        LightsPanel(
            values = values,
            dimmers = emptyList(),
            switches = Topics.toiletSwitches,
            onToggleDimmer = {},
            onLevel = { _, _ -> },
            onToggleSwitch = onToggleSwitch,
            groupLabel = "Туалет",
            modifier = Modifier.weight(1f),
        )
    }
}
