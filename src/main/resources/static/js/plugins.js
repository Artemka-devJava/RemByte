/**
 * Страница запуска плагина.
 * Плагин рендерится внутри sandbox iframe и изолирован от CRM.
 *
 * Для плагинов добавлен безопасный SDK хранения:
 * - RemBytePluginAPI.getData(defaultValue)
 * - RemBytePluginAPI.setData(value)
 *
 * SDK работает через postMessage и хранит данные в localStorage хоста,
 * изолированно по pluginId.
 */

(function initPluginRunner() {
    const REGISTRY_KEY = 'rembyte_plugins_registry';
    const DATA_KEY_PREFIX = 'rembyte_plugin_data_';
    const MESSAGE_TYPE = 'rembyte-plugin-storage';
    const RESPONSE_TYPE = 'rembyte-plugin-storage-response';

    function loadRegistry() {
        try {
            const parsed = JSON.parse(localStorage.getItem(REGISTRY_KEY) || '[]');
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

    function getPluginData(pluginId) {
        try {
            const raw = localStorage.getItem(DATA_KEY_PREFIX + pluginId);
            if (!raw) return null;
            return JSON.parse(raw);
        } catch {
            return null;
        }
    }

    function setPluginData(pluginId, value) {
        localStorage.setItem(DATA_KEY_PREFIX + pluginId, JSON.stringify(value));
    }

    function buildSdkScript(pluginId) {
        const safePluginId = JSON.stringify(pluginId || '');
        return `
<script>
(function () {
  const MESSAGE_TYPE = '${MESSAGE_TYPE}';
  const RESPONSE_TYPE = '${RESPONSE_TYPE}';
  const pluginId = ${safePluginId};

  function request(action, payload, defaultValue) {
    return new Promise((resolve, reject) => {
      const requestId = 'req_' + Date.now() + '_' + Math.random().toString(36).slice(2, 10);

      function onMessage(event) {
        const msg = event && event.data;
        if (!msg || msg.type !== RESPONSE_TYPE || msg.requestId !== requestId) return;
        window.removeEventListener('message', onMessage);
        if (msg.ok) resolve(msg.data);
        else reject(new Error(msg.error || 'Plugin storage request failed'));
      }

      window.addEventListener('message', onMessage);
      window.parent.postMessage({
        type: MESSAGE_TYPE,
        requestId,
        pluginId,
        action,
        payload,
        defaultValue
      }, '*');
    });
  }

  window.RemBytePluginAPI = {
    pluginId,
    getData(defaultValue) {
      return request('get', null, defaultValue);
    },
    setData(value) {
      return request('set', value, null);
    }
  };

  window.dispatchEvent(new Event('rembyte-plugin-api-ready'));
})();
</script>
`;
    }

    function injectSdk(pluginHtml, pluginId) {
        const html = String(pluginHtml || '').trim();
        const sdk = buildSdkScript(pluginId);
        if (!html) return sdk + '<h3>Пустой плагин</h3>';

        if (/<head[^>]*>/i.test(html)) {
            return html.replace(/<head[^>]*>/i, match => `${match}\n${sdk}`);
        }
        if (/<body[^>]*>/i.test(html)) {
            return html.replace(/<body[^>]*>/i, match => `${match}\n${sdk}`);
        }
        return sdk + html;
    }

    function sendResponse(targetWindow, requestId, ok, data, error) {
        if (!targetWindow || !requestId) return;
        targetWindow.postMessage(
            { type: RESPONSE_TYPE, requestId, ok, data: data ?? null, error: error ?? null },
            '*'
        );
    }

    function onStorageMessage(event) {
        const iframe = document.getElementById('pluginFrame');
        const msg = event && event.data;
        if (!msg || msg.type !== MESSAGE_TYPE) return;
        if (!iframe || event.source !== iframe.contentWindow) return;

        const pluginId = getPluginIdFromUrl();
        if (!pluginId) {
            sendResponse(event.source, msg.requestId, false, null, 'Plugin is not selected');
            return;
        }

        if (msg.pluginId && msg.pluginId !== pluginId) {
            sendResponse(event.source, msg.requestId, false, null, 'Plugin id mismatch');
            return;
        }

        try {
            if (msg.action === 'get') {
                const data = getPluginData(pluginId);
                const result = data == null ? (msg.defaultValue ?? null) : data;
                sendResponse(event.source, msg.requestId, true, result, null);
                return;
            }

            if (msg.action === 'set') {
                setPluginData(pluginId, msg.payload ?? null);
                sendResponse(event.source, msg.requestId, true, { saved: true }, null);
                return;
            }

            sendResponse(event.source, msg.requestId, false, null, 'Unsupported action');
        } catch (error) {
            sendResponse(event.source, msg.requestId, false, null, error?.message || 'Storage error');
        }
    }

    function render() {
        const pluginId = getPluginIdFromUrl();
        const titleEl = document.getElementById('pluginTitle');
        const descEl = document.getElementById('pluginDescription');
        const iframe = document.getElementById('pluginFrame');
        const empty = document.getElementById('pluginEmpty');

        if (!pluginId) {
            titleEl.textContent = 'Плагин не выбран';
            descEl.textContent = 'Откройте плагин через левое меню или вкладку «Настройки → Плагины».';
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

        // Изоляция: плагин не получает доступа к DOM CRM и сессии.
        iframe.setAttribute('sandbox', 'allow-scripts allow-modals');
        iframe.srcdoc = injectSdk(plugin.html, plugin.id);
        empty.style.display = 'none';
        iframe.style.display = 'block';
    }

    window.addEventListener('message', onStorageMessage);
    document.addEventListener('DOMContentLoaded', render);
})();

