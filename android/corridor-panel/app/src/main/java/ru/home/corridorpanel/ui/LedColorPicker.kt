package ru.home.corridorpanel.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.home.corridorpanel.model.hsvToColor
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/** Цветовой круг: hue по углу, saturation по радиусу. Тап вне круга — закрытие. */
@Composable
fun LedColorPickerDialog(
    hue: Int,
    saturation: Int,
    brightness: Int,
    onChange: (hue: Int, saturation: Int, brightness: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var h by remember { mutableIntStateOf(hue) }
    var s by remember { mutableIntStateOf(saturation) }
    val v = brightness.coerceIn(1, 100)
    val onChangeState = rememberUpdatedState(onChange)
    val onDismissState = rememberUpdatedState(onDismiss)

    fun emitHs(nh: Int, ns: Int) {
        h = nh.coerceIn(0, 360)
        s = ns.coerceIn(0, 100)
        onChangeState.value(h, s, v)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .pointerInput(Unit) {
                    detectTapGestures { onDismissState.value() }
                },
            contentAlignment = Alignment.Center,
        ) {
            ColorWheel(
                hue = h,
                saturation = s,
                value = v,
                onHsChange = ::emitHs,
                onTapOutsideWheel = { onDismissState.value() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp)
                    .aspectRatio(1f),
            )
        }
    }
}

@Composable
private fun ColorWheel(
    hue: Int,
    saturation: Int,
    value: Int,
    onHsChange: (hue: Int, saturation: Int) -> Unit,
    onTapOutsideWheel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onHs = rememberUpdatedState(onHsChange)
    val onOutside = rememberUpdatedState(onTapOutsideWheel)
    val hueState = rememberUpdatedState(hue)
    val satState = rememberUpdatedState(saturation)
    val valState = rememberUpdatedState(value)

    val hueColors = remember {
        listOf(
            Color.Red, Color.Yellow, Color.Green, Color.Cyan,
            Color.Blue, Color.Magenta, Color.Red,
        )
    }

    fun pick(pos: Offset, side: Int): Boolean {
        if (side <= 0) return false
        val cx = side / 2f
        val cy = side / 2f
        val dx = pos.x - cx
        val dy = pos.y - cy
        val rMax = minOf(cx, cy) * 0.98f
        val dist = hypot(dx, dy)
        if (dist > rMax) return false
        var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        deg = (deg + 90f + 360f) % 360f
        val sat = ((dist / rMax) * 100f).roundToInt().coerceIn(0, 100)
        onHs.value(deg.roundToInt().coerceIn(0, 360), sat)
        return true
    }

    Canvas(
        modifier = modifier
            .shadow(14.dp, CircleShape)
            .clip(CircleShape)
            .border(1.5.dp, Color.White.copy(alpha = 0.14f), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    val side = size.width.coerceAtMost(size.height)
                    if (!pick(pos, side)) onOutside.value()
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { pos ->
                        val side = size.width.coerceAtMost(size.height)
                        pick(pos, side)
                    },
                ) { change, _ ->
                    change.consume()
                    val side = size.width.coerceAtMost(size.height)
                    pick(change.position, side)
                }
            },
    ) {
        val r = size.minDimension / 2f
        rotate(-90f) {
            drawCircle(brush = Brush.sweepGradient(hueColors))
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color.Transparent),
                center = center,
                radius = r,
            ),
        )
        val vv = valState.value
        if (vv < 100) {
            drawCircle(Color.Black.copy(alpha = (100 - vv) / 100f * 0.45f))
        }

        val hh = hueState.value
        val ss = satState.value
        val rad = (hh - 90f) * PI / 180.0
        val dist = ss / 100f * r * 0.96f
        val tx = center.x + (cos(rad) * dist).toFloat()
        val ty = center.y + (sin(rad) * dist).toFloat()
        val fill = Color(hsvToColor(hh, ss, vv.coerceAtLeast(22)))
        drawCircle(Color.Black.copy(alpha = 0.22f), radius = 13f, center = Offset(tx, ty + 1f))
        drawCircle(Color.White, radius = 11f, center = Offset(tx, ty))
        drawCircle(fill, radius = 8f, center = Offset(tx, ty))
    }
}
