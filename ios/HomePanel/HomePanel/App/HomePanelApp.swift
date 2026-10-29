import SwiftUI

@main
struct HomePanelApp: App {
    @State private var session = HomeSession()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            RootTabView(session: session)
                .onAppear { session.start() }
                .onChange(of: scenePhase) { _, phase in
                    if phase == .active {
                        session.start()
                    }
                }
        }
    }
}
