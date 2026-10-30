package ru.home.corridorpanel.sip

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.linphone.core.Account
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType

data class SipUiState(
    val registration: SipRegistration = SipRegistration.None,
    val registrationMessage: String = "",
    val callPhase: SipCallPhase = SipCallPhase.Idle,
    val remoteDisplay: String = "",
    val lastError: String? = null,
    val isInCall: Boolean = false,
    val isMicMuted: Boolean = false,
    val isSpeakerMuted: Boolean = false,
    val config: SipConfig = SipConfig(),
)

/**
 * SIP через Linphone (ext 102 → Asterisk).
 * Видео двери — отдельно RTSP/JPEG ([DoorCameraFeed]), не SIP H.264.
 */
class SipService(private val appContext: Context) {
    private val _state = MutableStateFlow(SipUiState())
    val state: StateFlow<SipUiState> = _state.asStateFlow()

    private var core: Core? = null
    private var account: Account? = null
    private var didRequestVideoUpgrade = false

    /** Ошибки SIP → журнал (включая «IO error»). */
    var onError: ((String) -> Unit)? = null

    private val listener = object : CoreListenerStub() {
        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState?,
            message: String,
        ) {
            handleRegistration(state, message)
        }

        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State?,
            message: String,
        ) {
            handleCallState(call, state, message)
        }
    }

    fun start(config: SipConfig) {
        stop()
        _state.update {
            it.copy(config = config, lastError = null, registrationMessage = "")
        }
        try {
            val factory = Factory.instance()
            factory.setDebugMode(false, TAG)
            // Картинка двери — ISAPI; SIP только для аудио (+ video offer в SDP).
            val cfg = factory.createConfig(null)
            cfg.setBool("video", "capture", false)
            cfg.setBool("video", "display", false)
            cfg.setBool("sound", "use_native_ringing", true)
            val core = factory.createCoreWithConfig(cfg, appContext)
            core.isAutoIterateEnabled = true
            core.isVideoCaptureEnabled = false
            core.isVideoDisplayEnabled = false
            core.videoActivationPolicy?.let { policy ->
                policy.automaticallyAccept = true
                policy.automaticallyInitiate = false
            }
            configureVideoCodecs(core)
            core.addListener(listener)
            this.core = core
            core.start()
            configureAccount(config, core)
        } catch (e: Exception) {
            Log.e(TAG, "start failed", e)
            _state.update {
                it.copy(
                    registration = SipRegistration.Failed,
                    registrationMessage = e.message.orEmpty(),
                )
            }
            reportError(e.message.orEmpty())
        }
    }

    fun stop() {
        try {
            core?.terminateAllCalls()
        } catch (_: Exception) {
        }
        account?.let { acc ->
            try {
                core?.removeAccount(acc)
            } catch (_: Exception) {
            }
        }
        account = null
        try {
            core?.clearAllAuthInfo()
            core?.playbackGainDb = 0f
            core?.removeListener(listener)
            core?.stop()
        } catch (_: Exception) {
        }
        core = null
        didRequestVideoUpgrade = false
        _state.update {
            it.copy(
                registration = SipRegistration.None,
                registrationMessage = "",
                callPhase = SipCallPhase.Idle,
                isInCall = false,
                remoteDisplay = "",
                isMicMuted = false,
                isSpeakerMuted = false,
            )
        }
    }

    fun callDoor() {
        val core = core ?: run {
            reportError("SIP core не запущен")
            return
        }
        val cfg = _state.value.config
        val domain = core.defaultAccount?.params?.domain?.takeIf { it.isNotBlank() }
            ?: cfg.domain.takeIf { it.isNotBlank() }
            ?: run {
                reportError("не задан SIP domain")
                return
            }
        try {
            val params = core.createCallParams(null) ?: return
            params.isVideoEnabled = true
            params.isAudioEnabled = true
            core.inviteAddressWithParams(
                factoryAddress("sip:${cfg.doorExtension}@$domain"),
                params,
            )
        } catch (e: Exception) {
            reportError(e.message.orEmpty())
        }
    }

    fun accept() {
        val core = core ?: return
        val call = core.currentCall ?: return
        try {
            val params = core.createCallParams(call) ?: return
            params.isVideoEnabled = true
            params.isAudioEnabled = true
            call.acceptWithParams(params)
        } catch (e: Exception) {
            reportError(e.message.orEmpty())
        }
    }

    fun hangup() {
        setMicMuted(false)
        setSpeakerMuted(false)
        try {
            core?.terminateAllCalls()
        } catch (_: Exception) {
        }
        resetCallUi()
    }

    fun toggleMicMute() = setMicMuted(!_state.value.isMicMuted)

    fun toggleSpeakerMute() = setSpeakerMuted(!_state.value.isSpeakerMuted)

    fun setMicMuted(muted: Boolean) {
        _state.update { it.copy(isMicMuted = muted) }
        core?.isMicEnabled = !muted
        core?.currentCall?.microphoneMuted = muted
    }

    fun setSpeakerMuted(muted: Boolean) {
        _state.update { it.copy(isSpeakerMuted = muted) }
        core?.currentCall?.speakerMuted = muted
        core?.playbackGainDb = if (muted) -1000f else 0f
    }

    private fun configureAccount(config: SipConfig, core: Core) {
        val factory = Factory.instance()
        val user = config.username.trim()
        val domain = config.domain.trim()
        val transport = when (config.transport.trim().lowercase()) {
            "tcp" -> TransportType.Tcp
            "tls" -> TransportType.Tls
            else -> TransportType.Udp
        }

        core.clearAllAuthInfo()
        val auth = factory.createAuthInfo(user, user, config.password, null, null, domain)
        core.addAuthInfo(auth)

        val identity = factory.createAddress("sip:$user@$domain")
            ?: throw IllegalStateException("bad identity")
        val server = factory.createAddress("sip:$domain")
            ?: throw IllegalStateException("bad server")
        server.transport = transport

        val params = core.createAccountParams()
        params.identityAddress = identity
        params.serverAddress = server
        params.isRegisterEnabled = true

        val account = core.createAccount(params)
        core.addAccount(account)
        core.defaultAccount = account
        this.account = account
        _state.update {
            it.copy(
                registration = SipRegistration.Progress,
                registrationMessage = "регистрируемся…",
            )
        }
    }

    private fun handleRegistration(state: RegistrationState?, message: String) {
        val reg = when (state) {
            RegistrationState.None -> SipRegistration.None
            RegistrationState.Progress, RegistrationState.Refreshing -> SipRegistration.Progress
            RegistrationState.Ok -> SipRegistration.Ok
            RegistrationState.Cleared -> SipRegistration.Cleared
            RegistrationState.Failed -> SipRegistration.Failed
            else -> SipRegistration.None
        }
        val uiError = when (reg) {
            SipRegistration.Failed -> {
                val friendly = friendlySipMessage(message)
                if (friendly.isNotBlank()) onError?.invoke(friendly)
                if (isSipNoise(message)) null else friendly.ifBlank { null }
            }
            SipRegistration.Ok -> null
            else -> _state.value.lastError
        }
        _state.update {
            it.copy(
                registration = reg,
                registrationMessage = message,
                lastError = uiError,
            )
        }
    }

    private fun handleCallState(call: Call, state: Call.State?, message: String) {
        val remote = call.remoteAddress?.displayName?.takeIf { it.isNotBlank() }
            ?: call.remoteAddress?.asStringUriOnly().orEmpty()

        when (state) {
            Call.State.OutgoingInit,
            Call.State.OutgoingProgress,
            Call.State.OutgoingRinging,
            Call.State.OutgoingEarlyMedia,
            -> {
                _state.update {
                    it.copy(callPhase = SipCallPhase.Outgoing, isInCall = true, remoteDisplay = remote)
                }
            }

            Call.State.IncomingReceived,
            Call.State.IncomingEarlyMedia,
            Call.State.PushIncomingReceived,
            -> {
                _state.update {
                    it.copy(callPhase = SipCallPhase.Incoming, isInCall = true, remoteDisplay = remote)
                }
                try {
                    call.acceptEarlyMedia()
                } catch (_: Exception) {
                }
            }

            Call.State.Connected,
            Call.State.StreamsRunning,
            Call.State.Updating,
            Call.State.EarlyUpdatedByRemote,
            Call.State.EarlyUpdating,
            Call.State.Resuming,
            -> {
                _state.update {
                    it.copy(callPhase = SipCallPhase.Connected, isInCall = true, remoteDisplay = remote)
                }
                val s = _state.value
                if (s.isMicMuted) setMicMuted(true)
                if (s.isSpeakerMuted) setSpeakerMuted(true)
                requestVideoUpgradeIfNeeded(call)
            }

            Call.State.UpdatedByRemote -> {
                _state.update {
                    it.copy(callPhase = SipCallPhase.Connected, isInCall = true, remoteDisplay = remote)
                }
                try {
                    val params = core?.createCallParams(call)
                    if (params != null && !params.isVideoEnabled) {
                        params.isVideoEnabled = true
                        call.acceptUpdate(params)
                    } else {
                        call.acceptUpdate(null)
                    }
                } catch (_: Exception) {
                }
            }

            Call.State.End, Call.State.Released -> endCallCleanup()

            Call.State.Error -> {
                reportError(message, showInUi = !isSipNoise(message))
                endCallCleanup()
            }

            else -> Unit
        }
    }

    private fun endCallCleanup() {
        didRequestVideoUpgrade = false
        setMicMuted(false)
        setSpeakerMuted(false)
        resetCallUi()
    }

    private fun resetCallUi() {
        _state.update {
            it.copy(
                callPhase = SipCallPhase.Idle,
                isInCall = false,
                remoteDisplay = "",
            )
        }
    }

    private fun configureVideoCodecs(core: Core) {
        for (pt in core.videoPayloadTypes) {
            val mime = pt.mimeType.lowercase()
            pt.enable(mime == "h264" || mime == "vp8")
        }
        for (mime in listOf("H264", "VP8")) {
            core.getPayloadType(mime, 90000, -1)?.enable(true)
        }
    }

    private fun requestVideoUpgradeIfNeeded(call: Call) {
        if (didRequestVideoUpgrade) return
        val active = call.currentParams?.isVideoEnabled == true
        val remote = call.remoteParams?.isVideoEnabled == true
        if (active && remote) return
        val params = core?.createCallParams(call) ?: return
        params.isVideoEnabled = true
        params.isAudioEnabled = true
        didRequestVideoUpgrade = true
        try {
            call.update(params)
        } catch (_: Exception) {
        }
    }

    /** Пишет в журнал; showInUi — красная строка под дверью. */
    private fun reportError(message: String, showInUi: Boolean = true) {
        val friendly = friendlySipMessage(message)
        if (friendly.isNotBlank()) onError?.invoke(friendly)
        _state.update {
            it.copy(lastError = if (showInUi) friendly.ifBlank { null } else null)
        }
    }

    private fun isSipNoise(message: String): Boolean {
        val m = message.lowercase()
        return "io error" in m || "ioerror" in m || m == "io"
    }

    private fun friendlySipMessage(message: String): String {
        val m = message.lowercase()
        return when {
            isSipNoise(message) -> "Нет связи с дверью"
            "forbidden" in m || "603" in m || "decline" in m -> "Дверь отклонила вызов"
            "busy" in m -> "Дверь занята"
            "temporarily unavailable" in m || "480" in m -> "Дверь недоступна"
            else -> message
        }
    }

    private fun factoryAddress(uri: String) =
        Factory.instance().createAddress(uri) ?: error("bad address $uri")

    companion object {
        private const val TAG = "CorridorSip"
    }
}
