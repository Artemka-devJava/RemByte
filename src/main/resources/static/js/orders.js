/**
 * FixByte CRM — Управление заказами
 */

let allOrders   = [];
let allServices = [];
let currentViewOrderId = null; // ID заказа в модале просмотра
let currentViewOrder   = null; // полный объект заказа для чека

// Состав нового/редактируемого заказа: [{ tempId, serviceId|null, name, unitPrice, quantity }]
let orderLines = [];
let lineTempSeq = 0;

// Autocomplete-контроллеры
let clientAc = null;
let serviceAc = null;
let viewServiceAc = null;

const pendingAttachmentFiles = {
    createAttachmentFiles: [],
    vAttachmentFiles: []
};

const ALLOWED_IMAGE_EXTENSIONS = new Set(['.jpg', '.jpeg', '.png', '.webp', '.gif']);
const ALLOWED_VIDEO_EXTENSIONS = new Set(['.mp4', '.webm', '.mov', '.m4v']);
const ALLOWED_FILE_EXTENSIONS  = new Set(['.txt', '.pdf', '.doc', '.docx', '.xls', '.xlsx', '.csv', '.rtf']);
const MAX_IMAGE_SIZE_BYTES = 15 * 1024 * 1024;
const MAX_VIDEO_SIZE_BYTES = 120 * 1024 * 1024;
const MAX_FILE_SIZE_BYTES  = 20 * 1024 * 1024;

const STATUS_LABELS = {
    'NEW':               '🆕 Новый',
    'IN_PROGRESS':       '🔧 В работе',
    'WAITING_FOR_PARTS': '⏳ Ожидание деталей',
    'READY':             '✅ Готов',
    'COMPLETED':         '🏁 Завершён',
    'CANCELLED':         '❌ Отменён'
};

// Ключ настроек чека в localStorage (совпадает с admin.html)
const RECEIPT_SETTINGS_KEY = 'rembyte_receipt_settings';
function loadReceiptSettings() {
    try { return JSON.parse(localStorage.getItem(RECEIPT_SETTINGS_KEY) || '{}'); }
    catch { return {}; }
}

