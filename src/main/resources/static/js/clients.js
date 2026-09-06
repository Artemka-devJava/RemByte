/**
 * FixByte CRM — список клиентов. Строка → карточка клиента (/clients/{id}).
 */

const TYPE_LABEL = { INDIVIDUAL: 'Физлицо', COMPANY: 'Организация' };

document.addEventListener('DOMContentLoaded', () => {
    loadClients();
    const s = document.getElementById('searchClient');
    if (s) s.addEventListener('keyup', e => { if (e.key === 'Enter') searchClients(); });
});

async function loadClients() {
    try {
        const mode = document.getElementById('filterMode')?.value || 'active';
        const all = await ClientAPI.getAll();
        let list = Array.isArray(all) ? all : [];
        if (mode === 'active')   list = list.filter(c => !c.archivedAt);
        if (mode === 'archived') list = list.filter(c => !!c.archivedAt);
        renderClientsTable(list);
    } catch (e) {
        console.error('Error loading clients:', e);
    }
}

function renderClientsTable(clients) {
    const tbody = document.getElementById('clientsTable');
    if (!clients || !clients.length) {
        tbody.innerHTML = '<tr><td colspan="5" class="empty">Нет клиентов</td></tr>';
        return;
    }
    tbody.innerHTML = clients.map(c => {
        const tags = (c.tags || '').split(',').map(s => s.trim()).filter(Boolean)
            .map(t => `<span class="tag-chip">${escHtml(t)}</span>`).join(' ');
        return `<tr style="cursor:pointer;" onclick="location.href='/clients/${c.id}'">
            <td><strong>${escHtml(c.name)}</strong>${c.archivedAt ? ' <span class="status-badge status-cancelled">архив</span>' : ''}</td>
            <td>${escHtml(c.phone || '—')}</td>
            <td>${TYPE_LABEL[c.type] || 'Физлицо'}</td>
            <td>${tags || '—'}</td>
            <td>${formatDate(c.createdAt)}</td>
        </tr>`;
    }).join('');
}

function openCreateClientForm() {
    const form = document.getElementById('createClientForm');
    form.reset();
    document.getElementById('createClientModal').style.display = 'block';
    setTimeout(() => document.getElementById('clientName')?.focus(), 50);
}

function closeCreateClientForm() {
    document.getElementById('createClientModal').style.display = 'none';
}

window.onclick = function (event) {
    const modal = document.getElementById('createClientModal');
    if (event.target === modal) closeCreateClientForm();
};

async function submitClient(event) {
    event.preventDefault();
    const data = {
        name: document.getElementById('clientName').value.trim(),
        phone: document.getElementById('clientPhone').value.trim(),
        type: document.getElementById('clientType').value,
        email: document.getElementById('clientEmail').value.trim() || null,
        address: document.getElementById('clientAddress').value.trim() || null,
        isActive: true
    };
    if (!data.name || !data.phone) { showNotification('Заполните имя и телефон', 'warning'); return; }

    const res = await ClientAPI.create(data);

    if (res && res.duplicate) {
        const ex = res.existing;
        if (ex && ex.id && confirm(`Клиент с этим телефоном уже есть: ${ex.name}. Открыть его карточку?`)) {
            location.href = `/clients/${ex.id}`;
        }
        return;
    }
    if (res && res.id) {
        showNotification('Клиент создан', 'success');
        location.href = `/clients/${res.id}`;
        return;
    }
    showNotification('Ошибка при создании клиента', 'error');
}

async function searchClients() {
    const term = document.getElementById('searchClient').value.trim();
    if (!term) { loadClients(); return; }
    const results = await ClientAPI.search(term);
    renderClientsTable(results || []);
}

function escHtml(s) { const d = document.createElement('div'); d.appendChild(document.createTextNode(s == null ? '' : s)); return d.innerHTML; }
