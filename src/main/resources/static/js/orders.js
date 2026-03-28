/**
 * FixByte CRM — Управление заказами
 */

let allOrders   = [];
let allClients  = [];
let allServices = [];
let currentViewOrderId = null; // ID заказа в модале просмотра
let currentViewOrder   = null; // полный объект заказа для чека
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
    loadClientsForSelect();
    loadServicesForSelect();

    const searchInput = document.getElementById('searchOrder');
    if (searchInput) {
        searchInput.addEventListener('keyup', e => { if (e.key === 'Enter') searchOrders(); });
    }

    initAttachmentInputAndDropzone('createAttachmentFiles', 'createAttachmentFilesList', 'createAttachmentDropzone');
    initAttachmentInputAndDropzone('vAttachmentFiles', 'vAttachmentFilesList', 'vAttachmentDropzone');
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

// ===== ПРОСМОТР ЗАКАЗА (МОДАЛ С ВОЗМОЖНОСТЬЮ МЕНЯТЬ СТАТУС) =====

async function viewOrder(id) {
    const order = await OrderAPI.getById(id);
    if (!order) { showNotification('Заказ не найден', 'error'); return; }

    currentViewOrderId = id;
    currentViewOrder   = order;

    // Показать / скрыть кнопку «Напечатать чек»
    togglePrintReceiptBtn(order.status === 'COMPLETED');

    // Заполнить поля детали
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

    renderOrderMedia(order);
    const viewAttachmentInput = document.getElementById('vAttachmentFiles');
    if (viewAttachmentInput) viewAttachmentInput.value = '';
    clearPendingFiles('vAttachmentFiles');
    renderSelectedFileList('vAttachmentFiles', 'vAttachmentFilesList');
    resetUploadProgress('view');

    // Установить текущий статус в select
    const statusSelect = document.getElementById('vOrderStatus');
    statusSelect.value = order.status || 'NEW';

    // Сбросить поле оплаты
    document.getElementById('vPaymentAmount').value = '';

    document.getElementById('viewOrderModal').style.display = 'block';
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
            // Обновить currentViewOrder и объект в массиве
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
    const attachmentInput = document.getElementById('createAttachmentFiles');
    if (attachmentInput) attachmentInput.value = '';
    clearPendingFiles('createAttachmentFiles');
    renderSelectedFileList('createAttachmentFiles', 'createAttachmentFilesList');
    resetUploadProgress('create');
    document.getElementById('createOrderForm').onsubmit = submitOrder;
    updateOrderPrice();
}

function closeCreateOrderForm() {
    document.getElementById('createOrderModal').style.display = 'none';
    clearPendingFiles('createAttachmentFiles');
    renderSelectedFileList('createAttachmentFiles', 'createAttachmentFilesList');
    resetUploadProgress('create');
}

