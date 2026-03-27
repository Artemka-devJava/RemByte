/**
 * FixByte CRM — Управление заказами
 */

let allOrders   = [];
let allClients  = [];
let allServices = [];
let currentViewOrderId = null; // ID заказа в модале просмотра

const STATUS_LABELS = {
    'NEW':               '🆕 Новый',
    'IN_PROGRESS':       '🔧 В работе',
    'WAITING_FOR_PARTS': '⏳ Ожидание деталей',
    'READY':             '✅ Готов',
    'COMPLETED':         '🏁 Завершён',
    'CANCELLED':         '❌ Отменён'
};

document.addEventListener('DOMContentLoaded', () => {
    loadOrders();
    loadClientsForSelect();
    loadServicesForSelect();

    const searchInput = document.getElementById('searchOrder');
    if (searchInput) {
        searchInput.addEventListener('keyup', e => { if (e.key === 'Enter') searchOrders(); });
    }
});

// ===== ЗАГРУЗКА =====

async function loadOrders() {
    try {
        const orders = await OrderAPI.getAll();
        allOrders = orders;
        renderOrdersTable(orders);
    } catch (error) {
        console.error('Error loading orders:', error);
        showNotification('Ошибка загрузки заказов', 'error');
    }
}

async function loadClientsForSelect() {
    try {
        const clients = await ClientAPI.getActive();
        allClients = clients;
        const select = document.getElementById('clientSelect');
        if (select) {
            select.innerHTML = '<option value="">Выберите клиента...</option>' +
                clients.map(c => `<option value="${c.id}">${c.name} (${c.phone})</option>`).join('');
        }
    } catch (error) {
        console.error('Error loading clients:', error);
    }
}

async function loadServicesForSelect() {
    try {
        const services = await ServiceAPI.getActive();
        allServices = services;
        renderServicesCheckbox(services);
    } catch (error) {
        console.error('Error loading services:', error);
    }
}

// ===== РЕНДЕР ТАБЛИЦЫ =====

function renderOrdersTable(orders) {
    const tbody = document.getElementById('ordersTable');
    if (!orders || orders.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="empty">Нет заказов</td></tr>';
        return;
    }

    tbody.innerHTML = orders.map(order => {
        const balance = (order.totalPrice || 0) - (order.paidAmount || 0);
        const balanceStyle = balance > 0 ? 'color:var(--c-danger);font-weight:600;' : 'color:var(--c-success);font-weight:600;';
        return `
        <tr>
            <td><strong>${order.orderNumber}</strong></td>
            <td>${order.client?.name || '—'}</td>
            <td style="max-width:160px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;"
                title="${order.deviceDescription || ''}">${(order.deviceDescription || '—').substring(0,30)}</td>
            <td><span class="status-badge status-${(order.status || 'new').toLowerCase()}">${STATUS_LABELS[order.status] || order.status}</span></td>
            <td><strong>${formatCurrency(order.totalPrice)}</strong></td>
            <td>${formatCurrency(order.paidAmount)}</td>
            <td><span style="${balanceStyle}">${formatCurrency(balance)}</span></td>
            <td>${formatDate(order.createdAt)}</td>
            <td style="white-space:nowrap;">
                <button class="btn btn-sm btn-primary" onclick="viewOrder(${order.id})" title="Просмотр и смена статуса">👁 Открыть</button>
                <button class="btn btn-sm btn-secondary" onclick="editOrder(${order.id})" title="Редактировать">✏️</button>
            </td>
        </tr>`;
    }).join('');
}

// ===== ПРОСМОТР ЗАКАЗА (МОДАЛ С ВОЗМОЖНОСТЬЮ МЕНЯТЬ СТАТУС) =====

