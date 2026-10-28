package ru.home.roompanel.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Teal = Color(0xFF5EC8D8)
val TealDim = Color(0xFF3EA8BA)
val Bg = Color(0xFF0A1018)
val Surface = Color(0xFF141C28)
val Surface2 = Color(0xFF1C2634)
val TextPrimary = Color(0xFFEDF2F8)
val TextMuted = Color(0xFF8B9AAB)
val Amber = Color(0xFFF0B35A)

private val scheme = darkColorScheme(
    primary = Teal,
    onPrimary = Bg,
    secondary = Amber,
    background = Bg,
    surface = Surface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextMuted,
    outline = Color(0x28E8EEF6),
)

@Composable
fun RoomPanelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = scheme,
        typography = PanelTypography,
        content = content,
    )
}
