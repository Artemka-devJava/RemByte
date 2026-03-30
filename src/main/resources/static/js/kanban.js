let kanbanBoard = null;
let currentBoardId = null;
let draggingCardId = null;
let pendingAttachments = [];

const boardSelectEl = () => document.getElementById('boardSelect');
const boardTitleEl = () => document.getElementById('kanbanBoardTitle');
const boardNameInputEl = () => document.getElementById('boardNameInput');
const boardNameActionsEl = () => document.getElementById('boardNameActions');

const cardModalEl = () => document.getElementById('cardModal');
const cardIdEl = () => document.getElementById('cardId');
const cardColumnIdEl = () => document.getElementById('cardColumnId');
const cardTitleEl = () => document.getElementById('cardTitle');
const cardDescriptionEl = () => document.getElementById('cardDescription');
const cardAttachmentInputEl = () => document.getElementById('cardAttachmentsInput');
const pendingAttachmentsEl = () => document.getElementById('pendingAttachments');
const currentAttachmentsEl = () => document.getElementById('currentCardAttachments');


document.addEventListener('DOMContentLoaded', () => {
    initializeKanban().catch(err => {
        console.error(err);
        showNotification('Ошибка инициализации канбана', 'error');
    });
});

async function initializeKanban() {
    await loadBoards();
}

async function loadBoards(selectBoardId) {
    const boards = await KanbanAPI.getBoards();
    if (!Array.isArray(boards) || !boards.length) {
        showNotification('Не удалось загрузить доски', 'error');
        return;
    }

    const select = boardSelectEl();
    select.innerHTML = boards
        .map(board => `<option value="${board.id}">${escapeHtml(board.name || 'Без названия')}</option>`)
        .join('');

    if (Number.isFinite(Number(selectBoardId))) {
        currentBoardId = Number(selectBoardId);
    } else if (!Number.isFinite(Number(currentBoardId)) || !boards.some(b => Number(b.id) === Number(currentBoardId))) {
        currentBoardId = Number(boards[0].id);
    }

    select.value = String(currentBoardId);
    await loadKanbanBoard();
}

async function loadKanbanBoard() {
    const board = await KanbanAPI.getBoard(currentBoardId);
    if (!board || board.error) {
        showNotification(board?.error || 'Не удалось загрузить канбан-доску', 'error');
        return;
    }

    kanbanBoard = board;
    currentBoardId = Number(board.id);
    renderKanbanBoard();
    syncBoardRenameInput();
}

function renderKanbanBoard() {
    boardTitleEl().textContent = `Доска: ${kanbanBoard.name || 'Без названия'}`;

    const columnsContainer = document.getElementById('kanbanColumns');
    const columns = Array.isArray(kanbanBoard.columns) ? kanbanBoard.columns : [];

    if (!columns.length) {
        columnsContainer.innerHTML = '<div class="kanban-empty">Колонки отсутствуют</div>';
        return;
    }

    columnsContainer.innerHTML = columns.map(column => {
        const cards = Array.isArray(column.cards) ? column.cards : [];
        const cardsHtml = cards.length
            ? cards.map(card => renderCardHtml(card)).join('')
            : '<div class="kanban-empty">Перетащите карточку сюда или создайте новую</div>';

        return `
          <section class="kanban-column" data-column-id="${column.id}" ondragover="handleColumnDragOver(event)" ondrop="handleColumnDrop(event, ${column.id})" ondragleave="handleColumnDragLeave(event)">
            <div class="kanban-column-head">
              <div class="kanban-column-head-main">
                <h2 class="kanban-column-title">${escapeHtml(column.name)}</h2>
                <button class="btn btn-sm btn-secondary" type="button" onclick="renameColumn(${column.id})">Переименовать</button>
              </div>
              <div style="display:flex;align-items:center;gap:8px;">
                <span class="kanban-column-count">${cards.length}</span>
                <button class="btn btn-sm btn-primary" type="button" onclick="openCreateCardModal(${column.id})">+ Карточка</button>
              </div>
            </div>
            <div class="kanban-cards" data-cards-column="${column.id}">
              ${cardsHtml}
            </div>
          </section>`;
    }).join('');
}

function renderCardHtml(card) {
    const description = (card.description || '').trim();
    const hasAttachments = Array.isArray(card.attachments) && card.attachments.length;
    const previewHtml = card.previewImageUrl
        ? `<img class="kanban-card-preview" src="${escapeHtml(card.previewImageUrl)}" alt="Превью карточки">`
        : '';

    return `
      <article class="kanban-card" draggable="true" data-card-id="${card.id}" ondragstart="handleCardDragStart(event, ${card.id})" ondragend="handleCardDragEnd(event)">
        ${previewHtml}
        <h3 class="kanban-card-title">${escapeHtml(card.title || 'Без названия')}</h3>
        <p class="kanban-card-desc">${description ? escapeHtml(description) : 'Без описания'}</p>
        ${hasAttachments ? `<div class="kanban-attach-hint">Вложений: ${card.attachments.length}</div>` : ''}
        <div class="kanban-card-actions">
          <button class="btn btn-sm btn-secondary" type="button" onclick="openEditCardModal(${card.id})">Редактировать</button>
          <button class="btn btn-sm btn-danger" type="button" onclick="deleteCard(${card.id})">Удалить</button>
        </div>
      </article>`;
}

