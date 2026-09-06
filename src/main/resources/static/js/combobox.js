/**
 * FixByte CRM — Autocomplete
 * Небольшой безбиблиотечный компонент «поиск + выпадающий список».
 * Используется для выбора клиента и подбора услуг в форме заказа.
 *
 * Autocomplete.attach(inputEl, {
 *   menu:      HTMLElement,                       // контейнер выпадашки (обязателен)
 *   minChars:  0,                                 // с какого числа символов искать
 *   debounce:  180,
 *   openOnFocus: true,
 *   emptyText: 'Ничего не найдено',
 *   getItems:  async (query) => [ { id, label, sublabel, group, kind, data } ],
 *   footer:    (query) => [ { id:'__create__', label:'➕ Создать…', kind:'action', data } ],
 *   onSelect:  (item) => {}
 * }) -> { open, close, clear, refresh, destroy }
 */
(function (global) {
    'use strict';

    function el(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text != null) node.textContent = text;
        return node;
    }

    function attach(input, opts) {
        if (!input || !opts || !opts.menu) {
            throw new Error('Autocomplete.attach: нужен input и opts.menu');
        }

        const menu = opts.menu;
        const minChars = opts.minChars != null ? opts.minChars : 0;
        const debounceMs = opts.debounce != null ? opts.debounce : 180;
        const openOnFocus = opts.openOnFocus !== false;
        const emptyText = opts.emptyText || 'Ничего не найдено';

        let items = [];
        let activeIndex = -1;
        let debounceTimer = null;
        let lastQuery = null;
        let seq = 0;
        let open = false;

        function setOpen(next) {
            open = next;
            menu.hidden = !next;
            if (!next) {
                activeIndex = -1;
            }
        }

        function currentItems() {
            const footerItems = typeof opts.footer === 'function'
                ? (opts.footer(input.value.trim()) || [])
                : [];
            return items.concat(footerItems);
        }

        function render() {
            const all = currentItems();
            menu.innerHTML = '';

            if (all.length === 0) {
                menu.appendChild(el('div', 'combo-empty', emptyText));
                setOpen(true);
                return;
            }

            let lastGroup = null;
            all.forEach((item, index) => {
                if (item.group && item.group !== lastGroup) {
                    menu.appendChild(el('div', 'combo-group', item.group));
                    lastGroup = item.group;
                }
                const row = el('div', 'combo-item');
                if (item.kind === 'action') row.classList.add('combo-item-action');
                if (index === activeIndex) row.classList.add('active');
                row.setAttribute('role', 'option');
                row.dataset.index = String(index);

                const main = el('span', 'combo-item-label', item.label);
                row.appendChild(main);
                if (item.sublabel) row.appendChild(el('span', 'combo-item-sub', item.sublabel));

                row.addEventListener('mousedown', (e) => {
                    e.preventDefault(); // не терять фокус до обработки
                    choose(index);
                });
                menu.appendChild(row);
            });

            setOpen(true);
        }

        function choose(index) {
            const all = currentItems();
            const item = all[index];
            if (!item) return;
            if (typeof opts.onSelect === 'function') opts.onSelect(item);
        }

        function moveActive(delta) {
            const all = currentItems();
            if (all.length === 0) return;
            activeIndex = (activeIndex + delta + all.length) % all.length;
            Array.from(menu.querySelectorAll('.combo-item')).forEach((row) => {
                row.classList.toggle('active', Number(row.dataset.index) === activeIndex);
            });
            const activeRow = menu.querySelector('.combo-item.active');
            if (activeRow) activeRow.scrollIntoView({ block: 'nearest' });
        }

        async function search(force) {
            const query = input.value.trim();
            if (!force && query === lastQuery) {
                if (items.length || query.length >= minChars) render();
                return;
            }
            lastQuery = query;

            if (query.length < minChars) {
                items = [];
                setOpen(false);
                return;
            }

            const mySeq = ++seq;
            try {
                const result = await opts.getItems(query);
                if (mySeq !== seq) return; // пришёл устаревший ответ
                items = Array.isArray(result) ? result : [];
            } catch (err) {
                if (mySeq !== seq) return;
                console.error('Autocomplete getItems error:', err);
                items = [];
            }
            activeIndex = -1;
            render();
        }

        function scheduleSearch(force) {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => search(force), debounceMs);
        }

        function onInput() { scheduleSearch(false); }

        function onFocus() {
            if (openOnFocus) search(true);
        }

        function onKeyDown(e) {
            if (e.key === 'ArrowDown') {
                if (!open) { search(true); return; }
                e.preventDefault();
                moveActive(1);
            } else if (e.key === 'ArrowUp') {
                if (!open) return;
                e.preventDefault();
                moveActive(-1);
            } else if (e.key === 'Enter') {
                // Внутри формы Enter не должен её отправлять, пока открыт список
                if (open) {
                    e.preventDefault();
                    const all = currentItems();
                    if (activeIndex >= 0) {
                        choose(activeIndex);
                    } else {
                        // Нет выбора стрелками: берём первое реальное совпадение,
                        // а если совпадений нет — действие «создать / разовая».
                        const firstReal = all.findIndex(it => it.kind !== 'action');
                        if (firstReal >= 0) choose(firstReal);
                        else if (all.length && all[all.length - 1].kind === 'action') choose(all.length - 1);
                    }
                }
            } else if (e.key === 'Escape') {
                if (open) { e.stopPropagation(); setOpen(false); }
            }
        }

        function onDocClick(e) {
            if (e.target === input) return;
            if (menu.contains(e.target)) return;
            setOpen(false);
        }

        input.setAttribute('autocomplete', 'off');
        input.addEventListener('input', onInput);
        input.addEventListener('focus', onFocus);
        input.addEventListener('keydown', onKeyDown);
        document.addEventListener('click', onDocClick);

        return {
            open: () => search(true),
            close: () => setOpen(false),
            clear: () => { input.value = ''; items = []; lastQuery = null; setOpen(false); },
            refresh: () => search(true),
            destroy: () => {
                clearTimeout(debounceTimer);
                input.removeEventListener('input', onInput);
                input.removeEventListener('focus', onFocus);
                input.removeEventListener('keydown', onKeyDown);
                document.removeEventListener('click', onDocClick);
                setOpen(false);
            }
        };
    }

    global.Autocomplete = { attach };
})(window);
