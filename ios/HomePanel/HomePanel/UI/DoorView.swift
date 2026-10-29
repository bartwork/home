import SwiftUI

struct DoorView: View {
    @Bindable var sip: SipService
    @Bindable var settings: AppSettings

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    statusCard
                    callCard
                    hintCard

                    if let err = sip.lastError {
                        errorBanner(err)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
                .padding(.bottom, 28)
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("Домофон")
            .fullScreenCover(isPresented: Binding(
                get: { sip.isInCall },
                set: { presented in
                    if !presented { sip.hangup() }
                }
            )) {
                ActiveCallOverlay(sip: sip, settings: settings)
            }
        }
    }

    private var statusCard: some View {
        PanelCard {
            VStack(alignment: .leading, spacing: 12) {
                StatusDot(
                    kind: registrationKind,
                    title: sip.registration.title,
                    detail: sip.registration == .failed ? sip.lastError : nil
                )

                Divider().opacity(0.5)

                HStack(spacing: 10) {
                    Image(systemName: "person.crop.circle")
                        .foregroundStyle(HomeTheme.accent)
                    Text(settings.sipIdentity)
                        .font(.caption.monospaced())
                        .foregroundStyle(.secondary)
                        .textSelection(.enabled)
                }

                if !sip.registrationMessage.isEmpty, sip.registration != .ok {
                    Text(sip.registrationMessage)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
        }
    }

    private var callCard: some View {
        PanelCard(padding: 20) {
            VStack(spacing: 18) {
                ZStack {
                    Circle()
                        .fill(HomeTheme.accentSoft)
                        .frame(width: 96, height: 96)
                    Image(systemName: "door.left.hand.closed")
                        .font(.system(size: 36, weight: .medium))
                        .foregroundStyle(HomeTheme.accent)
                }

                VStack(spacing: 6) {
                    Text("Вызов двери")
                        .font(.title3.weight(.semibold))
                    Text("SIP \(settings.doorExtension) · камера \(settings.doorCameraHost)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }

                HomePrimaryButton(
                    title: "Позвонить",
                    systemImage: "phone.fill",
                    enabled: sip.registration == .ok && !sip.isInCall
                ) {
                    sip.callDoor(extension: settings.doorExtension)
                }
            }
            .frame(maxWidth: .infinity)
        }
    }

    private var hintCard: some View {
        PanelCard {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: "info.circle.fill")
                    .foregroundStyle(HomeTheme.accent)
                    .font(.title3)
                Text("Аудио идёт по SIP, картинка — с камеры панели (ISAPI). Можно ждать входящий с кнопки двери.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    private func errorBanner(_ text: String) -> some View {
        PanelCard {
            HStack(alignment: .top, spacing: 10) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .foregroundStyle(HomeTheme.danger)
                Text(text)
                    .font(.subheadline)
                    .foregroundStyle(HomeTheme.danger)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    private var registrationKind: StatusDot.Kind {
        switch sip.registration {
        case .ok: return .online
        case .progress: return .warning
        case .failed: return .offline
        case .none, .cleared: return .unknown
        }
    }
}
