/**
 * API клиент для FixByte CRM
 * Содержит все методы для взаимодействия с REST API
 */

const API_BASE = '/api';

// Ensure fetch sends cookies (session) by default for same-origin requests.
// Many API calls require authentication (JSESSIONID cookie). Browsers' fetch
// doesn't send credentials by default, so we set a default unless explicitly provided.
(function() {
    if (typeof window !== 'undefined' && window.fetch) {
        const _fetch = window.fetch.bind(window);
        window.fetch = function(input, init) {
            init = init || {};
            if (!('credentials' in init)) {
                init.credentials = 'same-origin';
            }
            return _fetch(input, init);
        };
    }
})();

// ====== CSRF PROTECTION ======
/**
 * Получить CSRF токен из meta-тага
 * Spring Security автоматически добавляет токен в HTML
 */
function getCsrfToken() {
    const tokenElement = document.querySelector('meta[name="_csrf"]');
    if (!tokenElement) {
        console.warn('⚠️ CSRF meta-тег не найден. CSRF защита может не работать.');
        return '';
    }
    return tokenElement.getAttribute('content') || '';
}

/**
 * Получить имя заголовка для CSRF токена (обычно "X-CSRF-TOKEN")
 */
function getCsrfHeaderName() {
    const headerElement = document.querySelector('meta[name="_csrf_header_name"]');
    return headerElement ? headerElement.getAttribute('content') : 'X-CSRF-TOKEN';
}

/**
 * Получить защищённые заголовки с CSRF токеном для POST/PUT/DELETE запросов
 */
function getSecureHeaders(contentType = 'application/json') {
    const headers = {};

    // Добавляем Content-Type только если он явно задан и непустой
    if (contentType) {
        headers['Content-Type'] = contentType;
    }

    // Добавляем CSRF заголовок только если есть токен и имя заголовка
    const csrf = getCsrfToken();
    const headerName = getCsrfHeaderName();
    if (csrf && headerName) {
        headers[headerName] = csrf;
    }

    return headers;
}

/**
 * Получить только CSRF заголовки (без Content-Type)
 * Используется для запросов без тела, где не нужно отправлять Content-Type
 */
function getCsrfOnlyHeaders() {
    const headers = {};
    const csrf = getCsrfToken();
    const headerName = getCsrfHeaderName();
    if (csrf && headerName) {
        headers[headerName] = csrf;
    }
    return headers;
}

