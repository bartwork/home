import AppKit
import SwiftUI
import UserNotifications

/// Lifecycle + иконка в menu bar (трей). Закрытие окна → в трей, не выход.
final class AppDelegate: NSObject, NSApplicationDelegate {
    let session = PanelSession()

    private var statusItem: NSStatusItem?
    private var callObserver: NSObjectProtocol?

    func applicationDidFinishLaunching(_ notification: Notification) {
        NSApp.setActivationPolicy(.regular)
        CallNotifier.configure(delegate: self)
        installStatusItem()
        observeIncomingCalls()
        // SIP/Linphone не поднимаем синхронно в didFinishLaunching —
        // гонка со SwiftUI onAppear роняла addAuthInfo (SIGSEGV).
        DispatchQueue.main.async {
            self.session.start()
            PanelChrome.attachCloseGuardIfNeeded()
            PanelChrome.enterFullScreenSoon()
        }
    }

    func applicationWillTerminate(_ notification: Notification) {
        if let callObserver { NotificationCenter.default.removeObserver(callObserver) }
        CallNotifier.clear()
        session.stop()
    }

    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool {
        false
    }

    func applicationShouldHandleReopen(_ sender: NSApplication, hasVisibleWindows flag: Bool) -> Bool {
        if !flag {
            PanelChrome.showFromTray(enterFullScreen: true)
        }
        return true
    }

    // MARK: - Tray

    private func installStatusItem() {
        let item = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
        if let button = item.button {
            button.image = NSImage(
                systemSymbolName: "door.left.hand.closed",
                accessibilityDescription: "Мой дом"
            )
            button.image?.isTemplate = true
            button.toolTip = "Мой дом"
        }
        item.menu = buildMenu()
        statusItem = item
    }

    private func buildMenu() -> NSMenu {
        let menu = NSMenu()

        menu.addItem(withTitle: "Показать панель", action: #selector(showPanel), keyEquivalent: "o")
            .target = self
        menu.addItem(withTitle: "Скрыть в трей", action: #selector(hidePanel), keyEquivalent: "h")
            .target = self
        menu.addItem(withTitle: "На весь экран", action: #selector(toggleFullScreen), keyEquivalent: "f")
            .target = self

        menu.addItem(.separator())

        let mqtt = menu.addItem(
            withTitle: mqttTitle(),
            action: nil,
            keyEquivalent: ""
        )
        mqtt.isEnabled = false
        mqtt.tag = 100

        let sip = menu.addItem(
            withTitle: sipTitle(),
            action: nil,
            keyEquivalent: ""
        )
        sip.isEnabled = false
        sip.tag = 101

        menu.addItem(.separator())
        menu.addItem(withTitle: "Выйти", action: #selector(quitApp), keyEquivalent: "q")
            .target = self

        menu.delegate = self
        return menu
    }

    private func mqttTitle() -> String {
        if session.mqtt.connected { return "MQTT · online" }
        if let err = session.mqtt.lastError, !err.isEmpty { return "MQTT · \(err)" }
        return "MQTT · offline"
    }

    private func sipTitle() -> String {
        "SIP · \(session.sip.registration.title)"
    }

    private func refreshStatusTitles() {
        guard let menu = statusItem?.menu else { return }
        menu.item(withTag: 100)?.title = mqttTitle()
        menu.item(withTag: 101)?.title = sipTitle()
    }

    @objc private func showPanel() {
        PanelChrome.showFromTray(enterFullScreen: true)
    }

    @objc private func hidePanel() {
        PanelChrome.hideToTray()
    }

    @objc private func toggleFullScreen() {
        PanelChrome.showFromTray(enterFullScreen: false)
        PanelChrome.toggleFullScreen()
    }

    @objc private func quitApp() {
        session.stop()
        NSApp.terminate(nil)
    }

    private func observeIncomingCalls() {
        callObserver = NotificationCenter.default.addObserver(
            forName: .sipIncomingCall,
            object: nil,
            queue: .main
        ) { [weak self] note in
            let remote = (note.userInfo?["remote"] as? String) ?? ""
            CallNotifier.notifyIncoming(from: remote)
            self?.flashTrayForCall()
            self?.showPanel()
        }
    }

    private func flashTrayForCall() {
        guard let button = statusItem?.button else { return }
        button.image = NSImage(
            systemSymbolName: "phone.badge.waveform.fill",
            accessibilityDescription: "Входящий звонок"
        )
        button.image?.isTemplate = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 8) { [weak self] in
            guard self?.session.sip.callPhase != .incoming else { return }
            self?.restoreTrayIcon()
        }
    }

    private func restoreTrayIcon() {
        statusItem?.button?.image = NSImage(
            systemSymbolName: "door.left.hand.closed",
            accessibilityDescription: "Мой дом"
        )
        statusItem?.button?.image?.isTemplate = true
    }
}

extension AppDelegate: NSMenuDelegate {
    func menuNeedsUpdate(_ menu: NSMenu) {
        refreshStatusTitles()
    }
}

extension AppDelegate: UNUserNotificationCenterDelegate {
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        // Баннер + звук даже если окно уже на экране.
        completionHandler([.banner, .sound, .list])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        switch response.actionIdentifier {
        case CallNotifier.actionAccept:
            showPanel()
            session.sip.accept()
        case CallNotifier.actionDecline:
            session.sip.hangup()
            CallNotifier.clear()
            restoreTrayIcon()
        default:
            // Клик по баннеру → открыть панель.
            showPanel()
        }
        completionHandler()
    }
}
