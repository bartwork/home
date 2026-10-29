import SwiftUI

/// Полноэкранный звонок: звук SIP + картинка двери по ISAPI.
struct ActiveCallOverlay: View {
    @Bindable var sip: SipService
    @Bindable var settings: AppSettings

    var body: some View {
        ZStack {
            DoorCameraView(
                host: settings.doorCameraHost,
                username: settings.doorCameraUser,
                password: settings.doorCameraPassword
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                topBar
                    .padding(.horizontal, 20)
                    .padding(.top, 12)

                Spacer(minLength: 0)

                bottomBar
            }
        }
        .background(Color.black)
        .statusBarHidden(true)
    }

    private var topBar: some View {
        HStack {
            statusChip
            Spacer()
            if !sip.remoteDisplay.isEmpty {
                Text(sip.remoteDisplay)
                    .font(.caption.weight(.medium))
                    .foregroundStyle(.white.opacity(0.9))
                    .lineLimit(1)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 7)
                    .background(.ultraThinMaterial, in: Capsule())
            }
        }
    }

    private var statusChip: some View {
        HStack(spacing: 8) {
            Circle()
                .fill(phaseColor)
                .frame(width: 8, height: 8)
                .shadow(color: phaseColor.opacity(0.8), radius: 4)
            Text(statusTitle)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.white)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .background(.ultraThinMaterial, in: Capsule())
    }

    private var bottomBar: some View {
        VStack(spacing: 18) {
            controls
        }
        .padding(.horizontal, 24)
        .padding(.top, 28)
        .padding(.bottom, 36)
        .frame(maxWidth: .infinity)
        .background {
            LinearGradient(
                colors: [.clear, HomeTheme.callScrim, Color.black.opacity(0.85)],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea(edges: .bottom)
        }
    }

    @ViewBuilder
    private var controls: some View {
        if sip.callPhase == .incoming {
            HStack(spacing: 48) {
                CallControlButton(
                    systemImage: "phone.down.fill",
                    label: "Отклонить",
                    style: .danger,
                    action: sip.hangup
                )
                CallControlButton(
                    systemImage: "phone.fill",
                    label: "Принять",
                    style: .primary,
                    action: sip.accept
                )
            }
        } else {
            HStack(alignment: .top, spacing: 28) {
                CallControlButton(
                    systemImage: sip.isSpeakerMuted ? "speaker.slash.fill" : "speaker.wave.2.fill",
                    label: sip.isSpeakerMuted ? "Звук выкл" : "Звук",
                    style: sip.isSpeakerMuted ? .muted : .ghost,
                    action: sip.toggleSpeakerMute
                )
                CallControlButton(
                    systemImage: sip.isMicMuted ? "mic.slash.fill" : "mic.fill",
                    label: sip.isMicMuted ? "Мик. выкл" : "Микрофон",
                    style: sip.isMicMuted ? .muted : .ghost,
                    action: sip.toggleMicMute
                )
                CallControlButton(
                    systemImage: "phone.down.fill",
                    label: "Завершить",
                    style: .danger,
                    action: sip.hangup
                )
            }
        }
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
        case .incoming: return HomeTheme.success
        case .outgoing: return HomeTheme.warning
        case .connected: return HomeTheme.accent
        case .idle: return .secondary
        }
    }
}
