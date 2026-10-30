package ru.home.corridorpanel

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Мягкий UI-клик через AudioTrack → встроенный динамик.
 * При выключении трек освобождается (нативка не висит).
 */
class ClickFeedback(context: Context) {
    private val appContext = context.applicationContext
    private val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val lock = Any()
    private val samples = SoftClick.samples

    @Volatile private var enabled = false
    private var track: AudioTrack? = null
    private var speaker: AudioDeviceInfo? = null
    private var speakerphoneForced = false

    fun setEnabled(value: Boolean) {
        synchronized(lock) {
            enabled = value
            if (value) {
                ensureMusicVolumeLocked()
                ensureTrackLocked()
            } else {
                releaseTrackLocked()
                restoreSpeakerphoneLocked()
            }
        }
    }

    fun play() {
        if (!enabled) return
        synchronized(lock) {
            try {
                ensureMusicVolumeLocked()
                val t = ensureTrackLocked() ?: return
                routeToSpeakerLocked(t)
                if (t.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    t.stop()
                    t.flush()
                }
                t.reloadStaticData()
                t.play()
            } catch (e: Exception) {
                Log.w(TAG, "play failed", e)
                releaseTrackLocked()
            }
        }
    }

    fun release() {
        synchronized(lock) {
            enabled = false
            releaseTrackLocked()
            restoreSpeakerphoneLocked()
        }
    }

    private fun ensureTrackLocked(): AudioTrack? {
        track?.let { return it }
        val bytes = samples.size * 2
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(SoftClick.SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        return try {
            val minBuf = AudioTrack.getMinBufferSize(
                SoftClick.SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(format)
                .setBufferSizeInBytes(maxOf(bytes, minBuf))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
                .also { at ->
                    at.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
                    at.setVolume(0.72f)
                    routeToSpeakerLocked(at)
                    track = at
                }
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack init failed", e)
            null
        }
    }

    private fun routeToSpeakerLocked(track: AudioTrack) {
        try {
            val sp = speaker ?: audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                ?.also { speaker = it }
            if (sp != null) track.setPreferredDevice(sp)
            if (!speakerphoneForced) {
                @Suppress("DEPRECATION")
                audio.isSpeakerphoneOn = true
                speakerphoneForced = true
            }
        } catch (_: Exception) {
        }
    }

    private fun restoreSpeakerphoneLocked() {
        if (!speakerphoneForced) return
        try {
            @Suppress("DEPRECATION")
            audio.isSpeakerphoneOn = false
        } catch (_: Exception) {
        }
        speakerphoneForced = false
    }

    private fun ensureMusicVolumeLocked() {
        try {
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val cur = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            val minWanted = (max * 0.55f).toInt().coerceAtLeast(4)
            if (max > 0 && cur < minWanted) {
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, minWanted, 0)
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseTrackLocked() {
        try {
            track?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) it.stop()
                it.release()
            }
        } catch (_: Exception) {
        }
        track = null
    }

    companion object {
        private const val TAG = "ClickFeedback"
    }
}

/**
 * Короткий «стеклянный» UI-клик в духе macOS Aqua/Tink:
 * яркий тик ~35 мс, без басового «булька».
 */
private object SoftClick {
    const val SAMPLE_RATE = 44_100
    val samples: ShortArray = synthesize()

    private fun synthesize(): ShortArray {
        val n = (SAMPLE_RATE * 0.038).toInt()
        val out = ShortArray(n)
        var noiseState = 0xC0FF_EE42
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            // почти мгновенная атака, как у Mac
            val attack = if (t < 0.00035) (t / 0.00035) else 1.0
            val tickEnv = attack * exp(-t * 160.0)
            val ringEnv = attack * exp(-t * 78.0)
            val airEnv = attack * exp(-t * 220.0)

            // основной тон ~2.4 кГц слышен на планшетном динамике
            val fund = sin(2.0 * PI * 2420.0 * t) * 0.62 * ringEnv
            val glass = sin(2.0 * PI * 4850.0 * t + 0.4) * 0.28 * tickEnv
            val air = sin(2.0 * PI * 7100.0 * t) * 0.12 * airEnv
            // чуть «дерева» в середине — иначе на мелком динамике слишком тонко
            val wood = sin(2.0 * PI * 1180.0 * t + 0.15) * 0.14 * ringEnv

            noiseState = noiseState * 1103515245 + 12345
            val noise = ((noiseState ushr 16) / 32768.0 - 1.0) * 0.09 * airEnv

            var sample = fund + glass + air + wood + noise
            // мягкий soft-clip без жёсткого клиппинга
            sample *= 0.9
            sample /= sqrt(1.0 + sample * sample * 0.85)
            // хвост в ноль
            val fade = 0.5 * (1.0 + cos(PI * (i.toDouble() / n)))
            sample *= 0.25 + 0.75 * fade

            out[i] = (sample * Short.MAX_VALUE * 0.55).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return out
    }
}
