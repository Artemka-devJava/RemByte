/**
 * API клиент для FixByte CRM
 * Содержит все методы для взаимодействия с REST API
 */

const API_BASE = '/api';

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
                headers: { 'Content-Type': 'application/json' },
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
                headers: { 'Content-Type': 'application/json' },
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
            await fetch(`${API_BASE}/clients/${id}`, { method: 'DELETE' });
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
                headers: { 'Content-Type': 'application/json' },
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
                headers: { 'Content-Type': 'application/json' },
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
            await fetch(`${API_BASE}/services/${id}`, { method: 'DELETE' });
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
                headers: { 'Content-Type': 'application/json' },
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
                headers: { 'Content-Type': 'application/json' },
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
                method: 'PUT'
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
                method: 'POST'
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

            const response = await fetch(`${API_BASE}/orders/${id}/attachments`, {
                method: 'POST',
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
                method: 'DELETE'
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

    // Получить статистику
    getStatistics: async (from, to) => {
        try {
            const response = await fetch(`${API_BASE}/orders/statistics?from=${from}&to=${to}`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching statistics:', error);
            return null;
        }
    },

    // Удалить заказ
    delete: async (id) => {
        try {
            await fetch(`${API_BASE}/orders/${id}`, { method: 'DELETE' });
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
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(folderData)
            });

            return await response.json();
        } catch (error) {
            console.error('Error creating note folder:', error);
            return null;
        }
    },

    getNotesByFolder: async (folderId) => {
        try {
            const response = await fetch(`${API_BASE}/notes-plugin/folders/${folderId}/notes`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching notes:', error);
            return [];
        }
    },

    createNote: async (folderId, noteData) => {
        try {
            const response = await fetch(`${API_BASE}/notes-plugin/folders/${folderId}/notes`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
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
                headers: { 'Content-Type': 'application/json' },
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
                method: 'DELETE'
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
            return await response.json();
        } catch (error) {
            console.error('Error fetching chat conversations:', error);
            return [];
        }
    },

    getConversation: async (id) => {
        try {
            const response = await fetch(`${API_BASE}/chat/conversations/${id}`);
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
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message })
            });
            return await response.json();
        } catch (error) {
            console.error('Error sending operator message:', error);
            return null;
        }
    },

    updateStatus: async (id, status) => {
        try {
            const response = await fetch(`${API_BASE}/chat/conversations/${id}/status?status=${encodeURIComponent(status)}`, {
                method: 'PUT'
            });
            return await response.json();
        } catch (error) {
            console.error('Error updating chat status:', error);
            return null;
        }
    },

    markRead: async (id) => {
        try {
            const response = await fetch(`${API_BASE}/chat/conversations/${id}/read`, {
                method: 'POST'
            });
            return response.ok;
        } catch (error) {
            console.error('Error marking chat as read:', error);
            return false;
        }
    },

    getSummary: async () => {
        try {
            const response = await fetch(`${API_BASE}/chat/summary`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching chat summary:', error);
            return { unreadConversations: 0, openConversations: 0, totalConversations: 0 };
        }
    },

    getWidgetSite: async () => {
        try {
            const response = await fetch(`${API_BASE}/chat/widget-site`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching chat widget settings:', error);
            return null;
        }
    },

    saveWidgetSite: async (payload) => {
        try {
            const response = await fetch(`${API_BASE}/chat/widget-site`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
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

    createBoard: async (payload) => {
        try {
            const response = await fetch(`${API_BASE}/kanban/boards`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
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
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            return await response.json();
        } catch (error) {
            console.error('Error renaming kanban board:', error);
            return null;
        }
    },

    getBoard: async (boardId) => {
        try {
            const suffix = Number.isFinite(Number(boardId)) ? `?boardId=${encodeURIComponent(boardId)}` : '';
            const response = await fetch(`${API_BASE}/kanban/board${suffix}`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching kanban board:', error);
            return null;
        }
    },

    renameColumn: async (columnId, payload) => {
        try {
            const response = await fetch(`${API_BASE}/kanban/columns/${columnId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
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
                headers: { 'Content-Type': 'application/json' },
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
                headers: { 'Content-Type': 'application/json' },
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
                headers: { 'Content-Type': 'application/json' },
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
                method: 'DELETE'
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
            const response = await fetch(`${API_BASE}/kanban/cards/${cardId}/attachments`, {
                method: 'POST',
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
                method: 'DELETE'
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
