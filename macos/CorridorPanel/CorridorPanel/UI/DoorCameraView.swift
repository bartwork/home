import SwiftUI
import AppKit
import VLCKitSPM

/// Превью двери: RTSP (субпоток через VLC) или JPEG fallback.
struct DoorCameraView: View {
    @Bindable var feed: DoorCameraFeed
    var compact: Bool = false
    var expandable: Bool = false
    var onExpand: (() -> Void)? = nil

    private var corner: CGFloat { compact ? 12 : 14 }

    var body: some View {
        ZStack {
            Color.black
            switch feed.display {
            case .rtsp(let url):
                // compact: crop без letterbox; fullscreen: fit.
                RtspPlayerView(url: url, muted: feed.audioMuted, fill: compact) {
                    feed.fallbackToJpeg(reason: "RTSP недоступен")
                }
            case .jpeg:
                if let image = feed.image {
                    Image(nsImage: image)
                        .resizable()
                        .aspectRatio(contentMode: compact ? .fill : .fit)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if feed.errorText == nil {
                    ProgressView().controlSize(.small)
                }
            case .idle:
                ProgressView().controlSize(.small)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
        .clipShape(RoundedRectangle(cornerRadius: corner, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: corner, style: .continuous)
                .strokeBorder(Color.primary.opacity(0.08), lineWidth: 1)
        }
        .contentShape(RoundedRectangle(cornerRadius: corner, style: .continuous))
        .onTapGesture {
            guard expandable, let onExpand else { return }
            onExpand()
        }
    }
}

/// VLC letterbox'ит, если drawable ≠ пропорциям потока.
/// Делаем surface ровно под videoSize и scale cover/contain в клип.
private struct RtspPlayerView: NSViewRepresentable {
    let url: URL
    var muted: Bool
    var fill: Bool
    var onFail: () -> Void

    func makeCoordinator() -> Coordinator { Coordinator(onFail: onFail) }

    func makeNSView(context: Context) -> AspectFillHost {
        let host = AspectFillHost(frame: .zero)
        host.aspectFill = fill
        context.coordinator.onFail = onFail
        context.coordinator.attach(url: url, muted: muted, to: host)
        return host
    }

    func updateNSView(_ nsView: AspectFillHost, context: Context) {
        context.coordinator.onFail = onFail
        nsView.aspectFill = fill
        if context.coordinator.currentURL != url {
            context.coordinator.attach(url: url, muted: muted, to: nsView)
        } else {
            context.coordinator.applyMute(muted)
            nsView.needsLayout = true
        }
    }

    static func dismantleNSView(_ nsView: AspectFillHost, coordinator: Coordinator) {
        coordinator.tearDown()
    }

    final class Coordinator: NSObject, VLCMediaPlayerDelegate {
        var onFail: () -> Void
        var currentURL: URL?
        private weak var host: AspectFillHost?
        private var player: VLCMediaPlayer?
        private var failWork: DispatchWorkItem?
        private var didFail = false
        private var sawPlaying = false

        init(onFail: @escaping () -> Void) { self.onFail = onFail }

        func attach(url: URL, muted: Bool, to host: AspectFillHost) {
            tearDown()
            didFail = false
            sawPlaying = false
            currentURL = url
            self.host = host

            let media = VLCMedia(url: url)
            media.addOptions([
                "rtsp-tcp": true,
                "network-caching": 400,
                "live-caching": 400,
                "clock-jitter": 0,
                "clock-synchro": 0,
            ])

            let player = VLCMediaPlayer()
            player.delegate = self
            player.drawable = host.videoSurface
            player.media = media
            self.player = player
            applyMute(muted)
            player.play()

            let work = DispatchWorkItem { [weak self] in
                guard let self, !self.didFail else { return }
                if !self.sawPlaying { self.failOnce() }
            }
            failWork = work
            DispatchQueue.main.asyncAfter(deadline: .now() + 8, execute: work)
        }

        func applyMute(_ muted: Bool) {
            player?.audio?.isMuted = muted
            if !muted {
                player?.audio?.volume = 100
            }
        }

        func mediaPlayerStateChanged(_ aNotification: Notification) {
            DispatchQueue.main.async { [weak self] in
                guard let self, let player = self.player else { return }
                switch player.state {
                case .playing, .buffering:
                    if player.state == .playing {
                        self.sawPlaying = true
                        self.failWork?.cancel()
                    }
                    self.syncAspect(from: player)
                case .error:
                    self.failOnce()
                default:
                    break
                }
            }
        }

        private func syncAspect(from player: VLCMediaPlayer) {
            let size = player.videoSize
            guard size.width > 1, size.height > 1, let host else { return }
            let aspect = size.width / size.height
            if abs(host.contentAspect - aspect) > 0.01 {
                host.contentAspect = aspect
                host.needsLayout = true
                host.layoutSubtreeIfNeeded()
            }
        }

        private func failOnce() {
            guard !didFail else { return }
            didFail = true
            failWork?.cancel()
            tearDown()
            onFail()
        }

        func tearDown() {
            failWork?.cancel()
            failWork = nil
            player?.delegate = nil
            if player?.isPlaying == true { player?.stop() }
            player?.drawable = nil
            player = nil
            host = nil
        }
    }
}

/// Surface под пропорции потока → VLC без внутренних чёрных полос; host клипует cover/contain.
private final class AspectFillHost: NSView {
    let videoSurface = NSView(frame: .zero)
    var aspectFill = true
    /// До первого кадра — типичный 4:3 у Hikvision door station.
    var contentAspect: CGFloat = 4.0 / 3.0

    override init(frame frameRect: NSRect) {
        super.init(frame: frameRect)
        wantsLayer = true
        layer?.backgroundColor = NSColor.black.cgColor
        layer?.masksToBounds = true
        videoSurface.wantsLayer = true
        addSubview(videoSurface)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError() }

    override func layout() {
        super.layout()
        let b = bounds
        guard b.width > 1, b.height > 1, contentAspect > 0 else { return }
        let viewAspect = b.width / b.height
        let w: CGFloat
        let h: CGFloat
        if aspectFill {
            // cover
            if contentAspect > viewAspect {
                h = b.height
                w = h * contentAspect
            } else {
                w = b.width
                h = w / contentAspect
            }
        } else {
            // contain
            if contentAspect > viewAspect {
                w = b.width
                h = w / contentAspect
            } else {
                h = b.height
                w = h * contentAspect
            }
        }
        videoSurface.frame = CGRect(
            x: (b.width - w) * 0.5,
            y: (b.height - h) * 0.5,
            width: w,
            height: h
        )
    }
}
