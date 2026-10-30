package ru.home.corridorpanel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import ru.home.corridorpanel.ui.theme.Bg
import kotlin.math.min

/**
 * Логический холст YC-SM10P: landscape 1280×800 (10.1″ IPS).
 * В отличие от квадратных SM08P / YC-SM41P (480×480).
 */
private const val DESIGN_W = 1280f
private const val DESIGN_H = 800f

@Composable
fun PanelFit(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Bg),
        contentAlignment = Alignment.Center,
    ) {
        val w = constraints.maxWidth.coerceAtLeast(1).toFloat()
        val h = constraints.maxHeight.coerceAtLeast(1).toFloat()
        val scale = min(w / DESIGN_W, h / DESIGN_H)
        val panelDensity = Density(density = scale, fontScale = 1f)
        CompositionLocalProvider(LocalDensity provides panelDensity) {
            Box(
                Modifier.size(width = DESIGN_W.dp, height = DESIGN_H.dp),
                propagateMinConstraints = true,
            ) {
                content()
            }
        }
    }
}
