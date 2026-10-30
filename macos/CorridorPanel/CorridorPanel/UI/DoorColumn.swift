import SwiftUI

struct DoorColumn: View {
    @Bindable var session: PanelSession
    @Binding var cameraExpanded: Bool

    private var callEnabled: Bool {
        session.sip.registration == .ok && !session.sip.isInCall
    }

    var body: some View {
        VStack(spacing: 10) {
            cameraSlot
                // KV6103 даёт ~4:3 (не 16:9) — рамка под поток, без боковых полос.
                .aspectRatio(4 / 3, contentMode: .fit)
                .frame(maxWidth: .infinity)
                .fixedSize(horizontal: false, vertical: true)

            callRow

            OutdoorWeatherRow(mqtt: session.mqtt)

            if let err = session.sip.lastError, !err.isEmpty {
                Text(err)
                    .font(.caption2)
                    .foregroundStyle(PanelTheme.danger)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            Spacer(minLength: 0)
        }
    }

    private var callRow: some View {
        HStack(spacing: 10) {
            Button {
                session.sip.callDoor(extension: session.settings.doorExtension)
            } label: {
                Label("Позвонить", systemImage: "phone.fill")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(PanelTheme.accent)
            .controlSize(.large)
            .disabled(!callEnabled)

            Button {
                session.doorCamera.toggleAudioMute()
            } label: {
                Image(systemName: session.doorCamera.audioMuted
                      ? "speaker.slash.fill"
                      : "speaker.wave.2.fill")
                    .font(.body.weight(.semibold))
                    .frame(width: 22, height: 22)
                    .frame(maxHeight: .infinity)
                    .padding(.horizontal, 14)
            }
            .buttonStyle(.bordered)
            .controlSize(.large)
            .help(session.doorCamera.audioMuted ? "Включить звук камеры" : "Выключить звук камеры")
            .tint(session.doorCamera.audioMuted ? .secondary : PanelTheme.accent)
        }
        .fixedSize(horizontal: false, vertical: true)
    }

    @ViewBuilder
    private var cameraSlot: some View {
        if cameraExpanded {
            ZStack {
                Color.black
                Image(systemName: "arrow.up.left.and.arrow.down.right")
                    .font(.title2)
                    .foregroundStyle(.white.opacity(0.35))
            }
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(Color.primary.opacity(0.08), lineWidth: 1)
            }
        } else {
            DoorCameraView(
                feed: session.doorCamera,
                compact: true,
                expandable: true,
                onExpand: { cameraExpanded = true }
            )
        }
    }
}
