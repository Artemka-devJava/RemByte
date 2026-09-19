let notesState = {
    folders: [],
    notes: [],
    selectedFolderId: null,
    selectedNoteId: null
};

let notesPluginEnabled = true;

// Автосохранение: пишем через AUTOSAVE_DELAY_MS после того, как перестали печатать.
const AUTOSAVE_DELAY_MS = 900;
let autosaveTimer = null;
let noteDirty = false;

function escapeHtml(str) {
    return String(str ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function shortDate(value) {
    if (!value) return '';
    const d = new Date(value);
    return d.toLocaleString('ru-RU', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
}

function noteExcerpt(content, maxLen = 70) {
    const plain = String(content ?? '').replace(/\s+/g, ' ').trim();
    if (!plain) return 'Пустая заметка';
    return plain.length > maxLen ? plain.slice(0, maxLen) + '…' : plain;
}

async function loadFolders() {
    if (!notesPluginEnabled) {
        return;
    }

    const folders = await NotesPluginAPI.getFolders();
    if (folders && folders.error) {
        showNotification(folders.error, 'error');
        return;
    }
    notesState.folders = Array.isArray(folders) ? folders : [];
    renderFolders();

    if (!notesState.selectedFolderId && notesState.folders.length > 0) {
        await selectFolder(notesState.folders[0].id);
    }
}

function setDisabledState(message) {
    notesPluginEnabled = false;
    document.getElementById('btnCreateFolder').disabled = true;
    document.getElementById('btnCreateNote').disabled = true;

    const foldersList = document.getElementById('foldersList');
    const notesList = document.getElementById('notesList');
    if (foldersList) foldersList.innerHTML = `<div class="empty">${escapeHtml(message)}</div>`;
    if (notesList) notesList.innerHTML = `<div class="empty">${escapeHtml(message)}</div>`;

    hideEditor();
}

async function checkPluginEnabled() {
    try {
        const response = await fetch('/api/plugin-settings/notes', { cache: 'no-store' });
        if (!response.ok) return true;
        const data = await response.json();
        return data?.enabled !== false;
    } catch {
        return true;
    }
}

function renderFolders() {
    const box = document.getElementById('foldersList');
    if (!box) return;

    if (!notesState.folders.length) {
        box.innerHTML = '<div class="empty">Папок пока нет — создайте первую выше</div>';
        return;
    }

    box.innerHTML = notesState.folders.map(folder => `
      <div class="list-row ${folder.id === notesState.selectedFolderId ? 'active' : ''}" data-folder-id="${folder.id}">
        <div class="list-row-main">
          <div class="list-row-title">${escapeHtml(folder.name)}</div>
        </div>
        <span class="list-row-meta">${folder.notesCount ?? 0}</span>
        <div class="list-row-actions">
          <button class="icon-btn" data-action="rename" title="Переименовать">✎</button>
          <button class="icon-btn danger" data-action="delete" title="Удалить папку">✕</button>
        </div>
      </div>
    `).join('');
}

async function renameFolderPrompt(folderId) {
    const folder = notesState.folders.find(f => f.id === folderId);
    if (!folder) return;
    const name = prompt('Новое название папки:', folder.name);
    if (name === null) return;
    const trimmed = name.trim();
    if (!trimmed || trimmed === folder.name) return;

    const updated = await NotesPluginAPI.updateFolder(folderId, { name: trimmed, description: '' });
    if (!updated || updated.error) {
        showNotification(updated?.error || 'Не удалось переименовать папку', 'error');
        return;
    }
    showNotification('Папка переименована', 'success');
    await loadFolders();
}

async function deleteFolderPrompt(folderId) {
    const folder = notesState.folders.find(f => f.id === folderId);
    if (!folder) return;
    if (!(await confirmAction(`Удалить папку «${folder.name}» вместе со всеми заметками в ней?`))) return;

    if (notesState.selectedFolderId === folderId) {
        await flushPendingSave();
    }

    const ok = await NotesPluginAPI.deleteFolder(folderId);
    if (!ok) {
        showNotification('Не удалось удалить папку', 'error');
        return;
    }
    showNotification('Папка удалена', 'info');
    if (notesState.selectedFolderId === folderId) {
        notesState.selectedFolderId = null;
        notesState.notes = [];
    }
    await loadFolders();
    if (!notesState.selectedFolderId) {
        document.getElementById('btnCreateNote').disabled = true;
        renderNotes();
        hideEditor();
    }
}

async function selectFolder(folderId) {
    if (notesState.selectedFolderId === Number(folderId)) return;
    await flushPendingSave();

    notesState.selectedFolderId = Number(folderId);
    notesState.selectedNoteId = null;
    document.getElementById('btnCreateNote').disabled = false;

    renderFolders();
    await loadNotes(folderId);
}

async function loadNotes(folderId) {
    const notes = await NotesPluginAPI.getNotesByFolder(folderId);
    notesState.notes = Array.isArray(notes) ? notes : [];
    renderNotes();
    hideEditor();
}

function renderNotes() {
    const box = document.getElementById('notesList');
    if (!box) return;

    if (!notesState.selectedFolderId) {
        box.innerHTML = '<div class="empty">Выберите папку слева</div>';
        return;
    }

    if (!notesState.notes.length) {
        box.innerHTML = '<div class="empty">В этой папке пока нет заметок</div>';
        return;
    }

    box.innerHTML = notesState.notes.map(note => `
      <div class="list-row ${note.id === notesState.selectedNoteId ? 'active' : ''}" data-note-id="${note.id}">
        <div class="list-row-main">
          <div class="list-row-title">${escapeHtml(note.title)}</div>
          <div class="list-row-excerpt">${escapeHtml(noteExcerpt(note.content))}</div>
        </div>
        <span class="list-row-meta">${shortDate(note.updatedAt)}</span>
      </div>
    `).join('');
}

function showEditor(note) {
    const wrap = document.getElementById('noteEditorWrap');
    document.getElementById('noteTitle').value = note?.title || '';
    document.getElementById('noteContent').value = note?.content || '';
    wrap.style.display = 'block';
    setSaveStatus('saved');
}

function hideEditor() {
    const wrap = document.getElementById('noteEditorWrap');
    wrap.style.display = 'none';
    document.getElementById('noteTitle').value = '';
    document.getElementById('noteContent').value = '';
    setSaveStatus('');
}

function getSelectedNote() {
    return notesState.notes.find(n => n.id === notesState.selectedNoteId) || null;
}

function setSaveStatus(state) {
    const el = document.getElementById('noteSaveStatus');
    if (!el) return;
    el.classList.remove('unsaved');
    if (state === 'saving') {
        el.textContent = 'Сохранение…';
    } else if (state === 'saved') {
        el.textContent = 'Сохранено';
    } else if (state === 'unsaved') {
        el.textContent = 'Есть несохранённые изменения…';
        el.classList.add('unsaved');
    } else if (state === 'error') {
        el.textContent = 'Не удалось сохранить';
        el.classList.add('unsaved');
    } else {
        el.textContent = '';
    }
}

/** Вызывать перед любым переключением заметки/папки/уходом со страницы. */
async function flushPendingSave() {
    if (!noteDirty) return;
    clearTimeout(autosaveTimer);
    autosaveTimer = null;
    await saveCurrentNote();
}

function scheduleAutosave() {
    noteDirty = true;
    setSaveStatus('unsaved');
    clearTimeout(autosaveTimer);
    autosaveTimer = setTimeout(saveCurrentNote, AUTOSAVE_DELAY_MS);
}

async function createFolder() {
    if (!notesPluginEnabled) return;
    const input = document.getElementById('newFolderName');
    const name = input.value.trim();
    if (!name) {
        showNotification('Введите название папки', 'warning');
        return;
    }

    const created = await NotesPluginAPI.createFolder({ name, description: '' });
    if (!created || created.error) {
        showNotification(created?.error || 'Не удалось создать папку', 'error');
        return;
    }

    input.value = '';
    await loadFolders();
    await selectFolder(created.id);
}

async function createNote() {
    if (!notesPluginEnabled) return;
    if (!notesState.selectedFolderId) {
        showNotification('Сначала выберите папку', 'warning');
        return;
    }
    await flushPendingSave();

    const payload = { title: 'Новая заметка', content: '' };
    const created = await NotesPluginAPI.createNote(notesState.selectedFolderId, payload);
    if (!created || created.error) {
        showNotification(created?.error || 'Не удалось создать заметку', 'error');
        return;
    }

    await loadFolders();
    await loadNotes(notesState.selectedFolderId);
    notesState.selectedNoteId = created.id;
    renderNotes();
    showEditor(created);
    document.getElementById('noteTitle').focus();
}

/** Автосохранение и ручное сохранение идут через один и тот же путь. */
async function saveCurrentNote() {
    if (!notesPluginEnabled) return;
    const note = getSelectedNote();
    if (!note) return;

    clearTimeout(autosaveTimer);
    autosaveTimer = null;

    const payload = {
        title: document.getElementById('noteTitle').value.trim() || 'Без названия',
        content: document.getElementById('noteContent').value
    };

    setSaveStatus('saving');
    const updated = await NotesPluginAPI.updateNote(note.id, payload);
    if (!updated || updated.error) {
        setSaveStatus('error');
        showNotification(updated?.error || 'Не удалось сохранить заметку', 'error');
        return;
    }

    noteDirty = false;
    setSaveStatus('saved');

    // Обновляем список на месте — без showEditor()/полной перезагрузки,
    // чтобы не сбить курсор и не помешать вводу, если пользователь уже
    // печатает следующую правку.
    const idx = notesState.notes.findIndex(n => n.id === updated.id);
    if (idx !== -1) notesState.notes[idx] = updated;
    renderNotes();
    highlightActiveNoteRow();
}

function highlightActiveNoteRow() {
    document.querySelectorAll('#notesList .list-row').forEach(el => {
        el.classList.toggle('active', Number(el.getAttribute('data-note-id')) === notesState.selectedNoteId);
    });
}

async function deleteCurrentNote() {
    if (!notesPluginEnabled) return;
    const note = getSelectedNote();
    if (!note) {
        showNotification('Выберите заметку', 'warning');
        return;
    }
    if (!(await confirmAction(`Удалить заметку «${note.title}»?`))) return;

    clearTimeout(autosaveTimer);
    autosaveTimer = null;
    noteDirty = false;

    const ok = await NotesPluginAPI.deleteNote(note.id);
    if (!ok) {
        showNotification('Не удалось удалить заметку', 'error');
        return;
    }

    notesState.selectedNoteId = null;
    showNotification('Заметка удалена', 'info');
    await loadFolders();
    await loadNotes(notesState.selectedFolderId);
}

function downloadCurrentNote() {
    if (!notesPluginEnabled) return;
    const note = getSelectedNote();
    if (!note) {
        showNotification('Выберите заметку', 'warning');
        return;
    }
    window.open(`/api/notes-plugin/notes/${note.id}/download`, '_blank');
}

function bindEvents() {
    document.getElementById('btnCreateFolder').addEventListener('click', createFolder);
    document.getElementById('btnCreateNote').addEventListener('click', createNote);
    document.getElementById('btnDeleteNote').addEventListener('click', deleteCurrentNote);
    document.getElementById('btnDownloadNote').addEventListener('click', downloadCurrentNote);

    document.getElementById('noteTitle')?.addEventListener('input', scheduleAutosave);
    document.getElementById('noteContent')?.addEventListener('input', scheduleAutosave);

    document.getElementById('foldersList').addEventListener('click', async (e) => {
        const item = e.target.closest('[data-folder-id]');
        if (!item) return;
        const folderId = Number(item.getAttribute('data-folder-id'));

        const actionBtn = e.target.closest('[data-action]');
        if (actionBtn) {
            e.stopPropagation();
            if (actionBtn.getAttribute('data-action') === 'rename') {
                await renameFolderPrompt(folderId);
            } else if (actionBtn.getAttribute('data-action') === 'delete') {
                await deleteFolderPrompt(folderId);
            }
            return;
        }

        await selectFolder(folderId);
    });

    document.getElementById('notesList').addEventListener('click', async (e) => {
        const el = e.target.closest('[data-note-id]');
        if (!el) return;
        const noteId = Number(el.getAttribute('data-note-id'));
        if (noteId === notesState.selectedNoteId) return;

        await flushPendingSave();
        notesState.selectedNoteId = noteId;
        renderNotes();
        showEditor(getSelectedNote());
    });

    // Не даём случайно уйти со страницы, пока автосохранение ещё не отработало.
    window.addEventListener('beforeunload', (e) => {
        if (!noteDirty) return;
        e.preventDefault();
        e.returnValue = '';
    });
}

document.addEventListener('DOMContentLoaded', async () => {
    const enabled = await checkPluginEnabled();
    if (!enabled) {
        setDisabledState('Плагин заметок отключен администратором.');
        return;
    }

    bindEvents();
    await loadFolders();
});