function onBoardChange(value) {
    currentBoardId = Number(value);
    loadKanbanBoard();
}

async function createBoard() {
    const name = prompt('Название новой доски:', 'Новая доска');
    if (name === null) return;

    const result = await KanbanAPI.createBoard({ name: (name || '').trim() });
    if (!result || result.error) {
        showNotification(result?.error || 'Не удалось создать доску', 'error');
        return;
    }

    await loadBoards(result.id);
    showNotification('Доска создана', 'success');
}

function enableRenameBoard() {
    const input = boardNameInputEl();
    input.disabled = false;
    input.focus();
    input.select();
    boardNameActionsEl().style.display = 'flex';
}

function cancelRenameBoard() {
    boardNameInputEl().disabled = true;
    boardNameActionsEl().style.display = 'none';
    syncBoardRenameInput();
}

async function submitRenameBoard() {
    if (!kanbanBoard?.id) return;

    const newName = (boardNameInputEl().value || '').trim();
    if (!newName) {
        showNotification('Введите название доски', 'warning');
        return;
    }

    const result = await KanbanAPI.renameBoard(kanbanBoard.id, { name: newName });
    if (!result || result.error) {
        showNotification(result?.error || 'Не удалось переименовать доску', 'error');
        return;
    }

    cancelRenameBoard();
    await loadBoards(kanbanBoard.id);
    showNotification('Название доски обновлено', 'success');
}

function syncBoardRenameInput() {
    if (!boardNameInputEl()) return;
    boardNameInputEl().value = kanbanBoard?.name || '';
    boardNameInputEl().disabled = true;
    boardNameActionsEl().style.display = 'none';
}

async function renameColumn(columnId) {
    const column = findColumnById(columnId);
    if (!column) {
        showNotification('Колонка не найдена', 'error');
        return;
    }

    const nextName = prompt('Новое название колонки:', column.name || '');
    if (nextName === null) return;

    const result = await KanbanAPI.renameColumn(columnId, { name: (nextName || '').trim() });
    if (!result || result.error) {
        showNotification(result?.error || 'Не удалось переименовать колонку', 'error');
        return;
    }

    await loadKanbanBoard();
}

function handleCardDragStart(event, cardId) {
    draggingCardId = cardId;
    event.dataTransfer.effectAllowed = 'move';
    event.currentTarget.classList.add('dragging');
}

function handleCardDragEnd(event) {
    event.currentTarget.classList.remove('dragging');
    draggingCardId = null;
    document.querySelectorAll('.kanban-column.drag-over').forEach(col => col.classList.remove('drag-over'));
}

function handleColumnDragOver(event) {
    event.preventDefault();
    event.currentTarget.classList.add('drag-over');
}

function handleColumnDragLeave(event) {
    event.currentTarget.classList.remove('drag-over');
}

async function handleColumnDrop(event, columnId) {
    event.preventDefault();
    event.currentTarget.classList.remove('drag-over');

    if (!draggingCardId) return;

    const targetColumn = findColumnById(columnId);
    if (!targetColumn) return;

    const payload = {
        columnId,
        position: Array.isArray(targetColumn.cards) ? targetColumn.cards.length : 0
    };

    const result = await KanbanAPI.moveCard(draggingCardId, payload);
    if (!result || result.error) {
        showNotification(result?.error || 'Не удалось переместить карточку', 'error');
        return;
    }

    await loadKanbanBoard();
}

function openCreateCardModal(columnId) {
    document.getElementById('cardModalTitle').textContent = 'Новая карточка';
    cardIdEl().value = '';
    cardColumnIdEl().value = String(columnId);
    cardTitleEl().value = '';
    cardDescriptionEl().value = '';
    resetPendingAttachments();
    renderCurrentAttachments([]);
    cardModalEl().style.display = 'block';
}

function openEditCardModal(cardId) {
    const cardInfo = findCard(cardId);
    if (!cardInfo) {
        showNotification('Карточка не найдена', 'error');
        return;
    }

    document.getElementById('cardModalTitle').textContent = 'Редактирование карточки';
    cardIdEl().value = String(cardInfo.card.id);
    cardColumnIdEl().value = String(cardInfo.column.id);
    cardTitleEl().value = cardInfo.card.title || '';
    cardDescriptionEl().value = cardInfo.card.description || '';
    resetPendingAttachments();
    renderCurrentAttachments(cardInfo.card.attachments || []);
    cardModalEl().style.display = 'block';
}

