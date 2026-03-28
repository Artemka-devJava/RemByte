/**
 * Страница запуска плагина.
 * Плагин рендерится внутри sandbox iframe и изолирован от CRM.
 */

(function initPluginRunner() {
    const STORAGE_KEY = 'rembyte_plugins_registry';

    function loadRegistry() {
        try {
            const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
            return Array.isArray(parsed) ? parsed : [];
        } catch {
            return [];
        }
    }

    function getPluginById(id) {
        return loadRegistry().find(p => p && p.id === id);
    }

    function getPluginIdFromUrl() {
        return new URLSearchParams(window.location.search).get('pluginId') || '';
    }

    function render() {
        const pluginId = getPluginIdFromUrl();
        const titleEl = document.getElementById('pluginTitle');
        const descEl = document.getElementById('pluginDescription');
        const iframe = document.getElementById('pluginFrame');
        const empty = document.getElementById('pluginEmpty');

        if (!pluginId) {
            titleEl.textContent = 'Плагин не выбран';
            descEl.textContent = 'Откройте плагин через левое меню или вкладку «Настройки → Плагины».\n';
            empty.style.display = 'block';
            iframe.style.display = 'none';
            return;
        }

        const plugin = getPluginById(pluginId);
        if (!plugin) {
            titleEl.textContent = 'Плагин не найден';
            descEl.textContent = 'Возможно, плагин был удален из настроек администратора.';
            empty.style.display = 'block';
            iframe.style.display = 'none';
            return;
        }

        titleEl.textContent = `🧩 ${plugin.name || 'Плагин'}`;
        descEl.textContent = plugin.description || 'Плагин запущен в изолированной песочнице (sandbox iframe).';

        // Полная изоляция: нет доступа к parent/window CRM, cookie/session, DOM CRM.
        iframe.setAttribute('sandbox', 'allow-scripts allow-modals');
        iframe.srcdoc = plugin.html || '<h3>Пустой плагин</h3>';
        empty.style.display = 'none';
        iframe.style.display = 'block';
    }

    document.addEventListener('DOMContentLoaded', render);
})();

