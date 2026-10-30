import Foundation
import Darwin
import linphonesw

enum SipRegistrationState: String {
    case none, progress, ok, failed, cleared

    var title: String {
        switch self {
        case .ok: return "SIP online"
        case .progress: return "SIP…"
        case .failed: return "SIP ошибка"
        case .cleared: return "SIP сброшен"
        case .none: return "SIP выкл"
        }
    }
}

enum SipCallPhase: String {
    case idle, outgoing, incoming, connected
}

/// SIP через Linphone SDK (ext 102 → Asterisk). Видео двери — RTSP/JPEG отдельно.
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
    /// Защита от re-entrant start (AppDelegate + onAppear → crash в addAuthInfo).
    private var isStarting = false
    /// Если restart пришёл во время boot — применить после.
    private var pendingRestart: AppSettings?
    var isRunning: Bool { core != nil }
    /// Ошибки SIP → журнал (включая «IO error»).
    var onError: ((String) -> Void)?

    /// Мягкий старт: живой Core с ok/progress не трогаем; failed/cleared — перезапуск.
    func start(settings: AppSettings) {
        if isStarting {
            pendingRestart = settings
            return
        }
        if core != nil {
            switch registration {
            case .ok, .progress: return
            case .none, .failed, .cleared: break
            }
        }
        boot(settings: settings)
    }

    /// Принудительный перезапуск (настройки / reconnect).
    func restart(settings: AppSettings) {
        if isStarting {
            pendingRestart = settings
            return
        }
        boot(settings: settings)
    }

    private func boot(settings: AppSettings) {
        isStarting = true
        pendingRestart = nil
        registration = .progress
        registrationMessage = "запуск…"
        lastError = nil
        configuredDomain = settings.sipDomain.trimmingCharacters(in: .whitespacesAndNewlines)

        teardownCore()

        do {
            let factory = Factory.Instance
            LoggingService.Instance.logLevel = .Warning
            try Self.configureLinphoneResources(factory)
            let configPath = Self.ensureConfigPath()
            let core = try factory.createCore(
                configPath: configPath,
                factoryConfigPath: nil,
                systemContext: nil
            )
            // Картинка двери — ISAPI; SIP в основном для аудио (+ video в SDP).
            core.videoCaptureEnabled = false
            core.videoDisplayEnabled = false
            if let policy = try? factory.createVideoActivationPolicy() {
                policy.automaticallyAccept = true
                policy.automaticallyInitiate = false
                core.videoActivationPolicy = policy
            }
            Self.loadOpenH264IfNeeded()
            configureVideoCodecs(core)
            if let device = core.videoDevicesList.first(where: { $0.localizedCaseInsensitiveContains("StaticImage") })
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
            isStarting = false
            if let pending = pendingRestart {
                pendingRestart = nil
                boot(settings: pending)
            }
        } catch {
            isStarting = false
            teardownCore()
            registration = .failed
            registrationMessage = error.localizedDescription
            reportError(error.localizedDescription)
            if let pending = pendingRestart {
                pendingRestart = nil
                boot(settings: pending)
            }
        }
    }

    func stop() {
        isStarting = false
        pendingRestart = nil
        teardownCore()
        registration = .none
        registrationMessage = ""
        resetCallUI()
    }

    private func teardownCore() {
        iterateTimer?.invalidate()
        iterateTimer = nil
        try? core?.terminateAllCalls()
        if let account {
            core?.removeAccount(account: account)
        }
        account = nil
        if let core {
            if let coreDelegate {
                core.removeDelegate(delegate: coreDelegate)
            }
            // clearAllAuthInfo после stop() на macOS даёт SIGSEGV — только до stop.
            core.clearAllAuthInfo()
            core.playbackGainDb = 0
            core.stop()
        }
        core = nil
        coreDelegate = nil
        didRequestVideoUpgrade = false
        isMicMuted = false
        isSpeakerMuted = false
    }

    /// belle-sip ищет sip_grammar.belr относительно top/data resources — без явного пути
    /// (и при кириллице в .app) loadGrammar() → bctbx_fatal → abort.
    private static func configureLinphoneResources(_ factory: Factory) throws {
        let frameworks = Bundle.main.privateFrameworksURL
        let belle = frameworks?
            .appendingPathComponent("belle-sip.framework/Resources")
        let linphone = frameworks?
            .appendingPathComponent("linphone.framework/Resources")
        guard let belle,
              FileManager.default.fileExists(atPath: belle.appendingPathComponent("sip_grammar.belr").path)
        else {
            throw SipBootError.missingGrammar
        }
        factory.topResourcesDir = belle.path
        setenv("BELLE_SIP_CONFIG_DIR", belle.path, 1)
        if let linphone, FileManager.default.fileExists(atPath: linphone.path) {
            factory.dataResourcesDir = linphone.path
            factory.imageResourcesDir = linphone.path
            factory.ringResourcesDir = linphone.path
            factory.soundResourcesDir = linphone.path
        }
    }

    private static func ensureConfigPath() -> String {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? URL(fileURLWithPath: NSTemporaryDirectory())
        let dir = base.appendingPathComponent("HomePanel", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        let path = dir.appendingPathComponent("linphonerc")
        sanitizeConfigIfNeeded(at: path)
        return path.path
    }

    /// Убираем битый [push_notification] и мёртвые пути к старым .app.
    private static func sanitizeConfigIfNeeded(at path: URL) {
        let legacy = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)
            .first?
            .appendingPathComponent("CorridorPanel/linphonerc")
        if let legacy, FileManager.default.fileExists(atPath: legacy.path) {
            try? FileManager.default.removeItem(at: legacy)
        }
        guard FileManager.default.fileExists(atPath: path.path),
              let text = try? String(contentsOf: path, encoding: .utf8)
        else {
            FileManager.default.createFile(atPath: path.path, contents: Data(), attributes: nil)
            return
        }
        let dirty = text.contains("[push_notification]")
            || text.contains("Мой дом.app")
            || text.contains("CorridorPanel.app")
        if dirty {
            try? Data().write(to: path, options: .atomic)
        }
    }

    func callDoor(extension doorExt: String) {
        guard let core else {
            reportError("SIP core не запущен")
            return
        }
        let domain = core.defaultAccount?.params?.domain?.nilIfEmpty
            ?? configuredDomain.nilIfEmpty
            ?? ""
        guard !domain.isEmpty else {
            reportError("не задан SIP domain")
            return
        }
        do {
            let params = try core.createCallParams(call: nil)
            params.videoEnabled = true
            params.audioEnabled = true
            _ = core.inviteWithParams(url: "sip:\(doorExt)@\(domain)", params: params)
        } catch {
            reportError(error.localizedDescription)
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
            reportError(error.localizedDescription)
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
            // «IO error» — в журнал да, красной строкой под дверью нет.
            reportError(message, showInUi: !Self.isSipNoise(message))
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
            let wasIncoming = callPhase == .incoming
            callPhase = .incoming
            isInCall = true
            if !wasIncoming {
                NotificationCenter.default.post(
                    name: .sipIncomingCall,
                    object: nil,
                    userInfo: ["remote": remoteDisplay]
                )
            }
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
            reportError(message, showInUi: !Self.isSipNoise(message))
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
        DispatchQueue.main.async { CallNotifier.clear() }
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

    /// Пишет в журнал; `showInUi` — красная строка под дверью.
    private func reportError(_ message: String, showInUi: Bool = true) {
        let friendly = Self.friendlySipMessage(message)
        onError?(friendly)
        if showInUi {
            lastError = friendly
        } else {
            lastError = nil
        }
    }

    private static func isSipNoise(_ message: String) -> Bool {
        let m = message.lowercased()
        return m.contains("io error") || m.contains("ioerror") || m == "io"
    }

    private static func friendlySipMessage(_ message: String) -> String {
        let m = message.lowercased()
        if isSipNoise(message) { return "Нет связи с дверью" }
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

private enum SipBootError: LocalizedError {
    case missingGrammar

    var errorDescription: String? {
        switch self {
        case .missingGrammar:
            return "SIP grammar не найдена (belle-sip.framework/Resources/sip_grammar.belr)"
        }
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
