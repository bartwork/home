import SwiftUI

struct WetRoomsColumn: View {
    @Bindable var mqtt: MqttService

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            wetGroup(title: "Ванна", switches: Topics.bathSwitches)
            wetGroup(title: "Туалет", switches: Topics.toiletSwitches)
        }
    }

    private func wetGroup(title: String, switches: [Topics.LightSwitch]) -> some View {
        VStack(spacing: 8) {
            Text(title)
                .font(.caption.weight(.medium))
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
            ForEach(switches) { sw in
                SwitchRow(mqtt: mqtt, item: sw)
            }
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }
}
