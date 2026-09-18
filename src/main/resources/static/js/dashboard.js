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

function calculatePeriodTotals(orders, periodStart, periodEnd) {
    const inPeriod = (orders || []).filter(order => {
        if (!order?.createdAt) return false;
        const createdAt = new Date(order.createdAt);
        if (Number.isNaN(createdAt.getTime())) return false;
        return createdAt >= periodStart && createdAt <= periodEnd;
    });

    return inPeriod.reduce((acc, order) => {
        acc.totalRevenue += Number(order.totalPrice) || 0;
        acc.totalPaid += Number(order.paidAmount) || 0;
        acc.totalMaterialCost += Number(order.materialCost) || 0;
        acc.netProfit = acc.totalRevenue - acc.totalMaterialCost;
        return acc;
    }, { totalRevenue: 0, totalPaid: 0, totalMaterialCost: 0, netProfit: 0 });
}

/** Диапазон дат для карточек «Выручка/Оплачено за период» — см. #statsPeriod. */
function statsPeriodRange() {
    const period = document.getElementById('statsPeriod')?.value || 'month';
    const now = new Date();

    if (period === 'custom') {
        const fromVal = document.getElementById('statsFromDate')?.value;
        const toVal = document.getElementById('statsToDate')?.value;
        const from = fromVal ? new Date(fromVal + 'T00:00:00') : new Date(now.getTime() - 30 * 86400000);
        const to = toVal ? new Date(toVal + 'T23:59:59.999') : now;
        return { from, to: to < from ? from : to };
    }

    let from;
    if (period === 'today') { from = new Date(now); from.setHours(0, 0, 0, 0); }
    else if (period === 'week') { from = new Date(now.getTime() - 7 * 86400000); }
    else if (period === 'month') { from = new Date(now.getTime() - 30 * 86400000); }
    else { from = new Date(2000, 0, 1); } // 'all'
    return { from, to: now };
}

/** Показать/скрыть выбор дат при переключении #statsPeriod на «Свой период». */
function onStatsPeriodChange() {
    const period = document.getElementById('statsPeriod')?.value;
    const customBox = document.getElementById('statsCustomRange');
    if (!customBox) return;

    if (period === 'custom') {
        customBox.hidden = false;
        const fromInput = document.getElementById('statsFromDate');
        const toInput = document.getElementById('statsToDate');
        // Подставляем стартовый диапазон (последние 30 дней), дальше пользователь правит сам.
        if (fromInput && toInput && !fromInput.value && !toInput.value) {
            const now = new Date();
            const from = new Date(now.getTime() - 30 * 86400000);
            toInput.value = toDateInputValue(now);
            fromInput.value = toDateInputValue(from);
        }
        return; // ждём, пока нажмут «Показать»
    }

    customBox.hidden = true;
    refreshPeriodStats();
}

function toDateInputValue(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
}

// Загрузить статистику при загрузке страницы
document.addEventListener('DOMContentLoaded', () => {
    loadDashboardData();
    loadReminders();
    setupReminderClientAutocomplete();
    // Обновлять статистику каждые 30 секунд
    setInterval(() => { loadDashboardData(); loadReminders(); }, 30000);
});

// ===== Напоминания =====

let reminderClientId = null;
let reminderClientAc = null;

function setupReminderClientAutocomplete() {
    const input = document.getElementById('reminderClientSearch');
    const menu = document.getElementById('reminderClientComboMenu');
    if (!input || !menu || !window.Autocomplete) return;

    reminderClientAc = Autocomplete.attach(input, {
        menu,
        minChars: 0,
        emptyText: 'Клиенты не найдены',
        getItems: async (query) => {
            const list = query ? await ClientAPI.search(query) : await ClientAPI.getActive();
            return (list || []).slice(0, 20).map(c => ({
                id: c.id,
                label: c.name,
                sublabel: c.phone || '',
                data: c
            }));
        },
        onSelect: (item) => {
            selectReminderClient(item.data);
            reminderClientAc.close();
        }
    });
}

