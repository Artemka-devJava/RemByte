/**
 * FixByte CRM — карточка клиента (/clients/{id})
 * Всё про клиента на одном экране, действия управления всегда на виду.
 */

const CLIENT_ID = Number((location.pathname.match(/\/clients\/(\d+)/) || [])[1]);
const IS_ADMIN = !!document.getElementById('adminMarker');

let client = null;
let cardDirty = false;

const TYPE_LABEL = { INDIVIDUAL: 'Физлицо', COMPANY: 'Организация' };
const NOTE_ICON  = { NOTE: '📝', CALL: '📞', MESSAGE: '💬' };
const STATUS_LABEL = {
    NEW: '🆕 Новый', IN_PROGRESS: '🔧 В работе', WAITING_FOR_PARTS: '⏳ Детали',
    READY: '✅ Готов', COMPLETED: '🏁 Завершён', CANCELLED: '❌ Отменён'
};

document.addEventListener('DOMContentLoaded', init);

async function init() {
    if (!CLIENT_ID) { location.href = '/clients'; return; }

    client = await ClientAPI.getById(CLIENT_ID);
    if (!client || !client.id) {
        document.getElementById('cardLoading').textContent = 'Клиент не найден';
        return;
    }

    document.getElementById('cardLoading').hidden = true;
    document.getElementById('cardContent').hidden = false;

    fillCard();
    wireEvents();

    loadSummaryAndOrders();
    loadDevices();
    loadNotes();
}

// ===== Шапка + форма =====

function fillCard() {
    document.getElementById('cardTitle').textContent = client.name || 'Клиент';

    set('fName', client.name);
    set('fPhone', client.phone);
    set('fEmail', client.email);
    set('fAddress', client.address);
    set('fType', client.type || 'INDIVIDUAL');
    set('fSource', client.source);
    set('fChannel', client.preferredChannel || '');
    set('fTags', client.tags);
    set('fCompany', client.companyDetails);
    set('fNotes', client.notes);

    renderTypeAndTags();
    toggleCompanyField();
    updateContactButtons();

    document.getElementById('archBadge').hidden = !client.archivedAt;
    const archBtn = document.getElementById('btnArchive');
    archBtn.textContent = client.archivedAt ? '♻ Вернуть из архива' : '🗄 В архив';

    // Согласия
    document.getElementById('cPdn').checked = !!client.consentPdnAt;
    document.getElementById('cMarketing').checked = !!client.consentMarketingAt;
    document.getElementById('cPdnDate').textContent = client.consentPdnAt ? '· ' + formatDate(client.consentPdnAt) : '';
    document.getElementById('cMarketingDate').textContent = client.consentMarketingAt ? '· ' + formatDate(client.consentMarketingAt) : '';
}

function renderTypeAndTags() {
    document.getElementById('typeBadge').textContent = TYPE_LABEL[client.type] || 'Физлицо';
    const chips = (client.tags || '').split(',').map(s => s.trim()).filter(Boolean);
    document.getElementById('tagChips').innerHTML =
        chips.map(t => `<span class="tag-chip">${escHtml(t)}</span>`).join('');
}

function toggleCompanyField() {
    document.getElementById('companyWrap').hidden = document.getElementById('fType').value !== 'COMPANY';
}

function updateContactButtons() {
    const phone = (client.phone || '').replace(/[^\d+]/g, '');
    document.getElementById('btnCall').href = phone ? `tel:${phone}` : '#';

    const digits = phone.replace(/\D/g, '');
    const write = document.getElementById('btnWrite');
    const ch = client.preferredChannel;
    if (ch === 'TELEGRAM')        { write.href = `https://t.me/+${digits}`; write.textContent = '✈ Telegram'; }
    else if (ch === 'WHATSAPP')   { write.href = `https://wa.me/${digits}`; write.textContent = '🟢 WhatsApp'; }
    else if (ch === 'EMAIL' && client.email) { write.href = `mailto:${client.email}`; write.textContent = '✉ Email'; }
    else if (client.email)        { write.href = `mailto:${client.email}`; write.textContent = '✉ Email'; }
    else                         { write.href = `https://wa.me/${digits}`; write.textContent = '✉ Написать'; }
}

