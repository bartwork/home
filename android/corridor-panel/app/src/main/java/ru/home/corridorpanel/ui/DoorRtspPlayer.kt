package ru.home.corridorpanel.ui

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

/** Живой RTSP (TCP) через Media3; при ошибке/таймауте — [onFail]. */
@Composable
fun DoorRtspPlayer(
    url: String,
    compact: Boolean,
    muted: Boolean,
    onFail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val fail by rememberUpdatedState(onFail)
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            volume = if (muted) 0f else 1f
            playWhenReady = true
            val source = RtspMediaSource.Factory()
                .setForceUseRtpTcp(true)
                .setTimeoutMs(5_000)
                .createMediaSource(MediaItem.fromUri(url))
            setMediaSource(source)
            prepare()
        }
    }

    LaunchedEffect(muted, player) {
        player.volume = if (muted) 0f else 1f
    }

    DisposableEffect(player) {
        var failed = false
        fun failOnce() {
            if (failed) return
            failed = true
            fail()
        }
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                failOnce()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) failed = false
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(url, player) {
        delay(8_000)
        if (player.playbackState != Player.STATE_READY) {
            fail()
        }
    }

    val resize = if (compact) {
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    } else {
        AspectRatioFrameLayout.RESIZE_MODE_FIT
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                this.player = player
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                resizeMode = resize
            }
        },
        update = { view ->
            view.player = player
            view.resizeMode = resize
        },
        modifier = modifier,
    )
}
