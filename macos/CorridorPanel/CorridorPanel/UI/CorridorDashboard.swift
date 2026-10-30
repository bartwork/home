import SwiftUI

/// Центр «Мой дом»: rail зон + дверь + контент.
struct CorridorDashboard: View {
    @Bindable var session: PanelSession
    @State private var showSettings = false
    @State private var cameraExpanded = false
    @State private var zone: HomeZone = .room

    var body: some View {
        ZStack {
            PanelTheme.surface2

            VStack(spacing: 10) {
                header
                bodyRow
            }
            .padding(12)
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            if cameraExpanded && !session.sip.isInCall {
                DoorCameraFullscreen(feed: session.doorCamera) {
                    cameraExpanded = false
                }
                .transition(.opacity)
                .zIndex(1)
            }

            if session.sip.isInCall {
                ActiveCallOverlay(sip: session.sip, camera: session.doorCamera)
                    .transition(.opacity)
                    .zIndex(2)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .preferredColorScheme(session.settings.appearance.colorScheme)
        .sheet(isPresented: $showSettings) {
            SettingsView(
                settings: session.settings,
                usage: session.usage,
                errors: session.errors,
                onSave: {
                    session.reconnect()
                    showSettings = false
                }
            )
            .frame(width: 480, height: 640)
            .preferredColorScheme(session.settings.appearance.colorScheme)
        }
        .onChange(of: session.sip.callPhase) { _, phase in
            if phase == .incoming {
                cameraExpanded = false
                PanelChrome.showFromTray(enterFullScreen: true)
            }
        }
        .onChange(of: session.sip.isInCall) { _, inCall in
            if inCall { cameraExpanded = false }
        }
        .animation(.easeInOut(duration: 0.2), value: cameraExpanded)
    }

    private var header: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(Topics.displayName)
                    .font(.title2.weight(.semibold))
                Text("центр управления")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 8)
            StatusPill(
                online: session.sip.registration == .ok,
                warn: session.sip.registration == .progress,
                label: session.sip.registration.title
            )
            StatusPill(
                online: session.mqtt.connected,
                label: session.mqtt.connected
                    ? "MQTT"
                    : (session.mqtt.lastError.map { "MQTT: \($0)" } ?? "MQTT")
            )
            Button { showSettings = true } label: {
                Image(systemName: "gearshape")
            }
            .help("Настройки")
            Button { PanelChrome.hideToTray() } label: {
                Image(systemName: "menubar.rectangle")
            }
            .help("Скрыть в трей")
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(PanelTheme.surface)
                .overlay {
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .strokeBorder(PanelTheme.hairline, lineWidth: 1)
                }
        )
    }

    private var bodyRow: some View {
        HStack(alignment: .top, spacing: 10) {
            ZoneRail(selected: $zone)
                .frame(width: 96)

            ColumnCard(title: nil) {
                DoorColumn(session: session, cameraExpanded: $cameraExpanded)
            }
            .frame(width: 300)

            ColumnCard(title: zone.title) {
                ZoneContent(mqtt: session.mqtt, zone: zone)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct ZoneRail: View {
    @Binding var selected: HomeZone

    var body: some View {
        VStack(spacing: 6) {
            Image(systemName: "house.fill")
                .font(.title3)
                .foregroundStyle(PanelTheme.accent)
                .frame(width: 40, height: 40)
                .background(PanelTheme.accentSoft, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                .padding(.bottom, 4)

            ForEach(HomeZone.allCases) { z in
                let on = z == selected
                Button {
                    selected = z
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: z.symbol)
                            .font(.body.weight(.semibold))
                        Text(z.title)
                            .font(.caption2.weight(.medium))
                            .lineLimit(1)
                    }
                    .foregroundStyle(on ? PanelTheme.accent : .secondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .fill(on ? PanelTheme.accentSoft : .clear)
                            .overlay {
                                RoundedRectangle(cornerRadius: 14, style: .continuous)
                                    .strokeBorder(on ? PanelTheme.accent.opacity(0.45) : .clear, lineWidth: 1)
                            }
                    )
                }
                .buttonStyle(.plain)
            }
            Spacer(minLength: 0)
        }
        .padding(.vertical, 10)
        .padding(.horizontal, 8)
        .frame(maxHeight: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(PanelTheme.surface)
                .overlay {
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .strokeBorder(PanelTheme.hairline, lineWidth: 1)
                }
        )
    }
}
