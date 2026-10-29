import SwiftUI
import UIKit

/// Живое превью двери через ISAPI JPEG (обход SIP/H.264).
struct DoorCameraView: View {
    let host: String
    let username: String
    let password: String
    var channel: Int = 101
    var interval: TimeInterval = 0.4

    @State private var image: UIImage?
    @State private var errorText: String?
    @State private var client: DoorCameraClient?

    var body: some View {
        ZStack {
            Color.black
            if let image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
                    .frame(maxWidth: .infinity)
            } else {
                ProgressView()
                    .tint(.white)
            }
            if let errorText {
                VStack {
                    Spacer()
                    Text(errorText)
                        .font(.caption2.weight(.medium))
                        .foregroundStyle(.white.opacity(0.9))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(.ultraThinMaterial, in: Capsule())
                        .padding(.bottom, 12)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .onAppear { start() }
        .onDisappear { stop() }
        .onChange(of: host) { _, _ in restart() }
        .onChange(of: username) { _, _ in restart() }
        .onChange(of: password) { _, _ in restart() }
    }

    private func restart() {
        stop()
        start()
    }

    private func start() {
        stop()
        let client = DoorCameraClient(
            host: host,
            username: username,
            password: password,
            channel: channel,
            interval: interval
        )
        self.client = client
        client.start { frame, error in
            if let frame { image = frame }
            errorText = error
        }
    }

    private func stop() {
        client?.stop()
        client = nil
    }
}

/// Один URLSession на весь звонок (не на каждый кадр).
private final class DoorCameraClient {
    private let url: URL?
    private let interval: TimeInterval
    private let session: URLSession
    private let auth: DigestAuthDelegate
    private var task: Task<Void, Never>?

    init(host: String, username: String, password: String, channel: Int, interval: TimeInterval) {
        let clean = host.trimmingCharacters(in: .whitespacesAndNewlines)
        self.url = URL(string: "http://\(clean)/ISAPI/Streaming/channels/\(channel)/picture")
        self.interval = interval
        self.auth = DigestAuthDelegate(username: username, password: password)
        self.session = URLSession(configuration: .ephemeral, delegate: auth, delegateQueue: nil)
    }

    func start(onFrame: @MainActor @escaping (UIImage?, String?) -> Void) {
        task?.cancel()
        guard let url else {
            Task { @MainActor in onFrame(nil, "нет URL камеры") }
            return
        }
        task = Task { [session, interval] in
            while !Task.isCancelled {
                do {
                    let request = URLRequest(url: url, timeoutInterval: 4)
                    let (data, response) = try await session.data(for: request)
                    let code = (response as? HTTPURLResponse)?.statusCode ?? 0
                    if code == 200, let jpeg = UIImage(data: data) {
                        await onFrame(jpeg, nil)
                    } else {
                        await onFrame(nil, "камера: HTTP \(code)")
                    }
                } catch is CancellationError {
                    return
                } catch {
                    await onFrame(nil, error.localizedDescription)
                }
                try? await Task.sleep(nanoseconds: UInt64(interval * 1_000_000_000))
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