function closeCardModal() {
    cardModalEl().style.display = 'none';
    resetPendingAttachments();
}

function onAttachmentsSelected(input) {
    pendingAttachments = Array.from(input.files || []);
    renderPendingAttachments();
}

function resetPendingAttachments() {
    pendingAttachments = [];
    if (cardAttachmentInputEl()) cardAttachmentInputEl().value = '';
    renderPendingAttachments();
}

function renderPendingAttachments() {
    if (!pendingAttachmentsEl()) return;
    if (!pendingAttachments.length) {
        pendingAttachmentsEl().innerHTML = '<div class="kanban-attach-empty">Новые вложения не выбраны</div>';
        return;
    }

    pendingAttachmentsEl().innerHTML = pendingAttachments
        .map(file => `<div class="kanban-attach-row">📎 ${escapeHtml(file.name)} (${formatFileSize(file.size)})</div>`)
        .join('');
}

function renderCurrentAttachments(attachments) {
    if (!currentAttachmentsEl()) return;

    if (!Array.isArray(attachments) || !attachments.length) {
        currentAttachmentsEl().innerHTML = '<div class="kanban-attach-empty">Вложений нет</div>';
        return;
    }

    currentAttachmentsEl().innerHTML = attachments.map(att => {
        const isImage = att.type === 'IMAGE';
        const preview = isImage ? `<img src="${escapeHtml(att.url)}" class="kanban-mini-preview" alt="preview">` : '📄';
        return `<div class="kanban-attach-row">
          <span>${preview} <a href="${escapeHtml(att.url)}" target="_blank" rel="noopener">${escapeHtml(att.originalName || 'Файл')}</a></span>
          <button type="button" class="btn btn-sm btn-danger" onclick="removeAttachment(${cardIdEl().value || 0}, '${escapeJs(att.url)}')">Удалить</button>
        </div>`;
    }).join('');
}

async function removeAttachment(cardId, url) {
    if (!cardId || !url) return;
    if (!confirm('Удалить вложение?')) return;

    const result = await KanbanAPI.deleteAttachment(cardId, url);
    if (!result || result.error) {
        showNotification(result?.error || 'Не удалось удалить вложение', 'error');
        return;
    }

    const attachments = await KanbanAPI.getAttachments(cardId);
    renderCurrentAttachments(Array.isArray(attachments) ? attachments : []);
    await loadKanbanBoard();
}

async function submitCard(event) {
    event.preventDefault();

    const cardId = cardIdEl().value;
    const columnId = Number(cardColumnIdEl().value);
    const payload = {
        title: (cardTitleEl().value || '').trim(),
        description: (cardDescriptionEl().value || '').trim()
    };

    if (!payload.title) {
        showNotification('Введите название карточки', 'warning');
        return;
    }

    const result = cardId
        ? await KanbanAPI.updateCard(Number(cardId), payload)
        : await KanbanAPI.createCard(columnId, payload);

    if (!result || result.error) {
        showNotification(result?.error || 'Не удалось сохранить карточку', 'error');
        return;
    }

    const savedCardId = Number(result.id || cardId);
    if (savedCardId && pendingAttachments.length) {
        const uploadResult = await KanbanAPI.uploadAttachments(savedCardId, pendingAttachments);
        if (!uploadResult || uploadResult.error) {
            showNotification(uploadResult?.error || 'Карточка сохранена, но вложения не загружены', 'warning');
        }
    }

    closeCardModal();
    await loadKanbanBoard();
}

async function deleteCard(cardId) {
    if (!confirm('Удалить карточку?')) return;

    const result = await KanbanAPI.deleteCard(cardId);
    if (!result || result.error) {
        showNotification(result?.error || 'Не удалось удалить карточку', 'error');
        return;
    }

    await loadKanbanBoard();
}

function findColumnById(columnId) {
    const columns = Array.isArray(kanbanBoard?.columns) ? kanbanBoard.columns : [];
    return columns.find(column => Number(column.id) === Number(columnId)) || null;
}

function findCard(cardId) {
    const columns = Array.isArray(kanbanBoard?.columns) ? kanbanBoard.columns : [];
    for (const column of columns) {
        const cards = Array.isArray(column.cards) ? column.cards : [];
        const card = cards.find(item => Number(item.id) === Number(cardId));
        if (card) return { column, card };
    }
    return null;
}

function formatFileSize(size) {
    if (!Number.isFinite(size)) return '0 B';
    if (size < 1024) return `${size} B`;
    if (size < 1024 * 1024) return `${Math.round(size / 1024)} KB`;
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function escapeJs(value) {
    return String(value ?? '').replace(/\\/g, '\\\\').replace(/'/g, "\\'");
}

window.addEventListener('click', (event) => {
    if (event.target === cardModalEl()) {
        closeCardModal();
    }
});
