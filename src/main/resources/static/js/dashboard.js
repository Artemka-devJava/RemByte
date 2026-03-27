/**
 * JavaScript код для панели управления
 */

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

        // Рассчитать выручку за текущий месяц
        const now = new Date();
        const monthStart = new Date(now.getFullYear(), now.getMonth(), 1).toISOString();
        const monthEnd = now.toISOString();

        const monthlyStats = await OrderAPI.getStatistics(monthStart, monthEnd);
        if (monthlyStats) {
            document.getElementById('monthlyRevenue').textContent = formatCurrency(monthlyStats.totalRevenue);
            document.getElementById('totalPaid').textContent = formatCurrency(monthlyStats.totalPaid);
        }

        // Загрузить последние заказы
        renderRecentOrders(orders.slice(-5).reverse());

        // Загрузить популярные услуги
        await loadPopularServices(orders);

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
                    ${order.status || 'NEW'}
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
        order.services?.forEach(service => {
            serviceCount[service.name] = (serviceCount[service.name] || 0) + 1;
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

