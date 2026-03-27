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

