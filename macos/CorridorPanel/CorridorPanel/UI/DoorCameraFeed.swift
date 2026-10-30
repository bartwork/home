import Foundation
import AppKit

/// Превью двери: RTSP субпоток предпочтительно, ISAPI JPEG — fallback.
@Observable
final class DoorCameraFeed {
    enum Display: Equatable {
        case idle
        case rtsp(URL)
        case jpeg
    }

    private(set) var display: Display = .idle
    private(set) var image: NSImage?
    private(set) var errorText: String?
    /// Звук RTSP по умолчанию выкл (панель всегда на экране).
    private(set) var audioMuted = true

    private var client: DoorJpegClient?
    private var consumers = 0
    private var host = ""
    private var username = ""
    private var password = ""
    private var failStreak = 0
    private var forcedJpeg = false
    /// Ошибки камеры → журнал.
    var onError: ((String) -> Void)?

    func retain(host: String, username: String, password: String) {
        let changed = host != self.host || username != self.username || password != self.password
        self.host = host
        self.username = username
        self.password = password
        consumers += 1
        if changed || (display == .idle && consumers == 1) {
            forcedJpeg = false
            errorText = nil
            startPreferred()
        }
    }

    func release() {
        consumers = max(0, consumers - 1)
        if consumers == 0 {
            stopJpeg()
            image = nil
            errorText = nil
            display = .idle
        }
    }

    func toggleAudioMute() {
        audioMuted.toggle()
    }

    func fallbackToJpeg(reason: String) {
        guard !forcedJpeg else { return }
        guard case .rtsp = display else { return }
        forcedJpeg = true
        reportError(reason)
        startJpeg()
    }

    private func reportError(_ message: String) {
        let msg = message.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !msg.isEmpty else { return }
        errorText = msg
        onError?(msg)
    }

    private func startPreferred() {
        if Topics.Camera.preferRtsp,
           !forcedJpeg,
           let url = Topics.Camera.rtspURL(host: host, user: username, password: password, substream: true)
        {
            stopJpeg()
            image = nil
            errorText = nil
            display = .rtsp(url)
            return
        }
        startJpeg()
    }

    private func startJpeg() {
        display = .jpeg
        stopJpeg()
        failStreak = 0
        let client = DoorJpegClient(
            host: host,
            username: username,
            password: password,
            channelMain: Topics.Camera.jpegChannelMain,
            channelSub: Topics.Camera.jpegChannelSub
        )
        self.client = client
        client.start { [weak self] frame, error in
            guard let self else { return }
            if let frame {
                self.image = frame
                self.failStreak = 0
                self.errorText = nil
                return
            }
            self.failStreak += 1
            if self.failStreak >= 3, let error, !error.isEmpty {
                self.reportError(error)
            }
        }
    }

    private func stopJpeg() {
        client?.stop()
        client = nil
    }
}

private final class DoorJpegClient {
    private let urls: [URL]
    private let interval: TimeInterval
    private let session: URLSession
    private let auth: DigestAuthDelegate
    private var task: Task<Void, Never>?
    private var urlIndex = 0

    init(
        host: String,
        username: String,
        password: String,
        channelMain: Int,
        channelSub: Int,
        interval: TimeInterval = 0.55
    ) {
        let clean = host.trimmingCharacters(in: .whitespacesAndNewlines)
        self.urls = [channelMain, channelSub].compactMap {
            URL(string: "http://\(clean)/ISAPI/Streaming/channels/\($0)/picture")
        }
        self.interval = interval
        self.auth = DigestAuthDelegate(username: username, password: password)
        let cfg = URLSessionConfiguration.ephemeral
        cfg.timeoutIntervalForRequest = 3
        cfg.httpMaximumConnectionsPerHost = 1
        self.session = URLSession(configuration: cfg, delegate: auth, delegateQueue: nil)
    }

    func start(onFrame: @MainActor @escaping (NSImage?, String?) -> Void) {
        task?.cancel()
        guard !urls.isEmpty else {
            Task { @MainActor in onFrame(nil, "нет URL камеры") }
            return
        }
        task = Task { [session, interval] in
            var backoff = interval
            while !Task.isCancelled {
                let url = urls[urlIndex % urls.count]
                do {
                    var request = URLRequest(url: url, timeoutInterval: 3)
                    request.cachePolicy = .reloadIgnoringLocalCacheData
                    let (data, response) = try await session.data(for: request)
                    let code = (response as? HTTPURLResponse)?.statusCode ?? 0
                    if code == 200, let jpeg = NSImage(data: data) {
                        backoff = interval
                        await onFrame(jpeg, nil)
                    } else {
                        if code == 404 { urlIndex = (urlIndex + 1) % max(urls.count, 1) }
                        backoff = min(2.5, backoff + 0.25)
                        await onFrame(nil, "камера: HTTP \(code)")
                    }
                } catch is CancellationError {
                    return
                } catch {
                    backoff = min(2.5, backoff + 0.25)
                    await onFrame(nil, error.localizedDescription)
                }
                try? await Task.sleep(nanoseconds: UInt64(backoff * 1_000_000_000))
            }
        }
    }

    func stop() {
        task?.cancel()
        task = nil
        session.invalidateAndCancel()
    }
}

private final class DigestAuthDelegate: NSObject, URLSessionTaskDelegate {
    let username: String
    let password: String

    init(username: String, password: String) {
        self.username = username
        self.password = password
    }

    func urlSession(
        _ session: URLSession,
        task: URLSessionTask,
        didReceive challenge: URLAuthenticationChallenge,
        completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void
    ) {
        let method = challenge.protectionSpace.authenticationMethod
        guard method == NSURLAuthenticationMethodHTTPDigest
            || method == NSURLAuthenticationMethodHTTPBasic
        else {
            completionHandler(.performDefaultHandling, nil)
            return
        }
        completionHandler(
            .useCredential,
            URLCredential(user: username, password: password, persistence: .forSession)
        )
    }
}
