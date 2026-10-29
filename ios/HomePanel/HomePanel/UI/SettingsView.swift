import SwiftUI

struct SettingsView: View {
    @Bindable var settings: AppSettings
    var onApply: () -> Void

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    labeledField("Host", text: $settings.mqttHost)
                    TextField("Port", value: $settings.mqttPort, format: .number)
                        .keyboardType(.numberPad)
                } header: {
                    Label("MQTT · Wiren Board", systemImage: "antenna.radiowaves.left.and.right")
                }

                Section {
                    labeledField("Domain / IP", text: $settings.sipDomain)
                    labeledField("Username", text: $settings.sipUsername)
                    SecureField("Password", text: $settings.sipPassword)
                    Picker("Transport", selection: $settings.sipTransport) {
                        Text("UDP").tag("udp")
                        Text("TCP").tag("tcp")
                        Text("TLS").tag("tls")
                    }
                    TextField("Домофон (ext)", text: $settings.doorExtension)
                        .keyboardType(.numberPad)
                } header: {
                    Label("SIP · Asterisk", systemImage: "phone.connection")
                }

                Section {
                    labeledField("Host / IP", text: $settings.doorCameraHost)
                    labeledField("User", text: $settings.doorCameraUser)
                    SecureField("Password", text: $settings.doorCameraPassword)
                } header: {
                    Label("Камера двери · ISAPI", systemImage: "camera.fill")
                }

                Section {
                    Button {
                        onApply()
                    } label: {
                        Label("Сохранить и переподключить", systemImage: "arrow.triangle.2.circlepath")
                            .frame(maxWidth: .infinity)
                            .font(.body.weight(.semibold))
                    }
                    .tint(HomeTheme.accent)
                }

                Section("Справка") {
                    LabeledContent("MQTT") {
                        Text("\(AppSettings.Defaults.mqttHost):\(AppSettings.Defaults.mqttPort)")
                            .font(.caption.monospaced())
                            .foregroundStyle(.secondary)
                    }
                    LabeledContent("SIP") {
                        Text("\(AppSettings.Defaults.sipUsername) → \(AppSettings.Defaults.sipDomain)")
                            .font(.caption.monospaced())
                            .foregroundStyle(.secondary)
                    }
                    LabeledContent("Дверь") {
                        Text("\(AppSettings.Defaults.doorExtension) / \(AppSettings.Defaults.doorCameraHost)")
                            .font(.caption.monospaced())
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .navigationTitle("Настройки")
        }
    }

    private func labeledField(_ title: String, text: Binding<String>) -> some View {
        TextField(title, text: text)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .keyboardType(.asciiCapable)
    }
}
