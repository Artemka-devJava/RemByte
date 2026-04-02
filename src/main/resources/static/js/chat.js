let chatConversations = [];
let filteredChatConversations = [];
let currentConversationId = null;
let currentConversationDetail = null;
let chatRefreshTimer = null;

document.addEventListener('DOMContentLoaded', () => {
    const search = document.getElementById('chatSearchInput');
    search?.addEventListener('input', filterChatConversations);
    refreshChatData(false);
    chatRefreshTimer = window.setInterval(() => refreshChatData(false), 5000);
});

window.addEventListener('beforeunload', () => {
    if (chatRefreshTimer) {
        window.clearInterval(chatRefreshTimer);
    }
});

async function refreshChatData(forceReloadCurrent) {
    try {
        const [conversations, summary] = await Promise.all([
            ChatAPI.getConversations(),
            ChatAPI.getSummary()
        ]);

        chatConversations = Array.isArray(conversations) ? conversations : [];
        filteredChatConversations = applyChatSearch(chatConversations);
        renderConversationList();
        renderChatSummary(summary);

        if (currentConversationId) {
            const stillExists = chatConversations.some(item => Number(item.id) === Number(currentConversationId));
            if (stillExists) {
                await openConversation(currentConversationId, !forceReloadCurrent);
            } else {
                clearCurrentConversation();
            }
        } else if (filteredChatConversations.length) {
            await openConversation(filteredChatConversations[0].id, true);
        }
    } catch (error) {
        console.error('Error refreshing chat data:', error);
        showNotification('Ошибка обновления чата: ' + (error && error.message ? error.message : ''), 'error');
    }
}

function renderChatSummary(summary) {
    document.getElementById('chatUnreadCount').textContent = summary?.unreadConversations ?? 0;
    document.getElementById('chatOpenCount').textContent = summary?.openConversations ?? 0;
}

function filterChatConversations() {
    filteredChatConversations = applyChatSearch(chatConversations);
    renderConversationList();
}

function applyChatSearch(list) {
    const query = (document.getElementById('chatSearchInput')?.value || '').trim().toLowerCase();
    if (!query) {
        return [...list];
    }
    return list.filter(item => {
        const haystack = [
            item.visitorName,
            item.visitorPhone,
            item.visitorEmail,
            item.lastMessagePreview,
            item.visitorPageUrl
        ].join(' ').toLowerCase();
        return haystack.includes(query);
    });
}

function renderConversationList() {
    const box = document.getElementById('chatConversationList');
    if (!filteredChatConversations.length) {
        box.innerHTML = '<div class="chat-empty-state">Диалоги не найдены</div>';
        return;
    }

    box.innerHTML = filteredChatConversations.map(item => {
        const active = Number(item.id) === Number(currentConversationId) ? ' active' : '';
        const unread = Number(item.unreadForOperator || 0);
        const statusLabel = item.status === 'CLOSED' ? 'Закрыт' : 'Открыт';
        return `
            <button type="button" class="chat-conversation-item${active}" onclick="openConversation(${item.id}, false)">
              <div class="chat-conversation-item-top">
                <span class="chat-conversation-name">${escapeHtml(item.visitorName || 'Посетитель')}</span>
                <span class="chat-conversation-time">${formatRelativeTime(item.lastMessageAt)}</span>
              </div>
              <div class="chat-conversation-preview">${escapeHtml(item.lastMessagePreview || 'Без сообщений')}</div>
              <div class="chat-conversation-meta-row">
                <span class="status-badge ${item.status === 'CLOSED' ? 'status-cancelled' : 'status-ready'}">${statusLabel}</span>
                ${unread > 0 ? `<span class="chat-unread-badge">${unread}</span>` : ''}
              </div>
            </button>`;
    }).join('');
}

async function openConversation(id, silent) {
    currentConversationId = id;
    try {
        const detail = await ChatAPI.getConversation(id);
        if (!detail || !detail.conversation) {
            throw new Error('Диалог не найден');
        }
        currentConversationDetail = detail;
        renderConversationHeader(detail.conversation);
        renderConversationInfo(detail.conversation);
        renderMessages(detail.messages || []);
        renderConversationList();
        await ChatAPI.markRead(id);
        if (!silent) {
            await refreshSidebarChatBadge();
        }
    } catch (error) {
        console.error('Error opening conversation:', error);
        showNotification('Ошибка загрузки диалога: ' + (error && error.message ? error.message : ''), 'error');
    }
}

