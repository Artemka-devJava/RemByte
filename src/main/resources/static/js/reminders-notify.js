/**
 * FixByte CRM — уведомления по напоминаниям.
 *
 * Подключается через fragments/common-scripts.html (uiBase), поэтому работает
 * на любой странице с сайдбаром, а не только на /dashboard — иначе оператор
 * ничего не узнает, если сидит, например, на /orders.
 *
 * Раз в 30 секунд запрашивает /api/reminders (тот же эндпоинт, что и
 * дашборд).
 *
 * ВАЖНО про каналы показа:
 * 1) Заголовок вкладки — 🔔 (N) — пересчитывается заново на каждом опросе
 *    из текущего состояния сервера (N = сколько просроченных-невыполненных
 *    напоминаний + зависших заказов прямо сейчас). Не завязан на события
 *    фокуса — они ненадёжны (переключение вкладок/окон, даже автоматизация
 *    браузера могут вызвать spurious focus и мгновенно сбросить наивный
 *    счётчик, что и обнаружилось при живом тестировании через Chrome).
 *    Пропадает сам, когда напоминание отмечено выполненным/удалено.
 * 2) Одноразовый всплывающий тост через showNotification() (api.js) в
 *    момент, когда что-то только что стало просроченным — если оператор в
 *    этот момент смотрит на экран. Дедуп по id в localStorage.
 * 3) Системное уведомление браузера (Notification API) — ТОЛЬКО бонус:
 *    эта API работает исключительно в secure context (HTTPS), с
 *    единственным исключением для http://localhost. FixByte обычно
 *    открывают по LAN-адресу вроде http://192.168.1.242:9087 (см.
 *    deploy/.env.example) — это НЕ secure context, поэтому браузер даже
 *    не покажет запрос разрешения. На таком адресе только каналы 1 и 2.
 */
(function () {
    const POLL_INTERVAL_MS = 30000;
    const KEY_NOTIFIED_REMINDERS = 'fixbyte_notified_reminders';
    const KEY_NOTIFIED_STALE = 'fixbyte_notified_stale_orders';

    const originalTitle = document.title;

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

    // Системное уведомление браузера — доступно только в secure context
    // (HTTPS или localhost), см. пояснение в шапке файла.
    function canUseOsNotification() {
        return window.isSecureContext && typeof Notification !== 'undefined' && Notification.permission === 'granted';
    }

    function toast(title, body, tag) {
        if (typeof showNotification === 'function') {
            showNotification(`${title}: ${body}`, 'warning');
        }
        if (canUseOsNotification()) {
            try {
                new Notification(title, { body, tag, icon: '/images/favicon.ico' });
            } catch (e) { /* игнорируем — тост уже показан */ }
        }
    }

    function updateTitle(dueCount) {
        document.title = dueCount > 0 ? `🔔 (${dueCount}) ${originalTitle}` : originalTitle;
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

        const dueReminders = reminders.filter(r => {
            if (!r.dueAt) return false;
            const due = new Date(r.dueAt).getTime();
            return !Number.isNaN(due) && due <= now;
        });

        // Заголовок вкладки — всегда актуальное число, независимо от того,
        // показывали ли мы уже тост по каждому конкретному пункту.
        updateTitle(dueReminders.length + staleOrders.length);

        const openReminderIds = new Set(dueReminders.map(r => r.id));
        const notifiedReminders = new Set(
            Array.from(loadIds(KEY_NOTIFIED_REMINDERS)).filter(id => openReminderIds.has(id))
        );
        for (const r of dueReminders) {
            if (notifiedReminders.has(r.id)) continue;
            toast('Напоминание', r.text || 'Свяжитесь с клиентом', 'reminder_' + r.id);
            notifiedReminders.add(r.id);
        }
        saveIds(KEY_NOTIFIED_REMINDERS, notifiedReminders);

        const openStaleIds = new Set(staleOrders.map(s => s.orderId));
        const notifiedStale = new Set(
            Array.from(loadIds(KEY_NOTIFIED_STALE)).filter(id => openStaleIds.has(id))
        );
        for (const s of staleOrders) {
            if (notifiedStale.has(s.orderId)) continue;
            toast('Готов, но не забрали',
                (s.clientName || 'Клиент') + ' — заказ ' + (s.orderNumber || ('№' + s.orderId)),
                'stale_' + s.orderId);
            notifiedStale.add(s.orderId);
        }
        saveIds(KEY_NOTIFIED_STALE, notifiedStale);
    }

    function requestOsPermissionIfPossible() {
        if (!window.isSecureContext) return; // см. пояснение в шапке файла — на LAN HTTP не сработает
        if (typeof Notification === 'undefined' || Notification.permission !== 'default') return;
        // Небольшая задержка вместо запроса прямо при загрузке страницы —
        // так браузеры реже глушат промпт как "спам сразу после открытия".
        setTimeout(() => { Notification.requestPermission().catch(() => {}); }, 1500);
    }

    document.addEventListener('DOMContentLoaded', () => {
        requestOsPermissionIfPossible();
        checkReminders();
        setInterval(checkReminders, POLL_INTERVAL_MS);
    });
})();