// ====== CLIENTS API ======
const ClientAPI = {
    // Получить всех клиентов
    getAll: async () => {
        try {
            const response = await fetch(`${API_BASE}/clients`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching clients:', error);
            return [];
        }
    },

    // Получить активных клиентов
    getActive: async () => {
        try {
            const response = await fetch(`${API_BASE}/clients/active`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching active clients:', error);
            return [];
        }
    },

    // Получить клиента по ID
    getById: async (id) => {
        try {
            const response = await fetch(`${API_BASE}/clients/${id}`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching client:', error);
            return null;
        }
    },

    // Поиск клиентов
    search: async (name) => {
        try {
            const response = await fetch(`${API_BASE}/clients/search?name=${encodeURIComponent(name)}`);
            return await response.json();
        } catch (error) {
            console.error('Error searching clients:', error);
            return [];
        }
    },

    // Создать нового клиента
    create: async (clientData) => {
        try {
            const response = await fetch(`${API_BASE}/clients`, {
                method: 'POST',
                headers: getSecureHeaders(),
                body: JSON.stringify(clientData)
            });
            return await response.json();
        } catch (error) {
            console.error('Error creating client:', error);
            return null;
        }
    },

    // Обновить клиента
    update: async (id, clientData) => {
        try {
            const response = await fetch(`${API_BASE}/clients/${id}`, {
                method: 'PUT',
                headers: getSecureHeaders(),
                body: JSON.stringify(clientData)
            });
            return await response.json();
        } catch (error) {
            console.error('Error updating client:', error);
            return null;
        }
    },

    // Удалить клиента
    delete: async (id) => {
        try {
            await fetch(`${API_BASE}/clients/${id}`, {
                method: 'DELETE',
                headers: getCsrfOnlyHeaders()
            });
            return true;
        } catch (error) {
            console.error('Error deleting client:', error);
            return false;
        }
    }
};

// ====== SERVICES API ======
const ServiceAPI = {
    // Получить все услуги
    getAll: async () => {
        try {
            const response = await fetch(`${API_BASE}/services`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching services:', error);
            return [];
        }
    },

    // Получить активные услуги
    getActive: async () => {
        try {
            const response = await fetch(`${API_BASE}/services/active`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching active services:', error);
            return [];
        }
    },

    // Получить услуги по категории
    getByCategory: async (category) => {
        try {
            const response = await fetch(`${API_BASE}/services/category/${encodeURIComponent(category)}`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching services by category:', error);
            return [];
        }
    },

    // Получить услугу по ID
    getById: async (id) => {
        try {
            const response = await fetch(`${API_BASE}/services/${id}`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching service:', error);
            return null;
        }
    },

    // Создать новую услугу
    create: async (serviceData) => {
        try {
            const response = await fetch(`${API_BASE}/services`, {
                method: 'POST',
                headers: getSecureHeaders(),
                body: JSON.stringify(serviceData)
            });
            return await response.json();
        } catch (error) {
            console.error('Error creating service:', error);
            return null;
        }
    },

    // Обновить услугу
    update: async (id, serviceData) => {
        try {
            const response = await fetch(`${API_BASE}/services/${id}`, {
                method: 'PUT',
                headers: getSecureHeaders(),
                body: JSON.stringify(serviceData)
            });
            return await response.json();
        } catch (error) {
            console.error('Error updating service:', error);
            return null;
        }
    },

    // Удалить услугу
    delete: async (id) => {
        try {
            await fetch(`${API_BASE}/services/${id}`, {
                method: 'DELETE',
                headers: getCsrfOnlyHeaders()
            });
            return true;
        } catch (error) {
            console.error('Error deleting service:', error);
            return false;
        }
    }
};

// ====== ORDERS API ======
const OrderAPI = {
    // Получить все заказы
    getAll: async () => {
        try {
            const response = await fetch(`${API_BASE}/orders`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching orders:', error);
            return [];
        }
    },

    // Получить заказы клиента
    getByClient: async (clientId) => {
        try {
            const response = await fetch(`${API_BASE}/orders/client/${clientId}`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching client orders:', error);
            return [];
        }
    },

    // Получить заказ по ID
    getById: async (id) => {
        try {
            const response = await fetch(`${API_BASE}/orders/${id}`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching order:', error);
            return null;
        }
    },

    // Получить заказ по номеру
    getByNumber: async (orderNumber) => {
        try {
            const response = await fetch(`${API_BASE}/orders/number/${encodeURIComponent(orderNumber)}`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching order by number:', error);
            return null;
        }
    },

    // Создать новый заказ
    create: async (orderData) => {
        try {
            const response = await fetch(`${API_BASE}/orders`, {
                method: 'POST',
                headers: getSecureHeaders(),
                body: JSON.stringify(orderData)
            });
            return await response.json();
        } catch (error) {
            console.error('Error creating order:', error);
            return null;
        }
    },

    // Обновить заказ
    update: async (id, orderData) => {
        try {
            const response = await fetch(`${API_BASE}/orders/${id}`, {
                method: 'PUT',
                headers: getSecureHeaders(),
                body: JSON.stringify(orderData)
            });
            return await response.json();
        } catch (error) {
            console.error('Error updating order:', error);
            return null;
        }
    },

    // Обновить статус заказа
    updateStatus: async (id, status) => {
        try {
            const response = await fetch(`${API_BASE}/orders/${id}/status?status=${encodeURIComponent(status)}`, {
                method: 'PUT',
                headers: getCsrfOnlyHeaders()
            });
            return await response.json();
        } catch (error) {
            console.error('Error updating order status:', error);
            return null;
        }
    },

    // Добавить платеж
    addPayment: async (id, amount) => {
        try {
            const response = await fetch(`${API_BASE}/orders/${id}/payment?amount=${amount}`, {
                method: 'POST',
                headers: getCsrfOnlyHeaders()
            });
            return await response.json();
        } catch (error) {
            console.error('Error adding payment:', error);
            return null;
        }
    },

    // Загрузить вложения (фото/видео) к заказу
    uploadAttachments: async (id, files) => {
        try {
            const formData = new FormData();
            Array.from(files || []).forEach(file => formData.append('files', file));

            const headers = {};
            const csrf = getCsrfToken();
            const headerName = getCsrfHeaderName();
            if (csrf && headerName) {
                headers[headerName] = csrf;
            }

            const response = await fetch(`${API_BASE}/orders/${id}/attachments`, {
                method: 'POST',
                headers: headers,
                body: formData
            });

            if (!response.ok) {
                const message = await response.text();
                throw new Error(message || 'Ошибка загрузки вложений');
            }

            return await response.json();
        } catch (error) {
            console.error('Error uploading order attachments:', error);
            throw error;
        }
    },

    // Удалить вложение у заказа
    deleteAttachment: async (id, attachmentUrl) => {
        try {
            const response = await fetch(`${API_BASE}/orders/${id}/attachments?url=${encodeURIComponent(attachmentUrl)}`, {
                method: 'DELETE',
                headers: getCsrfOnlyHeaders()
            });

            if (!response.ok) {
                const message = await response.text();
                throw new Error(message || 'Ошибка удаления вложения');
            }

            return await response.json();
        } catch (error) {
            console.error('Error deleting order attachment:', error);
            throw error;
        }
    },

    // Удалить заказ
    delete: async (id) => {
        try {
            await fetch(`${API_BASE}/orders/${id}`, {
                method: 'DELETE',
                headers: getCsrfOnlyHeaders()
            });
            return true;
        } catch (error) {
            console.error('Error deleting order:', error);
            return false;
        }
    }
};

// ====== NOTES PLUGIN API ======
const NotesPluginAPI = {
    getFolders: async () => {
        try {
            const response = await fetch(`${API_BASE}/notes-plugin/folders`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching note folders:', error);
            return [];
        }
    },

    createFolder: async (folderData) => {
        try {
            const response = await fetch(`${API_BASE}/notes-plugin/folders`, {
                method: 'POST',
                headers: getSecureHeaders(),
                body: JSON.stringify(folderData)
            });

            return await response.json();
        } catch (error) {
            console.error('Error creating note folder:', error);
            return null;
        }
    },

    // ...existing code...

    createNote: async (folderId, noteData) => {
        try {
            const response = await fetch(`${API_BASE}/notes-plugin/folders/${folderId}/notes`, {
                method: 'POST',
                headers: getSecureHeaders(),
                body: JSON.stringify(noteData)
            });

            return await response.json();
        } catch (error) {
            console.error('Error creating note:', error);
            return null;
        }
    },

    updateNote: async (noteId, noteData) => {
        try {
            const response = await fetch(`${API_BASE}/notes-plugin/notes/${noteId}`, {
                method: 'PUT',
                headers: getSecureHeaders(),
                body: JSON.stringify(noteData)
            });

            return await response.json();
        } catch (error) {
            console.error('Error updating note:', error);
            return null;
        }
    },

    deleteNote: async (noteId) => {
        try {
            const response = await fetch(`${API_BASE}/notes-plugin/notes/${noteId}`, {
                method: 'DELETE',
                headers: getCsrfOnlyHeaders()
            });
            return response.ok;
        } catch (error) {
            console.error('Error deleting note:', error);
            return false;
        }
    }
};

// ====== CHAT API ======
const ChatAPI = {
    getConversations: async () => {
        try {
            const response = await fetch(`${API_BASE}/chat/conversations`);
            if (!response.ok) {
                console.error('Error fetching chat conversations, status=', response.status);
                return [];
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching chat conversations:', error);
            return [];
        }
    },

    getSummary: async () => {
        try {
            const response = await fetch(`${API_BASE}/chat/summary`);
            if (!response.ok) {
                console.error('Error fetching chat summary, status=', response.status);
                return { unreadConversations: 0, openConversations: 0, totalConversations: 0 };
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching chat summary:', error);
            return { unreadConversations: 0, openConversations: 0, totalConversations: 0 };
        }
    },

    getConversation: async (id) => {
        try {
            const response = await fetch(`${API_BASE}/chat/conversations/${id}`);
            if (!response.ok) {
                console.error('Error fetching chat conversation, status=', response.status);
                return null;
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching chat conversation:', error);
            return null;
        }
    },

    sendMessage: async (id, message) => {
        try {
            const response = await fetch(`${API_BASE}/chat/conversations/${id}/messages`, {
                method: 'POST',
                headers: getSecureHeaders(),
                body: JSON.stringify({ message })
            });
            if (!response.ok) {
                const txt = await response.text().catch(() => null);
                console.error('Error sending operator message, status=', response.status, txt);
                return null;
            }
            return await response.json();
        } catch (error) {
            console.error('Error sending operator message:', error);
            return null;
        }
    },

    updateStatus: async (id, status) => {
        try {
            const response = await fetch(`${API_BASE}/chat/conversations/${id}/status?status=${encodeURIComponent(status)}`, {
                method: 'PUT',
                headers: getCsrfOnlyHeaders()
            });
            if (!response.ok) {
                console.error('Error updating chat status, status=', response.status);
                return null;
            }
            // Some endpoints return empty body on success
            const text = await response.text();
            try { return text ? JSON.parse(text) : {}; } catch (e) { return {}; }
        } catch (error) {
            console.error('Error updating chat status:', error);
            return null;
        }
    },

    markRead: async (id) => {
        try {
            const response = await fetch(`${API_BASE}/chat/conversations/${id}/read`, {
                method: 'POST',
                headers: getCsrfOnlyHeaders()
            });
            return response.ok;
        } catch (error) {
            console.error('Error marking chat as read:', error);
            return false;
        }
    },

    // ...existing code...

    saveWidgetSite: async (payload) => {
        try {
            const response = await fetch(`${API_BASE}/chat/widget-site`, {
                method: 'PUT',
                headers: getSecureHeaders(),
                body: JSON.stringify(payload)
            });
            return await response.json();
        } catch (error) {
            console.error('Error saving chat widget settings:', error);
            return null;
        }
    }
};

// ====== KANBAN API ======
const KanbanAPI = {
    getBoards: async () => {
        try {
            const response = await fetch(`${API_BASE}/kanban/boards`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching kanban boards:', error);
            return [];
        }
    },

    getBoard: async (boardId) => {
        try {
            const url = Number.isFinite(Number(boardId))
                ? `${API_BASE}/kanban/board?boardId=${encodeURIComponent(boardId)}`
                : `${API_BASE}/kanban/board`;
            const response = await fetch(url);
            return await response.json();
        } catch (error) {
            console.error('Error fetching kanban board:', error);
            return null;
        }
    },

    createBoard: async (payload) => {
        try {
            const response = await fetch(`${API_BASE}/kanban/boards`, {
                method: 'POST',
                headers: getSecureHeaders(),
                body: JSON.stringify(payload)
            });
            return await response.json();
        } catch (error) {
            console.error('Error creating kanban board:', error);
            return null;
        }
    },

    renameBoard: async (boardId, payload) => {
        try {
            const response = await fetch(`${API_BASE}/kanban/boards/${boardId}`, {
                method: 'PUT',
                headers: getSecureHeaders(),
                body: JSON.stringify(payload)
            });
            return await response.json();
        } catch (error) {
            console.error('Error renaming kanban board:', error);
            return null;
        }
    },

    // ...existing code...

    renameColumn: async (columnId, payload) => {
        try {
            const response = await fetch(`${API_BASE}/kanban/columns/${columnId}`, {
                method: 'PUT',
                headers: getSecureHeaders(),
                body: JSON.stringify(payload)
            });
            return await response.json();
        } catch (error) {
            console.error('Error renaming kanban column:', error);
            return null;
        }
    },

    createCard: async (columnId, payload) => {
        try {
            const response = await fetch(`${API_BASE}/kanban/columns/${columnId}/cards`, {
                method: 'POST',
                headers: getSecureHeaders(),
                body: JSON.stringify(payload)
            });
            return await response.json();
        } catch (error) {
            console.error('Error creating kanban card:', error);
            return null;
        }
    },

    updateCard: async (cardId, payload) => {
        try {
            const response = await fetch(`${API_BASE}/kanban/cards/${cardId}`, {
                method: 'PUT',
                headers: getSecureHeaders(),
                body: JSON.stringify(payload)
            });
            return await response.json();
        } catch (error) {
            console.error('Error updating kanban card:', error);
            return null;
        }
    },

    moveCard: async (cardId, payload) => {
        try {
            const response = await fetch(`${API_BASE}/kanban/cards/${cardId}/move`, {
                method: 'PUT',
                headers: getSecureHeaders(),
                body: JSON.stringify(payload)
            });
            return await response.json();
        } catch (error) {
            console.error('Error moving kanban card:', error);
            return null;
        }
    },

    deleteCard: async (cardId) => {
        try {
            const response = await fetch(`${API_BASE}/kanban/cards/${cardId}`, {
                method: 'DELETE',
                headers: getSecureHeaders()
            });
            return await response.json();
        } catch (error) {
            console.error('Error deleting kanban card:', error);
            return null;
        }
    },

    uploadAttachments: async (cardId, files) => {
        try {
            const formData = new FormData();
            Array.from(files || []).forEach(file => formData.append('files', file));
            
            const headers = {};
            const csrf = getCsrfToken();
            const headerName = getCsrfHeaderName();
            if (csrf && headerName) {
                headers[headerName] = csrf;
            }
            
            const response = await fetch(`${API_BASE}/kanban/cards/${cardId}/attachments`, {
                method: 'POST',
                headers: headers,
                body: formData
            });
            return await response.json();
        } catch (error) {
            console.error('Error uploading kanban attachments:', error);
            return null;
        }
    },

    getAttachments: async (cardId) => {
        try {
            const response = await fetch(`${API_BASE}/kanban/cards/${cardId}/attachments`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching kanban attachments:', error);
            return [];
        }
    },

    deleteAttachment: async (cardId, url) => {
        try {
            const response = await fetch(`${API_BASE}/kanban/cards/${cardId}/attachments?url=${encodeURIComponent(url)}`, {
                method: 'DELETE',
                headers: getSecureHeaders()
            });
            return await response.json();
        } catch (error) {
            console.error('Error deleting kanban attachment:', error);
            return null;
        }
    }
};

// ====== UTILITY FUNCTIONS ======
function formatCurrency(amount) {
    return Math.round(amount) + '₽';
}

function formatDate(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('ru-RU', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function showNotification(message, type = 'success') {
    const icons = { success: '✅', error: '❌', warning: '⚠️', info: 'ℹ️' };
    const container = document.getElementById('toast-container');
    if (!container) { console.log(`[${type.toUpperCase()}] ${message}`); return; }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
        <span class="toast-icon">${icons[type] || 'ℹ️'}</span>
        <span class="toast-msg">${message}</span>
        <span class="toast-close" onclick="this.parentElement.remove()">×</span>`;
    container.appendChild(toast);

    setTimeout(() => { toast.style.opacity = '0'; toast.style.transition = 'opacity .4s'; setTimeout(() => toast.remove(), 400); }, 3500);
}
