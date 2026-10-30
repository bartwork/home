import Foundation

struct UsageEntry: Identifiable, Equatable {
    var id: String { topic }
    let topic: String
    let title: String
    let count: Int
    let lastUsed: Date?
}

/// Счётчик команд MQTT (publish) — для будущего «главного» экрана по частоте.
@Observable
final class UsageStats {
    private static let prefsKey = "usage.topicCounts.v1"
    private static let lastKey = "usage.topicLast.v1"
    /// Антиспам слайдеров: не чаще 1 раза / 1.5 с на топик.
    private static let debounce: TimeInterval = 1.5

    private(set) var counts: [String: Int] = [:]
    private(set) var lastUsed: [String: Date] = [:]
    private var lastCountedAt: [String: Date] = [:]

    init() {
        load()
    }

    func record(topic: String) {
        let key = topic.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !key.isEmpty else { return }
        let now = Date()
        lastUsed[key] = now
        if let prev = lastCountedAt[key], now.timeIntervalSince(prev) < Self.debounce {
            persist()
            return
        }
        lastCountedAt[key] = now
        counts[key, default: 0] += 1
        persist()
    }

    func clear() {
        counts = [:]
        lastUsed = [:]
        lastCountedAt = [:]
        persist()
    }

    var totalActions: Int { counts.values.reduce(0, +) }

    func top(limit: Int = 20) -> [UsageEntry] {
        counts
            .sorted { lhs, rhs in
                if lhs.value != rhs.value { return lhs.value > rhs.value }
                let l = lastUsed[lhs.key] ?? .distantPast
                let r = lastUsed[rhs.key] ?? .distantPast
                return l > r
            }
            .prefix(limit)
            .map { topic, count in
                UsageEntry(
                    topic: topic,
                    title: TopicLabels.displayTitle(for: topic),
                    count: count,
                    lastUsed: lastUsed[topic]
                )
            }
    }

    private func load() {
        let defaults = UserDefaults.standard
        if let raw = defaults.dictionary(forKey: Self.prefsKey) as? [String: Int] {
            counts = raw
        }
        if let rawDates = defaults.dictionary(forKey: Self.lastKey) as? [String: Double] {
            lastUsed = rawDates.mapValues { Date(timeIntervalSince1970: $0) }
        }
    }

    private func persist() {
        let defaults = UserDefaults.standard
        defaults.set(counts, forKey: Self.prefsKey)
        defaults.set(lastUsed.mapValues(\.timeIntervalSince1970), forKey: Self.lastKey)
    }
}
