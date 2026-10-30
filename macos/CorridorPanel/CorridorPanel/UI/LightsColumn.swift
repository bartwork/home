import SwiftUI

enum HomeZone: String, CaseIterable, Identifiable {
    case room, kitchen, hall, wet, climate
    var id: String { rawValue }
    var title: String {
        switch self {
        case .room: return "Зал"
        case .kitchen: return "Кухня"
        case .hall: return "Прихожая"
        case .wet: return "Санузел"
        case .climate: return "Климат"
        }
    }
    var symbol: String {
        switch self {
        case .room: return "sofa"
        case .kitchen: return "fork.knife"
        case .hall: return "door.left.hand.closed"
        case .wet: return "drop"
        case .climate: return "thermometer.medium"
        }
    }
}

struct ZoneContent: View {
    @Bindable var mqtt: MqttService
    let zone: HomeZone

    var body: some View {
        switch zone {
        case .room:
            ScrollView {
                VStack(spacing: 8) {
                    ForEach(Topics.roomDimmers) { DimmerRow(mqtt: mqtt, dimmer: $0) }
                    LedStripBlock(mqtt: mqtt)
                }
            }
        case .kitchen:
            ScrollView {
                VStack(spacing: 8) {
                    ForEach(Topics.kitchenDimmers) { DimmerRow(mqtt: mqtt, dimmer: $0) }
                    Text("Выключатели")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    ForEach(Topics.kitchenSwitches) { SwitchRow(mqtt: mqtt, item: $0) }
                }
            }
        case .hall:
            ScrollView {
                VStack(spacing: 8) {
                    ForEach(Topics.masterSwitches) { SwitchRow(mqtt: mqtt, item: $0) }
                    ForEach(Topics.hallDimmers) { DimmerRow(mqtt: mqtt, dimmer: $0) }
                    Text("Кладовка")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    ForEach(Topics.storageSwitches) { SwitchRow(mqtt: mqtt, item: $0) }
                }
            }
        case .wet:
            ScrollView {
                WetRoomsColumn(mqtt: mqtt)
            }
        case .climate:
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Кондиционеры")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.secondary)
                    ClimateColumn(mqtt: mqtt)
                    Text("Тёплый пол")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.secondary)
                        .padding(.top, 4)
                    FloorColumn(mqtt: mqtt)
                        .frame(minHeight: 220)
                }
            }
        }
    }
}
