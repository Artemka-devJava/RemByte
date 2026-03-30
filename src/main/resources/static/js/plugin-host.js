/**
 * Отображение установленных плагинов в левом меню.
 * Плагины хранятся локально в браузере (localStorage).
 */

(function initPluginHost() {
    const STORAGE_KEY = 'rembyte_plugins_registry';
    const CHAT_BADGE_REFRESH_MS = 5000;
    let chatBadgeTimerId = null;
    const BUILTIN_KANBAN_LINK = {
        href: '/kanban',
        label: 'Канбан',
        icon: '🗂️'
    };
    const BUILTIN_NOTES_LINK = {
        href: '/notes',
        label: 'Заметки',
        icon: '📝',
        settingUrl: '/api/plugin-settings/notes'
    };
    const PLUGIN_NAME_LOCALIZATION = {
        'text-editor-plugin': 'Текстовый редактор',
        'text-editor': 'Текстовый редактор',
        'snake-game-plugin': 'Змейка',
        'snake-game': 'Змейка',
        'tetris-game-plugin': 'Тетрис',
        'tetris-game': 'Тетрис'
    };

    function normalizePluginName(name) {
        const raw = String(name || '').trim();
        if (!raw) return 'Плагин';

        const key = raw.toLowerCase().replace(/\.html?$/i, '');
        return PLUGIN_NAME_LOCALIZATION[key] || raw;
    }

    function loadRegistry() {
        try {
            const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
            return Array.isArray(parsed) ? parsed : [];
        } catch {
            return [];
        }
    }

    async function isBuiltInPluginEnabled(url) {
        try {
            const response = await fetch(url, { cache: 'no-store' });
            if (!response.ok) return true;
            const data = await response.json();
            return data && data.enabled !== false;
        } catch {
            return true;
        }
    }

    function ensureBuiltinLink(nav, link) {
        let anchor = nav.querySelector(`a[href="${link.href}"]`);
        if (!anchor) {
            anchor = document.createElement('a');
            anchor.className = 'nav-item plugin-nav-item plugin-nav-builtin';
            anchor.href = link.href;
            nav.appendChild(anchor);
        }

        anchor.classList.toggle('active', window.location.pathname === link.href);
        anchor.title = `Модуль: ${link.label}`;
        anchor.innerHTML = `<span class="nav-icon">${link.icon}</span>${escapeHtml(link.label)}`;
        return anchor;
    }

    async function decorateChatBadge(anchor) {
        if (!anchor) return;
        try {
            const response = await fetch('/api/chat/summary', { cache: 'no-store' });
            if (!response.ok) return;
            const data = await response.json();
            const unread = Number(data?.unreadConversations || 0);
            anchor.querySelectorAll('.nav-badge').forEach(el => el.remove());
            if (unread > 0) {
                const badge = document.createElement('span');
                badge.className = 'nav-badge';
                badge.textContent = unread > 99 ? '99+' : String(unread);
                anchor.appendChild(badge);
            }
        } catch {
            // игнорируем на публичных/логин-страницах
        }
    }

    async function renderPluginLinks() {
        const nav = document.querySelector('.sidebar-nav');
        if (!nav) return;

        // Чистим старые кнопки плагинов
        nav.querySelectorAll('.plugin-nav-item').forEach(el => el.remove());

        // Чат теперь системный раздел: только обновляем бейдж, не создаем/скрываем пункт меню
        await decorateChatBadge(nav.querySelector('a[href="/chat"]'));

        // Нативный раздел канбана всегда доступен для внутренних пользователей
        ensureBuiltinLink(nav, BUILTIN_KANBAN_LINK);

        const notesEnabled = await isBuiltInPluginEnabled(BUILTIN_NOTES_LINK.settingUrl);
        if (notesEnabled) {
            ensureBuiltinLink(nav, BUILTIN_NOTES_LINK);
        }

        const plugins = loadRegistry().filter(p => p && p.enabled !== false);
        if (!plugins.length) return;

        plugins.forEach(plugin => {
            const pluginName = normalizePluginName(plugin.name);
            const a = document.createElement('a');
            a.className = 'nav-item plugin-nav-item';
            a.href = `/plugins?pluginId=${encodeURIComponent(plugin.id)}`;
            a.title = `Плагин: ${pluginName}`;
            a.innerHTML = `<span class="nav-icon">🧩</span>${escapeHtml(pluginName)}`;
            nav.appendChild(a);
        });
    }

    function escapeHtml(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    window.addEventListener('storage', (event) => {
        if (event.key === STORAGE_KEY) renderPluginLinks();
    });

    window.fixbyteRefreshChatBadge = async function fixbyteRefreshChatBadge() {
        const nav = document.querySelector('.sidebar-nav');
        if (!nav) return;
        await decorateChatBadge(nav.querySelector('a[href="/chat"]'));
    };

    function startChatBadgeAutoRefresh() {
        if (chatBadgeTimerId) return;
        chatBadgeTimerId = window.setInterval(async () => {
            // Не дергаем API, когда вкладка неактивна.
            if (document.hidden) return;
            await window.fixbyteRefreshChatBadge();
        }, CHAT_BADGE_REFRESH_MS);
    }

    document.addEventListener('visibilitychange', () => {
        if (!document.hidden) {
            window.fixbyteRefreshChatBadge();
        }
    });

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', async () => {
            await renderPluginLinks();
            startChatBadgeAutoRefresh();
        });
    } else {
        renderPluginLinks();
        startChatBadgeAutoRefresh();
    }
})();
