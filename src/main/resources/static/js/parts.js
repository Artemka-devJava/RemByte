/**
 * Комплектующие: закупка/продажа деталей и сборных лотов.
 */

let currentView = 'items';
let selectedItemIds = new Set();
let sellContext = null; // { type: 'item'|'lot', id, title }
let allItemsCache = [];

document.addEventListener('DOMContentLoaded', () => {
    switchView('items');
    loadStats();
    loadBudget();
});

// ===== Переключение вида =====

function switchView(view) {
    currentView = view;
    document.getElementById('itemsView').style.display = view === 'items' ? '' : 'none';
    document.getElementById('lotsView').style.display = view === 'lots' ? '' : 'none';
    document.getElementById('viewTabItems').className = 'btn btn-sm ' + (view === 'items' ? 'btn-primary' : 'btn-secondary');
    document.getElementById('viewTabLots').className = 'btn btn-sm ' + (view === 'lots' ? 'btn-primary' : 'btn-secondary');
    if (view === 'items') loadItems(); else loadLots();
}

// ===== Статистика и бюджет =====

async function loadStats() {
    const period = document.getElementById('periodSelect').value;
    let from = null;
    const now = new Date();
    if (period === 'today') { from = new Date(now); from.setHours(0, 0, 0, 0); }
    else if (period === 'week') { from = new Date(now.getTime() - 7 * 86400000); }
    else if (period === 'month') { from = new Date(now.getTime() - 30 * 86400000); }

    const stats = await PartsAPI.getStats(from, null);
    if (!stats) return;
    document.getElementById('statBalance').textContent = formatCurrency(stats.currentBalance);
    document.getElementById('statProfit').textContent = formatCurrency(stats.profitSum);
    document.getElementById('statRevenue').textContent = formatCurrency(stats.revenueSum);
    document.getElementById('statPurchased').textContent = formatCurrency(stats.purchasedSum);
    document.getElementById('statInventory').textContent = formatCurrency(stats.inventoryValue);
}

async function loadBudget() {
    const budget = await PartsAPI.getBudget();
    if (budget) document.getElementById('budgetInput').value = budget.startingAmount;
}

async function saveBudget() {
    const amount = parseFloat(document.getElementById('budgetInput').value);
    if (Number.isNaN(amount)) { showNotification('Введите число', 'warning'); return; }
    await PartsAPI.setBudget(amount);
    showNotification('Стартовый бюджет сохранён', 'success');
    loadStats();
}

// ===== Детали =====

async function loadItems() {
    const status = document.getElementById('itemStatusFilter').value;
    const items = await PartsAPI.getAll(status);
    allItemsCache = items;
    selectedItemIds.clear();
    updateMakeLotButton();

    const grid = document.getElementById('itemsGrid');
    if (!items.length) { grid.innerHTML = '<p class="empty">Пока нет деталей</p>'; return; }

    grid.innerHTML = items.map(renderItemCard).join('');
}

function renderItemCard(item) {
    const canSell = item.status === 'IN_STOCK' && !item.lotId;
    const canSelect = item.status === 'IN_STOCK' && !item.lotId;
    const soldBlock = item.salePrice != null ? `
        <div class="part-price">Продано: ${formatCurrency(item.salePrice)}
            <span class="${item.profit >= 0 ? 'profit-pos' : 'profit-neg'}">(${item.profit >= 0 ? '+' : ''}${formatCurrency(item.profit)})</span>
        </div>` : '';
    const lotBadge = item.lotId ? `<span class="badge" style="background:#ede9fe;color:#6d28d9;">В лоте: ${escHtml(item.lotTitle || '')}</span>` : '';

    return `
    <div class="part-card">
        <div class="part-title">${escHtml(item.title)}</div>
        <div class="part-meta">${escHtml(item.category || '—')}${item.source ? ' · ' + escHtml(item.source) : ''}</div>
        <div class="part-price">Закупка: ${formatCurrency(item.purchasePrice)} · ${formatDate(item.purchaseDate)}</div>
        ${soldBlock}
        <div>
            <span class="status-badge status-${item.status.toLowerCase()}">${item.status === 'SOLD' ? 'Продано' : 'В наличии'}</span>
            ${lotBadge}
        </div>
        <div class="card-actions">
            ${canSelect ? `<label class="checkbox-select"><input type="checkbox" onchange="toggleSelect(${item.id}, this.checked)"> в лот</label>` : ''}
            <button class="btn btn-sm btn-secondary" onclick="openItemPhotos(${item.id}, '${escHtml(item.title)}')">📷 Фото</button>
            ${canSell ? `<button class="btn btn-sm btn-success" onclick="openSell('item', ${item.id}, '${escHtml(item.title)}')">Продано</button>` : ''}
            ${item.status !== 'SOLD' && !item.lotId ? `<button class="btn btn-sm btn-danger" onclick="deleteItem(${item.id})">Удалить</button>` : ''}
        </div>
    </div>`;
}

