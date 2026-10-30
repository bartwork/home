import SwiftUI

enum PanelCanvas {
    /// Как Android SM10P / YC landscape.
    static let width: CGFloat = 1280
    static let height: CGFloat = 800

    static func scale(for size: CGSize) -> CGFloat {
        min(max(size.width, 1) / width, max(size.height, 1) / height)
    }
}

/// Логический холст 1280×800: масштабируется под окно (letterbox, как Android `PanelFit`).
struct PanelFit<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        GeometryReader { geo in
            let scale = PanelCanvas.scale(for: geo.size)
            ZStack {
                PanelTheme.surface2
                content()
                    .frame(width: PanelCanvas.width, height: PanelCanvas.height)
                    .scaleEffect(scale, anchor: .center)
                    .frame(width: geo.size.width, height: geo.size.height)
            }
        }
        .background(PanelTheme.surface2)
    }
}
