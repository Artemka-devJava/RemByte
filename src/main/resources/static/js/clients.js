/**
 * JavaScript код для страницы управления клиентами
 */

let allClients = [];

// Загрузить клиентов при загрузке страницы
document.addEventListener('DOMContentLoaded', () => {
    loadClients();
});

// Загрузить клиентов
async function loadClients() {
    try {
        const filter = document.getElementById('filterActive')?.value;
        let clients;

        if (filter === 'true') {
            clients = await ClientAPI.getActive();
        } else if (filter === 'false') {
            // Получаем всех и фильтруем неактивных
            const allClientsList = await ClientAPI.getAll();
            clients = allClientsList.filter(c => !c.isActive);
        } else {
            clients = await ClientAPI.getAll();
        }

        allClients = clients;
        renderClientsTable(clients);
    } catch (error) {
        console.error('Error loading clients:', error);
    }
}

// Отобразить таблицу клиентов
function renderClientsTable(clients) {
    const tbody = document.getElementById('clientsTable');

    if (!clients || clients.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="empty">Нет клиентов</td></tr>';
        return;
    }

    tbody.innerHTML = clients.map(client => `
        <tr>
            <td><strong>${client.name}</strong></td>
            <td><a href="tel:${client.phone}">${client.phone}</a></td>
            <td>${client.email || '—'}</td>
            <td>${client.address ? client.address.substring(0, 30) + (client.address.length > 30 ? '…' : '') : '—'}</td>
            <td>
                <span class="status-badge ${client.isActive ? 'status-ready' : 'status-cancelled'}">
                    ${client.isActive ? '✓ Активный' : '✗ Неактивный'}
                </span>
            </td>
            <td>${formatDate(client.createdAt)}</td>
            <td>
                <button class="btn btn-sm btn-secondary" onclick="editClient(${client.id})">✏️ Ред.</button>
                <button class="btn btn-sm btn-danger" onclick="deleteClient(${client.id})">🗑</button>
            </td>
        </tr>
    `).join('');
}

// Открыть форму создания клиента
function openCreateClientForm() {
    document.getElementById('clientModalTitle').textContent = 'Новый клиент';
    document.getElementById('createClientModal').style.display = 'block';
    document.getElementById('createClientForm').reset();
}

// Закрыть форму создания клиента
function closeCreateClientForm() {
    document.getElementById('createClientModal').style.display = 'none';
}

// Закрыть модальное окно при клике на закрытие
window.onclick = function(event) {
    const modal = document.getElementById('createClientModal');
    if (event.target === modal) closeCreateClientForm();
};

// Отправить форму создания клиента
async function submitClient(event) {
    event.preventDefault();

    const clientData = {
        name: document.getElementById('clientName').value,
        phone: document.getElementById('clientPhone').value,
        email: document.getElementById('clientEmail').value || null,
        address: document.getElementById('clientAddress').value || null,
        notes: document.getElementById('clientNotes').value || null,
        isActive: true
    };

    try {
        const result = await ClientAPI.create(clientData);
        if (result && result.id) {
            showNotification('Клиент создан успешно!');
            closeCreateClientForm();
            loadClients();
        } else {
            showNotification('Ошибка при создании клиента', 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        showNotification('Ошибка при создании клиента', 'error');
    }
}

// Редактировать клиента
async function editClient(id) {
    const client = await ClientAPI.getById(id);
    if (!client) {
        alert('Клиент не найден');
        return;
    }

    document.getElementById('clientName').value = client.name;
    document.getElementById('clientPhone').value = client.phone;
    document.getElementById('clientEmail').value = client.email || '';
    document.getElementById('clientAddress').value = client.address || '';
    document.getElementById('clientNotes').value = client.notes || '';

    document.getElementById('clientModalTitle').textContent = `Редактировать: ${client.name}`;
    openCreateClientForm();

    // Изменить обработчик формы для редактирования
    document.getElementById('createClientForm').onsubmit = async (event) => {
        event.preventDefault();
        
        const updatedData = {
            name: document.getElementById('clientName').value,
            phone: document.getElementById('clientPhone').value,
            email: document.getElementById('clientEmail').value || null,
            address: document.getElementById('clientAddress').value || null,
            notes: document.getElementById('clientNotes').value || null,
            isActive: client.isActive
        };

        await ClientAPI.update(id, updatedData);
        showNotification('Клиент обновлен успешно!');
        closeCreateClientForm();
        loadClients();
        // Восстановить обработчик для создания
        document.getElementById('createClientForm').onsubmit = submitClient;
    };
}

// Удалить клиента
async function deleteClient(id) {
    if (confirm('Вы уверены, что хотите удалить этого клиента?')) {
        const success = await ClientAPI.delete(id);
        if (success) {
            showNotification('Клиент удален успешно!');
            loadClients();
        } else {
            showNotification('Ошибка при удалении клиента', 'error');
        }
    }
}

// Поиск клиентов
async function searchClients() {
    const searchTerm = document.getElementById('searchClient').value;
    if (searchTerm.trim() === '') {
        loadClients();
        return;
    }

    const results = await ClientAPI.search(searchTerm);
    renderClientsTable(results);
}

// Поиск при вводе
document.addEventListener('DOMContentLoaded', () => {
    const searchInput = document.getElementById('searchClient');
    if (searchInput) {
        searchInput.addEventListener('keyup', (e) => {
            if (e.key === 'Enter') {
                searchClients();
            }
        });
    }
});