function toggleSelect(id, checked) {
    if (checked) selectedItemIds.add(id); else selectedItemIds.delete(id);
    updateMakeLotButton();
}

function updateMakeLotButton() {
    document.getElementById('btnMakeLot').style.display = selectedItemIds.size >= 2 ? '' : 'none';
}

async function deleteItem(id) {
    if (!confirm('Удалить деталь без возможности восстановления?')) return;
    await PartsAPI.delete(id);
    loadItems();
    loadStats();
}

// ===== Создание детали =====

function openCreateItemModal() {
    document.getElementById('createItemForm').reset();
    document.getElementById('itemPurchaseDate').value = nowLocalInputValue();
    document.getElementById('itemSource').value = 'Авито';
    openModal('createItemModal');
}

async function submitCreateItem(event) {
    event.preventDefault();
    const data = {
        title: val('itemTitle').trim(),
        category: val('itemCategory').trim(),
        source: val('itemSource').trim(),
        purchasePrice: parseFloat(val('itemPurchasePrice')),
        purchaseDate: val('itemPurchaseDate') ? toLocalDateTimeParam(val('itemPurchaseDate')) : null,
        notes: val('itemNotes').trim()
    };
    const created = await PartsAPI.create(data);
    if (!created || created.error) { showNotification('Ошибка: ' + (created ? created.error : 'сервер недоступен'), 'error'); return; }

    const photoInput = document.getElementById('itemPhotos');
    if (photoInput.files && photoInput.files.length) {
        await PartsAPI.uploadPhotos(created.id, photoInput.files);
    }

    closeModal('createItemModal');
    showNotification('Деталь добавлена', 'success');
    loadItems();
    loadStats();
}

// ===== Фото детали =====

let currentPhotosItemId = null;

async function openItemPhotos(id, title) {
    currentPhotosItemId = id;
    document.getElementById('itemPhotosTitle').textContent = 'Фото: ' + title;
    document.getElementById('itemPhotosUploadInput').value = '';
    await loadItemPhotos();
    openModal('itemPhotosModal');
}

async function loadItemPhotos() {
    const list = await PartsAPI.getPhotos(currentPhotosItemId);
    const grid = document.getElementById('itemPhotosGrid');
    grid.innerHTML = list.length ? list.map(p => `
        <figure class="photo-cell">
            <img src="${p.url}" alt="фото" loading="lazy">
            <button class="icon-btn photo-del" title="Удалить" onclick="deleteItemPhoto(${p.id})">×</button>
        </figure>`).join('') : '<div class="empty">Фото пока нет</div>';
}

async function uploadItemPhotos() {
    const input = document.getElementById('itemPhotosUploadInput');
    if (!input.files || !input.files.length) { showNotification('Выберите файлы', 'warning'); return; }
    const res = await PartsAPI.uploadPhotos(currentPhotosItemId, input.files);
    if (res && res.error) { showNotification('Ошибка: ' + res.error, 'error'); return; }
    input.value = '';
    loadItemPhotos();
    showNotification('Фото загружены', 'success');
}

async function deleteItemPhoto(photoId) {
    if (!confirm('Удалить фото?')) return;
    await PartsAPI.deletePhoto(currentPhotosItemId, photoId);
    loadItemPhotos();
}

// ===== Продажа (деталь или лот) =====

function openSell(type, id, title) {
    sellContext = { type, id, title };
    document.getElementById('sellModalTitle').textContent = 'Продажа: ' + title;
    document.getElementById('sellForm').reset();
    document.getElementById('sellDate').value = nowLocalInputValue();
    openModal('sellModal');
}

