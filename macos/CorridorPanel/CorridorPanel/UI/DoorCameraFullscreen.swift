import SwiftUI

/// Полноэкранный просмотр камеры двери (клик по превью).
struct DoorCameraFullscreen: View {
    @Bindable var feed: DoorCameraFeed
    var onClose: () -> Void

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            DoorCameraView(feed: feed, compact: false, expandable: false)
                .ignoresSafeArea()
                .clipShape(Rectangle())

            VStack {
                HStack {
                    Spacer()
                    Button(action: onClose) {
                        Image(systemName: "xmark")
                            .font(.title3.weight(.semibold))
                            .foregroundStyle(.white)
                            .frame(width: 44, height: 44)
                            .background(.ultraThinMaterial, in: Circle())
                    }
                    .buttonStyle(.plain)
                    .help("Закрыть")
                    .padding(20)
                }
                Spacer()
            }
        }
        .background(Color.black)
        .focusable()
        .onKeyPress(.escape) {
            onClose()
            return .handled
        }
    }
}
