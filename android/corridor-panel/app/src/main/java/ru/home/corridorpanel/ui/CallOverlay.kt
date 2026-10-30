package ru.home.corridorpanel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ru.home.corridorpanel.sip.DoorCameraFeed
import ru.home.corridorpanel.sip.SipCallPhase
import ru.home.corridorpanel.sip.SipUiState
import ru.home.corridorpanel.ui.theme.Teal
import ru.home.corridorpanel.ui.theme.TextMuted
import ru.home.corridorpanel.ui.theme.TextPrimary

/** Полноэкранный звонок: звук SIP + видео двери (RTSP / JPEG fallback). */
@Composable
fun ActiveCallOverlay(
    sip: SipUiState,
    camera: DoorCameraFeed,
    onAccept: () -> Unit,
    onHangup: () -> Unit,
    onToggleMic: () -> Unit,
    onToggleSpeaker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        DoorPreview(
            camera = camera,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(phase = sip.callPhase)
                if (sip.remoteDisplay.isNotBlank()) {
                    Text(
                        text = sip.remoteDisplay,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        maxLines = 1,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                        ),
                    )
                    .padding(top = 28.dp, bottom = 8.dp),
            ) {
                if (sip.callPhase == SipCallPhase.Incoming) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        CallControl(
                            icon = Icons.Outlined.CallEnd,
                            label = "Отклонить",
                            tint = Color(0xFFE57373),
                            onClick = onHangup,
                        )
                        CallControl(
                            icon = Icons.Outlined.Call,
                            label = "Принять",
                            tint = Teal,
                            onClick = onAccept,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        CallControl(
                            icon = if (sip.isSpeakerMuted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                            label = if (sip.isSpeakerMuted) "Звук выкл" else "Звук",
                            tint = if (sip.isSpeakerMuted) TextMuted else TextPrimary,
                            onClick = onToggleSpeaker,
                        )
                        CallControl(
                            icon = if (sip.isMicMuted) Icons.Outlined.MicOff else Icons.Outlined.Mic,
                            label = if (sip.isMicMuted) "Мик. выкл" else "Микрофон",
                            tint = if (sip.isMicMuted) TextMuted else TextPrimary,
                            onClick = onToggleMic,
                        )
                        CallControl(
                            icon = Icons.Outlined.CallEnd,
                            label = "Завершить",
                            tint = Color(0xFFE57373),
                            onClick = onHangup,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(phase: SipCallPhase) {
    val (title, color) = when (phase) {
        SipCallPhase.Incoming -> "Входящий" to Teal
        SipCallPhase.Outgoing -> "Вызов…" to Color(0xFFFFB74D)
        SipCallPhase.Connected -> "Разговор" to Teal
        SipCallPhase.Idle -> "" to TextMuted
    }
    if (title.isEmpty()) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

@Composable
private fun CallControl(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    val click = LocalClickSound.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable {
            click.play()
            onClick()
        },
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, tint.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(28.dp), tint = tint)
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}