function renderConversationHeader(conversation) {
    document.getElementById('chatThreadTitle').textContent = conversation.visitorName || 'Посетитель';
    document.getElementById('chatThreadMeta').textContent = `${conversation.visitorPhone || 'без телефона'}${conversation.visitorEmail ? ' · ' + conversation.visitorEmail : ''}`;

    const btn = document.getElementById('chatToggleStatusBtn');
    btn.style.display = 'inline-flex';
    btn.textContent = conversation.status === 'CLOSED' ? 'Открыть диалог' : 'Закрыть диалог';
    btn.className = `btn btn-sm ${conversation.status === 'CLOSED' ? 'btn-success' : 'btn-secondary'}`;
}

function renderConversationInfo(conversation) {
    const info = document.getElementById('chatConversationInfo');
    info.innerHTML = `
      <div class="chat-info-card"><strong>Сайт:</strong> ${escapeHtml(conversation.siteKey || 'main-site')}</div>
      <div class="chat-info-card"><strong>Страница:</strong> ${conversation.visitorPageUrl ? `<a href="${escapeHtml(conversation.visitorPageUrl)}" target="_blank" rel="noopener noreferrer">${escapeHtml(conversation.visitorPageUrl)}</a>` : '—'}</div>
      <div class="chat-info-card"><strong>Origin:</strong> ${escapeHtml(conversation.visitorOrigin || '—')}</div>
      <div class="chat-info-card"><strong>Оператор:</strong> ${escapeHtml(conversation.assignedOperatorUsername || 'не назначен')}</div>`;
}

function renderMessages(messages) {
    const box = document.getElementById('chatMessages');
    if (!messages.length) {
        box.innerHTML = '<div class="chat-empty-state">В этом диалоге пока нет сообщений</div>';
        return;
    }

    box.innerHTML = messages.map(message => {
        const mine = message.senderType === 'OPERATOR';
        const cls = mine ? 'mine' : (message.senderType === 'SYSTEM' ? 'system' : 'visitor');
        return `
          <div class="chat-message ${cls}">
            <div class="chat-message-author">${escapeHtml(message.senderName || (mine ? 'Оператор' : 'Посетитель'))}</div>
            <div class="chat-message-text">${escapeHtml(message.message || '').replace(/\n/g, '<br>')}</div>
            <div class="chat-message-time">${formatDate(message.createdAt)}</div>
          </div>`;
    }).join('');
    box.scrollTop = box.scrollHeight;
}

async function sendChatReply(event) {
    event.preventDefault();
    if (!currentConversationId) {
        showNotification('Сначала выберите диалог', 'warning');
        return;
    }

    const input = document.getElementById('chatReplyInput');
    const message = (input.value || '').trim();
    if (!message) {
        showNotification('Введите сообщение', 'warning');
        return;
    }

    const result = await ChatAPI.sendMessage(currentConversationId, message);
    if (!result || result.error) {
        showNotification(result?.error || 'Не удалось отправить сообщение', 'error');
        return;
    }

    input.value = '';
    await openConversation(currentConversationId, false);
    await refreshChatData(true);
}

async function toggleCurrentConversationStatus() {
    if (!currentConversationDetail?.conversation) return;
    const current = currentConversationDetail.conversation;
    const nextStatus = current.status === 'CLOSED' ? 'OPEN' : 'CLOSED';
    const result = await ChatAPI.updateStatus(current.id, nextStatus);
    if (!result || result.error) {
        showNotification(result?.error || 'Не удалось изменить статус диалога', 'error');
        return;
    }
    showNotification(nextStatus === 'CLOSED' ? 'Диалог закрыт' : 'Диалог открыт', 'success');
    await refreshChatData(true);
}

function clearCurrentConversation() {
    currentConversationId = null;
    currentConversationDetail = null;
    document.getElementById('chatThreadTitle').textContent = 'Выберите диалог';
    document.getElementById('chatThreadMeta').textContent = 'Сообщения с сайта будут появляться здесь';
    document.getElementById('chatConversationInfo').innerHTML = '';
    document.getElementById('chatMessages').innerHTML = '<div class="chat-empty-state">Слева выберите диалог, чтобы просмотреть переписку.</div>';
    document.getElementById('chatToggleStatusBtn').style.display = 'none';
}

async function refreshSidebarChatBadge() {
    try {
        if (typeof window.fixbyteRefreshChatBadge === 'function') {
            await window.fixbyteRefreshChatBadge();
        }
    } catch {
        // ignore
    }
}

function formatRelativeTime(value) {
    if (!value) return '—';
    const date = new Date(value);
    const diffMs = Date.now() - date.getTime();
    const diffMin = Math.round(diffMs / 60000);
    if (diffMin < 1) return 'только что';
    if (diffMin < 60) return `${diffMin} мин назад`;
    const diffHours = Math.round(diffMin / 60);
    if (diffHours < 24) return `${diffHours} ч назад`;
    return formatDate(value);
}

function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}
