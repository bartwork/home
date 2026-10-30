import AppKit
import UserNotifications

/// Баннер Notification Center при входящем с двери: звук + «Принять» / «Отклонить».
enum CallNotifier {
    static let categoryId = "door.incoming"
    static let actionAccept = "door.accept"
    static let actionDecline = "door.decline"
    private static let requestId = "door.incoming.active"

    static func configure(delegate: UNUserNotificationCenterDelegate) {
        let center = UNUserNotificationCenter.current()
        center.delegate = delegate

        let accept = UNNotificationAction(
            identifier: actionAccept,
            title: "Принять",
            options: [.foreground]
        )
        let decline = UNNotificationAction(
            identifier: actionDecline,
            title: "Отклонить",
            options: [.destructive]
        )
        let category = UNNotificationCategory(
            identifier: categoryId,
            actions: [accept, decline],
            intentIdentifiers: [],
            options: []
        )
        center.setNotificationCategories([category])
        center.requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
    }

    static func notifyIncoming(from remote: String) {
        let content = UNMutableNotificationContent()
        content.title = "Входящий звонок"
        content.subtitle = "Дверь · домофон"
        let clean = remote.trimmingCharacters(in: .whitespacesAndNewlines)
        if clean.isEmpty || clean.lowercased().hasPrefix("sip:") {
            content.body = "Откройте панель, чтобы ответить"
        } else {
            content.body = clean
        }
        content.sound = UNNotificationSound.default
        content.categoryIdentifier = categoryId
        content.threadIdentifier = "door"
        content.interruptionLevel = .timeSensitive
        content.relevanceScore = 1

        let request = UNNotificationRequest(
            identifier: requestId,
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request)

        // Док / внимание, если приложение не в фокусе.
        if NSApp.isHidden || NSApp.activationPolicy() == .accessory || NSApp.keyWindow == nil {
            NSApp.requestUserAttention(.criticalRequest)
        }
    }

    static func clear() {
        let center = UNUserNotificationCenter.current()
        center.removeDeliveredNotifications(withIdentifiers: [requestId])
        center.removePendingNotificationRequests(withIdentifiers: [requestId])
        NSApp.dockTile.badgeLabel = nil
    }
}