async function submitSell(event) {
    event.preventDefault();
    const price = parseFloat(val('sellPrice'));
    const date = val('sellDate');
    const res = sellContext.type === 'item'
        ? await PartsAPI.sell(sellContext.id, price, date)
        : await PartsAPI.sellLot(sellContext.id, price, date);
    if (!res || res.error) { showNotification('Ошибка: ' + (res ? res.error : 'сервер недоступен'), 'error'); return; }

    closeModal('sellModal');
    showNotification('Отмечено как проданное', 'success');
    if (sellContext.type === 'item') loadItems(); else loadLots();
    loadStats();
}

// ===== Лоты =====

async function loadLots() {
    const lots = await PartsAPI.getLots();
    const box = document.getElementById('lotsList');
    if (!lots.length) { box.innerHTML = '<p class="empty">Лотов пока нет</p>'; return; }

    box.innerHTML = lots.map(lot => {
        const soldBlock = lot.salePrice != null ? `
            <div class="part-price">Продано: ${formatCurrency(lot.salePrice)}
                <span class="${lot.profit >= 0 ? 'profit-pos' : 'profit-neg'}">(${lot.profit >= 0 ? '+' : ''}${formatCurrency(lot.profit)})</span>
            </div>` : `<div class="part-price">Себестоимость: ${formatCurrency(lot.costTotal)}</div>`;
        const itemsHtml = (lot.items || []).map(i => `<li>${escHtml(i.title)} — ${formatCurrency(i.purchasePrice)}</li>`).join('');
        const canSell = lot.status === 'IN_STOCK';

        return `
        <div class="lot-card">
            <div class="part-title">🧩 ${escHtml(lot.title)}
                <span class="status-badge status-${lot.status.toLowerCase()}">${lot.status === 'SOLD' ? 'Продано' : 'В наличии'}</span>
            </div>
            <ul class="lot-items-list">${itemsHtml}</ul>
            ${soldBlock}
            <div class="card-actions">
                ${canSell ? `<button class="btn btn-sm btn-success" onclick="openSell('lot', ${lot.id}, '${escHtml(lot.title)}')">Продано</button>` : ''}
                ${canSell ? `<button class="btn btn-sm btn-danger" onclick="disbandLot(${lot.id})">Разобрать</button>` : ''}
            </div>
        </div>`;
    }).join('');
}

async function disbandLot(id) {
    if (!confirm('Разобрать лот? Детали вернутся в самостоятельную продажу.')) return;
    const res = await PartsAPI.disbandLot(id);
    if (res && res.error) { showNotification('Ошибка: ' + res.error, 'error'); return; }
    loadLots();
}

function openCreateLotModal() {
    document.getElementById('createLotForm').reset();
    const selected = allItemsCache.filter(i => selectedItemIds.has(i.id));
    document.getElementById('lotItemsPreview').innerHTML = selected.map(i => `<li>${escHtml(i.title)} — ${formatCurrency(i.purchasePrice)}</li>`).join('');
    openModal('createLotModal');
}

async function submitCreateLot(event) {
    event.preventDefault();
    const title = val('lotTitle').trim();
    const itemIds = Array.from(selectedItemIds);
    const res = await PartsAPI.createLot(title, itemIds);
    if (!res || res.error) { showNotification('Ошибка: ' + (res ? res.error : 'сервер недоступен'), 'error'); return; }

    closeModal('createLotModal');
    showNotification('Лот создан', 'success');
    switchView('lots');
    loadStats();
}

// ===== Вспомогательное =====

function openModal(id) { document.getElementById(id).style.display = 'block'; }
function closeModal(id) { document.getElementById(id).style.display = 'none'; }

function set(id, v) { const el = document.getElementById(id); if (el) el.value = v == null ? '' : v; }
function val(id) { const el = document.getElementById(id); return el ? el.value : ''; }
function escHtml(s) { const d = document.createElement('div'); d.appendChild(document.createTextNode(s == null ? '' : s)); return d.innerHTML; }

function nowLocalInputValue() {
    const d = new Date();
    const pad = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
