import SwiftUI

enum AppAppearance: String, CaseIterable, Identifiable {
    case dark, light
    var id: String { rawValue }
    var title: String {
        switch self {
        case .dark: return "Тёмная"
        case .light: return "Светлая"
        }
    }
    var colorScheme: ColorScheme {
        switch self {
        case .dark: return .dark
        case .light: return .light
        }
    }
}

enum PanelTheme {
    static let accent = Color(red: 0.12, green: 0.55, blue: 0.58)
    static let accentSoft = accent.opacity(0.18)
    static let danger = Color(red: 0.90, green: 0.35, blue: 0.32)
    static let warning = Color(red: 0.95, green: 0.62, blue: 0.18)
    static let cardRadius: CGFloat = 18

    /// Системные поверхности — автоматически светлые/тёмные с preferredColorScheme.
    static let surface = Color(nsColor: .controlBackgroundColor)
    static let surface2 = Color(nsColor: .windowBackgroundColor)
    static let hairline = Color.primary.opacity(0.10)
}
