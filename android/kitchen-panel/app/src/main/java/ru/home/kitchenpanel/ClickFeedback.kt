package ru.home.kitchenpanel

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
                    at.setVolume(0.85f)
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

/** Синтез один раз на процесс. */
private object SoftClick {
    const val SAMPLE_RATE = 44_100
    val samples: ShortArray = synthesize()

    private fun synthesize(): ShortArray {
        val n = (SAMPLE_RATE * 0.065).toInt()
        val out = ShortArray(n)
        // детерминированный «шум» без Random на каждый sample-аллок
        var noiseState = 0xA5A5_1234
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val attack = if (t < 0.0015) (t / 0.0015) else 1.0
            val bodyEnv = attack * exp(-t * 42.0)
            val sparkleEnv = attack * exp(-t * 95.0)

            val body = sin(2.0 * PI * 620.0 * t) * 0.55
            val mid = sin(2.0 * PI * 1240.0 * t + 0.3) * 0.28
            val sparkleFreq = 3200.0 - t * 1800.0
            val sparkle = sin(2.0 * PI * sparkleFreq * t) * 0.22 * sparkleEnv

            noiseState = noiseState * 1103515245 + 12345
            val noise = ((noiseState ushr 16) / 32768.0 - 1.0) * 0.04 * sparkleEnv

            var sample = (body + mid) * bodyEnv + sparkle + noise
            sample *= 0.72
            sample /= sqrt(1.0 + sample * sample)
            val fade = 0.5 * (1.0 + cos(PI * (i.toDouble() / n)))
            sample *= 0.35 + 0.65 * fade

            out[i] = (sample * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return out
    }
}
