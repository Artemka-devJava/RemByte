/**
 * Отображение установленных плагинов в левом меню.
 * Плагины хранятся локально в браузере (localStorage).
 */

(function initPluginHost() {
    const STORAGE_KEY = 'rembyte_plugins_registry';

    function loadRegistry() {
        try {
            const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
            return Array.isArray(parsed) ? parsed : [];
        } catch {
            return [];
        }
    }

    function renderPluginLinks() {
        const nav = document.querySelector('.sidebar-nav');
        if (!nav) return;

        // Чистим старые кнопки плагинов
        nav.querySelectorAll('.plugin-nav-item').forEach(el => el.remove());

        const plugins = loadRegistry().filter(p => p && p.enabled !== false);
        if (!plugins.length) return;

        plugins.forEach(plugin => {
            const a = document.createElement('a');
            a.className = 'nav-item plugin-nav-item';
            a.href = `/plugins?pluginId=${encodeURIComponent(plugin.id)}`;
            a.title = `Плагин: ${plugin.name || 'Без имени'}`;
            a.innerHTML = `<span class="nav-icon">🧩</span>${escapeHtml(plugin.name || 'Плагин')}`;
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

