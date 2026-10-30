package ru.home.corridorpanel.sip

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.home.corridorpanel.model.Topics
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

/** Режим превью двери: живой RTSP или JPEG-снимки (fallback). */
sealed class DoorCameraMode {
    data object Idle : DoorCameraMode()
    data class Rtsp(val url: String) : DoorCameraMode()
    data object Jpeg : DoorCameraMode()
}

data class DoorCameraFrame(
    val bitmap: Bitmap? = null,
    val error: String? = null,
)

/**
 * Превью двери: RTSP субпоток предпочтительно, ISAPI JPEG — fallback.
 * Один источник на панель + оверлей звонка.
 */
class DoorCameraFeed {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _mode = MutableStateFlow<DoorCameraMode>(DoorCameraMode.Idle)
    val mode: StateFlow<DoorCameraMode> = _mode.asStateFlow()

    private val _frame = MutableStateFlow(DoorCameraFrame())
    val frame: StateFlow<DoorCameraFrame> = _frame.asStateFlow()

    /** Звук RTSP по умолчанию выкл (панель всегда на экране). */
    private val _audioMuted = MutableStateFlow(true)
    val audioMuted: StateFlow<Boolean> = _audioMuted.asStateFlow()

    private var jpegJob: Job? = null
    private var activeKey: String? = null
    private var host = ""
    private var username = ""
    private var password = ""
    private var forcedJpeg = false
    private var failStreak = 0

    /** Ошибки камеры → журнал. */
    var onError: ((String) -> Unit)? = null

    fun start(config: SipConfig) {
        val cfg = config.normalized()
        val key = "${cfg.cameraHost}\u0000${cfg.cameraUser}\u0000${cfg.cameraPassword}"
        if (key == activeKey && _mode.value !is DoorCameraMode.Idle) return

        stopJpeg()
        activeKey = key
        host = cfg.cameraHost
        username = cfg.cameraUser
        password = cfg.cameraPassword
        forcedJpeg = false
        _frame.value = DoorCameraFrame()
        startPreferred()
    }

    fun toggleAudioMute() {
        _audioMuted.value = !_audioMuted.value
    }

    fun fallbackToJpeg(reason: String) {
        if (forcedJpeg) return
        if (_mode.value !is DoorCameraMode.Rtsp) return
        forcedJpeg = true
        reportError(reason)
        if (host.isBlank()) return
        startJpeg()
    }

    fun stop(clear: Boolean = true) {
        stopJpeg()
        activeKey = null
        forcedJpeg = false
        if (clear) {
            host = ""
            username = ""
            password = ""
            _mode.value = DoorCameraMode.Idle
            _frame.value = DoorCameraFrame()
        }
    }

    fun shutdown() {
        stop(clear = true)
        scope.cancel()
    }

    private fun startPreferred() {
        if (Topics.Camera.PREFER_RTSP && !forcedJpeg) {
            val url = Topics.Camera.rtspUrl(
                host = host,
                user = username,
                password = password,
                substream = true,
            )
            _mode.value = DoorCameraMode.Rtsp(url)
            return
        }
        startJpeg()
    }

    private fun startJpeg() {
        stopJpeg()
        failStreak = 0
        _mode.value = DoorCameraMode.Jpeg
        val client = DoorJpegClient(
            host = host,
            username = username,
            password = password,
            channels = listOf(Topics.Camera.JPEG_CHANNEL_MAIN, Topics.Camera.JPEG_CHANNEL_SUB),
        )
        jpegJob = scope.launch {
            while (isActive) {
                try {
                    when (val result = client.fetch()) {
                        is DoorFetch.Ok -> {
                            failStreak = 0
                            _frame.value = DoorCameraFrame(bitmap = result.bitmap, error = null)
                        }
                        is DoorFetch.HttpError -> noteFail("камера: HTTP ${result.code}")
                        is DoorFetch.Fail -> noteFail(result.message)
                    }
                } catch (e: Exception) {
                    if (!isActive) return@launch
                    noteFail(e.message ?: e.javaClass.simpleName)
                }
                delay(INTERVAL_MS)
            }
        }
    }

    private fun noteFail(message: String) {
        failStreak += 1
        if (failStreak >= 3) {
            reportError(message)
        }
    }

    private fun reportError(message: String) {
        val msg = message.trim()
        if (msg.isEmpty()) return
        _frame.value = _frame.value.copy(error = msg)
        onError?.invoke(msg)
    }

    private fun stopJpeg() {
        jpegJob?.cancel()
        jpegJob = null
    }

    companion object {
        private const val INTERVAL_MS = 400L
    }
}

private sealed class DoorFetch {
    data class Ok(val bitmap: Bitmap) : DoorFetch()
    data class HttpError(val code: Int) : DoorFetch()
    data class Fail(val message: String) : DoorFetch()
}

