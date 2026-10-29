import SwiftUI

// MARK: - Theme

enum HomeTheme {
    /// Спокойный бирюзово-синий акцент (не «AI purple»).
    static let accent = Color(red: 0.12, green: 0.55, blue: 0.58)
    static let accentSoft = Color(red: 0.12, green: 0.55, blue: 0.58).opacity(0.14)
    static let danger = Color(red: 0.92, green: 0.28, blue: 0.30)
    static let success = Color(red: 0.18, green: 0.72, blue: 0.42)
    static let warning = Color(red: 0.95, green: 0.62, blue: 0.18)
    static let callScrim = Color.black.opacity(0.55)

    static let cardRadius: CGFloat = 18
    static let controlSize: CGFloat = 64
}

// MARK: - Status

struct StatusDot: View {
    enum Kind {
        case online, offline, warning, unknown

        var color: Color {
            switch self {
            case .online: return HomeTheme.success
            case .offline: return HomeTheme.danger
            case .warning: return HomeTheme.warning
            case .unknown: return .secondary
            }
        }
    }

    let kind: Kind
    let title: String
    var detail: String?

    var body: some View {
        HStack(spacing: 10) {
            ZStack {
                Circle()
                    .fill(kind.color.opacity(0.22))
                    .frame(width: 18, height: 18)
                Circle()
                    .fill(kind.color)
                    .frame(width: 8, height: 8)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline.weight(.medium))
                if let detail, !detail.isEmpty {
                    Text(detail)
                        .font(.caption2)
                        .foregroundStyle(HomeTheme.danger)
                        .lineLimit(2)
                }
            }
            Spacer(minLength: 0)
        }
    }
}

// MARK: - Surfaces

struct PanelCard<Content: View>: View {
    var padding: CGFloat = 16
    @ViewBuilder var content: () -> Content

    var body: some View {
        content()
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background {
                RoundedRectangle(cornerRadius: HomeTheme.cardRadius, style: .continuous)
                    .fill(Color(.secondarySystemGroupedBackground))
            }
            .overlay {
                RoundedRectangle(cornerRadius: HomeTheme.cardRadius, style: .continuous)
                    .strokeBorder(Color.primary.opacity(0.06), lineWidth: 1)
            }
    }
}

struct SectionHeaderLabel: View {
    let title: String
    var systemImage: String?

    var body: some View {
        HStack(spacing: 8) {
            if let systemImage {
                Image(systemName: systemImage)
                    .foregroundStyle(HomeTheme.accent)
            }
            Text(title)
                .font(.headline)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 4)
    }
}

// MARK: - Call controls

struct CallControlButton: View {
    enum Style {
        case primary, danger, muted, ghost
    }

    let systemImage: String
    let label: String
    var style: Style = .ghost
    var size: CGFloat = HomeTheme.controlSize
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: systemImage)
                    .font(.system(size: size * 0.34, weight: .semibold))
                    .foregroundStyle(foreground)
                    .frame(width: size, height: size)
                    .background(Circle().fill(fillColor))
                    .shadow(color: shadow, radius: 10, y: 4)
                Text(label)
                    .font(.caption.weight(.medium))
                    .foregroundStyle(.white.opacity(0.9))
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }

    private var foreground: Color {
        switch style {
        case .primary, .danger, .muted: return .white
        case .ghost: return .white
        }
    }

    private var fillColor: Color {
        switch style {
        case .primary: return HomeTheme.success
        case .danger: return HomeTheme.danger
        case .muted: return HomeTheme.warning
        case .ghost: return Color.white.opacity(0.18)
        }
    }

    private var shadow: Color {
        switch style {
        case .danger: return HomeTheme.danger.opacity(0.35)
        case .primary: return HomeTheme.success.opacity(0.35)
        default: return .clear
        }
    }
}

// MARK: - Primary actions

struct HomePrimaryButton: View {
    let title: String
    let systemImage: String
    var enabled: Bool = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Label(title, systemImage: systemImage)
                .font(.headline)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .foregroundStyle(.white)
                .background(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(enabled ? HomeTheme.accent : Color.secondary.opacity(0.35))
                )
        }
        .disabled(!enabled)
        .buttonStyle(.plain)
    }
}

// MARK: - Tablet-style controls

struct PanelChip: View {
    let title: String
    var active: Bool = false
    var compact: Bool = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font((compact ? Font.caption : Font.subheadline).weight(.semibold))
                .lineLimit(1)
                .minimumScaleFactor(0.75)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .foregroundStyle(active ? HomeTheme.accent : .primary)
                .background(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(active ? HomeTheme.accentSoft : Color.primary.opacity(0.06))
                )
                .overlay {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .strokeBorder(active ? HomeTheme.accent.opacity(0.45) : .clear, lineWidth: 1)
                }
        }
        .buttonStyle(.plain)
    }
}

struct RoundIconButton: View {
    let systemImage: String?
    var title: String? = nil
    var active: Bool = false
    var size: CGFloat = 48
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Group {
                if let systemImage {
                    Image(systemName: systemImage)
                        .font(.system(size: size * 0.38, weight: .semibold))
                } else if let title {
                    Text(title)
                        .font(.system(size: size * 0.34, weight: .bold))
                }
            }
            .foregroundStyle(active ? .white : .primary)
            .frame(width: size, height: size)
            .background(
                Circle().fill(active ? HomeTheme.accent : Color.primary.opacity(0.08))
            )
        }
        .buttonStyle(.plain)
    }
}

struct SegmentBar: View {
    let titles: [String]
    @Binding var selection: Int

    var body: some View {
        HStack(spacing: 6) {
            ForEach(titles.indices, id: \.self) { i in
                Button {
                    selection = i
                } label: {
                    Text(titles[i])
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 11)
                        .foregroundStyle(selection == i ? .white : .secondary)
                        .background(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .fill(selection == i ? HomeTheme.accent : Color.primary.opacity(0.06))
                        )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(4)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(Color(.secondarySystemGroupedBackground))
        )
    }
}

/// iPad: в ряд; iPhone: столбик.
struct AdaptivePanelStack<Content: View>: View {
    var sideBySide: Bool
    @ViewBuilder var content: () -> Content

    var body: some View {
        Group {
            if sideBySide {
                HStack(alignment: .top, spacing: 14) { content() }
            } else {
                VStack(spacing: 14) { content() }
            }
        }
    }
}

extension Color {
    static func ledHSV(h: Int, s: Int, v: Int) -> Color {
        let rgb = LedPalette.rgb(h: h, s: s, v: v)
        return Color(red: rgb.0, green: rgb.1, blue: rgb.2)
    }
}

