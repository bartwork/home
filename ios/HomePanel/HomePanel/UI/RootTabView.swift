import SwiftUI

struct RootTabView: View {
    @Bindable var session: HomeSession

    var body: some View {
        TabView {
            LightsView(mqtt: session.mqtt)
                .tabItem { Label("Дом", systemImage: "house.fill") }

            DoorView(sip: session.sip, settings: session.settings)
                .tabItem { Label("Домофон", systemImage: "door.left.hand.closed") }

            SettingsView(settings: session.settings) {
                session.reconnect()
            }
            .tabItem { Label("Настройки", systemImage: "gearshape.fill") }
        }
        .tint(HomeTheme.accent)
    }
}
