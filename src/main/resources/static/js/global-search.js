/**
 * FixByte CRM — сквозной поиск по сайдбару (клиенты + заказы), доступен на
 * любой странице. Использует уже существующий Autocomplete (combobox.js) —
 * тот же визуальный компонент, что и подбор клиента/услуги в форме заказа.
 *
 * Раньше поиск был только локальный на каждой странице отдельно — нельзя
 * было найти клиента, находясь на /orders, и наоборот.
 */
document.addEventListener('DOMContentLoaded', () => {
    const input = document.getElementById('globalSearchInput');
    const menu = document.getElementById('globalSearchMenu');
    if (!input || !menu || !window.Autocomplete) return;

    Autocomplete.attach(input, {
        menu,
        minChars: 2,
        openOnFocus: false,
        emptyText: 'Ничего не найдено',
        getItems: async (query) => {
            const [clients, ordersResp] = await Promise.all([
                fetch(`/api/clients/search?name=${encodeURIComponent(query)}`)
                    .then(r => r.ok ? r.json() : [])
                    .catch(() => []),
                fetch(`/api/orders/page?page=0&size=5&q=${encodeURIComponent(query)}`)
                    .then(r => r.ok ? r.json() : { content: [] })
                    .catch(() => ({ content: [] }))
            ]);

            const clientItems = (Array.isArray(clients) ? clients : []).slice(0, 5).map(c => ({
                id: 'client-' + c.id,
                label: c.name,
                sublabel: c.phone || '',
                group: 'Клиенты',
                data: { type: 'client', id: c.id }
            }));
            const orderItems = (ordersResp.content || []).map(o => ({
                id: 'order-' + o.id,
                label: o.orderNumber,
                sublabel: o.clientName || '',
                group: 'Заказы',
                data: { type: 'order', id: o.id }
            }));

            return clientItems.concat(orderItems);
        },
        onSelect: (item) => {
            if (item.data.type === 'client') {
                location.href = '/clients/' + item.data.id;
            } else {
                location.href = '/orders?open=' + item.data.id;
            }
        }
    });

    // "/" фокусирует поиск из любого места на странице — если не набираем
    // текст в другом поле.
    document.addEventListener('keydown', (e) => {
        if (e.key !== '/' || e.ctrlKey || e.metaKey || e.altKey) return;
        const tag = document.activeElement && document.activeElement.tagName;
        if (tag === 'INPUT' || tag === 'TEXTAREA' || document.activeElement?.isContentEditable) return;
        e.preventDefault();
        input.focus();
    });

    input.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') input.blur();
    });
});
