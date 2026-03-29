/**
 * Отображение установленных плагинов в левом меню.
 * Плагины хранятся локально в браузере (localStorage).
 */

(function initPluginHost() {
    const STORAGE_KEY = 'rembyte_plugins_registry';
    const BUILTIN_NOTES_LINK = {
        href: '/notes',
        label: 'Заметки',
        icon: '📝'
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

    async function isNotesPluginEnabled() {
        try {
            const response = await fetch('/api/plugin-settings/notes', { cache: 'no-store' });
            if (!response.ok) return true;
            const data = await response.json();
            return data && data.enabled !== false;
        } catch {
            return true;
        }
    }

    async function renderPluginLinks() {
        const nav = document.querySelector('.sidebar-nav');
        if (!nav) return;

        // Чистим старые кнопки плагинов
        nav.querySelectorAll('.plugin-nav-item').forEach(el => el.remove());

        const notesEnabled = await isNotesPluginEnabled();
        if (notesEnabled) {
            const notesLink = document.createElement('a');
            notesLink.className = 'nav-item plugin-nav-item plugin-nav-builtin';
            if (window.location.pathname === BUILTIN_NOTES_LINK.href) {
                notesLink.classList.add('active');
            }
            notesLink.href = BUILTIN_NOTES_LINK.href;
            notesLink.title = `Плагин: ${BUILTIN_NOTES_LINK.label}`;
            notesLink.innerHTML = `<span class="nav-icon">${BUILTIN_NOTES_LINK.icon}</span>${escapeHtml(BUILTIN_NOTES_LINK.label)}`;
            nav.appendChild(notesLink);
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

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', renderPluginLinks);
    } else {
        renderPluginLinks();
    }
})();

