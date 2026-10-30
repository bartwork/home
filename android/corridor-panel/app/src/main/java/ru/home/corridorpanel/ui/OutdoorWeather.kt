package ru.home.corridorpanel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.home.corridorpanel.model.weatherState
import ru.home.corridorpanel.ui.theme.Surface2
import ru.home.corridorpanel.ui.theme.Teal
import ru.home.corridorpanel.ui.theme.TextMuted
import ru.home.corridorpanel.ui.theme.TextPrimary
import java.util.Locale

/** Zigbee у наружного блока кондея — погода на улице. */
@Composable
fun OutdoorWeatherRow(
    values: Map<String, String>,
    modifier: Modifier = Modifier,
) {
    val w = values.weatherState()
    val tempText = w.temp?.let { String.format(Locale.US, "%.1f°", it) } ?: "—"
    val humText = w.humidity?.let { "${it.toInt()}%" } ?: "—"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface2)
            .border(
                1.dp,
                if (w.temp != null) Teal.copy(alpha = 0.28f) else Color.Transparent,
                RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Улица",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
        )
        Icon(Icons.Outlined.Thermostat, contentDescription = null, tint = Teal)
        Text(
            text = tempText,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = TextMuted)
        Text(
            text = humText,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
        )
    }
}
