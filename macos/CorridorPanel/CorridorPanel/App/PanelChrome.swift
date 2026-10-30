import AppKit

extension Notification.Name {
    static let sipIncomingCall = Notification.Name("ru.home.corridorpanel.sipIncomingCall")
    static let panelHideToTray = Notification.Name("ru.home.corridorpanel.hideToTray")
    static let panelShowFromTray = Notification.Name("ru.home.corridorpanel.showFromTray")
}

/// Управление главным окном: fullscreen, скрытие в трей, показ.
enum PanelChrome {
    private static var closeGuard = WindowCloseGuard()
    private static var didAutoFullScreen = false

    static func mainWindow() -> NSWindow? {
        NSApp.windows.first(where: { isPanelWindow($0) && $0.isVisible })
            ?? NSApp.windows.first(where: isPanelWindow)
            ?? NSApp.keyWindow
            ?? NSApp.windows.first(where: { $0.canBecomeKey })
    }

    static func isPanelWindow(_ window: NSWindow) -> Bool {
        // Settings / popovers — не трогаем.
        guard window.canBecomeKey else { return false }
        let title = window.title
        if title.localizedCaseInsensitiveContains("Settings")
            || title.localizedCaseInsensitiveContains("Настройки") {
            return false
        }
        return window.contentView != nil
    }

    static func attachCloseGuardIfNeeded() {
        guard let window = mainWindow() else { return }
        if window.delegate !== closeGuard {
            window.delegate = closeGuard
            window.isReleasedWhenClosed = false
        }
        rewireTrafficLights(on: window)
    }

    static func enterFullScreenSoon(force: Bool = false) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            attachCloseGuardIfNeeded()
            guard let window = mainWindow() else { return }
            if !force && didAutoFullScreen { return }
            if !window.styleMask.contains(.fullScreen) {
                window.toggleFullScreen(nil)
            }
            didAutoFullScreen = true
        }
    }

    static func toggleFullScreen() {
        attachCloseGuardIfNeeded()
        mainWindow()?.toggleFullScreen(nil)
    }

    static func hideToTray() {
        guard let window = mainWindow() else {
            NSApp.setActivationPolicy(.accessory)
            return
        }
        let finish = {
            window.orderOut(nil)
            // Только иконка в menu bar — без Dock, пока панель скрыта.
            NSApp.setActivationPolicy(.accessory)
        }
        if window.styleMask.contains(.fullScreen) {
            window.toggleFullScreen(nil)
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.45, execute: finish)
        } else {
            finish()
        }
    }

    static func showFromTray(enterFullScreen: Bool = true) {
        NSApp.setActivationPolicy(.regular)
        NSApp.activate(ignoringOtherApps: true)
        let window = NSApp.windows.first(where: isPanelWindow) ?? mainWindow()
        guard let window else { return }
        window.deminiaturize(nil)
        window.makeKeyAndOrderFront(nil)
        attachCloseGuardIfNeeded()
        if enterFullScreen && !window.styleMask.contains(.fullScreen) {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                window.toggleFullScreen(nil)
            }
        }
    }

    private static func rewireTrafficLights(on window: NSWindow) {
        if let close = window.standardWindowButton(.closeButton) {
            close.target = closeGuard
            close.action = #selector(WindowCloseGuard.hideToTrayAction(_:))
        }
        if let mini = window.standardWindowButton(.miniaturizeButton) {
            mini.target = closeGuard
            mini.action = #selector(WindowCloseGuard.hideToTrayAction(_:))
        }
    }
}

private final class WindowCloseGuard: NSObject, NSWindowDelegate {
    @objc func hideToTrayAction(_ sender: Any?) {
        PanelChrome.hideToTray()
    }

    func windowShouldClose(_ sender: NSWindow) -> Bool {
        PanelChrome.hideToTray()
        return false
    }

    func windowDidBecomeKey(_ notification: Notification) {
        if let window = notification.object as? NSWindow {
            PanelChrome.rewireTrafficLightsPublic(window)
        }
    }
}

private extension PanelChrome {
    static func rewireTrafficLightsPublic(_ window: NSWindow) {
        rewireTrafficLights(on: window)
    }
}
