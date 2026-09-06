import SwiftUI

@MainActor
final class Session: ObservableObject {

    @Published var loggedIn = false
    @Published var checking = true
    @Published var roles: [String] = []

    var isAdmin: Bool { roles.contains("ROLE_ADMIN") }

    func bootstrap() async {
        Api.shared.restoreCookies()
        if let info = await Api.shared.me() {
            roles = info.roles
            loggedIn = true
        }
        checking = false
    }

    func afterLogin(_ info: AuthInfo) {
        roles = info.roles
        loggedIn = true
    }

    func logout() async {
        await Api.shared.logout()
        roles = []
        loggedIn = false
    }
}
