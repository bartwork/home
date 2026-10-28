package ru.home.roompanel.ui

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
import ru.home.roompanel.ui.theme.Bg
import kotlin.math.min

/** Логический квадрат панели YC-SM41P: 480×480 dp ≈ вся короткая сторона экрана. */
private const val DESIGN_SIDE = 480f

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
        val sidePx = min(constraints.maxWidth, constraints.maxHeight).coerceAtLeast(1).toFloat()
        val panelDensity = Density(density = sidePx / DESIGN_SIDE, fontScale = 1f)
        CompositionLocalProvider(LocalDensity provides panelDensity) {
            Box(Modifier.size(DESIGN_SIDE.dp), propagateMinConstraints = true) {
                content()
            }
        }
    }
}
