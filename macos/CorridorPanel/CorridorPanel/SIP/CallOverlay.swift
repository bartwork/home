import SwiftUI

/// Полноэкранный звонок: звук SIP + картинка двери по ISAPI.
struct ActiveCallOverlay: View {
    @Bindable var sip: SipService
    @Bindable var camera: DoorCameraFeed

    var body: some View {
        ZStack {
            DoorCameraView(feed: camera)
                .ignoresSafeArea()
                .clipShape(Rectangle())

            VStack(spacing: 0) {
                HStack {
                    statusChip
                    Spacer()
                    if !sip.remoteDisplay.isEmpty {
                        Text(sip.remoteDisplay)
                            .font(.caption.weight(.medium))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 7)
                            .background(.ultraThinMaterial, in: Capsule())
                    }
                }
                .padding(20)

                Spacer()

                controls
                    .padding(.bottom, 36)
                    .padding(.horizontal, 24)
                    .frame(maxWidth: .infinity)
                    .background {
                        LinearGradient(
                            colors: [.clear, .black.opacity(0.75)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                        .ignoresSafeArea(edges: .bottom)
                    }
            }
        }
        .background(Color.black)
    }

    private var statusChip: some View {
        HStack(spacing: 8) {
            Circle()
                .fill(phaseColor)
                .frame(width: 8, height: 8)
            Text(statusTitle)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.white)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .background(.ultraThinMaterial, in: Capsule())
    }

    @ViewBuilder
    private var controls: some View {
        if sip.callPhase == .incoming {
            HStack(spacing: 48) {
                callButton(title: "Отклонить", systemImage: "phone.down.fill", tint: PanelTheme.danger, action: sip.hangup)
                callButton(title: "Принять", systemImage: "phone.fill", tint: PanelTheme.accent, action: sip.accept)
            }
        } else {
            HStack(spacing: 36) {
                callButton(
                    title: sip.isSpeakerMuted ? "Звук выкл" : "Звук",
                    systemImage: sip.isSpeakerMuted ? "speaker.slash.fill" : "speaker.wave.2.fill",
                    tint: .white,
                    action: sip.toggleSpeakerMute
                )
                callButton(
                    title: sip.isMicMuted ? "Мик. выкл" : "Микрофон",
                    systemImage: sip.isMicMuted ? "mic.slash.fill" : "mic.fill",
                    tint: .white,
                    action: sip.toggleMicMute
                )
                callButton(title: "Завершить", systemImage: "phone.down.fill", tint: PanelTheme.danger, action: sip.hangup)
            }
        }
    }

    private func callButton(title: String, systemImage: String, tint: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: systemImage)
                    .font(.title2)
                    .foregroundStyle(tint)
                    .frame(width: 64, height: 64)
                    .background(Color.white.opacity(0.12), in: Circle())
                Text(title)
                    .font(.caption)
                    .foregroundStyle(.white)
            }
        }
        .buttonStyle(.plain)
    }

    private var statusTitle: String {
        switch sip.callPhase {
        case .incoming: return "Входящий"
        case .outgoing: return "Вызов…"
        case .connected: return "Разговор"
        case .idle: return ""
        }
    }

    private var phaseColor: Color {
        switch sip.callPhase {
        case .incoming: return PanelTheme.accent
        case .outgoing: return PanelTheme.warning
        case .connected: return PanelTheme.accent
        case .idle: return .secondary
        }
    }
}
