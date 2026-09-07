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

/**
 * Преобразует Date/строку в формат LocalDateTime без timezone: yyyy-MM-ddTHH:mm:ss
 */
function toLocalDateTimeParam(value) {
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) {
        throw new Error('Invalid date value for statistics request');
    }

    const pad = (n) => String(n).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
        `T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
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

    // Создать клиента. При дубле телефона -> { duplicate:true, existing:<клиент> }
    create: async (clientData) => {
        try {
            const response = await fetch(`${API_BASE}/clients`, {
                method: 'POST',
                headers: getSecureHeaders(),
                body: JSON.stringify(clientData)
            });
            if (response.status === 409) {
                return { duplicate: true, existing: await response.json().catch(() => null) };
            }
            if (!response.ok) return null;
            return await response.json();
        } catch (error) {
            console.error('Error creating client:', error);
            return null;
        }
    },

    // Обновить клиента. При дубле телефона -> { duplicate:true, existing:<клиент> }
    update: async (id, clientData) => {
        try {
            const response = await fetch(`${API_BASE}/clients/${id}`, {
                method: 'PUT',
                headers: getSecureHeaders(),
                body: JSON.stringify(clientData)
            });
            if (response.status === 409) {
                return { duplicate: true, existing: await response.json().catch(() => null) };
            }
            if (!response.ok) return null;
            return await response.json();
        } catch (error) {
            console.error('Error updating client:', error);
            return null;
        }
    },

    delete: async (id) => {
        try {
            await fetch(`${API_BASE}/clients/${id}`, { method: 'DELETE', headers: getCsrfOnlyHeaders() });
            return true;
        } catch (error) {
            console.error('Error deleting client:', error);
            return false;
        }
    },

    archive: async (id) => _clientAction(id, 'archive'),
    restore: async (id) => _clientAction(id, 'restore'),

    getSummary: async (id) => _clientGet(`${id}/summary`, null),

    // Журнал
    getNotes:   async (id) => _clientGet(`${id}/notes`, []),
    addNote:    async (id, note) => _clientPost(`${id}/notes`, note),
    deleteNote: async (id, noteId) => _clientDelete(`${id}/notes/${noteId}`),

    // Устройства
    getDevices:   async (id) => _clientGet(`${id}/devices`, []),
    addDevice:    async (id, device) => _clientPost(`${id}/devices`, device),
    updateDevice: async (id, deviceId, device) => _clientPut(`${id}/devices/${deviceId}`, device),
    deleteDevice: async (id, deviceId) => _clientDelete(`${id}/devices/${deviceId}`),

    // Фото
    getPhotos: async (id) => _clientGet(`${id}/photos`, []),
    uploadPhotos: async (id, files, caption, deviceId) => {
        try {
            const fd = new FormData();
            Array.from(files || []).forEach(f => fd.append('files', f));
            if (caption) fd.append('caption', caption);
            if (deviceId) fd.append('deviceId', deviceId);
            const response = await fetch(`${API_BASE}/clients/${id}/photos`, {
                method: 'POST', headers: getCsrfOnlyHeaders(), body: fd
            });
            if (!response.ok) return { error: await response.text().catch(() => 'Ошибка загрузки') };
            return await response.json();
        } catch (error) {
            console.error('Error uploading client photos:', error);
            return { error: 'network' };
        }
    },
    deletePhoto: async (id, photoId) => _clientDelete(`${id}/photos/${photoId}`)
};

async function _clientGet(path, fallback) {
    try {
        const r = await fetch(`${API_BASE}/clients/${path}`);
        if (!r.ok) return fallback;
        return await r.json();
    } catch (e) { console.error('client GET', path, e); return fallback; }
}
async function _clientPost(path, body) {
    try {
        const r = await fetch(`${API_BASE}/clients/${path}`, {
            method: 'POST', headers: getSecureHeaders(), body: JSON.stringify(body)
        });
        return r.ok ? await r.json() : null;
    } catch (e) { console.error('client POST', path, e); return null; }
}
async function _clientPut(path, body) {
    try {
        const r = await fetch(`${API_BASE}/clients/${path}`, {
            method: 'PUT', headers: getSecureHeaders(), body: JSON.stringify(body)
        });
        return r.ok ? await r.json() : null;
    } catch (e) { console.error('client PUT', path, e); return null; }
}
async function _clientDelete(path) {
    try {
        const r = await fetch(`${API_BASE}/clients/${path}`, { method: 'DELETE', headers: getCsrfOnlyHeaders() });
        return r.ok;
    } catch (e) { console.error('client DELETE', path, e); return false; }
}
async function _clientAction(id, action) {
    try {
        const r = await fetch(`${API_BASE}/clients/${id}/${action}`, {
            method: 'PUT', headers: getCsrfOnlyHeaders()
        });
        return r.ok ? await r.json() : null;
    } catch (e) { console.error('client action', action, e); return null; }
}

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

    // Добавить позицию (услугу / разовую работу) в заказ
    addLine: async (id, line) => {
        try {
            const response = await fetch(`${API_BASE}/orders/${id}/lines`, {
                method: 'POST',
                headers: getSecureHeaders(),
                body: JSON.stringify(line)
            });
            if (!response.ok) return null;
            return await response.json();
        } catch (error) {
            console.error('Error adding order line:', error);
            return null;
        }
    },

    // Изменить позицию заказа
    updateLine: async (id, lineId, line) => {
        try {
            const response = await fetch(`${API_BASE}/orders/${id}/lines/${lineId}`, {
                method: 'PUT',
                headers: getSecureHeaders(),
                body: JSON.stringify(line)
            });
            if (!response.ok) return null;
            return await response.json();
        } catch (error) {
            console.error('Error updating order line:', error);
            return null;
        }
    },

    // Удалить позицию из заказа
    deleteLine: async (id, lineId) => {
        try {
            const response = await fetch(`${API_BASE}/orders/${id}/lines/${lineId}`, {
                method: 'DELETE',
                headers: getCsrfOnlyHeaders()
            });
            if (!response.ok) return null;
            return await response.json();
        } catch (error) {
            console.error('Error deleting order line:', error);
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

    // Получить статистику заказов за период
    getStatistics: async (from, to) => {
        try {
            const fromParam = encodeURIComponent(toLocalDateTimeParam(from));
            const toParam = encodeURIComponent(toLocalDateTimeParam(to));
            const response = await fetch(`${API_BASE}/orders/statistics?from=${fromParam}&to=${toParam}`);

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || `Statistics request failed with status ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error fetching order statistics:', error);
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
    getWidgetSite: async () => {
        try {
            const response = await fetch(`${API_BASE}/chat/widget-site`, { cache: 'no-store' });
            if (!response.ok) {
                const text = await response.text().catch(() => '');
                return { error: text || `HTTP ${response.status}` };
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading chat widget settings:', error);
            return { error: 'network_error' };
        }
    },

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

    deleteConversation: async (id) => {
        try {
            const response = await fetch(`${API_BASE}/chat/conversations/${id}`, {
                method: 'DELETE',
                headers: getCsrfOnlyHeaders()
            });
            if (!response.ok) {
                const txt = await response.text().catch(() => null);
                console.error('Error deleting chat conversation, status=', response.status, txt);
                return null;
            }
            const text = await response.text();
            try { return text ? JSON.parse(text) : { success: true }; } catch { return { success: true }; }
        } catch (error) {
            console.error('Error deleting chat conversation:', error);
            return null;
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

            const text = await response.text();
            let data = {};
            try {
                data = text ? JSON.parse(text) : {};
            } catch {
                data = {};
            }

            if (!response.ok) {
                return { error: data?.error || text || `HTTP ${response.status}` };
            }

            return data;
        } catch (error) {
            console.error('Error saving chat widget settings:', error);
            return { error: 'network_error' };
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

    createColumn: async (boardId, payload) => {
        try {
            const response = await fetch(`${API_BASE}/kanban/boards/${boardId}/columns`, {
                method: 'POST',
                headers: getSecureHeaders(),
                body: JSON.stringify(payload)
            });

            const text = await response.text();
            let data = {};
            try {
                data = text ? JSON.parse(text) : {};
            } catch {
                data = {};
            }

            if (!response.ok) {
                return { error: data?.error || text || `HTTP ${response.status}` };
            }

            return data;
        } catch (error) {
            console.error('Error creating kanban column:', error);
            return { error: 'network_error' };
        }
    },

    deleteColumn: async (columnId) => {
        try {
            const response = await fetch(`${API_BASE}/kanban/columns/${columnId}`, {
                method: 'DELETE',
                headers: getSecureHeaders()
            });

            const text = await response.text();
            let data = {};
            try {
                data = text ? JSON.parse(text) : {};
            } catch {
                data = {};
            }

            if (!response.ok) {
                return { error: data?.error || text || `HTTP ${response.status}` };
            }

            return data;
        } catch (error) {
            console.error('Error deleting kanban column:', error);
            return { error: 'network_error' };
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

// ====== PARTS API (комплектующие: закупка/продажа деталей и лотов) ======
const PartsAPI = {
    getAll: async (status) => {
        try {
            const qs = status ? `?status=${encodeURIComponent(status)}` : '';
            const response = await fetch(`${API_BASE}/parts${qs}`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching parts:', error);
            return [];
        }
    },

    getById: async (id) => _partsGet(`${id}`, null),

    create: async (data) => {
        try {
            const response = await fetch(`${API_BASE}/parts`, {
                method: 'POST', headers: getSecureHeaders(), body: JSON.stringify(data)
            });
            if (!response.ok) return { error: await response.text().catch(() => 'Ошибка создания') };
            return await response.json();
        } catch (error) {
            console.error('Error creating part item:', error);
            return { error: 'network' };
        }
    },

    update: async (id, data) => _partsPut(`${id}`, data),

    delete: async (id) => _partsDelete(`${id}`),

    sell: async (id, salePrice, saleDate) => {
        try {
            const response = await fetch(`${API_BASE}/parts/${id}/sell`, {
                method: 'POST', headers: getSecureHeaders(),
                body: JSON.stringify({ salePrice, saleDate: saleDate ? toLocalDateTimeParam(saleDate) : null })
            });
            if (!response.ok) return { error: await response.text().catch(() => 'Ошибка продажи') };
            return await response.json();
        } catch (error) {
            console.error('Error selling part item:', error);
            return { error: 'network' };
        }
    },

    // Фото
    getPhotos: async (id) => _partsGet(`${id}/photos`, []),
    uploadPhotos: async (id, files) => {
        try {
            const fd = new FormData();
            Array.from(files || []).forEach(f => fd.append('files', f));
            const response = await fetch(`${API_BASE}/parts/${id}/photos`, {
                method: 'POST', headers: getCsrfOnlyHeaders(), body: fd
            });
            if (!response.ok) return { error: await response.text().catch(() => 'Ошибка загрузки') };
            return await response.json();
        } catch (error) {
            console.error('Error uploading part photos:', error);
            return { error: 'network' };
        }
    },
    deletePhoto: async (id, photoId) => _partsDelete(`${id}/photos/${photoId}`),

    // Лоты
    getLots: async () => {
        try {
            const response = await fetch(`${API_BASE}/parts/lots`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching part lots:', error);
            return [];
        }
    },
    getLot: async (id) => _partsGet(`lots/${id}`, null),
    createLot: async (title, itemIds) => {
        try {
            const response = await fetch(`${API_BASE}/parts/lots`, {
                method: 'POST', headers: getSecureHeaders(), body: JSON.stringify({ title, itemIds })
            });
            if (!response.ok) return { error: await response.text().catch(() => 'Ошибка создания лота') };
            return await response.json();
        } catch (error) {
            console.error('Error creating part lot:', error);
            return { error: 'network' };
        }
    },
    updateLot: async (id, title, itemIds) => _partsPut(`lots/${id}`, { title, itemIds }),
    disbandLot: async (id) => _partsDelete(`lots/${id}`),
    sellLot: async (id, salePrice, saleDate) => {
        try {
            const response = await fetch(`${API_BASE}/parts/lots/${id}/sell`, {
                method: 'POST', headers: getSecureHeaders(),
                body: JSON.stringify({ salePrice, saleDate: saleDate ? toLocalDateTimeParam(saleDate) : null })
            });
            if (!response.ok) return { error: await response.text().catch(() => 'Ошибка продажи') };
            return await response.json();
        } catch (error) {
            console.error('Error selling part lot:', error);
            return { error: 'network' };
        }
    },

    // Бюджет и статистика
    getBudget: async () => {
        try {
            const response = await fetch(`${API_BASE}/parts/budget`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching parts budget:', error);
            return null;
        }
    },
    setBudget: async (startingAmount) => {
        try {
            const response = await fetch(`${API_BASE}/parts/budget`, {
                method: 'PUT', headers: getSecureHeaders(), body: JSON.stringify({ startingAmount })
            });
            return await response.json();
        } catch (error) {
            console.error('Error updating parts budget:', error);
            return null;
        }
    },
    getStats: async (from, to) => {
        try {
            let qs = '';
            if (from) qs += `${qs ? '&' : '?'}from=${encodeURIComponent(toLocalDateTimeParam(from))}`;
            if (to) qs += `${qs ? '&' : '?'}to=${encodeURIComponent(toLocalDateTimeParam(to))}`;
            const response = await fetch(`${API_BASE}/parts/stats${qs}`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching parts statistics:', error);
            return null;
        }
    }
};

async function _partsGet(path, fallback) {
    try {
        const r = await fetch(`${API_BASE}/parts/${path}`);
        if (!r.ok) return fallback;
        return await r.json();
    } catch (e) { console.error('parts GET', path, e); return fallback; }
}
async function _partsPut(path, body) {
    try {
        const r = await fetch(`${API_BASE}/parts/${path}`, {
            method: 'PUT', headers: getSecureHeaders(), body: JSON.stringify(body)
        });
        if (!r.ok) return { error: await r.text().catch(() => 'Ошибка') };
        return await r.json();
    } catch (e) { console.error('parts PUT', path, e); return { error: 'network' }; }
}
async function _partsDelete(path) {
    try {
        const r = await fetch(`${API_BASE}/parts/${path}`, { method: 'DELETE', headers: getCsrfOnlyHeaders() });
        if (!r.ok) return { error: await r.text().catch(() => 'Ошибка') };
        return true;
    } catch (e) { console.error('parts DELETE', path, e); return { error: 'network' }; }
}

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
