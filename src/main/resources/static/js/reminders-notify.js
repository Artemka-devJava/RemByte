/**
 * FixByte CRM — браузерные уведомления по напоминаниям.
 *
 * Подключается через fragments/common-scripts.html (uiBase), поэтому работает
 * на любой странице с сайдбаром, а не только на /dashboard — иначе оператор
 * ничего не узнает, если сидит, например, на /orders.
 *
 * Логика зеркалит мобильный ReminderChecker (mobile-client/kmp/.../reminders/
 * ReminderChecker.kt): раз в 30 секунд запрашиваем /api/reminders (тот же
 * эндпоинт, что и дашборд), и для каждого открытого напоминания/зависшего
 * заказа, время которого уже наступило, показываем системное уведомление
 * браузера — один раз (id уже показанных храним в localStorage, "отваливаются"
 * сами, как только пропадают из открытого списка на сервере — выполнено,
 * удалено, забрано).
 *
 * Ограничение: это Notification API из обычного контекста страницы — работает,
 * пока вкладка открыта (даже в фоне/неактивной), но не переживает закрытие
 * вкладки/браузера. "Настоящие" push-уведомления при закрытой вкладке
 * потребовали бы Service Worker + Push API, а тот, в свою очередь, HTTPS —
 * чего это самостоятельно хостящееся на LAN приложение по умолчанию не имеет.
 */
(function () {
    const POLL_INTERVAL_MS = 30000;
    const KEY_NOTIFIED_REMINDERS = 'fixbyte_notified_reminders';
    const KEY_NOTIFIED_STALE = 'fixbyte_notified_stale_orders';

    function loadIds(key) {
        try {
            const raw = localStorage.getItem(key);
            const arr = raw ? JSON.parse(raw) : [];
            return new Set(Array.isArray(arr) ? arr : []);
        } catch (e) { return new Set(); }
    }

    function saveIds(key, set) {
        try { localStorage.setItem(key, JSON.stringify(Array.from(set))); } catch (e) { /* ignore */ }
    }

    function canNotify() {
        return typeof Notification !== 'undefined' && Notification.permission === 'granted';
    }

    function notify(tag, title, body) {
        if (!canNotify()) return;
        try {
            new Notification(title, { body, tag, icon: '/images/favicon.ico' });
        } catch (e) { /* некоторые окружения (например, iframe без разрешения) бросают тут */ }
    }

    async function checkReminders() {
        let data;
        try {
            const r = await fetch('/api/reminders', { cache: 'no-store' });
            if (!r.ok) return; // 401 на /login и т.п. — тихо пропускаем
            data = await r.json();
        } catch (e) {
            return;
        }

        const now = Date.now();
        const reminders = Array.isArray(data.reminders) ? data.reminders : [];
        const staleOrders = Array.isArray(data.staleOrders) ? data.staleOrders : [];

        const openReminderIds = new Set(reminders.map(r => r.id));
        const notifiedReminders = new Set(
            Array.from(loadIds(KEY_NOTIFIED_REMINDERS)).filter(id => openReminderIds.has(id))
        );
        for (const r of reminders) {
            if (notifiedReminders.has(r.id) || !r.dueAt) continue;
            const due = new Date(r.dueAt).getTime();
            if (Number.isNaN(due) || due > now) continue;
            notify('reminder_' + r.id, 'Напоминание', r.text || 'Свяжитесь с клиентом');
            notifiedReminders.add(r.id);
        }
        saveIds(KEY_NOTIFIED_REMINDERS, notifiedReminders);

        const openStaleIds = new Set(staleOrders.map(s => s.orderId));
        const notifiedStale = new Set(
            Array.from(loadIds(KEY_NOTIFIED_STALE)).filter(id => openStaleIds.has(id))
        );
        for (const s of staleOrders) {
            if (notifiedStale.has(s.orderId)) continue;
            notify('stale_' + s.orderId, 'Готов, но не забрали',
                (s.clientName || 'Клиент') + ' — заказ ' + (s.orderNumber || ('№' + s.orderId)));
            notifiedStale.add(s.orderId);
        }
        saveIds(KEY_NOTIFIED_STALE, notifiedStale);
    }

    function requestPermissionIfNeeded() {
        if (typeof Notification === 'undefined' || Notification.permission !== 'default') return;
        // Небольшая задержка вместо запроса прямо при загрузке страницы —
        // так браузеры реже глушат промпт как "спам сразу после открытия".
        setTimeout(() => { Notification.requestPermission().catch(() => {}); }, 1500);
    }

    document.addEventListener('DOMContentLoaded', () => {
        requestPermissionIfNeeded();
        checkReminders();
        setInterval(checkReminders, POLL_INTERVAL_MS);
    });
})();
