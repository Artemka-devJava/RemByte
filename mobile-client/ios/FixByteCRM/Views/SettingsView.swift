import SwiftUI

/// Настройки подключения: адрес сервера CRM.
/// Отдельно от экрана входа — задаётся один раз при установке приложения.
struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss

    @State private var serverURL = Api.shared.baseURL

    var body: some View {
        NavigationStack {
            Form {
                Section("Сервер") {
                    TextField("http://IP:9087", text: $serverURL)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.URL)
                }
                Text("Адрес сервера CRM. Задаётся один раз при настройке приложения.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            .navigationTitle("Настройки")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Отмена") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Сохранить") {
                        let url = serverURL.trimmedTrailingSlash
                        if !url.isEmpty { Api.shared.baseURL = url }
                        dismiss()
                    }
                }
            }
        }
    }
}
