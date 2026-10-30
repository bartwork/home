package ru.home.corridorpanel.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    Dark,
    Light,
}

data class PanelPalette(
    val teal: Color,
    val tealDim: Color,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val amber: Color,
    val isDark: Boolean,
)

val DarkPalette = PanelPalette(
    teal = Color(0xFF5EC8D8),
    tealDim = Color(0xFF3EA8BA),
    bg = Color(0xFF0A1018),
    surface = Color(0xFF141C28),
    surface2 = Color(0xFF1C2634),
    textPrimary = Color(0xFFEDF2F8),
    textMuted = Color(0xFF8B9AAB),
    amber = Color(0xFFF0B35A),
    isDark = true,
)

val LightPalette = PanelPalette(
    teal = Color(0xFF0E8A9A),
    tealDim = Color(0xFF0A6F7C),
    bg = Color(0xFFF3F6FA),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFE8EEF5),
    textPrimary = Color(0xFF15202B),
    textMuted = Color(0xFF5A6B7D),
    amber = Color(0xFFC47F1A),
    isDark = false,
)

val LocalPanelPalette = staticCompositionLocalOf { DarkPalette }

/** Совместимые имена — читают текущую палитру. */
val Teal: Color
    @Composable @ReadOnlyComposable get() = LocalPanelPalette.current.teal

val TealDim: Color
    @Composable @ReadOnlyComposable get() = LocalPanelPalette.current.tealDim

val Bg: Color
    @Composable @ReadOnlyComposable get() = LocalPanelPalette.current.bg

val Surface: Color
    @Composable @ReadOnlyComposable get() = LocalPanelPalette.current.surface

val Surface2: Color
    @Composable @ReadOnlyComposable get() = LocalPanelPalette.current.surface2

val TextPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalPanelPalette.current.textPrimary

val TextMuted: Color
    @Composable @ReadOnlyComposable get() = LocalPanelPalette.current.textMuted

val Amber: Color
    @Composable @ReadOnlyComposable get() = LocalPanelPalette.current.amber

@Composable
fun CorridorPanelTheme(
    mode: ThemeMode = ThemeMode.Dark,
    content: @Composable () -> Unit,
) {
    val palette = when (mode) {
        ThemeMode.Dark -> DarkPalette
        ThemeMode.Light -> LightPalette
    }
    val scheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.teal,
            onPrimary = palette.bg,
            secondary = palette.amber,
            background = palette.bg,
            surface = palette.surface,
            surfaceVariant = palette.surface2,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            onSurfaceVariant = palette.textMuted,
            outline = palette.textMuted.copy(alpha = 0.28f),
        )
    } else {
        lightColorScheme(
            primary = palette.teal,
            onPrimary = Color.White,
            secondary = palette.amber,
            background = palette.bg,
            surface = palette.surface,
            surfaceVariant = palette.surface2,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            onSurfaceVariant = palette.textMuted,
            outline = palette.textMuted.copy(alpha = 0.28f),
        )
    }
    CompositionLocalProvider(LocalPanelPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = PanelTypography,
            content = content,
        )
    }
}