async function viewOrder(id) {
    const order = await OrderAPI.getById(id);
    if (!order) { showNotification('Заказ не найден', 'error'); return; }

    currentViewOrderId = id;

    // Заполнить поля детали
    document.getElementById('viewOrderTitle').textContent = `Заказ ${order.orderNumber}`;
    document.getElementById('vOrderNumber').textContent  = order.orderNumber;
    document.getElementById('vOrderClient').textContent  = order.client
        ? `${order.client.name} ${order.client.phone ? '· ' + order.client.phone : ''}`
        : '—';
    document.getElementById('vOrderDevice').textContent  = order.deviceDescription || '—';
    document.getElementById('vOrderServices').innerHTML  = order.services && order.services.length
        ? order.services.map(s => `<span class="status-badge status-new" style="margin:2px;">${s.name}</span>`).join('')
        : '—';
    document.getElementById('vOrderNotes').textContent   = order.notes || '—';
    document.getElementById('vOrderTotal').textContent   = formatCurrency(order.totalPrice);
    document.getElementById('vOrderPaid').textContent    = formatCurrency(order.paidAmount);

    const balance = (order.totalPrice || 0) - (order.paidAmount || 0);
    const balEl = document.getElementById('vOrderBalance');
    balEl.textContent = formatCurrency(balance);
    balEl.style.color = balance > 0 ? 'var(--c-danger)' : 'var(--c-success)';

    document.getElementById('vOrderCreated').textContent = formatDate(order.createdAt);

    // Установить текущий статус в select
    const statusSelect = document.getElementById('vOrderStatus');
    statusSelect.value = order.status || 'NEW';

    // Сбросить поле оплаты
    document.getElementById('vPaymentAmount').value = '';

    document.getElementById('viewOrderModal').style.display = 'block';
}

function closeViewOrderModal() {
    document.getElementById('viewOrderModal').style.display = 'none';
    currentViewOrderId = null;
}

// ===== СОХРАНИТЬ СТАТУС =====

async function saveOrderStatus() {
    if (!currentViewOrderId) return;

    const newStatus = document.getElementById('vOrderStatus').value;
    try {
        const result = await OrderAPI.updateStatus(currentViewOrderId, newStatus);
        if (result && result.id) {
            showNotification(`Статус изменён: ${STATUS_LABELS[newStatus] || newStatus}`, 'success');
            // Обновить значок в таблице без перезагрузки
            const idx = allOrders.findIndex(o => o.id === currentViewOrderId);
            if (idx !== -1) { allOrders[idx].status = newStatus; }
            renderOrdersTable(applyCurrentFilter());
            // Обновить заголовок модала
            document.getElementById('viewOrderTitle').textContent = `Заказ ${result.orderNumber}`;
        } else {
            showNotification('Не удалось изменить статус', 'error');
        }
    } catch (error) {
        console.error('Error updating status:', error);
        showNotification('Ошибка при изменении статуса', 'error');
    }
}

// ===== ДОБАВИТЬ ОПЛАТУ ИЗ МОДАЛА =====

async function addPaymentFromModal() {
    if (!currentViewOrderId) return;
    const amountStr = document.getElementById('vPaymentAmount').value;
    const amount = parseFloat(amountStr);
    if (!amount || amount <= 0) { showNotification('Введите корректную сумму оплаты', 'warning'); return; }

    try {
        const result = await OrderAPI.addPayment(currentViewOrderId, amount);
        if (result && result.id) {
            showNotification(`Оплата ${formatCurrency(amount)} добавлена`, 'success');
            document.getElementById('vPaymentAmount').value = '';
            // Обновить отображение в модале
            document.getElementById('vOrderPaid').textContent = formatCurrency(result.paidAmount);
            const balance = (result.totalPrice || 0) - (result.paidAmount || 0);
            const balEl = document.getElementById('vOrderBalance');
            balEl.textContent = formatCurrency(balance);
            balEl.style.color = balance > 0 ? 'var(--c-danger)' : 'var(--c-success)';
            // Обновить в массиве
            const idx = allOrders.findIndex(o => o.id === currentViewOrderId);
            if (idx !== -1) { allOrders[idx].paidAmount = result.paidAmount; }
            renderOrdersTable(applyCurrentFilter());
        } else {
            showNotification('Ошибка при добавлении оплаты', 'error');
        }
    } catch (error) {
        console.error('Error adding payment:', error);
        showNotification('Ошибка при добавлении оплаты', 'error');
    }
}

