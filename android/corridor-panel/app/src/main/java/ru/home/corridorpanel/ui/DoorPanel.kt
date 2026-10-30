package ru.home.corridorpanel.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.home.corridorpanel.sip.DoorCameraFeed
import ru.home.corridorpanel.sip.DoorCameraFrame
import ru.home.corridorpanel.sip.DoorCameraMode
import ru.home.corridorpanel.sip.SipRegistration
import ru.home.corridorpanel.sip.SipUiState
import ru.home.corridorpanel.ui.theme.Surface2
import ru.home.corridorpanel.ui.theme.Teal
import ru.home.corridorpanel.ui.theme.TextMuted
import ru.home.corridorpanel.ui.theme.TextPrimary

@Composable
fun DoorPanel(
    sip: SipUiState,
    camera: DoorCameraFeed,
    onCall: () -> Unit,
    modifier: Modifier = Modifier,
    cameraExpanded: Boolean = false,
    onExpandCamera: () -> Unit = {},
    values: Map<String, String> = emptyMap(),
) {
    val muted by camera.audioMuted.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (cameraExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .border(1.dp, Surface2, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "на весь экран",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.35f),
                )
            }
        } else {
            DoorPreview(
                camera = camera,
                compact = true,
                expandable = true,
                onExpand = onExpandCamera,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f),
            )
        }

        CallAndMuteRow(
            callEnabled = sip.registration == SipRegistration.Ok && !sip.isInCall,
            muted = muted,
            onCall = onCall,
            onToggleMute = camera::toggleAudioMute,
        )

        OutdoorWeatherRow(values = values)

        sip.lastError?.takeIf { it.isNotBlank() }?.let { err ->
            Text(
                text = err,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE57373),
                maxLines = 2,
            )
        }
    }
}

/**
 * Превью двери: RTSP или JPEG.
 * [compact]=true → Crop (без полос); false → Fit.
 */
@Composable
fun DoorPreview(
    camera: DoorCameraFeed,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    expandable: Boolean = false,
    onExpand: () -> Unit = {},
) {
    val mode by camera.mode.collectAsStateWithLifecycle()
    val frame by camera.frame.collectAsStateWithLifecycle()
    val muted by camera.audioMuted.collectAsStateWithLifecycle()
    val radius = if (compact) 12.dp else 14.dp
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(Color.Black)
            .border(1.dp, Surface2, RoundedCornerShape(radius))
            .then(
                if (expandable) Modifier.clickable(onClick = onExpand) else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (val m = mode) {
            is DoorCameraMode.Rtsp -> {
                DoorRtspPlayer(
                    url = m.url,
                    compact = compact,
                    muted = muted,
                    onFail = { camera.fallbackToJpeg("RTSP недоступен") },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            DoorCameraMode.Jpeg -> DoorJpegContent(frame = frame, compact = compact)
            DoorCameraMode.Idle -> ProgressTeal()
        }
    }
}

@Composable
private fun CallAndMuteRow(
    callEnabled: Boolean,
    muted: Boolean,
    onCall: () -> Unit,
    onToggleMute: () -> Unit,
) {
    val click = LocalClickSound.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(if (callEnabled) Teal.copy(alpha = 0.28f) else Surface2)
                .border(
                    1.dp,
                    if (callEnabled) Teal.copy(alpha = 0.55f) else Color.Transparent,
                    RoundedCornerShape(14.dp),
                )
                .clickable(enabled = callEnabled) {
                    click.play()
                    onCall()
                }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Outlined.Phone,
                contentDescription = null,
                tint = if (callEnabled) Teal else TextMuted,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = "Позвонить",
                style = MaterialTheme.typography.titleMedium,
                color = if (callEnabled) TextPrimary else TextMuted,
            )
        }

        Box(
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(if (!muted) Teal.copy(alpha = 0.28f) else Surface2)
                .border(
                    1.dp,
                    if (!muted) Teal.copy(alpha = 0.55f) else Surface2,
                    RoundedCornerShape(14.dp),
                )
                .clickable {
                    click.play()
                    onToggleMute()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (muted) {
                    Icons.AutoMirrored.Outlined.VolumeOff
                } else {
                    Icons.AutoMirrored.Outlined.VolumeUp
                },
                contentDescription = if (muted) "Включить звук" else "Выключить звук",
                tint = if (!muted) Teal else TextMuted,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun DoorJpegContent(frame: DoorCameraFrame, compact: Boolean) {
    val bmp = frame.bitmap
    if (bmp != null && !bmp.isRecycled) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Камера двери",
            modifier = Modifier.fillMaxSize(),
            contentScale = if (compact) ContentScale.Crop else ContentScale.Fit,
        )
    } else {
        ProgressTeal()
    }
}

@Composable
private fun ProgressTeal() {
    CircularProgressIndicator(
        modifier = Modifier.size(24.dp),
        color = Teal,
        strokeWidth = 2.dp,
    )
}
