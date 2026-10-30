import SwiftUI

struct SettingsView: View {
    @Bindable var settings: AppSettings
    @Bindable var usage: UsageStats
    @Bindable var errors: ErrorLog
    var onSave: () -> Void

    var body: some View {
        Form {
            Section("Оформление") {
                Picker("Тема", selection: $settings.appearance) {
                    ForEach(AppAppearance.allCases) { mode in
                        Text(mode.title).tag(mode)
                    }
                }
                .pickerStyle(.segmented)
            }
            Section("MQTT") {
                TextField("Хост", text: $settings.mqttHost)
                TextField("Порт", value: $settings.mqttPort, format: .number)
            }
            Section("SIP") {
                TextField("Domain (Asterisk)", text: $settings.sipDomain)
                TextField("User", text: $settings.sipUsername)
                SecureField("Пароль", text: $settings.sipPassword)
                TextField("Transport (udp/tcp/tls)", text: $settings.sipTransport)
                TextField("Ext двери", text: $settings.doorExtension)
            }
            Section("Камера двери (ISAPI)") {
                TextField("Host", text: $settings.doorCameraHost)
                TextField("User", text: $settings.doorCameraUser)
                SecureField("Пароль", text: $settings.doorCameraPassword)
            }

            usageSection
            errorLogSection

            Section {
                Button("Сохранить и переподключить") {
                    onSave()
                }
                .keyboardShortcut(.defaultAction)
            }
        }
        .formStyle(.grouped)
        .padding()
    }

    @ViewBuilder
    private var usageSection: some View {
        let top = usage.top(limit: 15)
        Section {
            if top.isEmpty {
                Text("Пока пусто — нажимайте выключатели и свет.")
                    .foregroundStyle(.secondary)
                    .font(.caption)
            } else {
                ForEach(top) { entry in
                    HStack(alignment: .firstTextBaseline) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(entry.title)
                                .font(.body.weight(.medium))
                            Text(entry.topic)
                                .font(.caption2.monospaced())
                                .foregroundStyle(.tertiary)
                                .lineLimit(1)
                        }
                        Spacer(minLength: 8)
                        Text("\(entry.count)×")
                            .font(.body.monospacedDigit().weight(.semibold))
                            .foregroundStyle(PanelTheme.accent)
                    }
                }
            }
            Button("Очистить статистику", role: .destructive) {
                usage.clear()
            }
            .disabled(usage.totalActions == 0)
        } header: {
            Text("Частое использование")
        } footer: {
            Text("Считаются команды MQTT (с антиспамом слайдеров). Потом по этому топу можно собрать главный экран. Всего действий: \(usage.totalActions).")
        }
    }

    @ViewBuilder
    private var errorLogSection: some View {
        Section {
            if errors.isEmpty {
                Text("Пока пусто.")
                    .foregroundStyle(.secondary)
                    .font(.caption)
            } else {
                ForEach(errors.entries.prefix(40)) { entry in
                    HStack(alignment: .top, spacing: 10) {
                        Text(ErrorTimeFormat.string(from: entry.at))
                            .font(.caption.monospacedDigit())
                            .foregroundStyle(.secondary)
                            .frame(width: 92, alignment: .leading)
                        Text(entry.kind.title)
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(PanelTheme.accent)
                            .frame(width: 72, alignment: .leading)
                        Text(entry.message)
                            .font(.caption)
                            .foregroundStyle(.primary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            }
            Button("Очистить журнал", role: .destructive) {
                errors.clear()
            }
            .disabled(errors.isEmpty)
        } header: {
            Text("Журнал ошибок")
        } footer: {
            Text("MQTT, SIP и камера. Повтор одной и той же ошибки не чаще раза в 30 с. Хранится до \(100) записей.")
        }
    }
}