// Перейти к редактированию из модала просмотра
function openEditFromView() {
    const id = currentViewOrderId;
    closeViewOrderModal();
    editOrder(id);
}

// ===== ПРИМЕНИТЬ ТЕКУЩИЙ ФИЛЬТР =====
function applyCurrentFilter() {
    const status = document.getElementById('statusFilter')?.value;
    const search = document.getElementById('searchOrder')?.value?.toLowerCase().trim();
    let result = allOrders;
    if (status) result = result.filter(o => o.status === status);
    if (search) result = result.filter(o =>
        o.orderNumber.toLowerCase().includes(search) ||
        (o.client?.name || '').toLowerCase().includes(search)
    );
    return result;
}

// ===== СОЗДАНИЕ ЗАКАЗА =====

function openCreateOrderForm() {
    document.getElementById('orderModalTitle').textContent = '➕ Новый заказ';
    document.getElementById('createOrderModal').style.display = 'block';
    document.getElementById('createOrderForm').reset();
    document.getElementById('createOrderForm').onsubmit = submitOrder;
    updateOrderPrice();
}

function closeCreateOrderForm() {
    document.getElementById('createOrderModal').style.display = 'none';
}

async function submitOrder(event) {
    event.preventDefault();
    const clientId = document.getElementById('clientSelect').value;
    const deviceDescription = document.getElementById('deviceDescription').value;
    const notes = document.getElementById('orderNotes').value;
    const selectedServices = Array.from(document.querySelectorAll('#servicesCheckbox input[type="checkbox"]:checked'))
        .map(cb => allServices.find(s => s.id == cb.value)).filter(Boolean);

    const orderData = {
        client: { id: clientId },
        deviceDescription,
        notes: notes || null,
        services: selectedServices,
        status: 'NEW',
        totalPrice: selectedServices.reduce((sum, s) => sum + s.basePrice, 0),
        paidAmount: 0
    };

    try {
        const result = await OrderAPI.create(orderData);
        if (result && result.id) {
            showNotification('Заказ создан успешно!', 'success');
            closeCreateOrderForm();
            loadOrders();
        } else {
            showNotification('Ошибка при создании заказа', 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        showNotification('Ошибка при создании заказа', 'error');
    }
}

// ===== РЕДАКТИРОВАТЬ ЗАКАЗ =====

async function editOrder(id) {
    const order = await OrderAPI.getById(id);
    if (!order) { showNotification('Заказ не найден', 'error'); return; }

    document.getElementById('orderModalTitle').textContent = `✏️ Редактирование заказа ${order.orderNumber}`;
    document.getElementById('clientSelect').value = order.client?.id || '';
    document.getElementById('deviceDescription').value = order.deviceDescription || '';
    document.getElementById('orderNotes').value = order.notes || '';

    // Отметить услуги
    const checkboxes = document.querySelectorAll('#servicesCheckbox input[type="checkbox"]');
    checkboxes.forEach(cb => {
        cb.checked = order.services?.some(s => s.id === parseInt(cb.value)) || false;
    });

    openCreateOrderForm();
    updateOrderPrice();

    document.getElementById('createOrderForm').onsubmit = async (event) => {
        event.preventDefault();
        const selectedServices = Array.from(document.querySelectorAll('#servicesCheckbox input[type="checkbox"]:checked'))
            .map(cb => allServices.find(s => s.id == cb.value)).filter(Boolean);

        const updatedData = {
            ...order,
            client: { id: document.getElementById('clientSelect').value },
            deviceDescription: document.getElementById('deviceDescription').value,
            notes: document.getElementById('orderNotes').value || null,
            services: selectedServices,
            totalPrice: selectedServices.reduce((sum, s) => sum + s.basePrice, 0),
        };

        const result = await OrderAPI.update(id, updatedData);
        if (result && result.id) {
            showNotification('Заказ обновлён!', 'success');
            closeCreateOrderForm();
            loadOrders();
            document.getElementById('createOrderForm').onsubmit = submitOrder;
        } else {
            showNotification('Ошибка при обновлении заказа', 'error');
        }
    };
}

// ===== ПОИСК И ФИЛЬТР =====

function searchOrders() {
    renderOrdersTable(applyCurrentFilter());
}

function filterOrders() {
    renderOrdersTable(applyCurrentFilter());
}

// ===== СЕРВИСЫ ЧЕКБОКС =====

const CATEGORY_ICONS = {
    'Диагностика':'🔍','Ремонт':'🔧','Чистка':'🧹','Замена':'🔄',
    'Установка':'💿','Обслуживание':'⚙️','Данные':'💾','Прочее':'📦'
};

function getCategoryIcon(name) {
    for (const [k, v] of Object.entries(CATEGORY_ICONS)) {
        if (name && name.toLowerCase().includes(k.toLowerCase())) return v;
    }
    return '🛠️';
}

function detectCategory(service) {
    const n = (service.name || '').toLowerCase();
    if (n.includes('диагност'))    return 'Диагностика';
    if (n.includes('чист'))        return 'Чистка';
    if (n.includes('замен'))       return 'Замена';
    if (n.includes('установ') || n.includes('инстал')) return 'Установка';
    if (n.includes('восстан') || n.includes('данн') || n.includes('резерв')) return 'Данные';
    if (n.includes('ремонт') || n.includes('пайк') || n.includes('плат')) return 'Ремонт';
    if (n.includes('обслуж') || n.includes('настр') || n.includes('оптим')) return 'Обслуживание';
    return 'Прочее';
}

function renderServicesCheckbox(services) {
    const container = document.getElementById('servicesCheckbox');
    if (!container) return;

    const groups = {};
    services.forEach(s => {
        const cat = detectCategory(s);
        if (!groups[cat]) groups[cat] = [];
        groups[cat].push(s);
    });

    const sortOrder = ['Диагностика','Ремонт','Замена','Чистка','Установка','Обслуживание','Данные','Прочее'];
    const sortedCats = Object.keys(groups).sort((a, b) => {
        const ai = sortOrder.indexOf(a), bi = sortOrder.indexOf(b);
        return (ai === -1 ? 99 : ai) - (bi === -1 ? 99 : bi);
    });

    container.innerHTML = sortedCats.map(cat => {
        const icon = getCategoryIcon(cat);
        const items = groups[cat].map(service => `
            <div class="service-item" onclick="toggleService(this)">
                <input type="checkbox" id="svc_${service.id}" value="${service.id}"
                       data-price="${service.basePrice}" onchange="updateOrderPrice()">
                <label for="svc_${service.id}">${service.name}</label>
                <span class="service-price">${formatCurrency(service.basePrice)}</span>
            </div>`).join('');

        return `
            <div class="services-group">
                <div class="services-group-header" onclick="toggleGroup(this.parentElement)">
                    ${icon} ${cat} <small style="opacity:.75;font-weight:400;">(${groups[cat].length})</small>
                </div>
                <div class="services-group-body">${items}</div>
            </div>`;
    }).join('');
}

function toggleGroup(groupEl) { groupEl.classList.toggle('collapsed'); }

function toggleService(itemEl) {
    const cb = itemEl.querySelector('input[type="checkbox"]');
    if (cb && event.target !== cb && event.target.tagName !== 'LABEL') {
        cb.checked = !cb.checked;
        updateOrderPrice();
    }
}

function updateOrderPrice() {
    const checkboxes = document.querySelectorAll('#servicesCheckbox input[type="checkbox"]:checked');
    let total = 0;
    checkboxes.forEach(cb => { total += parseFloat(cb.dataset.price || 0); });
    const el = document.getElementById('orderTotalPrice');
    if (el) el.textContent = formatCurrency(total);
}

// ===== ЗАКРЫТИЕ МОДАЛОВ =====

window.onclick = function(event) {
    const viewModal   = document.getElementById('viewOrderModal');
    const createModal = document.getElementById('createOrderModal');
    if (event.target === viewModal)   closeViewOrderModal();
    if (event.target === createModal) closeCreateOrderForm();
};
