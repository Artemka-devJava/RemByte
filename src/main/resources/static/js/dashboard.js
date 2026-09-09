/**
 * JavaScript код для панели управления
 */

const DASHBOARD_STATUS_LABELS = {
    NEW: 'Новый',
    IN_PROGRESS: 'В работе',
    WAITING_FOR_PARTS: 'Ожидание деталей',
    READY: 'Готов',
    COMPLETED: 'Завершён',
    CANCELLED: 'Отменён'
};

function getStatusLabel(status) {
    return DASHBOARD_STATUS_LABELS[status] || status || 'Новый';
}

function calculateMonthlyTotals(orders, monthStart, monthEnd) {
    const inMonth = (orders || []).filter(order => {
        if (!order?.createdAt) return false;
        const createdAt = new Date(order.createdAt);
        if (Number.isNaN(createdAt.getTime())) return false;
        return createdAt >= monthStart && createdAt <= monthEnd;
    });

    return inMonth.reduce((acc, order) => {
        acc.totalRevenue += Number(order.totalPrice) || 0;
        acc.totalPaid += Number(order.paidAmount) || 0;
        return acc;
    }, { totalRevenue: 0, totalPaid: 0 });
}

// Загрузить статистику при загрузке страницы
document.addEventListener('DOMContentLoaded', () => {
    loadDashboardData();
    loadReminders();
    // Обновлять статистику каждые 30 секунд
    setInterval(() => { loadDashboardData(); loadReminders(); }, 30000);
});

// ===== Напоминания =====

async function loadReminders() {
    try {
        const r = await fetch('/api/reminders', { cache: 'no-store' });
        if (!r.ok) return;
        const data = await r.json();
        const reminders = Array.isArray(data.reminders) ? data.reminders : [];
        const stale = Array.isArray(data.staleOrders) ? data.staleOrders : [];

        const count = reminders.length + stale.length;
        document.getElementById('reminderCount').textContent = count ? `(${count})` : '';

        const staleBlock = document.getElementById('staleOrdersBlock');
        const staleUl = document.getElementById('staleOrders');
        if (stale.length) {
            staleBlock.style.display = 'block';
            staleUl.innerHTML = stale.map(s => `
                <li>
                  <a href="/orders" style="color:var(--c-primary);text-decoration:none;">№ ${escHtmlD(s.orderNumber || s.orderId)}</a>
                  — ${escHtmlD(s.clientName || '')}
                  <span style="color:var(--c-muted);">· лежит с ${fmtDateD(s.since)}</span>
                </li>`).join('');
        } else {
            staleBlock.style.display = 'none';
        }

        const list = document.getElementById('reminderList');
        list.innerHTML = reminders.length ? reminders.map(x => `
            <li style="display:flex;align-items:center;gap:8px;">
              <button class="btn btn-sm btn-secondary" title="Выполнено" onclick="doneReminder(${x.id})">✓</button>
              <span>${escHtmlD(x.text)}${x.dueAt ? ` <span style="color:var(--c-muted);">· до ${fmtDateD(x.dueAt)}</span>` : ''}</span>
            </li>`).join('') : '<li style="color:var(--c-muted)">Нет активных задач</li>';
    } catch (e) {
        /* напоминания необязательны */
    }
}

async function addReminder() {
    const textEl = document.getElementById('newReminderText');
    const dateEl = document.getElementById('newReminderDate');
    const text = (textEl.value || '').trim();
    if (!text) { showNotification('Введите текст напоминания', 'warning'); return; }
    const body = { text };
    if (dateEl.value) body.dueAt = dateEl.value + 'T09:00:00';
    try {
        const r = await fetch('/api/reminders', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        if (!r.ok) { showNotification('Не удалось добавить', 'error'); return; }
        textEl.value = ''; dateEl.value = '';
        loadReminders();
    } catch (e) {
        showNotification('Ошибка сети: ' + e.message, 'error');
    }
}

async function doneReminder(id) {
    try {
        await fetch('/api/reminders/' + id + '/done', { method: 'PUT' });
        loadReminders();
    } catch (e) { /* ignore */ }
}

function escHtmlD(s) {
    return String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
function fmtDateD(iso) {
    const d = new Date(iso);
    return isNaN(d) ? String(iso).slice(0, 10) : d.toLocaleDateString('ru-RU');
}

// Загрузить данные панели управления
async function loadDashboardData() {
    try {
        // Загрузить клиентов
        const clients = await ClientAPI.getAll();
        document.getElementById('totalClients').textContent = clients.length;

        // Загрузить заказы
        const orders = await OrderAPI.getAll();
        const activeOrders = orders.filter(o => !['COMPLETED', 'CANCELLED'].includes(o.status)).length;
        document.getElementById('activeOrders').textContent = activeOrders;

        // Загрузить последние заказы
        renderRecentOrders(orders.slice(-5).reverse());

        // Загрузить популярные услуги
        await loadPopularServices(orders);

        // Рассчитать выручку за текущий месяц (не блокирует остальные виджеты)
        const now = new Date();
        const monthStart = new Date(now.getFullYear(), now.getMonth(), 1);
        const monthlyStats = await OrderAPI.getStatistics(monthStart, now);
        const fallback = calculateMonthlyTotals(orders, monthStart, now);
        const totalRevenue = Number(monthlyStats?.totalRevenue);
        const totalPaid = Number(monthlyStats?.totalPaid);

        document.getElementById('monthlyRevenue').textContent = formatCurrency(
            Number.isFinite(totalRevenue) ? totalRevenue : fallback.totalRevenue
        );
        document.getElementById('totalPaid').textContent = formatCurrency(
            Number.isFinite(totalPaid) ? totalPaid : fallback.totalPaid
        );

    } catch (error) {
        console.error('Error loading dashboard data:', error);
    }
}

// Отобразить последние заказы
function renderRecentOrders(orders) {
    const tbody = document.querySelector('#recentOrders tbody');

    if (!orders || orders.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="empty">Нет заказов</td></tr>';
        return;
    }

    tbody.innerHTML = orders.map(order => `
        <tr>
            <td><strong>${order.orderNumber}</strong></td>
            <td>${order.client?.name || '-'}</td>
            <td>
                <span class="status-badge status-${order.status?.toLowerCase() || 'new'}">
                    ${getStatusLabel(order.status)}
                </span>
            </td>
            <td>${formatCurrency(order.totalPrice)}</td>
            <td>${formatCurrency(order.paidAmount)}</td>
        </tr>
    `).join('');
}

// Загрузить популярные услуги
async function loadPopularServices(orders) {
    const serviceCount = {};
    
    orders.forEach(order => {
        (order.lines || []).forEach(line => {
            serviceCount[line.name] = (serviceCount[line.name] || 0) + 1;
        });
    });

    const sorted = Object.entries(serviceCount)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 5);

    const list = document.getElementById('popularServices');
    if (sorted.length === 0) {
        list.innerHTML = '<li>Нет данных о популярных услугах</li>';
    } else {
        list.innerHTML = sorted.map(([name, count]) => 
            `<li>📌 <strong>${name}</strong> - ${count} заказ${count > 1 ? 'ов' : ''}</li>`
        ).join('');
    }
}

