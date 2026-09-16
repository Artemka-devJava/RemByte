/**
 * FixByte CRM — список клиентов. Строка → карточка клиента (/clients/{id}).
 */

const TYPE_LABEL = { INDIVIDUAL: 'Физлицо', COMPANY: 'Организация' };

// Состояние серверной пагинации/фильтра/сортировки таблицы клиентов
const clientsPage = { page: 0, size: 20, totalPages: 0, totalElements: 0, mode: 'active', q: '', sort: 'name', dir: 'asc' };

document.addEventListener('DOMContentLoaded', () => {
    loadClients();
    const s = document.getElementById('searchClient');
    if (s) s.addEventListener('keyup', e => { if (e.key === 'Enter') searchClients(); });
});

async function loadClients() {
    try {
        clientsPage.mode = document.getElementById('filterMode')?.value || 'active';
        const result = await ClientAPI.getPage({
            page: clientsPage.page,
            size: clientsPage.size,
            mode: clientsPage.mode,
            q: clientsPage.q,
            sort: `${clientsPage.sort},${clientsPage.dir}`
        });
        const list = Array.isArray(result.content) ? result.content : [];
        clientsPage.totalPages = result.totalPages || 0;
        clientsPage.totalElements = result.totalElements || 0;
        renderClientsTable(list);
        renderClientsPagination(list.length);
        updateClientsSortArrows();
    } catch (e) {
        console.error('Error loading clients:', e);
        showNotification('Ошибка загрузки клиентов', 'error');
        const tbody = document.getElementById('clientsTable');
        if (tbody) tbody.innerHTML = '<tr><td colspan="5" class="empty">Не удалось загрузить список — проверьте соединение</td></tr>';
    }
}

/** Клик по заголовку столбца — сортировка на сервере (тот же клик второй раз меняет направление). */
function sortClientsBy(field) {
    if (clientsPage.sort === field) {
        clientsPage.dir = clientsPage.dir === 'asc' ? 'desc' : 'asc';
    } else {
        clientsPage.sort = field;
        clientsPage.dir = 'asc';
    }
    clientsPage.page = 0;
    loadClients();
}

function updateClientsSortArrows() {
    document.querySelectorAll('.data-table .sort-arrow').forEach(el => { el.textContent = ''; });
    const arrow = document.getElementById('sortArrow-' + clientsPage.sort);
    if (arrow) arrow.textContent = clientsPage.dir === 'asc' ? '▲' : '▼';
}

function renderClientsPagination(pageItemCount) {
    const el = document.getElementById('clientsPagination');
    if (!el) return;

    if (clientsPage.totalElements === 0) {
        el.innerHTML = '';
        return;
    }

    const from = clientsPage.page * clientsPage.size + 1;
    const to = Math.min(clientsPage.totalElements, from + pageItemCount - 1);
    const hasPrev = clientsPage.page > 0;
    const hasNext = clientsPage.page + 1 < clientsPage.totalPages;

    el.innerHTML = `
        <button class="btn btn-sm btn-secondary" ${hasPrev ? '' : 'disabled'} onclick="goToClientsPage(${clientsPage.page - 1})">← Назад</button>
        <span class="pagination-info">${from}–${to} из ${clientsPage.totalElements}</span>
        <button class="btn btn-sm btn-secondary" ${hasNext ? '' : 'disabled'} onclick="goToClientsPage(${clientsPage.page + 1})">Вперёд →</button>
    `;
}

function goToClientsPage(page) {
    if (page < 0 || page >= clientsPage.totalPages) return;
    clientsPage.page = page;
    loadClients();
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
            <td data-label="ФИО / Название"><strong>${escHtml(c.name)}</strong>${c.archivedAt ? ' <span class="status-badge status-cancelled">архив</span>' : ''}</td>
            <td data-label="Телефон">${escHtml(c.phone || '—')}</td>
            <td data-label="Тип">${TYPE_LABEL[c.type] || 'Физлицо'}</td>
            <td data-label="Метки">${tags || '—'}</td>
            <td data-label="Добавлен">${formatDate(c.createdAt)}</td>
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
        if (ex && ex.id && await confirmAction(`Клиент с этим телефоном уже есть: ${ex.name}. Открыть его карточку?`, { danger: false, confirmText: 'Открыть' })) {
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
    clientsPage.q = document.getElementById('searchClient')?.value?.trim() || '';
    clientsPage.page = 0;
    loadClients();
}

function filterClients() {
    clientsPage.page = 0;
    loadClients();
}

function escHtml(s) { const d = document.createElement('div'); d.appendChild(document.createTextNode(s == null ? '' : s)); return d.innerHTML; }
