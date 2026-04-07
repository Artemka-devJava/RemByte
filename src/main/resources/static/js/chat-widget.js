const widgetState = {
    siteKey: window.FIXBYTE_CHAT_SITE_KEY || 'main-site',
    parentOrigin: window.FIXBYTE_CHAT_PARENT_ORIGIN || '',
    site: null,
    token: null,
    pollTimer: null
};

document.addEventListener('DOMContentLoaded', initWidgetChat);

async function initWidgetChat() {
    await loadWidgetSiteConfig();
    restoreConversationToken();
    bindWidgetInputs();
    if (widgetState.token) {
        await reloadWidgetConversation();
    } else {
        renderWidgetEmpty();
    }
    widgetState.pollTimer = window.setInterval(reloadWidgetConversation, 4000);
}

window.addEventListener('beforeunload', () => {
    if (widgetState.pollTimer) {
        window.clearInterval(widgetState.pollTimer);
    }
});

function bindWidgetInputs() {
    document.getElementById('widgetMessageInput')?.addEventListener('keydown', (event) => {
        if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
            sendWidgetMessage(event);
        }
    });
}

async function loadWidgetSiteConfig() {
    try {
        const response = await fetch(`/public/chat/site/${encodeURIComponent(widgetState.siteKey)}?parentOrigin=${encodeURIComponent(widgetState.parentOrigin)}`);
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data?.error || 'Чат недоступен');
        }
        widgetState.site = data;
        document.getElementById('widgetTitle').textContent = data.widgetTitle || 'FixByte — чат';
        document.getElementById('widgetSubtitle').textContent = data.displayName || 'Мы на связи';
        document.getElementById('widgetWelcomeMessage').textContent = data.welcomeMessage || '';
        document.documentElement.style.setProperty('--widget-accent', data.accentColor || '#3699d9');
    } catch (error) {
        console.error('Widget config error:', error);
        disableWidget(error.message || 'Чат временно недоступен');
    }
}

async function sendWidgetMessage(event) {
    event?.preventDefault?.();
    const input = document.getElementById('widgetMessageInput');
    const message = (input?.value || '').trim();
    if (!message) {
        return;
    }

    try {
        if (!widgetState.token) {
            const created = await createConversation(message);
            if (!created) return;
        } else {
            const response = await fetch(`/public/chat/conversations/${encodeURIComponent(widgetState.token)}/messages`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message })
            });
            const data = await response.json();
            if (!response.ok) {
                // If operator removed the conversation, start a new one seamlessly.
                if (response.status === 404) {
                    clearConversationToken();
                    const created = await createConversation(message);
                    if (!created) return;
                } else {
                throw new Error(data?.error || 'Не удалось отправить сообщение');
                }
            }
        }

        input.value = '';
        await reloadWidgetConversation();
    } catch (error) {
        console.error('Widget send error:', error);
        disableWidget(error.message || 'Ошибка отправки сообщения');
    }
}

async function createConversation(initialMessage) {
    const payload = {
        siteKey: widgetState.siteKey,
        visitorName: document.getElementById('widgetVisitorName')?.value || '',
        visitorPhone: document.getElementById('widgetVisitorPhone')?.value || '',
        visitorEmail: document.getElementById('widgetVisitorEmail')?.value || '',
        pageUrl: document.referrer || '',
        parentOrigin: widgetState.parentOrigin,
        message: initialMessage
    };

    const response = await fetch('/public/chat/conversations', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    const data = await response.json();
    if (!response.ok) {
        throw new Error(data?.error || 'Не удалось начать диалог');
    }

    widgetState.token = data?.conversation?.publicToken || data?.conversation?.publictoken || data?.conversation?.public_token || data?.conversation?.publicToken;
    if (!widgetState.token && data?.conversation?.publicToken !== undefined) {
        widgetState.token = data.conversation.publicToken;
    }
    if (!widgetState.token && data?.conversation) {
        widgetState.token = data.conversation.publicToken;
    }
    saveConversationToken();
    renderWidgetConversation(data);
    return data;
}

async function reloadWidgetConversation() {
    if (!widgetState.token) return;
    try {
        const response = await fetch(`/public/chat/conversations/${encodeURIComponent(widgetState.token)}`);
        const data = await response.json();
        if (!response.ok) {
            if (response.status === 404) {
                clearConversationToken();
                renderWidgetEmpty();
                return;
            }
            throw new Error(data?.error || 'Диалог не найден');
        }
        renderWidgetConversation(data);
        await fetch(`/public/chat/conversations/${encodeURIComponent(widgetState.token)}/read`, { method: 'POST' });
    } catch (error) {
        console.error('Widget reload error:', error);
    }
}

function renderWidgetConversation(data) {
    const messages = Array.isArray(data?.messages) ? data.messages : [];
    const box = document.getElementById('widgetMessages');
    if (!messages.length) {
        renderWidgetEmpty();
        return;
    }

    box.innerHTML = messages.map(message => {
        const cls = message.senderType === 'OPERATOR' ? 'operator' : (message.senderType === 'SYSTEM' ? 'system' : 'visitor');
        return `
            <div class="widget-message ${cls}">
              <div class="widget-message-author">${escapeWidgetHtml(message.senderName || 'Сообщение')}</div>
              <div class="widget-message-text">${escapeWidgetHtml(message.message || '').replace(/\n/g, '<br>')}</div>
              <div class="widget-message-time">${formatWidgetDate(message.createdAt)}</div>
            </div>`;
    }).join('');
    box.scrollTop = box.scrollHeight;
}

function renderWidgetEmpty() {
    const box = document.getElementById('widgetMessages');
    box.innerHTML = '<div class="widget-empty">Начните диалог — сообщения появятся здесь.</div>';
}

function disableWidget(message) {
    document.getElementById('widgetBody').style.display = 'none';
    document.getElementById('widgetForm').style.display = 'none';
    const disabled = document.getElementById('widgetDisabledState');
    disabled.style.display = 'block';
    disabled.textContent = message;
}

function closeEmbeddedChat() {
    if (window.parent) {
        window.parent.postMessage({ type: 'fixbyte-chat-close' }, '*');
    }
}

function tokenStorageKey() {
    return `fixbyte_chat_token_${widgetState.siteKey}_${btoa(unescape(encodeURIComponent(widgetState.parentOrigin || 'default'))).replace(/=/g, '')}`;
}

function restoreConversationToken() {
    try {
        widgetState.token = localStorage.getItem(tokenStorageKey()) || null;
    } catch {
        widgetState.token = null;
    }
}

function saveConversationToken() {
    if (!widgetState.token) return;
    try {
        localStorage.setItem(tokenStorageKey(), widgetState.token);
    } catch {
        // ignore
    }
}

function clearConversationToken() {
    try {
        localStorage.removeItem(tokenStorageKey());
    } catch {
        // ignore
    }
    widgetState.token = null;
}

function formatWidgetDate(value) {
    if (!value) return '';
    return new Date(value).toLocaleString('ru-RU', {
        day: '2-digit',
        month: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function escapeWidgetHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

