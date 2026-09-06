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
    // Обновлять статистику каждые 30 секунд
    setInterval(loadDashboardData, 30000);
});

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