function wireEvents() {
    ['fName','fPhone','fEmail','fAddress','fType','fSource','fChannel','fTags','fCompany','fNotes']
        .forEach(id => document.getElementById(id).addEventListener('input', markDirty));
    document.getElementById('fType').addEventListener('change', toggleCompanyField);

    document.getElementById('btnSaveCard').addEventListener('click', saveCard);
    document.getElementById('btnNewOrder').addEventListener('click', goNewOrder);
    document.getElementById('btnNewOrder2').addEventListener('click', goNewOrder);
    document.getElementById('btnArchive').addEventListener('click', toggleArchive);
    document.getElementById('btnDelete').addEventListener('click', removeClient);

    document.getElementById('btnAddDevice').addEventListener('click', addDevice);
    document.getElementById('btnAddNote').addEventListener('click', addNote);
    document.getElementById('nText').addEventListener('keydown', e => { if (e.key === 'Enter') addNote(); });
    document.getElementById('btnSaveConsent').addEventListener('click', saveConsent);
}

function markDirty() {
    cardDirty = true;
    document.getElementById('btnSaveCard').disabled = false;
    document.getElementById('saveHint').textContent = 'есть несохранённые изменения';
}

async function saveCard() {
    const payload = {
        ...client,
        name: val('fName').trim(),
        phone: val('fPhone').trim(),
        email: val('fEmail').trim() || null,
        address: val('fAddress').trim() || null,
        type: val('fType'),
        source: val('fSource').trim() || null,
        preferredChannel: val('fChannel') || null,
        tags: val('fTags').trim() || null,
        companyDetails: val('fCompany').trim() || null,
        notes: val('fNotes').trim() || null
    };
    if (!payload.name || !payload.phone) { showNotification('Имя и телефон обязательны', 'warning'); return; }

    const res = await ClientAPI.update(CLIENT_ID, payload);
    if (res && res.duplicate) {
        showNotification(`Телефон занят карточкой: ${res.existing?.name || '—'}`, 'warning');
        return;
    }
    if (!res || !res.id) { showNotification('Не удалось сохранить', 'error'); return; }

    client = res;
    cardDirty = false;
    document.getElementById('btnSaveCard').disabled = true;
    document.getElementById('saveHint').textContent = 'сохранено';
    fillCard();
    showNotification('Карточка сохранена', 'success');
}

function goNewOrder() {
    if (cardDirty && !confirm('Есть несохранённые изменения карточки. Перейти к заказу?')) return;
    location.href = `/orders?new=1&clientId=${CLIENT_ID}`;
}

async function toggleArchive() {
    const toArchive = !client.archivedAt;
    if (toArchive && !confirm('Убрать клиента в архив? Он исчезнет из выбора при создании заказа, история сохранится.')) return;
    const res = toArchive ? await ClientAPI.archive(CLIENT_ID) : await ClientAPI.restore(CLIENT_ID);
    if (!res || !res.id) { showNotification('Не удалось', 'error'); return; }
    client = res;
    fillCard();
    showNotification(toArchive ? 'В архиве' : 'Возвращён из архива', 'success');
}

async function removeClient() {
    if (!confirm('Удалить клиента безвозвратно? Лучше используйте архив.')) return;
    const ok = await ClientAPI.delete(CLIENT_ID);
    if (ok) location.href = '/clients';
    else showNotification('Не удалось удалить (возможно, есть заказы)', 'error');
}

// ===== Сводка + заказы =====

async function loadSummaryAndOrders() {
    const [summary, orders] = await Promise.all([
        ClientAPI.getSummary(CLIENT_ID),
        OrderAPI.getByClient(CLIENT_ID)
    ]);

    if (summary) {
        document.getElementById('fsOrders').textContent = summary.ordersCount ?? 0;
        document.getElementById('fsRevenue').textContent = formatCurrency(summary.revenue || 0);
        const debtEl = document.getElementById('fsDebt');
        debtEl.textContent = formatCurrency(summary.debt || 0);
        debtEl.classList.toggle('has-debt', (summary.debt || 0) > 0);
    }

    const tbody = document.getElementById('ordersTable');
    document.getElementById('ordersCount').textContent = orders.length ? `(${orders.length})` : '';
    if (!orders.length) {
        tbody.innerHTML = '<tr><td colspan="6" class="empty">Заказов пока нет</td></tr>';
        return;
    }
    tbody.innerHTML = orders
        .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
        .map(o => {
            const bal = (o.totalPrice || 0) - (o.paidAmount || 0);
            return `<tr style="cursor:pointer;" onclick="location.href='/orders?open=${o.id}'">
                <td><strong>${escHtml(o.orderNumber)}</strong></td>
                <td>${escHtml((o.deviceDescription || '—').substring(0, 40))}</td>
                <td><span class="status-badge status-${(o.status || 'new').toLowerCase()}">${STATUS_LABEL[o.status] || o.status}</span></td>
                <td>${formatCurrency(o.totalPrice)}</td>
                <td style="color:${bal > 0 ? 'var(--c-danger)' : 'var(--c-success)'};font-weight:600;">${formatCurrency(bal)}</td>
                <td>${formatDate(o.createdAt)}</td>
            </tr>`;
        }).join('');
}

