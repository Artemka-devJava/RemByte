import UIKit
import BackgroundTasks
import ComposeApp

/// Идентификатор фоновой задачи — должен совпадать с записью в
/// BGTaskSchedulerPermittedIdentifiers (Resources/Info.plist).
private let reminderRefreshTaskId = "ru.fixbyte.crm.next.reminderRefresh"

/// Фоновый планировщик проверки напоминаний на iOS (BGTaskScheduler).
/// До этого файла ReminderChecker.checkAndNotify() срабатывал только при
/// открытии приложения на передний план (см. App.kt и оговорку, которая была
/// в Notifications.ios.kt) — аналог Android-варианта на WorkManager
/// (platform/ReminderWorker.android.kt), но фоновые задачи на iOS — не
/// гарантированный периодический таймер: earliestBeginDate — это нижняя
/// граница, а не обещание, когда именно ОС даст процессу время выполниться.
///
/// ВНИМАНИЕ: написано без возможности собрать/проверить — нет macOS/Xcode в
/// среде разработки (см. project.yml и Platform.ios.kt). Перед использованием
/// нужно на реальном Mac: собрать в Xcode, на устройстве (не симуляторе —
/// там BGTaskScheduler сам по расписанию не срабатывает, только вручную через
/// `e -l objc -- (void)[[BGTaskScheduler sharedScheduler] ...]` в отладчике)
/// и подтвердить, что регистрация identifier'а в Info.plist подхватилась.
class AppDelegate: NSObject, UIApplicationDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Регистрация обработчика ДОЛЖНА произойти до конца запуска приложения
        // (документированное требование BGTaskScheduler) — отсюда AppDelegate,
        // а не App.init().
        BGTaskScheduler.shared.register(forTaskWithIdentifier: reminderRefreshTaskId, using: nil) { task in
            guard let refreshTask = task as? BGAppRefreshTask else { return }
            handleReminderRefresh(task: refreshTask)
        }
        scheduleReminderRefresh()
        return true
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        // Подстраховка: подвинуть окно на очередные 15 минут каждый раз, когда
        // приложение уходит в фон, а не полагаться только на цепочку
        // "выполнилось → запланировало следующее".
        scheduleReminderRefresh()
    }
}

/// Ставит в очередь следующую фоновую проверку не раньше, чем через 15 минут
/// (та же периодичность, что у Android WorkManager) — конкретное время решает ОС.
private func scheduleReminderRefresh() {
    let request = BGAppRefreshTaskRequest(identifier: reminderRefreshTaskId)
    request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)
    do {
        try BGTaskScheduler.shared.submit(request)
    } catch {
        // На симуляторе / без разрешения на фоновое обновление submit может
        // упасть — не фатально, приложение всё равно проверит напоминания при
        // следующем открытии на передний план (см. App.kt).
    }
}

private func handleReminderRefresh(task: BGAppRefreshTask) {
    // Планируем следующий запуск сразу — иначе, если текущее выполнение не
    // дойдёт до конца, цепочка периодических проверок оборвётся насовсем.
    scheduleReminderRefresh()

    let work = Task {
        await checkRemindersInBackground()
        task.setTaskCompleted(success: true)
    }

    task.expirationHandler = {
        work.cancel()
    }
}

private func checkRemindersInBackground() async {
    await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
        ReminderChecker.shared.checkAndNotify { _ in
            continuation.resume()
        }
    }
}
