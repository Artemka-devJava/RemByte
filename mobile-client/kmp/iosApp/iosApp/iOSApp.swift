import SwiftUI

@main
struct iOSApp: App {
    // Регистрирует и планирует фоновую проверку напоминаний (BGTaskScheduler) —
    // см. AppDelegate.swift.
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.keyboard)
        }
    }
}
