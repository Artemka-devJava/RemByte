/**
 * FixByte CRM — общие UI-утилиты, подключаются везде через
 * fragments/common-scripts.html (uiBase).
 */

// ===== Модалки: открыть/закрыть по id =====
// Единая реализация вместо копий на отдельных страницах.
function openModal(id) {
    const el = document.getElementById(id);
    if (el) el.style.display = 'block';
}
function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.style.display = 'none';
}

// ===== Подтверждение действия (замена window.confirm) =====
/**
 * Модалка подтверждения в стиле приложения вместо браузерного confirm().
 * Возвращает Promise<boolean> — true, если подтвердили.
 *
 * Пример: if (!(await confirmAction('Удалить клиента?'))) return;
 */
function confirmAction(message, options) {
    const opts = options || {};
    const title = opts.title || 'Подтверждение';
    const confirmText = opts.confirmText || 'Да, удалить';
    const cancelText = opts.cancelText || 'Отмена';
    const danger = opts.danger !== false;

    return new Promise((resolve) => {
        const modal = document.getElementById('confirmModal');
        if (!modal) { resolve(window.confirm(message)); return; }

        document.getElementById('confirmModalTitle').textContent = title;
        document.getElementById('confirmModalMessage').textContent = message;
        const okBtn = document.getElementById('confirmModalOk');
        const cancelBtn = document.getElementById('confirmModalCancel');
        okBtn.textContent = confirmText;
        okBtn.className = 'btn btn-sm ' + (danger ? 'btn-danger' : 'btn-primary');
        cancelBtn.textContent = cancelText;

        let done = false;
        function finish(result) {
            if (done) return;
            done = true;
            modal.style.display = 'none';
            okBtn.removeEventListener('click', onOk);
            cancelBtn.removeEventListener('click', onCancel);
            modal.removeEventListener('click', onBackdrop);
            document.removeEventListener('keydown', onKey);
            resolve(result);
        }
        function onOk() { finish(true); }
        function onCancel() { finish(false); }
        function onBackdrop(e) { if (e.target === modal) finish(false); }
        function onKey(e) {
            if (e.key === 'Escape') finish(false);
            if (e.key === 'Enter') finish(true);
        }

        okBtn.addEventListener('click', onOk);
        cancelBtn.addEventListener('click', onCancel);
        modal.addEventListener('click', onBackdrop);
        document.addEventListener('keydown', onKey);
        modal.style.display = 'block';
        okBtn.focus();
    });
}