document.addEventListener('DOMContentLoaded', () => {
    loadOrders();
    loadServicesForSelect();
    setupClientAutocomplete();
    setupServiceAutocomplete();
    setupViewServiceAutocomplete();

    const searchInput = document.getElementById('searchOrder');
    if (searchInput) {
        searchInput.addEventListener('keyup', e => { if (e.key === 'Enter') searchOrders(); });
    }

    initAttachmentInputAndDropzone('createAttachmentFiles', 'createAttachmentFilesList', 'createAttachmentDropzone');
    initAttachmentInputAndDropzone('vAttachmentFiles', 'vAttachmentFilesList', 'vAttachmentDropzone');

    // Открыть форму нового заказа сразу, если пришли по /orders?new=1
    const params = new URLSearchParams(window.location.search);
    if (params.get('new')) {
        openCreateOrderForm();
        const clientId = params.get('clientId');
        if (clientId) {
            ClientAPI.getById(clientId).then(c => { if (c && c.id) selectClient(c); });
        }
    }
    // Открыть конкретный заказ: /orders?open=<id>
    const openId = params.get('open');
    if (openId) viewOrder(Number(openId));
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

async function loadServicesForSelect() {
    try {
        const services = await ServiceAPI.getActive();
        allServices = Array.isArray(services) ? services : [];
        applyCalculatorPreSelection();  // предзаполнить из калькулятора, если пришли оттуда
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

// ============================================================
//  ВЫБОР КЛИЕНТА (поиск + создание на месте)
// ============================================================

function setupClientAutocomplete() {
    const input = document.getElementById('clientSearch');
    const menu  = document.getElementById('clientComboMenu');
    if (!input || !menu || !window.Autocomplete) return;

    clientAc = Autocomplete.attach(input, {
        menu,
        minChars: 0,
        emptyText: 'Ничего не найдено — можно создать нового',
        getItems: async (query) => {
            const list = query
                ? await ClientAPI.search(query)
                : await ClientAPI.getActive();
            return (list || []).slice(0, 20).map(c => ({
                id: c.id,
                label: c.name,
                sublabel: c.phone || '',
                data: c
            }));
        },
        footer: (query) => query
            ? [{ id: '__create__', label: `➕ Создать клиента «${query}»`, kind: 'action', data: query }]
            : [],
        onSelect: (item) => {
            if (item.id === '__create__') {
                startCreateClient(item.data);
            } else {
                selectClient(item.data);
            }
            clientAc.close();
        }
    });
}

function selectClient(client) {
    document.getElementById('clientId').value = client.id;
    const box = document.getElementById('clientChosen');
    box.innerHTML =
        `<strong>${escHtml(client.name)}</strong>` +
        (client.phone ? `<span class="combo-chosen-phone">· ${escHtml(client.phone)}</span>` : '') +
        `<button type="button" title="Сбросить" onclick="clearClient()">&times;</button>`;
    box.hidden = false;

    document.getElementById('clientSearch').value = '';
    document.getElementById('clientSearch').hidden = true;
    document.getElementById('clientCreateBox').hidden = true;
}

function clearClient() {
    document.getElementById('clientId').value = '';
    document.getElementById('clientChosen').hidden = true;
    const search = document.getElementById('clientSearch');
    search.hidden = false;
    search.value = '';
    search.focus();
}

function startCreateClient(prefillName) {
    const box = document.getElementById('clientCreateBox');
    box.hidden = false;
    const nameEl = document.getElementById('newClientName');
    const phoneEl = document.getElementById('newClientPhone');
    // Если запрос похож на телефон — кладём его в поле телефона, иначе в имя
    if (/^[\d\s()+-]{4,}$/.test(prefillName || '')) {
        phoneEl.value = prefillName;
        nameEl.value = '';
        nameEl.focus();
    } else {
        nameEl.value = prefillName || '';
        phoneEl.value = '';
        phoneEl.focus();
    }
    if (clientAc) clientAc.close();
}

function cancelCreateClient() {
    document.getElementById('clientCreateBox').hidden = true;
    document.getElementById('newClientName').value = '';
    document.getElementById('newClientPhone').value = '';
}

async function confirmCreateClient() {
    const name  = document.getElementById('newClientName').value.trim();
    const phone = document.getElementById('newClientPhone').value.trim();

    if (!name)  { showNotification('Введите имя клиента', 'warning'); document.getElementById('newClientName').focus(); return; }
    if (!phone) { showNotification('Введите телефон клиента', 'warning'); document.getElementById('newClientPhone').focus(); return; }

    const created = await ClientAPI.create({ name, phone, isActive: true });

    if (created && created.id) {
        showNotification('Клиент создан', 'success');
        cancelCreateClient();
        selectClient(created);
        return;
    }

    // Дубль по телефону — сервер вернул существующую карточку.
    if (created && created.duplicate && created.existing) {
        showNotification('Клиент с этим телефоном уже есть — выбран существующий', 'info');
        cancelCreateClient();
        selectClient(created.existing);
        return;
    }

    showNotification('Не удалось создать клиента', 'error');
}

// ============================================================
//  СОСТАВ ЗАКАЗА (поиск услуг + строки)
// ============================================================

function serviceItemsForQuery(query) {
    const q = (query || '').toLowerCase().trim();
    const matched = allServices
        .filter(s => !q
            || (s.name || '').toLowerCase().includes(q)
            || (s.category || '').toLowerCase().includes(q))
        .sort((a, b) => (a.category || 'Прочее').localeCompare(b.category || 'Прочее', 'ru')
            || (a.name || '').localeCompare(b.name || '', 'ru'));

    return matched.slice(0, 40).map(s => ({
        id: s.id,
        label: s.name,
        sublabel: formatCurrency(s.basePrice),
        group: s.category || 'Прочее',
        data: s
    }));
}

function setupServiceAutocomplete() {
    const input = document.getElementById('serviceSearch');
    const menu  = document.getElementById('serviceComboMenu');
    if (!input || !menu || !window.Autocomplete) return;

    serviceAc = Autocomplete.attach(input, {
        menu,
        minChars: 0,
        emptyText: 'Нет такой услуги — впишите название и нажмите Enter',
        getItems: (query) => serviceItemsForQuery(query),
        footer: (query) => {
            const q = query.trim();
            if (!q) return [];
            const exact = allServices.some(s => (s.name || '').toLowerCase() === q.toLowerCase());
            return exact ? [] : [{ id: '__oneoff__', label: `➕ Разовая услуга «${q}»`, kind: 'action', data: q }];
        },
        onSelect: (item) => {
            if (item.id === '__oneoff__') {
                addOneOffLine(item.data);
            } else {
                addCatalogLine(item.data);
            }
            input.value = '';
            serviceAc.close();
            input.focus();
        }
    });
}

function addCatalogLine(service) {
    const existing = orderLines.find(l => l.serviceId === service.id);
    if (existing) {
        existing.quantity += 1;
    } else {
        orderLines.push({
            tempId: ++lineTempSeq,
            serviceId: service.id,
            name: service.name,
            unitPrice: Number(service.basePrice) || 0,
            quantity: 1
        });
    }
    renderOrderLines();
}

function addOneOffLine(name) {
    orderLines.push({
        tempId: ++lineTempSeq,
        serviceId: null,
        name: name,
        unitPrice: 0,
        quantity: 1
    });
    renderOrderLines();
    // Фокус на цену только что добавленной строки
    const last = document.querySelector('#orderLines .order-line:last-child input[data-field="price"]');
    if (last) { last.focus(); last.select(); }
}

function renderOrderLines() {
    const box = document.getElementById('orderLines');
    if (!box) return;

    if (orderLines.length === 0) {
        box.innerHTML = '<div class="order-lines-empty">Позиции не добавлены. Найдите услугу выше или впишите свою.</div>';
        updateOrderTotal();
        return;
    }

    box.innerHTML = orderLines.map(line => `
        <div class="order-line" data-temp="${line.tempId}">
            <span class="order-line-name" title="${escHtml(line.name)}">
                ${line.serviceId ? '' : '<span class="order-line-oneoff">разовая</span>'}${escHtml(line.name)}
            </span>
            <input type="number" data-field="price" min="0" step="50" value="${line.unitPrice}" aria-label="Цена">
            <span class="order-line-qty">
                <input type="number" data-field="qty" min="1" step="1" value="${line.quantity}" aria-label="Количество">
            </span>
            <button type="button" class="order-line-del" title="Убрать" onclick="removeOrderLine(${line.tempId})">🗑</button>
        </div>
    `).join('');

    box.querySelectorAll('.order-line').forEach(row => {
        const tempId = Number(row.dataset.temp);
        const line = orderLines.find(l => l.tempId === tempId);
        if (!line) return;
        row.querySelector('[data-field="price"]').addEventListener('input', e => {
            line.unitPrice = Math.max(0, Number(e.target.value) || 0);
            updateOrderTotal();
        });
        row.querySelector('[data-field="qty"]').addEventListener('input', e => {
            line.quantity = Math.max(1, Math.floor(Number(e.target.value) || 1));
            updateOrderTotal();
        });
    });

    updateOrderTotal();
}

function removeOrderLine(tempId) {
    orderLines = orderLines.filter(l => l.tempId !== tempId);
    renderOrderLines();
}

function computeOrderTotal() {
    return orderLines.reduce((sum, l) => sum + (Number(l.unitPrice) || 0) * (Number(l.quantity) || 1), 0);
}

function updateOrderTotal() {
    const el = document.getElementById('orderTotalPrice');
    if (el) el.textContent = formatCurrency(computeOrderTotal());
}

/** Собрать состав заказа для отправки; при необходимости — записать новые позиции в справочник. */
async function buildLinesPayload() {
    const saveToCatalog = document.getElementById('saveNewToCatalog')?.checked;
    const payload = [];

    for (const line of orderLines) {
        let serviceId = line.serviceId;

        if (!serviceId && saveToCatalog && line.name.trim()) {
            const created = await ServiceAPI.create({
                name: line.name.trim(),
                basePrice: Number(line.unitPrice) || 0,
                category: 'Прочее',
                description: null,
                isActive: true
            });
            if (created && created.id) {
                serviceId = created.id;
                allServices.push(created);
            }
        }

        payload.push({
            service: serviceId ? { id: serviceId } : null,
            name: line.name.trim() || 'Позиция',
            unitPrice: Number(line.unitPrice) || 0,
            quantity: Math.max(1, Math.floor(Number(line.quantity) || 1))
        });
    }

    return payload;
}

// ===== ПРОСМОТР ЗАКАЗА (МОДАЛ С ВОЗМОЖНОСТЬЮ МЕНЯТЬ СТАТУС) =====

async function viewOrder(id) {
    const order = await OrderAPI.getById(id);
    if (!order) { showNotification('Заказ не найден', 'error'); return; }

    currentViewOrderId = id;
    currentViewOrder   = order;

    togglePrintReceiptBtn(order.status === 'COMPLETED');

    document.getElementById('vOrderNumber').textContent  = order.orderNumber;
    document.getElementById('vOrderClient').textContent  = order.client
        ? `${order.client.name} ${order.client.phone ? '· ' + order.client.phone : ''}`
        : '—';
    document.getElementById('vOrderDevice').textContent  = order.deviceDescription || '—';
    document.getElementById('vOrderNotes').textContent   = order.notes || '—';
    document.getElementById('vOrderTotal').textContent   = formatCurrency(order.totalPrice);
    document.getElementById('vOrderPaid').textContent    = formatCurrency(order.paidAmount);

    renderViewOrderLines(order);

    const balance = (order.totalPrice || 0) - (order.paidAmount || 0);
    const balEl = document.getElementById('vOrderBalance');
    balEl.textContent = formatCurrency(balance);
    balEl.style.color = balance > 0 ? 'var(--c-danger)' : 'var(--c-success)';

    document.getElementById('vOrderCreated').textContent = formatDate(order.createdAt);

    renderOrderMedia(order);
    const viewAttachmentInput = document.getElementById('vAttachmentFiles');
    if (viewAttachmentInput) viewAttachmentInput.value = '';
    clearPendingFiles('vAttachmentFiles');
    renderSelectedFileList('vAttachmentFiles', 'vAttachmentFilesList');
    resetUploadProgress('view');

    const statusSelect = document.getElementById('vOrderStatus');
    statusSelect.value = order.status || 'NEW';
    document.getElementById('vPaymentAmount').value = '';
    const vsearch = document.getElementById('vServiceSearch');
    if (vsearch) vsearch.value = '';

    document.getElementById('viewOrderModal').style.display = 'block';
}

function renderViewOrderLines(order) {
    const box = document.getElementById('vOrderLines');
    if (!box) return;
    const lines = Array.isArray(order.lines) ? order.lines : [];

    if (lines.length === 0) {
        box.innerHTML = '<div class="order-lines-empty">Пока без услуг</div>';
        return;
    }

    box.innerHTML = lines.map(l => `
        <div class="order-line" style="grid-template-columns:1fr auto 40px;">
            <span class="order-line-name" title="${escHtml(l.name)}">
                ${l.serviceId ? '' : '<span class="order-line-oneoff">разовая</span>'}${escHtml(l.name)}
            </span>
            <span style="font-size:13px;color:var(--c-muted);white-space:nowrap;">
                ${l.quantity > 1 ? l.quantity + ' × ' : ''}${formatCurrency(l.unitPrice)} = <strong style="color:var(--c-text);">${formatCurrency(l.lineTotal)}</strong>
            </span>
            <button type="button" class="order-line-del" title="Убрать из заказа" onclick="deleteLineFromOpenOrder(${l.id})">🗑</button>
        </div>
    `).join('');
}

function setupViewServiceAutocomplete() {
    const input = document.getElementById('vServiceSearch');
    const menu  = document.getElementById('vServiceComboMenu');
    if (!input || !menu || !window.Autocomplete) return;

    viewServiceAc = Autocomplete.attach(input, {
        menu,
        minChars: 0,
        emptyText: 'Нет такой услуги — впишите название и нажмите Enter',
        getItems: (query) => serviceItemsForQuery(query),
        footer: (query) => {
            const q = query.trim();
            if (!q) return [];
            const exact = allServices.some(s => (s.name || '').toLowerCase() === q.toLowerCase());
            return exact ? [] : [{ id: '__oneoff__', label: `➕ Разовая услуга «${q}»`, kind: 'action', data: q }];
        },
        onSelect: async (item) => {
            input.value = '';
            viewServiceAc.close();
            if (item.id === '__oneoff__') {
                await addLineToOpenOrder({ service: null, name: item.data, unitPrice: 0, quantity: 1 });
            } else {
                await addLineToOpenOrder({
                    service: { id: item.data.id },
                    name: item.data.name,
                    unitPrice: Number(item.data.basePrice) || 0,
                    quantity: 1
                });
            }
        }
    });
}

async function addLineToOpenOrder(linePayload) {
    if (!currentViewOrderId) return;
    const updated = await OrderAPI.addLine(currentViewOrderId, linePayload);
    if (updated && updated.id) {
        currentViewOrder = updated;
        afterOpenOrderLinesChanged(updated);
        showNotification('Услуга добавлена в заказ', 'success');
    } else {
        showNotification('Не удалось добавить услугу', 'error');
    }
}

async function deleteLineFromOpenOrder(lineId) {
    if (!currentViewOrderId || !lineId) return;
    const updated = await OrderAPI.deleteLine(currentViewOrderId, lineId);
    if (updated && updated.id) {
        currentViewOrder = updated;
        afterOpenOrderLinesChanged(updated);
        showNotification('Позиция убрана', 'success');
    } else {
        showNotification('Не удалось убрать позицию', 'error');
    }
}

function afterOpenOrderLinesChanged(order) {
    renderViewOrderLines(order);
    document.getElementById('vOrderTotal').textContent = formatCurrency(order.totalPrice);
    const balance = (order.totalPrice || 0) - (order.paidAmount || 0);
    const balEl = document.getElementById('vOrderBalance');
    balEl.textContent = formatCurrency(balance);
    balEl.style.color = balance > 0 ? 'var(--c-danger)' : 'var(--c-success)';

    const idx = allOrders.findIndex(o => o.id === order.id);
    if (idx !== -1) {
        allOrders[idx].totalPrice = order.totalPrice;
        allOrders[idx].lines = order.lines;
    }
    renderOrdersTable(applyCurrentFilter());
}

function closeViewOrderModal() {
    document.getElementById('viewOrderModal').style.display = 'none';
    clearPendingFiles('vAttachmentFiles');
    renderSelectedFileList('vAttachmentFiles', 'vAttachmentFilesList');
    resetUploadProgress('view');
    currentViewOrderId = null;
    currentViewOrder   = null;
}

// ===== СОХРАНИТЬ СТАТУС =====

async function saveOrderStatus() {
    if (!currentViewOrderId) return;

    const newStatus = document.getElementById('vOrderStatus').value;
    try {
        const result = await OrderAPI.updateStatus(currentViewOrderId, newStatus);
        if (result && result.id) {
            showNotification(`Статус изменён: ${STATUS_LABELS[newStatus] || newStatus}`, 'success');
            togglePrintReceiptBtn(newStatus === 'COMPLETED');
            if (currentViewOrder) currentViewOrder.status = newStatus;
            const idx = allOrders.findIndex(o => o.id === currentViewOrderId);
            if (idx !== -1) { allOrders[idx].status = newStatus; }
            renderOrdersTable(applyCurrentFilter());
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
            document.getElementById('vOrderPaid').textContent = formatCurrency(result.paidAmount);
            const balance = (result.totalPrice || 0) - (result.paidAmount || 0);
            const balEl = document.getElementById('vOrderBalance');
            balEl.textContent = formatCurrency(balance);
            balEl.style.color = balance > 0 ? 'var(--c-danger)' : 'var(--c-success)';
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

// ===== СОЗДАНИЕ / РЕДАКТИРОВАНИЕ ЗАКАЗА =====

function resetOrderForm() {
    const form = document.getElementById('createOrderForm');
    form.reset();
    delete form.dataset.editingId;

    document.getElementById('clientId').value = '';
    document.getElementById('clientChosen').hidden = true;
    document.getElementById('clientSearch').hidden = false;
    document.getElementById('clientSearch').value = '';
    cancelCreateClient();

    orderLines = [];
    renderOrderLines();
    const saveToggle = document.getElementById('saveNewToCatalog');
    if (saveToggle) saveToggle.checked = false;

    const attachmentInput = document.getElementById('createAttachmentFiles');
    if (attachmentInput) attachmentInput.value = '';
    clearPendingFiles('createAttachmentFiles');
    renderSelectedFileList('createAttachmentFiles', 'createAttachmentFilesList');
    resetUploadProgress('create');
}

function openCreateOrderForm() {
    resetOrderForm();
    document.getElementById('orderModalTitle').textContent = '➕ Новый заказ';
    document.getElementById('createOrderModal').style.display = 'block';
    setTimeout(() => document.getElementById('clientSearch')?.focus(), 50);
}

function closeCreateOrderForm() {
    document.getElementById('createOrderModal').style.display = 'none';
    clearPendingFiles('createAttachmentFiles');
    renderSelectedFileList('createAttachmentFiles', 'createAttachmentFilesList');
    resetUploadProgress('create');
    cancelCreateClient();
}

async function submitOrder(event) {
    event.preventDefault();

    const form = document.getElementById('createOrderForm');
    const editingId = form.dataset.editingId;

    const clientId = document.getElementById('clientId').value;
    if (!clientId) { showNotification('Выберите или создайте клиента', 'warning'); return; }

    const deviceDescription = document.getElementById('deviceDescription').value.trim();
    if (!deviceDescription) { showNotification('Опишите устройство и проблему', 'warning'); return; }

    const notes = document.getElementById('orderNotes').value.trim();
    const lines = await buildLinesPayload();

    const basePayload = {
        client: { id: clientId },
        deviceDescription,
        notes: notes || null,
        lines
    };

    try {
        if (editingId) {
            const result = await OrderAPI.update(editingId, basePayload);
            if (result && result.id) {
                await uploadOrderFiles(editingId, 'createAttachmentFiles');
                showNotification('Заказ обновлён!', 'success');
                closeCreateOrderForm();
                loadOrders();
            } else {
                showNotification('Ошибка при обновлении заказа', 'error');
            }
        } else {
            const result = await OrderAPI.create({ ...basePayload, status: 'NEW', paidAmount: 0 });
            if (result && result.id) {
                await uploadOrderFiles(result.id, 'createAttachmentFiles');
                showNotification('Заказ создан успешно!', 'success');
                closeCreateOrderForm();
                loadOrders();
            } else {
                showNotification('Ошибка при создании заказа', 'error');
            }
        }
    } catch (error) {
        console.error('Error saving order:', error);
        showNotification('Ошибка при сохранении заказа', 'error');
    }
}

async function editOrder(id) {
    const order = await OrderAPI.getById(id);
    if (!order) { showNotification('Заказ не найден', 'error'); return; }

    resetOrderForm();
    document.getElementById('createOrderForm').dataset.editingId = String(id);
    document.getElementById('orderModalTitle').textContent = `✏️ Редактирование заказа ${order.orderNumber}`;
    document.getElementById('createOrderModal').style.display = 'block';

    if (order.client) selectClient(order.client);
    document.getElementById('deviceDescription').value = order.deviceDescription || '';
    document.getElementById('orderNotes').value = order.notes || '';

    orderLines = (order.lines || []).map(l => ({
        tempId: ++lineTempSeq,
        serviceId: l.serviceId || null,
        name: l.name,
        unitPrice: Number(l.unitPrice) || 0,
        quantity: Math.max(1, Number(l.quantity) || 1)
    }));
    renderOrderLines();
}

// ===== ПОИСК И ФИЛЬТР =====

function searchOrders() {
    renderOrdersTable(applyCurrentFilter());
}

function filterOrders() {
    renderOrdersTable(applyCurrentFilter());
}

// ===== ПРЕДЗАПОЛНЕНИЕ ИЗ КАЛЬКУЛЯТОРА =====

function applyCalculatorPreSelection() {
    const params = new URLSearchParams(window.location.search);
    if (!params.get('fromCalculator')) return;

    const raw = sessionStorage.getItem('calculatorData');
    if (!raw) return;

    let calcData;
    try { calcData = JSON.parse(raw); } catch { return; }
    sessionStorage.removeItem('calculatorData');

    openCreateOrderForm();

    const ids = calcData.serviceIds || [];
    ids.forEach(id => {
        const svc = allServices.find(s => Number(s.id) === Number(id));
        if (svc) addCatalogLine(svc);
    });

    if (calcData.extras && calcData.extras.length) {
        const notesEl = document.getElementById('orderNotes');
        if (notesEl) notesEl.value = `[Калькулятор] ${calcData.extras.join(', ')}`;
    }

    showNotification(
        `Из калькулятора: ${ids.length} услуг · Итого ~${formatCurrency(calcData.totalPrice || 0)}`,
        'info'
    );
}

function initAttachmentInputAndDropzone(inputId, listId, dropzoneId) {
    const input = document.getElementById(inputId);
    const dropzone = document.getElementById(dropzoneId);
    if (!input) return;

    input.addEventListener('change', () => {
        setPendingFiles(inputId, Array.from(input.files || []));
        renderSelectedFileList(inputId, listId);
    });

    if (!dropzone) return;

    dropzone.addEventListener('click', () => input.click());
    dropzone.addEventListener('keydown', (event) => {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            input.click();
        }
    });

    ['dragenter', 'dragover'].forEach(evt => {
        dropzone.addEventListener(evt, (event) => {
            event.preventDefault();
            event.stopPropagation();
            dropzone.classList.add('is-dragover');
        });
    });

    ['dragleave', 'drop'].forEach(evt => {
        dropzone.addEventListener(evt, (event) => {
            event.preventDefault();
            event.stopPropagation();
            dropzone.classList.remove('is-dragover');
        });
    });

    dropzone.addEventListener('drop', (event) => {
        const files = Array.from(event.dataTransfer?.files || []);
        setPendingFiles(inputId, files);
        renderSelectedFileList(inputId, listId);
    });
}

function setPendingFiles(inputId, files) {
    pendingAttachmentFiles[inputId] = Array.from(files || []);
}

function clearPendingFiles(inputId) {
    pendingAttachmentFiles[inputId] = [];
}

function getFilesForInput(inputId) {
    const pending = pendingAttachmentFiles[inputId] || [];
    if (pending.length) return pending;

    const input = document.getElementById(inputId);
    return Array.from((input && input.files) || []);
}

function renderSelectedFileList(inputId, listId) {
    const list = document.getElementById(listId);
    if (!list) return;

    const files = getFilesForInput(inputId);
    if (!files.length) {
        list.textContent = '';
        return;
    }

    list.textContent = `Выбрано файлов: ${files.length} (${files.map(f => f.name).join(', ')})`;
}

function getDocIcon(url) {
    const ext = getFileExtension(url || '');
    if (ext === '.pdf')                        return '📄';
    if (ext === '.doc' || ext === '.docx')     return '📝';
    if (ext === '.xls' || ext === '.xlsx')     return '📊';
    if (ext === '.csv')                        return '📋';
    if (ext === '.txt' || ext === '.rtf')      return '📃';
    return '📎';
}

function getUrlFileName(url) {
    if (!url) return 'файл';
    const parts = url.split('/');
    const raw = parts[parts.length - 1] || 'файл';
    const match = raw.match(/_[0-9a-f-]+(\.[a-z0-9]+)$/i);
    return match ? 'Документ' + match[1] : raw;
}

function renderOrderMedia(order) {
    const container = document.getElementById('vOrderMedia');
    if (!container) return;

    const photos = Array.isArray(order.photoUrls) ? order.photoUrls : [];
    const videos = Array.isArray(order.videoUrls) ? order.videoUrls : [];
    const files  = Array.isArray(order.fileUrls)  ? order.fileUrls  : [];

    if (!photos.length && !videos.length && !files.length) {
        container.innerHTML = '<div class="order-media-empty">Нет вложений</div>';
        return;
    }

    const esc = s => (s || '').replace(/'/g, "\\'");

    const photoCards = photos.map((url, i) => `
        <div class="order-media-card">
          <div class="order-media-preview">
            <img src="${url}" alt="Фото ${i + 1}" loading="lazy" onclick="openMediaLightbox('image','${esc(url)}')">
            <div class="order-media-actions">
              <button class="order-media-btn" type="button" onclick="openMediaLightbox('image','${esc(url)}')">👁️ Открыть</button>
              <button class="order-media-btn danger" type="button" onclick="deleteOrderAttachment('${esc(url)}')">🗑️ Удалить</button>
            </div>
          </div>
          <a class="order-media-link" href="#" onclick="openMediaLightbox('image','${esc(url)}');return false;">Фото ${i + 1}</a>
        </div>`).join('');

    const videoCards = videos.map((url, i) => `
        <div class="order-media-card">
          <div class="order-media-preview">
            <video src="${url}" controls preload="metadata"></video>
            <div class="order-media-actions">
              <button class="order-media-btn" type="button" onclick="openMediaLightbox('video','${esc(url)}')">👁️ Открыть</button>
              <button class="order-media-btn danger" type="button" onclick="deleteOrderAttachment('${esc(url)}')">🗑️ Удалить</button>
            </div>
          </div>
          <a class="order-media-link" href="#" onclick="openMediaLightbox('video','${esc(url)}');return false;">Видео ${i + 1}</a>
        </div>`).join('');

    const fileCards = files.map(url => `
        <div class="order-media-card doc-card">
          <div class="order-media-preview">
            <div class="order-media-doc-icon">${getDocIcon(url)}</div>
            <div class="order-media-doc-name">${getUrlFileName(url)}</div>
            <div class="order-media-actions">
              <a class="order-media-btn" href="${url}" target="_blank" download>⬇️ Скачать</a>
              <button class="order-media-btn danger" type="button" onclick="deleteOrderAttachment('${esc(url)}')">🗑️ Удалить</button>
            </div>
          </div>
          <a class="order-media-link" href="${url}" target="_blank" title="${getUrlFileName(url)}">${getUrlFileName(url)}</a>
        </div>`).join('');

    container.innerHTML = photoCards + videoCards + fileCards;
}

async function uploadOrderFiles(orderId, inputId) {
    const input = document.getElementById(inputId);
    const files = getFilesForInput(inputId);
    if (!files.length) return;

    const validation = validateFilesForUpload(files);
    if (!validation.ok) {
        showNotification(validation.message, 'warning');
        return;
    }

    try {
        const scope = inputId === 'createAttachmentFiles' ? 'create' : 'view';
        await uploadAttachmentsWithProgress(orderId, validation.files, scope);
        if (input) input.value = '';
        clearPendingFiles(inputId);
        if (inputId === 'createAttachmentFiles') {
            renderSelectedFileList('createAttachmentFiles', 'createAttachmentFilesList');
        }
        if (inputId === 'vAttachmentFiles') {
            renderSelectedFileList('vAttachmentFiles', 'vAttachmentFilesList');
        }
    } catch (error) {
        showNotification(`Вложения не загружены: ${error.message || 'ошибка'}`, 'warning');
    } finally {
        const scope = inputId === 'createAttachmentFiles' ? 'create' : 'view';
        setTimeout(() => resetUploadProgress(scope), 600);
    }
}

async function uploadAttachmentsFromView() {
    if (!currentViewOrderId) {
        notify('Сначала откройте конкретный заказ через кнопку "👁 Открыть"', 'warning');
        return;
    }

    setUploadProgress('view', 0, 'Проверка файлов...');

    const input = document.getElementById('vAttachmentFiles');
    const files = getFilesForInput('vAttachmentFiles');
    if (!files.length) {
        notify('Выберите файлы для загрузки', 'warning');
        resetUploadProgress('view');
        return;
    }

    const validation = validateFilesForUpload(files);
    if (!validation.ok) {
        notify(validation.message, 'warning');
        resetUploadProgress('view');
        return;
    }

    try {
        const updatedOrder = await uploadAttachmentsWithProgress(currentViewOrderId, validation.files, 'view');
        notify('Вложения загружены', 'success');
        renderOrderMedia(updatedOrder || {});
        if (input) input.value = '';
        clearPendingFiles('vAttachmentFiles');
        renderSelectedFileList('vAttachmentFiles', 'vAttachmentFilesList');

        const idx = allOrders.findIndex(o => o.id === currentViewOrderId);
        if (idx !== -1 && updatedOrder) {
            allOrders[idx].photoUrls = updatedOrder.photoUrls || [];
            allOrders[idx].videoUrls = updatedOrder.videoUrls || [];
        }
    } catch (error) {
        console.error('Error uploading attachments from view:', error);
        notify(`Ошибка загрузки: ${error.message || 'неизвестно'}`, 'error');
    } finally {
        setTimeout(() => resetUploadProgress('view'), 600);
    }
}

function notify(message, type = 'info') {
    if (typeof showNotification === 'function') {
        showNotification(message, type);
        return;
    }
    window.alert(message);
}

function uploadAttachmentsWithProgress(orderId, files, scope) {
    return new Promise((resolve, reject) => {
        const formData = new FormData();
        files.forEach(file => formData.append('files', file));

        const xhr = new XMLHttpRequest();
        xhr.open('POST', `/api/orders/${orderId}/attachments`);

        const csrf = (typeof getCsrfToken === 'function') ? getCsrfToken() : '';
        const csrfHeader = (typeof getCsrfHeaderName === 'function') ? getCsrfHeaderName() : 'X-CSRF-TOKEN';
        if (csrf && csrfHeader) xhr.setRequestHeader(csrfHeader, csrf);

        xhr.upload.onprogress = (event) => {
            if (!event.lengthComputable) {
                setUploadProgress(scope, 0, 'Загрузка...');
                return;
            }
            const percent = Math.min(100, Math.round((event.loaded / event.total) * 100));
            setUploadProgress(scope, percent, `Загрузка: ${percent}%`);
        };

        xhr.onload = () => {
            if (xhr.status >= 200 && xhr.status < 300) {
                setUploadProgress(scope, 100, 'Готово');
                try {
                    const parsed = JSON.parse(xhr.responseText || '{}');
                    resolve(parsed);
                } catch (e) {
                    reject(new Error('Ошибка чтения ответа сервера'));
                }
                return;
            }

            const message = xhr.responseText || 'Ошибка загрузки вложений';
            reject(new Error(message));
        };

        xhr.onerror = () => reject(new Error('Сетевая ошибка при загрузке'));
        xhr.onabort = () => reject(new Error('Загрузка прервана'));

        setUploadProgress(scope, 0, 'Подготовка загрузки...');
        xhr.send(formData);
    });
}

function setUploadProgress(scope, percent, text) {
    const block = document.getElementById(`${scope}UploadProgress`);
    const bar = document.getElementById(`${scope}UploadProgressBar`);
    const label = document.getElementById(`${scope}UploadProgressText`);
    if (!block || !bar || !label) return;

    block.style.display = 'block';
    bar.style.width = `${Math.max(0, Math.min(100, percent || 0))}%`;
    label.textContent = text || `${percent || 0}%`;
}

function resetUploadProgress(scope) {
    const block = document.getElementById(`${scope}UploadProgress`);
    const bar = document.getElementById(`${scope}UploadProgressBar`);
    const label = document.getElementById(`${scope}UploadProgressText`);
    if (!block || !bar || !label) return;

    bar.style.width = '0%';
    label.textContent = '0%';
    block.style.display = 'none';
}

function validateFilesForUpload(files) {
    const normalized = Array.from(files || []);
    if (!normalized.length) {
        return { ok: false, message: 'Выберите файлы для загрузки', files: [] };
    }

    const validFiles = [];
    for (const file of normalized) {
        const ext = getFileExtension(file.name);
        const isImage    = (file.type || '').startsWith('image/');
        const isVideo    = (file.type || '').startsWith('video/');
        const isDocument = !isImage && !isVideo;

        if (isDocument && !ALLOWED_FILE_EXTENSIONS.has(ext)) {
            return {
                ok: false,
                message: `Недопустимый тип файла: ${file.name}. Разрешены: фото, видео, pdf, doc(x), xls(x), txt, csv, rtf`,
                files: []
            };
        }

        if (isImage && !ALLOWED_IMAGE_EXTENSIONS.has(ext)) {
            return { ok: false, message: `Недопустимое расширение фото: ${file.name}`, files: [] };
        }
        if (isVideo && !ALLOWED_VIDEO_EXTENSIONS.has(ext)) {
            return { ok: false, message: `Недопустимое расширение видео: ${file.name}`, files: [] };
        }

        if (isImage && file.size > MAX_IMAGE_SIZE_BYTES) {
            return { ok: false, message: `Фото слишком большое: ${file.name} (макс. 15MB)`, files: [] };
        }
        if (isVideo && file.size > MAX_VIDEO_SIZE_BYTES) {
            return { ok: false, message: `Видео слишком большое: ${file.name} (макс. 120MB)`, files: [] };
        }
        if (isDocument && file.size > MAX_FILE_SIZE_BYTES) {
            return { ok: false, message: `Файл слишком большой: ${file.name} (макс. 20MB)`, files: [] };
        }

        validFiles.push(file);
    }

    return { ok: true, message: '', files: validFiles };
}

function getFileExtension(fileName) {
    if (!fileName) return '';
    const dot = fileName.lastIndexOf('.');
    if (dot < 0 || dot === fileName.length - 1) return '';
    return fileName.substring(dot).toLowerCase();
}

async function deleteOrderAttachment(attachmentUrl) {
    if (!currentViewOrderId || !attachmentUrl) return;
    const confirmed = window.confirm('Удалить это вложение из заказа?');
    if (!confirmed) return;

    try {
        const updatedOrder = await OrderAPI.deleteAttachment(currentViewOrderId, attachmentUrl);
        showNotification('Вложение удалено', 'success');
        renderOrderMedia(updatedOrder || {});

        const idx = allOrders.findIndex(o => o.id === currentViewOrderId);
        if (idx !== -1 && updatedOrder) {
            allOrders[idx].photoUrls = updatedOrder.photoUrls || [];
            allOrders[idx].videoUrls = updatedOrder.videoUrls || [];
            allOrders[idx].fileUrls  = updatedOrder.fileUrls  || [];
        }
    } catch (error) {
        console.error('Error deleting attachment:', error);
        showNotification(`Ошибка удаления: ${error.message || 'неизвестно'}`, 'error');
    }
}

function openMediaLightbox(type, url) {
    const lightbox = document.getElementById('mediaLightbox');
    const content = document.getElementById('mediaLightboxContent');
    if (!lightbox || !content || !url) return;

    if (type === 'video') {
        content.innerHTML = `<video src="${url}" controls autoplay></video>`;
    } else {
        content.innerHTML = `<img src="${url}" alt="Превью">`;
    }

    lightbox.style.display = 'flex';
}

function closeMediaLightbox() {
    const lightbox = document.getElementById('mediaLightbox');
    const content = document.getElementById('mediaLightboxContent');
    if (!lightbox || !content) return;

    lightbox.style.display = 'none';
    content.innerHTML = '';
}

// ===== ЗАКРЫТИЕ МОДАЛОВ =====

window.onclick = function(event) {
    const viewModal   = document.getElementById('viewOrderModal');
    const receiptMod  = document.getElementById('receiptModal');
    const lightbox    = document.getElementById('mediaLightbox');
    // Форму создания/редактирования по клику на фон не закрываем — чтобы не терять введённое.
    if (event.target === viewModal)   closeViewOrderModal();
    if (event.target === receiptMod)  closeReceiptModal();
    if (event.target === lightbox)    closeMediaLightbox();
};

window.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') closeMediaLightbox();
});

// ===== ЧЕК / КВИТАНЦИЯ =====

function togglePrintReceiptBtn(show) {
    const btn = document.getElementById('btnPrintReceipt');
    if (btn) btn.style.display = show ? 'inline-flex' : 'none';
}

function openCurrentReceipt() {
    if (currentViewOrder) openReceiptModal(currentViewOrder);
}

function openReceiptModal(order) {
    if (!order) return;
    const isAdmin = (typeof IS_ADMIN !== 'undefined' && IS_ADMIN);
    const ce = isAdmin ? 'true' : 'false';

    const cfg = loadReceiptSettings();

    const hint = document.getElementById('receiptEditHint');
    if (hint) hint.style.display = isAdmin ? 'block' : 'none';

    setRField('rCompanyName',    cfg.companyName    || 'FixByte');
    setRField('rCompanySub',     cfg.companySub     || 'Сервисный центр · Ремонт техники');
    setRField('rCompanyAddress', cfg.companyAddress || '');
    setRField('rOrderNumber',    order.orderNumber  || '—');
    setRField('rOrderDate',      formatDate(order.completedAt || order.updatedAt || order.createdAt || ''));
    setRField('rClientName',     order.client?.name    || '—');
    setRField('rClientPhone',    order.client?.phone   || '');
    setRField('rClientAddress',  order.client?.address || '');
    setRField('rDevice',         order.deviceDescription || '—');
    setRField('rNotes',          order.notes || '—');
    setRField('rTotal',          formatCurrency(order.totalPrice  || 0));
    setRField('rPaid',           formatCurrency(order.paidAmount  || 0));
    setRField('rEmployee',       cfg.employee || '________________');

    const balance = (order.totalPrice || 0) - (order.paidAmount || 0);
    setRField('rBalance', formatCurrency(balance));
    const balRow = document.getElementById('rBalanceRow');
    if (balRow) balRow.className = 'receipt-total-row ' + (balance <= 0 ? 'balance-ok' : 'balance-due');

    const tbody = document.getElementById('rServicesTbody');
    if (tbody) {
        const lines = Array.isArray(order.lines) ? order.lines : [];
        tbody.innerHTML = lines.length
            ? lines.map(l => `<tr>
                <td contenteditable="${ce}">${escHtml(l.name || '—')}${l.quantity > 1 ? ' × ' + l.quantity : ''}</td>
                <td contenteditable="${ce}" style="text-align:right;">${formatCurrency(l.lineTotal != null ? l.lineTotal : (l.unitPrice || 0) * (l.quantity || 1))}</td>
              </tr>`).join('')
            : `<tr><td colspan="2" style="color:#94a3b8;text-align:center;padding:12px 0;">Услуги не указаны</td></tr>`;
    }

    document.querySelectorAll('#receiptContent [data-editable]').forEach(el => {
        el.contentEditable = ce;
    });

    initReceiptToggles(isAdmin, cfg.hiddenFields || []);

    document.getElementById('receiptModal').style.display = 'block';
}

function closeReceiptModal() {
    const m = document.getElementById('receiptModal');
    if (m) m.style.display = 'none';
}

function initReceiptToggles(isAdmin, savedHidden = []) {
    const panel = document.getElementById('receiptFieldToggles');
    if (!panel) return;
    panel.style.display = isAdmin ? 'block' : 'none';

    const hiddenSet = new Set(savedHidden);

    panel.querySelectorAll('input[data-toggle-section]').forEach(cb => {
        const sectionId = cb.dataset.toggleSection;
        const shouldHide = hiddenSet.has(sectionId);
        cb.checked = !shouldHide;
        const sec = document.getElementById(sectionId);
        if (sec) sec.style.display = shouldHide ? 'none' : '';
    });

    if (!isAdmin) return;

    panel.querySelectorAll('input[data-toggle-section]').forEach(cb => {
        const fresh = cb.cloneNode(true);
        cb.parentNode.replaceChild(fresh, cb);
        fresh.addEventListener('change', function () {
            const sec = document.getElementById(this.dataset.toggleSection);
            if (sec) sec.style.display = this.checked ? '' : 'none';
        });
    });
}

function setRField(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value;
}

function escHtml(str) {
    const d = document.createElement('div');
    d.appendChild(document.createTextNode(str == null ? '' : str));
    return d.innerHTML;
}

function printReceiptDirect() {
    const receiptEl = document.getElementById('receiptContent');
    if (!receiptEl) return;

    let receiptCss = '';
    try {
        for (const sheet of document.styleSheets) {
            try {
                for (const rule of sheet.cssRules) {
                    if (rule.cssText && /receipt/i.test(rule.selectorText || '')) {
                        receiptCss += rule.cssText + '\n';
                    }
                }
            } catch { /* cross-origin */ }
        }
    } catch { /* ignore */ }

    const win = window.open('', '_blank', 'width=820,height=1060');
    if (!win) { showNotification('Разрешите всплывающие окна для печати', 'warning'); return; }

    const orderNum = document.getElementById('rOrderNumber')?.textContent?.trim() || '';
    win.document.write(`<!DOCTYPE html>
<html lang="ru"><head><meta charset="UTF-8">
<title>Чек ${orderNum}</title>
<style>
body{margin:24px;font-family:'Segoe UI',system-ui,sans-serif;background:#fff;}
[contenteditable]{outline:none!important;background:transparent!important;box-shadow:none!important;}
${receiptCss}
</style></head><body>
${receiptEl.outerHTML}
<script>setTimeout(function(){window.print();},350);<\/script>
</body></html>`);
    win.document.close();
}

async function saveReceiptAsPdf() {
    if (!currentViewOrderId) { showNotification('Нет активного заказа', 'error'); return; }

    const btn = document.getElementById('btnSavePdf');
    const origHtml = btn ? btn.innerHTML : '';
    if (btn) { btn.disabled = true; btn.innerHTML = '⏳ Генерация PDF…'; }

    try {
        if (!window.html2canvas) throw new Error('html2canvas не загружен. Проверьте интернет-соединение и обновите страницу.');
        if (!window.jspdf?.jsPDF) throw new Error('jsPDF не загружен. Проверьте интернет-соединение и обновите страницу.');

        const receiptEl = document.getElementById('receiptContent');

        const editables = [...receiptEl.querySelectorAll('[contenteditable]')];
        editables.forEach(el => el.removeAttribute('contenteditable'));

        const canvas = await window.html2canvas(receiptEl, {
            scale: 2, useCORS: true, backgroundColor: '#ffffff', logging: false
        });

        if (typeof IS_ADMIN !== 'undefined' && IS_ADMIN) {
            editables.forEach(el => el.setAttribute('contenteditable', 'true'));
        }

        const imgData = canvas.toDataURL('image/jpeg', 0.93);
        const { jsPDF } = window.jspdf;
        const pdf = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });

        const pageW = pdf.internal.pageSize.getWidth();
        const pageH = pdf.internal.pageSize.getHeight();
        const imgH  = (canvas.height * pageW) / canvas.width;

        if (imgH <= pageH) {
            pdf.addImage(imgData, 'JPEG', 0, 0, pageW, imgH);
        } else {
            let yOffset = 0;
            while (yOffset < imgH) {
                if (yOffset > 0) pdf.addPage();
                pdf.addImage(imgData, 'JPEG', 0, -yOffset, pageW, imgH);
                yOffset += pageH;
            }
        }

        const orderNum = (document.getElementById('rOrderNumber')?.textContent?.trim() || 'order')
                            .replace(/[^a-zA-Z0-9\-_А-Яа-я]/g, '_');
        const fileName = `receipt_${orderNum}_${Date.now()}.pdf`;

        const pdfBlob = pdf.output('blob');
        const pdfFile = new File([pdfBlob], fileName, { type: 'application/pdf' });
        const formData = new FormData();
        formData.append('files', pdfFile);

        const headers = {};
        const csrf = (typeof getCsrfToken === 'function') ? getCsrfToken() : '';
        const csrfHeader = (typeof getCsrfHeaderName === 'function') ? getCsrfHeaderName() : 'X-CSRF-TOKEN';
        if (csrf && csrfHeader) headers[csrfHeader] = csrf;

        const response = await fetch(`/api/orders/${currentViewOrderId}/attachments`, {
            method: 'POST', body: formData, headers
        });

        if (!response.ok) {
            const txt = await response.text();
            throw new Error(txt || 'Ошибка прикрепления PDF к заказу');
        }

        const updatedOrder = await response.json();

        renderOrderMedia(updatedOrder);
        const idx = allOrders.findIndex(o => o.id === currentViewOrderId);
        if (idx !== -1) allOrders[idx].fileUrls = updatedOrder.fileUrls || [];

        showNotification('✅ PDF создан и прикреплён к заказу!', 'success');

    } catch (error) {
        console.error('Receipt PDF error:', error);
        showNotification(`Ошибка: ${error.message || 'не удалось создать PDF'}`, 'error');
    } finally {
        if (btn) { btn.disabled = false; btn.innerHTML = origHtml; }
    }
}
