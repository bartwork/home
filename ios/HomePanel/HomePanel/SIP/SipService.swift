import Foundation
import Darwin
import linphonesw

enum SipRegistrationState: String {
    case none, progress, ok, failed, cleared

    var title: String {
        switch self {
        case .ok: return "SIP зарегистрирован"
        case .progress: return "SIP регистрация…"
        case .failed: return "SIP ошибка"
        case .cleared: return "SIP сброшен"
        case .none: return "SIP не запущен"
        }
    }
}

enum SipCallPhase: String {
    case idle, outgoing, incoming, connected
}

/// SIP через Linphone SDK (ext 102 → Asterisk). Видео двери — отдельно через ISAPI.
@Observable
final class SipService {
    private(set) var registration: SipRegistrationState = .none
    private(set) var registrationMessage = ""
    private(set) var callPhase: SipCallPhase = .idle
    private(set) var remoteDisplay = ""
    private(set) var lastError: String?
    private(set) var isInCall = false
    private(set) var isMicMuted = false
    private(set) var isSpeakerMuted = false

    private var core: Core?
    private var coreDelegate: CoreDelegateStub?
    private var iterateTimer: Timer?
    private var account: Account?
    private var configuredDomain = ""
    private var didRequestVideoUpgrade = false

    func start(settings: AppSettings) {
        stop()
        lastError = nil
        configuredDomain = settings.sipDomain.trimmingCharacters(in: .whitespacesAndNewlines)

        do {
            let factory = Factory.Instance
            LoggingService.Instance.logLevel = .Warning
            let core = try factory.createCore(configPath: nil, factoryConfigPath: nil, systemContext: nil)
            core.videoCaptureEnabled = true
            core.videoDisplayEnabled = true
            if let policy = try? factory.createVideoActivationPolicy() {
                policy.automaticallyAccept = true
                policy.automaticallyInitiate = true
                core.videoActivationPolicy = policy
            }
            Self.loadOpenH264IfNeeded()
            configureVideoCodecs(core)
            // Симулятор: StaticImage как источник, чтобы H.264 мог попасть в SDP.
            if let device = core.videoDevicesList.first(where: { $0.localizedCaseInsensitiveContains("StaticImage") })
                ?? core.videoDevicesList.first(where: { $0.localizedCaseInsensitiveContains("capture") })
                ?? core.videoDevicesList.first {
                try? core.setVideodevice(newValue: device)
            }
            self.core = core

            let delegate = CoreDelegateStub(
                onCallStateChanged: { [weak self] _, call, state, message in
                    DispatchQueue.main.async {
                        self?.handleCallState(call: call, state: state, message: message)
                    }
                },
                onAccountRegistrationStateChanged: { [weak self] _, _, state, message in
                    DispatchQueue.main.async {
                        self?.handleRegistration(state: state, message: message)
                    }
                }
            )
            core.addDelegate(delegate: delegate)
            coreDelegate = delegate

            try core.start()
            try configureAccount(settings: settings, core: core)
            startIterate()
        } catch {
            lastError = error.localizedDescription
            registration = .failed
            registrationMessage = error.localizedDescription
        }
    }

    func stop() {
        iterateTimer?.invalidate()
        iterateTimer = nil
        try? core?.terminateAllCalls()
        if let account {
            core?.removeAccount(account: account)
        }
        account = nil
        core?.clearAllAuthInfo()
        core?.playbackGainDb = 0
        core?.stop()
        core = nil
        coreDelegate = nil
        didRequestVideoUpgrade = false
        isMicMuted = false
        isSpeakerMuted = false
        registration = .none
        registrationMessage = ""
        resetCallUI()
    }

    func callDoor(extension doorExt: String) {
        guard let core else {
            lastError = "SIP core не запущен"
            return
        }
        let domain = core.defaultAccount?.params?.domain?.nilIfEmpty
            ?? configuredDomain.nilIfEmpty
            ?? ""
        guard !domain.isEmpty else {
            lastError = "не задан SIP domain"
            return
        }
        do {
            let params = try core.createCallParams(call: nil)
            params.videoEnabled = true
            params.audioEnabled = true
            _ = core.inviteWithParams(url: "sip:\(doorExt)@\(domain)", params: params)
        } catch {
            lastError = error.localizedDescription
        }
    }

    func accept() {
        guard let core, let call = core.currentCall else { return }
        do {
            let params = try core.createCallParams(call: call)
            params.videoEnabled = true
            params.audioEnabled = true
            try call.acceptWithParams(params: params)
        } catch {
            lastError = error.localizedDescription
        }
    }

    func hangup() {
        setMicMuted(false)
        setSpeakerMuted(false)
        try? core?.terminateAllCalls()
        resetCallUI()
    }

    func toggleMicMute() {
        setMicMuted(!isMicMuted)
    }

    func toggleSpeakerMute() {
        setSpeakerMuted(!isSpeakerMuted)
    }

    func setMicMuted(_ muted: Bool) {
        isMicMuted = muted
        core?.micEnabled = !muted
        core?.currentCall?.microphoneMuted = muted
    }

    func setSpeakerMuted(_ muted: Bool) {
        isSpeakerMuted = muted
        core?.currentCall?.speakerMuted = muted
        // На случай, если currentCall ещё нет / уже сменился — дублируем на core.
        if muted {
            core?.playbackGainDb = -1000
        } else {
            core?.playbackGainDb = 0
        }
    }

    // MARK: - Private

