let notesState = {
    folders: [],
    notes: [],
    selectedFolderId: null,
    selectedNoteId: null
};

let notesPluginEnabled = true;
let markdownMode = 'edit';

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

function noteExcerpt(content, maxLen = 90) {
    const plain = String(content ?? '')
        .replace(/```[\s\S]*?```/g, ' ')
        .replace(/[#>*`_-]/g, ' ')
        .replace(/\[([^\]]+)]\([^)]+\)/g, '$1')
        .replace(/\s+/g, ' ')
        .trim();
    if (!plain) return 'Пустая заметка';
    return plain.length > maxLen ? plain.slice(0, maxLen) + '…' : plain;
}

function escapeHtmlPreserve(str) {
    return String(str ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
}

function markdownToHtml(markdown) {
    const normalized = String(markdown ?? '').replace(/\r\n/g, '\n');
    let html = escapeHtmlPreserve(normalized);

    html = html.replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>');
    html = html.replace(/^###\s+(.+)$/gm, '<h3>$1</h3>');
    html = html.replace(/^##\s+(.+)$/gm, '<h2>$1</h2>');
    html = html.replace(/^#\s+(.+)$/gm, '<h1>$1</h1>');
    html = html.replace(/^>\s+(.+)$/gm, '<blockquote>$1</blockquote>');
    html = html.replace(/^---$/gm, '<hr>');

    html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
    html = html.replace(/\[([^\]]+)]\((https?:\/\/[^\s)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>');

    html = html.replace(/(?:^|\n)(- .+(?:\n- .+)*)/g, (match, listBlock) => {
        const items = listBlock
            .split('\n')
            .map(line => line.replace(/^-\s+/, '').trim())
            .filter(Boolean)
            .map(item => `<li>${item}</li>`)
            .join('');
        return `\n<ul>${items}</ul>`;
    });

    const blocks = html.split(/\n{2,}/).map(block => block.trim()).filter(Boolean);
    html = blocks.map(block => {
        if (/^<(h1|h2|h3|ul|blockquote|pre|hr)/.test(block)) return block;
        return `<p>${block.replace(/\n/g, '<br>')}</p>`;
    }).join('');

    return html || '<p><em>Пусто</em></p>';
}

function renderMarkdownPreview() {
    const preview = document.getElementById('notePreview');
    const content = document.getElementById('noteContent');
    if (!preview || !content) return;
    preview.innerHTML = markdownToHtml(content.value);
}

function switchMarkdownMode(mode) {
    markdownMode = mode === 'preview' ? 'preview' : 'edit';

    const editor = document.getElementById('noteContent');
    const preview = document.getElementById('notePreview');
    const editBtn = document.getElementById('mdEditModeBtn');
    const previewBtn = document.getElementById('mdPreviewModeBtn');

    const isPreview = markdownMode === 'preview';
    if (editor) editor.style.display = isPreview ? 'none' : 'block';
    if (preview) preview.style.display = isPreview ? 'block' : 'none';
    if (editBtn) editBtn.classList.toggle('active', !isPreview);
    if (previewBtn) previewBtn.classList.toggle('active', isPreview);

    if (isPreview) {
        renderMarkdownPreview();
    }
}

function insertAtCursor(textarea, before, after) {
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const value = textarea.value;
    const selected = value.slice(start, end);
    const replacement = `${before}${selected || 'текст'}${after}`;
    textarea.value = value.slice(0, start) + replacement + value.slice(end);
    const caret = start + replacement.length;
    textarea.focus();
    textarea.setSelectionRange(caret, caret);
    renderMarkdownPreview();
}

function insertSnippet(textarea, snippet) {
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const value = textarea.value;
    textarea.value = value.slice(0, start) + snippet + value.slice(end);
    const caret = start + snippet.length;
    textarea.focus();
    textarea.setSelectionRange(caret, caret);
    renderMarkdownPreview();
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
        box.innerHTML = '<div class="empty"><span class="empty-icon">🗂️</span>Папок пока нет — создайте первую выше</div>';
        return;
    }

    box.innerHTML = notesState.folders.map(folder => `
      <div class="folder-item ${folder.id === notesState.selectedFolderId ? 'active' : ''}" data-folder-id="${folder.id}">
        <div class="item-icon">📁</div>
        <div class="item-info">
          <p class="folder-name">${escapeHtml(folder.name)}</p>
          <p class="folder-meta">
            <span class="count-badge">${folder.notesCount ?? 0}</span>
            <span>${shortDate(folder.updatedAt)}</span>
          </p>
        </div>
        <div class="item-actions">
          <button class="icon-btn" data-action="rename" title="Переименовать">✏️</button>
          <button class="icon-btn danger" data-action="delete" title="Удалить папку">🗑</button>
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
    if (!confirm(`Удалить папку «${folder.name}» вместе со всеми заметками в ней?`)) return;

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
        box.innerHTML = '<div class="empty"><span class="empty-icon">👈</span>Выберите папку слева</div>';
        return;
    }

    if (!notesState.notes.length) {
        box.innerHTML = '<div class="empty"><span class="empty-icon">📝</span>В этой папке пока нет заметок</div>';
        return;
    }

    box.innerHTML = notesState.notes.map(note => `
      <div class="note-item ${note.id === notesState.selectedNoteId ? 'active' : ''}" data-note-id="${note.id}">
        <div class="item-icon">📝</div>
        <div class="item-info">
          <p class="note-name">${escapeHtml(note.title)}</p>
          <p class="note-excerpt">${escapeHtml(noteExcerpt(note.content))}</p>
          <p class="note-meta">🕒 ${shortDate(note.updatedAt)}</p>
        </div>
      </div>
    `).join('');
}

function showEditor(note) {
    const wrap = document.getElementById('noteEditorWrap');
    document.getElementById('noteTitle').value = note?.title || '';
    document.getElementById('noteContent').value = note?.content || '';
    wrap.style.display = 'block';
    switchMarkdownMode(markdownMode);
    renderMarkdownPreview();
}

function hideEditor() {
    const wrap = document.getElementById('noteEditorWrap');
    wrap.style.display = 'none';
    document.getElementById('noteTitle').value = '';
    document.getElementById('noteContent').value = '';
    const preview = document.getElementById('notePreview');
    if (preview) preview.innerHTML = '';
}

function getSelectedNote() {
    return notesState.notes.find(n => n.id === notesState.selectedNoteId) || null;
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
    showNotification('Папка создана', 'success');
    await loadFolders();
    await selectFolder(created.id);
}

async function createNote() {
    if (!notesPluginEnabled) return;
    if (!notesState.selectedFolderId) {
        showNotification('Сначала выберите папку', 'warning');
        return;
    }

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
}

async function saveCurrentNote() {
    if (!notesPluginEnabled) return;
    const note = getSelectedNote();
    if (!note) {
        showNotification('Выберите заметку', 'warning');
        return;
    }

    const payload = {
        title: document.getElementById('noteTitle').value.trim() || 'Без названия',
        content: document.getElementById('noteContent').value
    };

    const updated = await NotesPluginAPI.updateNote(note.id, payload);
    if (!updated || updated.error) {
        showNotification(updated?.error || 'Не удалось сохранить заметку', 'error');
        return;
    }

    showNotification('Заметка сохранена', 'success');
    await loadFolders();
    await loadNotes(notesState.selectedFolderId);
    notesState.selectedNoteId = updated.id;
    renderNotes();
    showEditor(updated);
}

async function deleteCurrentNote() {
    if (!notesPluginEnabled) return;
    const note = getSelectedNote();
    if (!note) {
        showNotification('Выберите заметку', 'warning');
        return;
    }
    if (!confirm(`Удалить заметку «${note.title}»?`)) return;

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
    document.getElementById('btnSaveNote').addEventListener('click', saveCurrentNote);
    document.getElementById('btnDeleteNote').addEventListener('click', deleteCurrentNote);
    document.getElementById('btnDownloadNote').addEventListener('click', downloadCurrentNote);

    document.getElementById('mdEditModeBtn')?.addEventListener('click', () => switchMarkdownMode('edit'));
    document.getElementById('mdPreviewModeBtn')?.addEventListener('click', () => switchMarkdownMode('preview'));

    document.getElementById('noteContent')?.addEventListener('input', () => {
        if (markdownMode === 'preview') {
            renderMarkdownPreview();
        }
    });

    document.querySelectorAll('[data-md-wrap]').forEach(btn => {
        btn.addEventListener('click', () => {
            const textarea = document.getElementById('noteContent');
            if (!textarea) return;
            const [before, after] = String(btn.getAttribute('data-md-wrap') || '|').split('|');
            insertAtCursor(textarea, before || '', after || '');
        });
    });

    document.querySelectorAll('[data-md-insert]').forEach(btn => {
        btn.addEventListener('click', () => {
            const textarea = document.getElementById('noteContent');
            if (!textarea) return;
            const snippet = btn.getAttribute('data-md-insert') || '';
            insertSnippet(textarea, snippet);
        });
    });

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

    document.getElementById('notesList').addEventListener('click', (e) => {
        const el = e.target.closest('[data-note-id]');
        if (!el) return;
        notesState.selectedNoteId = Number(el.getAttribute('data-note-id'));
        renderNotes();
        showEditor(getSelectedNote());
    });
}

document.addEventListener('DOMContentLoaded', async () => {
    const enabled = await checkPluginEnabled();
    if (!enabled) {
        setDisabledState('Плагин заметок отключен администратором.');
        return;
    }

    bindEvents();
    switchMarkdownMode('edit');
    await loadFolders();
});

