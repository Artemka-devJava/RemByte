import SwiftUI

struct LoginView: View {
    @EnvironmentObject private var session: Session

    @State private var username = Api.shared.lastUsername
    @State private var password = ""
    @State private var busy = false
    @State private var error: String?
    @State private var showSettings = false

    var body: some View {
        NavigationStack {
            Form {
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
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        showSettings = true
                    } label: {
                        Image(systemName: "gearshape")
                    }
                }
            }
            .sheet(isPresented: $showSettings) {
                SettingsView()
            }
        }
    }

    private func signIn() async {
        guard !username.isEmpty, !password.isEmpty else {
            error = "Заполните все поля"; return
        }
        busy = true; error = nil
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