    private func configureAccount(settings: AppSettings, core: Core) throws {
        let factory = Factory.Instance
        let user = settings.sipUsername.trimmingCharacters(in: .whitespacesAndNewlines)
        let domain = settings.sipDomain.trimmingCharacters(in: .whitespacesAndNewlines)
        let transport = settings.sipTransport.lowercased()

        core.clearAllAuthInfo()
        let auth = try factory.createAuthInfo(
            username: user,
            userid: user,
            passwd: settings.sipPassword,
            ha1: nil,
            realm: nil,
            domain: domain
        )
        core.addAuthInfo(info: auth)

        let identity = try factory.createAddress(addr: "sip:\(user)@\(domain)")
        let server = try factory.createAddress(addr: "sip:\(domain);transport=\(transport)")

        let params = try core.createAccountParams()
        try params.setIdentityaddress(newValue: identity)
        try params.setServeraddress(newValue: server)
        params.registerEnabled = true

        let account = try core.createAccount(params: params)
        try core.addAccount(account: account)
        core.defaultAccount = account
        self.account = account
        registration = .progress
        registrationMessage = "регистрируемся…"
    }

    private func startIterate() {
        iterateTimer?.invalidate()
        let timer = Timer(timeInterval: 0.02, repeats: true) { [weak self] _ in
            self?.core?.iterate()
        }
        RunLoop.main.add(timer, forMode: .common)
        iterateTimer = timer
    }

    private func handleRegistration(state: RegistrationState, message: String) {
        registrationMessage = message
        switch state {
        case .None:
            registration = .none
        case .Progress, .Refreshing:
            registration = .progress
        case .Ok:
            registration = .ok
            lastError = nil
        case .Cleared:
            registration = .cleared
        case .Failed:
            registration = .failed
            lastError = message
        }
    }

    private func handleCallState(call: Call, state: Call.State, message: String) {
        remoteDisplay = call.remoteAddress?.displayName?.nilIfEmpty
            ?? call.remoteAddress?.asStringUriOnly()
            ?? ""

        switch state {
        case .OutgoingInit, .OutgoingProgress, .OutgoingRinging, .OutgoingEarlyMedia:
            callPhase = .outgoing
            isInCall = true
        case .IncomingReceived, .IncomingEarlyMedia, .PushIncomingReceived:
            callPhase = .incoming
            isInCall = true
            try? call.acceptEarlyMedia()
        case .Connected, .StreamsRunning, .Updating,
             .EarlyUpdatedByRemote, .EarlyUpdating, .Resuming:
            callPhase = .connected
            isInCall = true
            // Mute мог быть выставлен до появления currentCall — применим снова.
            if isMicMuted { setMicMuted(true) }
            if isSpeakerMuted { setSpeakerMuted(true) }
            requestVideoUpgradeIfNeeded(call)
        case .UpdatedByRemote:
            callPhase = .connected
            isInCall = true
            if let params = try? core?.createCallParams(call: call), !params.videoEnabled {
                params.videoEnabled = true
                try? call.acceptUpdate(params: params)
            } else {
                try? call.acceptUpdate(params: nil)
            }
        case .End, .Released:
            endCallCleanup()
        case .Error:
            lastError = friendlyCallError(message)
            endCallCleanup()
        case .Idle, .Pausing, .Paused, .PausedByRemote, .Referred:
            break
        }
    }

    private func endCallCleanup() {
        didRequestVideoUpgrade = false
        setMicMuted(false)
        setSpeakerMuted(false)
        resetCallUI()
    }

    private func configureVideoCodecs(_ core: Core) {
        for mime in ["H264", "h264", "VP8", "vp8"] {
            if let pt = core.getPayloadType(type: mime, rate: 90000, channels: -1) {
                _ = pt.enable(enabled: true)
            }
        }
        var payloads = core.videoPayloadTypes
        for pt in payloads {
            let mime = pt.mimeType.lowercased()
            _ = pt.enable(enabled: mime == "h264" || mime == "vp8")
        }
        payloads.sort {
            rank($0.mimeType) < rank($1.mimeType)
        }
        core.videoPayloadTypes = payloads
    }

    private func rank(_ mime: String) -> Int {
        switch mime.lowercased() {
        case "h264": return 0
        case "vp8": return 1
        default: return 2
        }
    }

    private static func loadOpenH264IfNeeded() {
        typealias InitFn = @convention(c) () -> Void
        if let fw = Bundle.main.privateFrameworksURL?
            .appendingPathComponent("msopenh264.framework/msopenh264") {
            _ = dlopen(fw.path, RTLD_NOW)
        }
        for name in ["libmsopenh264_init", "msopenh264_init"] {
            if let sym = dlsym(dlopen(nil, RTLD_NOW), name) {
                unsafeBitCast(sym, to: InitFn.self)()
                return
            }
        }
    }

    private func requestVideoUpgradeIfNeeded(_ call: Call) {
        guard !didRequestVideoUpgrade else { return }
        let active = call.currentParams?.videoEnabled == true
        let remote = call.remoteParams?.videoEnabled == true
        guard !active || !remote else { return }
        guard let params = try? core?.createCallParams(call: call) else { return }
        params.videoEnabled = true
        params.audioEnabled = true
        didRequestVideoUpgrade = true
        try? call.update(params: params)
    }

    private func friendlyCallError(_ message: String) -> String {
        let m = message.lowercased()
        if m.contains("forbidden") || m.contains("603") || m.contains("decline") {
            return "Дверь отклонила вызов"
        }
        if m.contains("busy") { return "Дверь занята" }
        if m.contains("temporarily unavailable") || m.contains("480") {
            return "Дверь недоступна"
        }
        return message
    }

    private func resetCallUI() {
        callPhase = .idle
        isInCall = false
        remoteDisplay = ""
    }
}

private extension String {
    var nilIfEmpty: String? {
        let t = trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }
}

private extension Optional where Wrapped == String {
    var nilIfEmpty: String? {
        self?.nilIfEmpty
    }
}