async function submitOrder(event) {
    event.preventDefault();
    const clientId = document.getElementById('clientSelect').value;
    const deviceDescription = document.getElementById('deviceDescription').value;
    const notes = document.getElementById('orderNotes').value;
    const selectedServices = Array.from(document.querySelectorAll('#servicesCheckbox input[type="checkbox"]:checked'))
        .map(cb => allServices.find(s => Number(s.id) === Number(cb.value))).filter(Boolean);

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
            await uploadOrderFiles(result.id, 'createAttachmentFiles');
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
    document.getElementById('createOrderModal').style.display = 'block';
    document.getElementById('clientSelect').value = order.client?.id || '';
    document.getElementById('deviceDescription').value = order.deviceDescription || '';
    document.getElementById('orderNotes').value = order.notes || '';
    const attachmentInput = document.getElementById('createAttachmentFiles');
    if (attachmentInput) attachmentInput.value = '';
    clearPendingFiles('createAttachmentFiles');
    renderSelectedFileList('createAttachmentFiles', 'createAttachmentFilesList');
    resetUploadProgress('create');

    // Отметить услуги
    const checkboxes = document.querySelectorAll('#servicesCheckbox input[type="checkbox"]');
    checkboxes.forEach(cb => {
        cb.checked = order.services?.some(s => s.id === parseInt(cb.value)) || false;
    });

    updateOrderPrice();

    document.getElementById('createOrderForm').onsubmit = async (event) => {
        event.preventDefault();
        const selectedServices = Array.from(document.querySelectorAll('#servicesCheckbox input[type="checkbox"]:checked'))
            .map(cb => allServices.find(s => Number(s.id) === Number(cb.value))).filter(Boolean);

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
            await uploadOrderFiles(id, 'createAttachmentFiles');
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

// ===== ПРЕДЗАПОЛНЕНИЕ ИЗ КАЛЬКУЛЯТОРА =====

function applyCalculatorPreSelection() {
    const params = new URLSearchParams(window.location.search);
    if (!params.get('fromCalculator')) return;

    const raw = sessionStorage.getItem('calculatorData');
    if (!raw) return;

    let calcData;
    try { calcData = JSON.parse(raw); } catch { return; }
    sessionStorage.removeItem('calculatorData');

    // Открыть модал создания заказа
    openCreateOrderForm();

    // Предвыбрать услуги
    const ids = calcData.serviceIds || [];
    ids.forEach(id => {
        const cb = document.querySelector(`#servicesCheckbox input[value="${id}"]`);
        if (cb) {
            cb.checked = true;
            // Открыть группу, чтобы отмеченные услуги были видны
            const group = cb.closest('.services-group');
            if (group) group.classList.remove('collapsed');
        }
    });
    updateOrderPrice();

    // Сформировать заметку
    if (calcData.extras && calcData.extras.length) {
        const notesEl = document.getElementById('orderNotes');
        if (notesEl) notesEl.value = `[Калькулятор] ${calcData.extras.join(', ')}`;
    }

    showNotification(
        `Из калькулятора: ${ids.length} услуг · Итого ~${formatCurrency(calcData.totalPrice || 0)}`,
        'info'
    );
}

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
    // убрать timestamp-UUID-prefix (NN_uuid.ext → оставить расширение)
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
    // Fallback, если общий toast-код не загрузился.
    window.alert(message);
}

function uploadAttachmentsWithProgress(orderId, files, scope) {
    return new Promise((resolve, reject) => {
        const formData = new FormData();
        files.forEach(file => formData.append('files', file));

        const xhr = new XMLHttpRequest();
        xhr.open('POST', `/api/orders/${orderId}/attachments`);

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
    const createModal = document.getElementById('createOrderModal');
    const receiptMod  = document.getElementById('receiptModal');
    const lightbox    = document.getElementById('mediaLightbox');
    if (event.target === viewModal)   closeViewOrderModal();
    if (event.target === createModal) closeCreateOrderForm();
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

    // Загрузить сохранённые настройки из localStorage
    const cfg = loadReceiptSettings();

    // Подсказка для администратора
    const hint = document.getElementById('receiptEditHint');
    if (hint) hint.style.display = isAdmin ? 'block' : 'none';

    // Заполнить поля (приоритет: настройки из admin-панели → значения по умолчанию)
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

    // Таблица услуг
    const tbody = document.getElementById('rServicesTbody');
    if (tbody) {
        const svcs = Array.isArray(order.services) ? order.services : [];
        tbody.innerHTML = svcs.length
            ? svcs.map(s => `<tr>
                <td contenteditable="${ce}">${escHtml(s.name || '—')}</td>
                <td contenteditable="${ce}" style="text-align:right;">${formatCurrency(s.basePrice || 0)}</td>
              </tr>`).join('')
            : `<tr><td colspan="2" style="color:#94a3b8;text-align:center;padding:12px 0;">Услуги не указаны</td></tr>`;
    }

    // Выставить contenteditable на все [data-editable] поля
    document.querySelectorAll('#receiptContent [data-editable]').forEach(el => {
        el.contentEditable = ce;
    });

    // Инициализировать переключатели полей с учётом сохранённых настроек
    initReceiptToggles(isAdmin, cfg.hiddenFields || []);

    document.getElementById('receiptModal').style.display = 'block';
}

function closeReceiptModal() {
    const m = document.getElementById('receiptModal');
    if (m) m.style.display = 'none';
}

/** Инициализация чипов включения/отключения полей чека */
function initReceiptToggles(isAdmin, savedHidden = []) {
    const panel = document.getElementById('receiptFieldToggles');
    if (!panel) return;
    panel.style.display = isAdmin ? 'block' : 'none';

    const hiddenSet = new Set(savedHidden);

    // Применить видимость из сохранённых настроек
    panel.querySelectorAll('input[data-toggle-section]').forEach(cb => {
        const sectionId = cb.dataset.toggleSection;
        const shouldHide = hiddenSet.has(sectionId);
        cb.checked = !shouldHide;
        const sec = document.getElementById(sectionId);
        if (sec) sec.style.display = shouldHide ? 'none' : '';
    });

    if (!isAdmin) return;

    // Переподключить обработчики (клонирование убирает старые)
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
    d.appendChild(document.createTextNode(str));
    return d.innerHTML;
}

// Открыть окно браузерной печати
function printReceiptDirect() {
    const receiptEl = document.getElementById('receiptContent');
    if (!receiptEl) return;

    // Собрать CSS .receipt-* из подключённых таблиц
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

// Генерация PDF → автоматическое прикрепление к заказу + скачивание
async function saveReceiptAsPdf() {
    if (!currentViewOrderId) { showNotification('Нет активного заказа', 'error'); return; }

    const btn = document.getElementById('btnSavePdf');
    const origHtml = btn ? btn.innerHTML : '';
    if (btn) { btn.disabled = true; btn.innerHTML = '⏳ Генерация PDF…'; }

    try {
        if (!window.html2canvas) throw new Error('html2canvas не загружен. Проверьте интернет-соединение и обновите страницу.');
        if (!window.jspdf?.jsPDF) throw new Error('jsPDF не загружен. Проверьте интернет-соединение и обновите страницу.');

        const receiptEl = document.getElementById('receiptContent');

        // Временно убрать contenteditable для чистого рендера
        const editables = [...receiptEl.querySelectorAll('[contenteditable]')];
        editables.forEach(el => el.removeAttribute('contenteditable'));

        const canvas = await window.html2canvas(receiptEl, {
            scale: 2, useCORS: true, backgroundColor: '#ffffff', logging: false
        });

        // Вернуть contenteditable для администратора
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
            // Многостраничный чек
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

        // Загрузить как вложение к заказу
        const pdfBlob = pdf.output('blob');
        const pdfFile = new File([pdfBlob], fileName, { type: 'application/pdf' });
        const formData = new FormData();
        formData.append('files', pdfFile);

        const response = await fetch(`/api/orders/${currentViewOrderId}/attachments`, {
            method: 'POST', body: formData
        });

        if (!response.ok) {
            const txt = await response.text();
            throw new Error(txt || 'Ошибка прикрепления PDF к заказу');
        }

        const updatedOrder = await response.json();

        // Обновить медиа в открытом модале просмотра
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