function selectReminderClient(client) {
    reminderClientId = client.id;
    const box = document.getElementById('reminderClientChosen');
    box.innerHTML =
        `<strong>${escHtmlD(client.name)}</strong>` +
        (client.phone ? `<span class="combo-chosen-phone">· ${escHtmlD(client.phone)}</span>` : '') +
        `<button type="button" title="Сбросить" onclick="clearReminderClient()">&times;</button>`;
    box.hidden = false;

    const search = document.getElementById('reminderClientSearch');
    search.value = '';
    search.hidden = true;

    const textEl = document.getElementById('newReminderText');
    if (textEl && !textEl.value.trim()) textEl.value = `Перезвонить клиенту ${client.name}`;
}

function clearReminderClient() {
    reminderClientId = null;
    document.getElementById('reminderClientChosen').hidden = true;
    const search = document.getElementById('reminderClientSearch');
    search.hidden = false;
    search.value = '';
}

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
              <span>${escHtmlD(x.text)}${x.dueAt ? ` <span style="color:var(--c-muted);">· до ${fmtDateTimeD(x.dueAt)}</span>` : ''}</span>
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
    // datetime-local отдаёт "YYYY-MM-DDTHH:mm" (без секунд) — дополняем под ISO.
    if (dateEl.value) body.dueAt = dateEl.value.length === 16 ? dateEl.value + ':00' : dateEl.value;
    if (reminderClientId) body.clientId = reminderClientId;
    try {
        const r = await fetch('/api/reminders', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        if (!r.ok) { showNotification('Не удалось добавить', 'error'); return; }
        textEl.value = ''; dateEl.value = '';
        clearReminderClient();
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

function fmtDateTimeD(iso) {
    const d = new Date(iso);
    if (isNaN(d)) return String(iso).slice(0, 10);
    return d.toLocaleDateString('ru-RU') + ' ' + d.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
}

let dashboardOrdersCache = [];

// Загрузить данные панели управления
async function loadDashboardData() {
    try {
        // Загрузить клиентов
        const clients = await ClientAPI.getAll();
        document.getElementById('totalClients').textContent = clients.length;

        // Загрузить заказы
        const orders = await OrderAPI.getAll();
        dashboardOrdersCache = orders;
        const activeOrders = orders.filter(o => !['COMPLETED', 'CANCELLED'].includes(o.status)).length;
        document.getElementById('activeOrders').textContent = activeOrders;

        // Загрузить последние заказы
        renderRecentOrders(orders.slice(-5).reverse());

        // Загрузить популярные услуги
        await loadPopularServices(orders);

        // Выручка/оплачено за выбранный период (не блокирует остальные виджеты)
        await refreshPeriodStats();

    } catch (error) {
        console.error('Error loading dashboard data:', error);
    }
}

/** Перечитать карточки «Выручка/Оплачено за период» под текущий выбор в #statsPeriod. */
async function refreshPeriodStats() {
    const { from, to } = statsPeriodRange();
    const periodStats = await OrderAPI.getStatistics(from, to);
    const fallback = calculatePeriodTotals(dashboardOrdersCache, from, to);
    const totalRevenue = Number(periodStats?.totalRevenue);
    const totalPaid = Number(periodStats?.totalPaid);
    const netProfit = Number(periodStats?.netProfit);

    document.getElementById('periodRevenue').textContent = formatCurrency(
        Number.isFinite(totalRevenue) ? totalRevenue : fallback.totalRevenue
    );
    document.getElementById('totalPaid').textContent = formatCurrency(
        Number.isFinite(totalPaid) ? totalPaid : fallback.totalPaid
    );
    document.getElementById('periodProfit').textContent = formatCurrency(
        Number.isFinite(netProfit) ? netProfit : fallback.netProfit
    );
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

