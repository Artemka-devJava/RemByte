/**
 * FixByte CRM — Управление услугами с динамическими категориями
 */

let allServices    = [];
let allCategories  = [];   // [{id, name, icon}]
let editingServiceId  = null;
let editingCategoryId = null;

document.addEventListener('DOMContentLoaded', async () => {
    await loadCategories();
    await loadServices();
});

// ===== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ =====

/** Экранирует строку для вставки в HTML-атрибут onclick='...' */
function escapeAttr(str) {
    if (!str) return '';
    return str.replace(/\\/g, '\\\\').replace(/'/g, "\\'");
}

// ===== КАТЕГОРИИ =====

async function loadCategories() {
    try {
        const res = await fetch('/api/categories');
        if (!res.ok) throw new Error('Ошибка сервера');
        allCategories = await res.json();
        renderCategoryChips();
        renderCategoryFilter();
        renderCategorySelectInForm();
    } catch (e) {
        console.error('Ошибка загрузки категорий:', e);
        showNotification('Не удалось загрузить категории', 'error');
    }
}

function renderCategoryChips() {
    const el = document.getElementById('categoriesList');
    if (!el) return;   // скрыт для оператора
    if (allCategories.length === 0) {
        el.innerHTML = '<span style="color:var(--c-muted);font-size:13px;">Нет категорий — добавьте первую!</span>';
        return;
    }
    el.innerHTML = allCategories.map(c => `
        <span class="cat-chip">
            <span>${c.icon || '🔧'}</span>
            <span>${c.name}</span>
            <button class="cat-chip-edit" title="Редактировать"
                    onclick="openEditCategoryModal(${c.id}, '${escapeAttr(c.name)}', '${escapeAttr(c.icon || '🔧')}')">✏️</button>
            <button class="cat-chip-del"  title="Удалить"
                    onclick="deleteCategory(${c.id}, '${escapeAttr(c.name)}')">✕</button>
        </span>
    `).join('');
}

function renderCategoryFilter() {
    const sel = document.getElementById('categoryFilter');
    if (!sel) return;
    const cur = sel.value;
    while (sel.options.length > 1) sel.remove(1);
    allCategories.forEach(c => {
        const opt = new Option(`${c.icon || ''} ${c.name}`, c.name);
        sel.add(opt);
    });
    // Восстановить выбранное значение
    if ([...sel.options].some(o => o.value === cur)) sel.value = cur;
}

function renderCategorySelectInForm() {
    const sel = document.getElementById('serviceCategory');
    if (!sel) return;
    const cur = sel.value;
    sel.innerHTML = '';
    if (allCategories.length === 0) {
        sel.innerHTML = '<option value="">— Нет категорий —</option>';
        return;
    }
    allCategories.forEach(c => {
        const opt = new Option(`${c.icon || ''} ${c.name}`, c.name);
        sel.add(opt);
    });
    if (cur && [...sel.options].some(o => o.value === cur)) sel.value = cur;
}

// Открыть модал ДОБАВЛЕНИЯ категории
function openCategoryModal() {
    editingCategoryId = null;
    document.getElementById('categoryModalTitle').textContent = 'Новая категория';
    document.getElementById('categoryForm').reset();
    document.getElementById('categoryModal').style.display = 'block';
}

// Открыть модал РЕДАКТИРОВАНИЯ категории
function openEditCategoryModal(id, name, icon) {
    editingCategoryId = id;
    document.getElementById('categoryModalTitle').textContent = 'Редактировать категорию';
    document.getElementById('catName').value = name;
    document.getElementById('catIcon').value = icon;
    document.getElementById('categoryModal').style.display = 'block';
}

function closeCategoryModal() {
    document.getElementById('categoryModal').style.display = 'none';
    editingCategoryId = null;
}

async function submitCategory(event) {
    event.preventDefault();
    const name = document.getElementById('catName').value.trim();
    const icon = document.getElementById('catIcon').value.trim() || '🔧';

    if (!name) {
        showNotification('Укажите название категории', 'warning');
        return;
    }

    try {
        let res;
        if (editingCategoryId) {
            res = await fetch(`/api/categories/${editingCategoryId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name, icon })
            });
        } else {
            res = await fetch('/api/categories', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name, icon })
            });
        }
        if (res.ok) {
            showNotification(
                editingCategoryId ? `Категория "${name}" обновлена` : `Категория "${name}" добавлена`,
                'success'
            );
            closeCategoryModal();
            await loadCategories();
            await loadServices();   // обновить иконки в таблице
        } else {
            const d = await res.json().catch(() => ({}));
            showNotification(d.error || 'Ошибка при сохранении категории', 'error');
        }
    } catch (e) {
        showNotification('Ошибка при сохранении категории', 'error');
    }
}

async function deleteCategory(id, name) {
    if (!confirm(`Удалить категорию "${name}"?\nУслуги останутся, но категория будет снята.`)) return;
    try {
        const res = await fetch(`/api/categories/${id}`, { method: 'DELETE' });
        if (res.status === 204) {
            showNotification(`Категория "${name}" удалена`, 'success');
            await loadCategories();
            await loadServices();
        } else {
            const d = await res.json().catch(() => ({}));
            showNotification(d.error || 'Ошибка удаления категории', 'error');
        }
    } catch (e) {
        showNotification('Ошибка удаления категории', 'error');
    }
}

// ===== УСЛУГИ =====

async function loadServices() {
    try {
        const category = document.getElementById('categoryFilter')?.value || '';
        const services = category
            ? await ServiceAPI.getByCategory(category)
            : await ServiceAPI.getAll();
        allServices = services;
        renderServicesTable(services);
    } catch (error) {
        console.error('Ошибка загрузки услуг:', error);
        showNotification('Ошибка загрузки услуг', 'error');
    }
}

/** Ищет иконку категории по её названию в кеше allCategories */
function getCategoryIcon(categoryName) {
    const cat = allCategories.find(c => c.name === categoryName);
    return cat ? (cat.icon || '🔧') : '🔧';
}

function renderServicesTable(services) {
    const tbody = document.getElementById('servicesTable');
    if (!services || services.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="empty">Нет услуг</td></tr>';
        return;
    }
    tbody.innerHTML = services.map(service => `
        <tr>
            <td><strong>${service.name}</strong></td>
            <td>${service.description || '—'}</td>
            <td>
                <span class="status-badge status-new">
                    ${getCategoryIcon(service.category)} ${service.category || '—'}
                </span>
            </td>
            <td><strong>${formatCurrency(service.basePrice)}</strong></td>
            <td>
                <span class="status-badge ${service.isActive ? 'status-ready' : 'status-cancelled'}">
                    ${service.isActive ? '✓ Активна' : '✗ Неактивна'}
                </span>
            </td>
            <td style="white-space:nowrap;">
                <button class="btn btn-sm btn-secondary" onclick="editService(${service.id})">✏️</button>
                <button class="btn btn-sm btn-danger"    onclick="deleteService(${service.id}, '${escapeAttr(service.name)}')">🗑</button>
            </td>
        </tr>
    `).join('');
}

function openCreateServiceForm() {
    editingServiceId = null;
    document.getElementById('serviceModalTitle').textContent = 'Добавить услугу';
    document.getElementById('createServiceForm').reset();
    const activeChk = document.getElementById('serviceIsActive');
    if (activeChk) activeChk.checked = true;
    renderCategorySelectInForm();
    document.getElementById('createServiceModal').style.display = 'block';
}

function closeCreateServiceForm() {
    document.getElementById('createServiceModal').style.display = 'none';
    editingServiceId = null;
}

async function editService(id) {
    try {
        const res = await fetch(`/api/services/${id}`);
        if (!res.ok) throw new Error('not found');
        const s = await res.json();

        editingServiceId = id;
        document.getElementById('serviceModalTitle').textContent  = 'Редактировать услугу';
        document.getElementById('serviceName').value              = s.name;
        document.getElementById('serviceDescription').value       = s.description || '';
        document.getElementById('servicePrice').value             = s.basePrice;
        const activeChk = document.getElementById('serviceIsActive');
        if (activeChk) activeChk.checked = s.isActive !== false;

        renderCategorySelectInForm();
        document.getElementById('serviceCategory').value = s.category || '';

        document.getElementById('createServiceModal').style.display = 'block';
    } catch (e) {
        showNotification('Ошибка загрузки услуги', 'error');
    }
}

async function submitService(event) {
    event.preventDefault();

    const name        = document.getElementById('serviceName').value.trim();
    const description = document.getElementById('serviceDescription').value.trim();
    const category    = document.getElementById('serviceCategory').value;
    const basePrice   = parseFloat(document.getElementById('servicePrice').value);
    const activeChk   = document.getElementById('serviceIsActive');
    const isActive    = activeChk ? activeChk.checked : true;

    if (!name) {
        showNotification('Укажите название услуги', 'warning');
        return;
    }
    if (!category) {
        showNotification('Выберите категорию услуги', 'warning');
        return;
    }
    if (isNaN(basePrice) || basePrice < 0) {
        showNotification('Некорректная цена', 'warning');
        return;
    }

    const payload = { name, description, category, basePrice, isActive };

    try {
        let res;
        if (editingServiceId) {
            res = await fetch(`/api/services/${editingServiceId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
        } else {
            res = await fetch('/api/services', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
        }
        if (res.ok) {
            showNotification(editingServiceId ? 'Услуга обновлена' : 'Услуга добавлена', 'success');
            closeCreateServiceForm();
            await loadServices();
        } else {
            const d = await res.json().catch(() => ({}));
            showNotification(d.error || 'Ошибка сохранения услуги', 'error');
        }
    } catch (e) {
        showNotification('Ошибка сохранения', 'error');
    }
}

async function deleteService(id, name) {
    if (!confirm(`Удалить услугу "${name}"?`)) return;
    try {
        const res = await fetch(`/api/services/${id}`, { method: 'DELETE' });
        if (res.status === 204) {
            showNotification(`Услуга "${name}" удалена`, 'success');
            await loadServices();
        } else {
            showNotification('Ошибка удаления', 'error');
        }
    } catch (e) {
        showNotification('Ошибка удаления', 'error');
    }
}

window.onclick = e => {
    if (e.target === document.getElementById('createServiceModal')) closeCreateServiceForm();
    if (e.target === document.getElementById('categoryModal'))      closeCategoryModal();
};
