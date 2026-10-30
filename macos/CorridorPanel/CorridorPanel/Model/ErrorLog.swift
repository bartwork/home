import Foundation

enum ErrorKind: String, Codable, CaseIterable, Identifiable {
    case mqtt
    case sip
    case camera
    case app

    var id: String { rawValue }

    var title: String {
        switch self {
        case .mqtt: return "MQTT"
        case .sip: return "SIP"
        case .camera: return "Камера"
        case .app: return "Приложение"
        }
    }
}

struct ErrorEntry: Identifiable, Codable, Equatable {
    let id: UUID
    let at: Date
    let kind: ErrorKind
    let message: String

    init(id: UUID = UUID(), at: Date = Date(), kind: ErrorKind, message: String) {
        self.id = id
        self.at = at
        self.kind = kind
        self.message = message
    }
}

/// Журнал ошибок с временем и типом (persist).
@Observable
final class ErrorLog {
    private static let prefsKey = "error.log.v1"
    private static let maxEntries = 100
    /// Не дублировать ту же пару тип+текст чаще этого интервала.
    private static let debounce: TimeInterval = 30

    private(set) var entries: [ErrorEntry] = []

    init() {
        load()
    }

    func record(kind: ErrorKind, message: String) {
        let msg = message.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !msg.isEmpty else { return }
        let now = Date()
        if let last = entries.first,
           last.kind == kind,
           last.message == msg,
           now.timeIntervalSince(last.at) < Self.debounce
        {
            return
        }
        entries.insert(ErrorEntry(at: now, kind: kind, message: msg), at: 0)
        if entries.count > Self.maxEntries {
            entries = Array(entries.prefix(Self.maxEntries))
        }
        persist()
    }

    func clear() {
        entries = []
        persist()
    }

    var isEmpty: Bool { entries.isEmpty }

    private func load() {
        guard let data = UserDefaults.standard.data(forKey: Self.prefsKey) else { return }
        let decoded = try? JSONDecoder().decode([ErrorEntry].self, from: data)
        entries = decoded ?? []
    }

    private func persist() {
        guard let data = try? JSONEncoder().encode(entries) else { return }
        UserDefaults.standard.set(data, forKey: Self.prefsKey)
    }
}

enum ErrorTimeFormat {
    private static let formatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "ru_RU")
        f.dateFormat = "dd.MM HH:mm:ss"
        return f
    }()

    static func string(from date: Date) -> String {
        formatter.string(from: date)
    }
}
