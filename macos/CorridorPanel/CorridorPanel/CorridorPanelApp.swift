import SwiftUI

@main
struct CorridorPanelApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            PanelFit {
                CorridorDashboard(session: appDelegate.session)
            }
            .ignoresSafeArea()
            .frame(minWidth: 900, minHeight: 560)
            .preferredColorScheme(appDelegate.session.settings.appearance.colorScheme)
            .background(WindowAccessor())
            .onAppear {
                // Старт SIP — только из AppDelegate (один вход). Здесь — chrome.
                PanelChrome.attachCloseGuardIfNeeded()
            }
            .onChange(of: scenePhase) { _, phase in
                if phase == .active {
                    // Мягкий resume без повторного reconnect, если уже online.
                    appDelegate.session.start()
                    PanelChrome.attachCloseGuardIfNeeded()
                }
            }
        }
        .defaultSize(width: PanelCanvas.width, height: PanelCanvas.height)
        .windowResizability(.contentMinSize)
        .commands {
            CommandGroup(replacing: .newItem) {}
            CommandGroup(after: .windowArrangement) {
                Button("Скрыть в трей") {
                    PanelChrome.hideToTray()
                }
                .keyboardShortcut("w", modifiers: [.command])
                Button("Показать панель") {
                    PanelChrome.showFromTray(enterFullScreen: true)
                }
                .keyboardShortcut("o", modifiers: [.command])
                Button("На весь экран") {
                    PanelChrome.toggleFullScreen()
                }
                .keyboardShortcut("f", modifiers: [.command, .control])
            }
        }

        Settings {
            SettingsView(
                settings: appDelegate.session.settings,
                usage: appDelegate.session.usage,
                errors: appDelegate.session.errors,
                onSave: { appDelegate.session.reconnect() }
            )
            .frame(width: 480, height: 640)
            .preferredColorScheme(appDelegate.session.settings.appearance.colorScheme)
        }
    }
}

/// Подцепляет close→tray после появления NSWindow.
private struct WindowAccessor: NSViewRepresentable {
    func makeNSView(context: Context) -> NSView {
        let view = NSView()
        DispatchQueue.main.async {
            PanelChrome.attachCloseGuardIfNeeded()
        }
        return view
    }

    func updateNSView(_ nsView: NSView, context: Context) {
        DispatchQueue.main.async {
            PanelChrome.attachCloseGuardIfNeeded()
        }
    }
}
