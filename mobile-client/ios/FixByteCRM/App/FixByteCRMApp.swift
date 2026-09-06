import SwiftUI

@main
struct FixByteCRMApp: App {
    @StateObject private var session = Session()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(session)
                .task { await session.bootstrap() }
        }
    }
}

struct RootView: View {
    @EnvironmentObject private var session: Session

    var body: some View {
        Group {
            if session.checking {
                ProgressView("Загрузка…")
            } else if session.loggedIn {
                ClientsView()
            } else {
                LoginView()
            }
        }
    }
}