// ===== Устройства =====

async function loadDevices() {
    const list = await ClientAPI.getDevices(CLIENT_ID);
    document.getElementById('devCount').textContent = list.length ? `(${list.length})` : '';
    const box = document.getElementById('deviceList');
    box.innerHTML = list.length ? list.map(d => `
        <div class="stack-row">
            <div>${d.kind ? `<strong>${escHtml(d.kind)}</strong> · ` : ''}${escHtml(d.model)}${d.serialNumber ? ` <span class="hint">S/N ${escHtml(d.serialNumber)}</span>` : ''}
              ${d.notes ? `<div class="hint">${escHtml(d.notes)}</div>` : ''}</div>
            <button class="icon-btn" title="Удалить" onclick="deleteDevice(${d.id})">🗑</button>
        </div>`).join('') : '<div class="empty">Устройства не заведены</div>';
}

async function addDevice() {
    const model = val('dModel').trim();
    if (!model) { showNotification('Укажите модель', 'warning'); return; }
    const res = await ClientAPI.addDevice(CLIENT_ID, {
        kind: val('dKind').trim() || null, model, serialNumber: val('dSerial').trim() || null
    });
    if (!res) { showNotification('Не удалось добавить', 'error'); return; }
    set('dKind', ''); set('dModel', ''); set('dSerial', '');
    loadDevices();
}

async function deleteDevice(id) {
    if (!confirm('Удалить устройство?')) return;
    await ClientAPI.deleteDevice(CLIENT_ID, id);
    loadDevices();
}

// ===== Журнал =====

async function loadNotes() {
    const list = await ClientAPI.getNotes(CLIENT_ID);
    document.getElementById('noteCount').textContent = list.length ? `(${list.length})` : '';
    const box = document.getElementById('noteList');
    box.innerHTML = list.length ? list.map(n => `
        <div class="tl-item">
            <span class="tl-icon">${NOTE_ICON[n.kind] || '📝'}</span>
            <div class="tl-body">
                <div>${escHtml(n.text)}</div>
                <div class="hint">${formatDate(n.createdAt)}${n.author ? ' · ' + escHtml(n.author) : ''}</div>
            </div>
            <button class="icon-btn" title="Удалить" onclick="deleteNote(${n.id})">🗑</button>
        </div>`).join('') : '<div class="empty">Записей нет</div>';
}

async function addNote() {
    const text = val('nText').trim();
    if (!text) return;
    const res = await ClientAPI.addNote(CLIENT_ID, { kind: val('nKind'), text });
    if (!res) { showNotification('Не удалось добавить', 'error'); return; }
    set('nText', '');
    loadNotes();
}

async function deleteNote(id) {
    await ClientAPI.deleteNote(CLIENT_ID, id);
    loadNotes();
}

// ===== Согласия =====

async function saveConsent() {
    const payload = {
        ...client,
        consentPdnAt: document.getElementById('cPdn').checked ? (client.consentPdnAt || new Date().toISOString()) : null,
        consentMarketingAt: document.getElementById('cMarketing').checked ? (client.consentMarketingAt || new Date().toISOString()) : null
    };
    const res = await ClientAPI.update(CLIENT_ID, payload);
    if (!res || !res.id) { showNotification('Не удалось сохранить', 'error'); return; }
    client = res;
    fillCard();
    showNotification('Согласия обновлены', 'success');
}

// ===== helpers =====

function set(id, v) { const el = document.getElementById(id); if (el) el.value = v == null ? '' : v; }
function val(id) { const el = document.getElementById(id); return el ? el.value : ''; }
function escHtml(s) { const d = document.createElement('div'); d.appendChild(document.createTextNode(s == null ? '' : s)); return d.innerHTML; }
