/**
 * FixByte CRM — Управление пользователями (только ADMIN)
 */

let editingUserId = null;

const ROLE_LABELS = {
    'ADMIN':    '<span class="role-admin">👑 Администратор</span>',
    'OPERATOR': '<span class="role-operator">👷 Оператор</span>'
};

document.addEventListener('DOMContentLoaded', loadUsers);

async function loadUsers() {
    try {
        const res = await fetch('/api/users');
        if (res.status === 403) {
            document.getElementById('usersTable').innerHTML =
                '<tr><td colspan="7" class="empty" style="color:var(--c-danger);">⛔ Доступ запрещён</td></tr>';
            return;
        }
        const users = await res.json();
        renderUsersTable(users);
    } catch (e) {
        console.error(e);
        showNotification('Ошибка загрузки пользователей', 'error');
    }
}

function renderUsersTable(users) {
    const tbody = document.getElementById('usersTable');
    if (!users || users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="empty">Нет пользователей</td></tr>';
        return;
    }

    tbody.innerHTML = users.map(u => `
        <tr>
            <td>${u.id}</td>
            <td><code style="font-size:13px;">${u.username}</code></td>
            <td>${u.displayName || '—'}</td>
            <td>${ROLE_LABELS[u.role] || u.role}</td>
            <td>
                <span class="status-badge ${u.enabled ? 'status-ready' : 'status-cancelled'}">
                    ${u.enabled ? '✓ Активен' : '✗ Заблокирован'}
                </span>
            </td>
            <td>${formatDate(u.createdAt)}</td>
            <td style="white-space:nowrap;">
                <button class="btn btn-sm btn-secondary" onclick="editUser(${u.id})">✏️ Изменить</button>
                <button class="btn btn-sm btn-danger" onclick="deleteUser(${u.id}, '${u.username}')">🗑</button>
            </td>
        </tr>
    `).join('');
}

// ===== ОТКРЫТЬ МОДАЛ СОЗДАНИЯ =====

function openCreateUserModal() {
    editingUserId = null;
    document.getElementById('userModalTitle').textContent = 'Новый пользователь';
    document.getElementById('userForm').reset();
    document.getElementById('uUsername').disabled = false;
    document.getElementById('uEnabledGroup').style.display = 'none';
    document.getElementById('uPasswordHint').textContent = '(обязательно при создании)';
    document.getElementById('userModal').style.display = 'block';
    setTimeout(() => document.getElementById('uUsername')?.focus(), 50);
}

// ===== РЕДАКТИРОВАТЬ =====

async function editUser(id) {
    try {
        const res = await fetch(`/api/users/${id}`);
        const user = await res.json();

        editingUserId = id;
        document.getElementById('userModalTitle').textContent = `Изменить: ${user.username}`;
        document.getElementById('uUsername').value     = user.username;
        document.getElementById('uUsername').disabled  = true; // логин не меняем
        document.getElementById('uDisplayName').value  = user.displayName || '';
        document.getElementById('uPassword').value     = '';
        document.getElementById('uRole').value         = user.role;
        document.getElementById('uEnabled').checked    = user.enabled;
        document.getElementById('uEnabledGroup').style.display = 'block';
        document.getElementById('uPasswordHint').textContent = '(оставьте пустым, чтобы не менять)';
        document.getElementById('userModal').style.display = 'block';
    } catch (e) {
        showNotification('Ошибка загрузки пользователя', 'error');
    }
}

function closeUserModal() {
    document.getElementById('userModal').style.display = 'none';
    editingUserId = null;
}

// ===== СОХРАНИТЬ =====

async function submitUser(event) {
    event.preventDefault();

    const username    = document.getElementById('uUsername').value.trim();
    const displayName = document.getElementById('uDisplayName').value.trim();
    const password    = document.getElementById('uPassword').value;
    const role        = document.getElementById('uRole').value;
    const enabled     = document.getElementById('uEnabled').checked;

    if (!editingUserId && !password) {
        showNotification('Пароль обязателен при создании пользователя', 'warning');
        return;
    }

    const payload = { username, displayName, role, enabled };
    if (password) payload.password = password;

    try {
        let res;
        if (editingUserId) {
            res = await fetch(`/api/users/${editingUserId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
        } else {
            payload.password = password;
            res = await fetch('/api/users', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
        }

        const data = await res.json();
        if (res.ok) {
            showNotification(editingUserId ? 'Пользователь обновлён!' : 'Пользователь создан!', 'success');
            closeUserModal();
            loadUsers();
        } else {
            showNotification(data.error || 'Ошибка сохранения', 'error');
        }
    } catch (e) {
        console.error(e);
        showNotification('Ошибка при сохранении', 'error');
    }
}

// ===== УДАЛИТЬ =====

async function deleteUser(id, username) {
    if (!confirm(`Удалить пользователя "${username}"?\nЭто действие необратимо.`)) return;

    try {
        const res = await fetch(`/api/users/${id}`, { method: 'DELETE' });
        if (res.status === 204) {
            showNotification(`Пользователь "${username}" удалён`, 'success');
            loadUsers();
        } else {
            showNotification('Ошибка при удалении', 'error');
        }
    } catch (e) {
        showNotification('Ошибка при удалении', 'error');
    }
}

window.onclick = e => {
    if (e.target === document.getElementById('userModal')) closeUserModal();
};