/** ISAPI JPEG с Digest; пробует несколько каналов при 404. */
private class DoorJpegClient(
    host: String,
    private val username: String,
    private val password: String,
    channels: List<Int>,
) {
    private val urls = channels.map { ch ->
        URL("http://${host.trim()}/ISAPI/Streaming/channels/$ch/picture")
    }
    private var urlIndex = 0
    private val cachedAuth = AtomicReference<String?>(null)

    @Volatile
    private var lastWwwAuthenticate: String? = null

    fun fetch(): DoorFetch {
        if (urls.isEmpty()) return DoorFetch.Fail("нет URL камеры")
        val url = urls[urlIndex % urls.size]

        cachedAuth.get()?.let { auth ->
            val (code, bytes) = request(url, auth)
            when {
                code == 200 && bytes != null -> {
                    val bmp = decode(bytes) ?: return DoorFetch.Fail("пустой кадр")
                    return DoorFetch.Ok(bmp)
                }
                code == 401 -> cachedAuth.set(null)
                else -> {
                    if (code == 404) advanceChannel()
                    return DoorFetch.HttpError(code)
                }
            }
        }

        val (code1, body1) = request(url, null)
        if (code1 == 200 && body1 != null) {
            val bmp = decode(body1) ?: return DoorFetch.Fail("пустой кадр")
            return DoorFetch.Ok(bmp)
        }
        if (code1 != 401) {
            if (code1 == 404) advanceChannel()
            return DoorFetch.HttpError(code1)
        }

        val www = lastWwwAuthenticate.orEmpty()
        val auth = when {
            www.startsWith("Digest", ignoreCase = true) -> digestHeader(www, url)
            else -> basicHeader()
        }
        cachedAuth.set(auth)
        val (code2, body2) = request(url, auth)
        if (code2 == 200 && body2 != null) {
            val bmp = decode(body2) ?: return DoorFetch.Fail("пустой кадр")
            return DoorFetch.Ok(bmp)
        }
        if (code2 == 401) cachedAuth.set(null)
        if (code2 == 404) advanceChannel()
        return DoorFetch.HttpError(code2)
    }

    private fun advanceChannel() {
        urlIndex = (urlIndex + 1) % urls.size
    }

    private fun request(url: URL, authHeader: String?): Pair<Int, ByteArray?> {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 4_000
            readTimeout = 4_000
            requestMethod = "GET"
            instanceFollowRedirects = true
            useCaches = false
            if (authHeader != null) setRequestProperty("Authorization", authHeader)
        }
        return try {
            val code = conn.responseCode
            if (code == 401) {
                lastWwwAuthenticate = conn.getHeaderField("WWW-Authenticate")
            }
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            code to stream?.use { it.readBytes() }
        } catch (e: Exception) {
            Log.w(TAG, "door camera request", e)
            throw e
        } finally {
            conn.disconnect()
        }
    }

    private fun decode(bytes: ByteArray): Bitmap? =
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    private fun basicHeader(): String {
        val raw = "$username:$password".toByteArray(Charsets.ISO_8859_1)
        return "Basic " + Base64.encodeToString(raw, Base64.NO_WRAP)
    }

    private fun digestHeader(wwwAuthenticate: String, url: URL): String {
        val requestUri = url.path + (url.query?.let { "?$it" } ?: "")
        val params = parseAuthParams(wwwAuthenticate.removePrefix("Digest").trim())
        val realm = params["realm"].orEmpty()
        val nonce = params["nonce"].orEmpty()
        val qop = params["qop"]?.split(",")?.map { it.trim() }?.firstOrNull { it == "auth" }
        val opaque = params["opaque"]
        val algorithm = params["algorithm"] ?: "MD5"
        val ha1 = md5Hex("$username:$realm:$password")
        val ha2 = md5Hex("GET:$requestUri")
        return if (qop != null) {
            val nc = "00000001"
            val cnonce = Random.nextBytes(8).joinToString("") { "%02x".format(it) }
            val resp = md5Hex("$ha1:$nonce:$nc:$cnonce:$qop:$ha2")
            buildString {
                append("Digest username=\"$username\", realm=\"$realm\", nonce=\"$nonce\", ")
                append("uri=\"$requestUri\", algorithm=$algorithm, response=\"$resp\", ")
                append("qop=$qop, nc=$nc, cnonce=\"$cnonce\"")
                if (opaque != null) append(", opaque=\"$opaque\"")
            }
        } else {
            val resp = md5Hex("$ha1:$nonce:$ha2")
            buildString {
                append("Digest username=\"$username\", realm=\"$realm\", nonce=\"$nonce\", ")
                append("uri=\"$requestUri\", response=\"$resp\"")
                if (opaque != null) append(", opaque=\"$opaque\"")
            }
        }
    }

    private fun parseAuthParams(raw: String): Map<String, String> {
        val out = linkedMapOf<String, String>()
        val re = Regex("""(\w+)=(?:"([^"]*)"|([^,\s]*))""")
        for (m in re.findAll(raw)) {
            out[m.groupValues[1].lowercase()] = m.groupValues[2].ifEmpty { m.groupValues[3] }
        }
        return out
    }

    private fun md5Hex(s: String): String {
        val dig = MessageDigest.getInstance("MD5").digest(s.toByteArray(Charsets.ISO_8859_1))
        return dig.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val TAG = "DoorCamera"
    }
}
