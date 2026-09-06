import SwiftUI

struct LoginView: View {
    @EnvironmentObject private var session: Session

    @State private var serverURL = Api.shared.baseURL
    @State private var username = Api.shared.lastUsername
    @State private var password = ""
    @State private var busy = false
    @State private var error: String?

    var body: some View {
        NavigationStack {
            Form {
                Section("Сервер") {
                    TextField("http://IP:9087", text: $serverURL)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.URL)
                }
                Section("Учётная запись") {
                    TextField("Логин", text: $username)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    SecureField("Пароль", text: $password)
                }
                if let error {
                    Text(error).foregroundStyle(.red).font(.footnote)
                }
                Section {
                    Button {
                        Task { await signIn() }
                    } label: {
                        HStack {
                            Spacer()
                            if busy { ProgressView() } else { Text("Войти").bold() }
                            Spacer()
                        }
                    }
                    .disabled(busy)
                }
            }
            .navigationTitle("FixByte CRM")
        }
    }

    private func signIn() async {
        let url = serverURL.trimmedTrailingSlash
        guard !url.isEmpty, !username.isEmpty, !password.isEmpty else {
            error = "Заполните все поля"; return
        }
        busy = true; error = nil
        Api.shared.baseURL = url
        do {
            let info = try await Api.shared.login(username: username, password: password)
            Api.shared.lastUsername = username
            session.afterLogin(info)
        } catch {
            self.error = error.localizedDescription
        }
        busy = false
    }
}
