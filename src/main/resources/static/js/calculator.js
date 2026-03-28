/**
 * JavaScript код для калькулятора стоимости
 */

let selectedServices = new Map();
let currentCalcResult = { servicesTotal: 0, additionalTotal: 0, discountAmount: 0, finalTotal: 0 };

// Определить категорию услуги по названию (аналогично orders.js)
function detectCalcCategory(service) {
    const n = (service.name || '').toLowerCase();
    if (n.includes('диагност'))                               return 'Диагностика';
    if (n.includes('чист'))                                   return 'Чистка';
    if (n.includes('замен'))                                  return 'Замена';
    if (n.includes('установ') || n.includes('инстал'))        return 'Установка';
    if (n.includes('восстан') || n.includes('данн') || n.includes('резерв')) return 'Данные';
    if (n.includes('ремонт') || n.includes('пайк') || n.includes('плат'))    return 'Ремонт';
    if (n.includes('обслуж') || n.includes('настр') || n.includes('оптим'))  return 'Обслуживание';
    return service.category || 'Прочее';
}

const CALC_CAT_ICONS = {
    'Диагностика': '🔍', 'Ремонт': '🔧', 'Чистка': '🧹', 'Замена': '🔄',
    'Установка': '💿', 'Обслуживание': '⚙️', 'Данные': '💾', 'Прочее': '📦'
};
const CALC_CAT_ORDER = ['Диагностика','Ремонт','Замена','Чистка','Установка','Обслуживание','Данные','Прочее'];

// Загрузить услуги при загрузке страницы
document.addEventListener('DOMContentLoaded', () => {
    loadServicesForCalculator();
});

// Загрузить услуги для калькулятора
async function loadServicesForCalculator() {
    try {
        const services = await ServiceAPI.getActive();
        renderCalculatorServices(services);
        renderPriceTable(services);
    } catch (error) {
        console.error('Error loading services:', error);
    }
}

// Отобразить услуги в калькуляторе (сгруппированы по категориям)
function renderCalculatorServices(services) {
    const container = document.getElementById('servicesList');

    // Группировка по категориям
    const groups = {};
    services.forEach(s => {
        const cat = detectCalcCategory(s);
        if (!groups[cat]) groups[cat] = [];
        groups[cat].push(s);
    });

    const sortedCats = Object.keys(groups).sort((a, b) => {
        const ai = CALC_CAT_ORDER.indexOf(a), bi = CALC_CAT_ORDER.indexOf(b);
        return (ai === -1 ? 99 : ai) - (bi === -1 ? 99 : bi);
    });

    let html = '';
    sortedCats.forEach(cat => {
        const icon = CALC_CAT_ICONS[cat] || '🛠️';
        html += `<div class="calc-cat-header">${icon} ${cat}</div>`;
        groups[cat].forEach(service => {
            html += `
            <div class="check-group">
              <label class="check-label">
                <input type="checkbox" value="${service.id}" data-price="${service.basePrice}"
                       data-name="${service.name}" onchange="calculatePrice()">
                <span>${service.name}</span>
                <span class="check-badge">${formatCurrency(service.basePrice)}</span>
              </label>
            </div>`;
        });
    });

    container.innerHTML = html || '<div style="color:var(--c-muted);font-size:13px;padding:10px 0;">Нет доступных услуг</div>';
}

// Отобразить прайс-лист
function renderPriceTable(services) {
    const tbody = document.querySelector('#priceTable tbody');
    
    const grouped = {};
    services.forEach(service => {
        if (!grouped[service.category]) {
            grouped[service.category] = [];
        }
        grouped[service.category].push(service);
    });

    let html = '';
    Object.entries(grouped).forEach(([category, categoryServices]) => {
        categoryServices.forEach((service, index) => {
            if (index === 0) {
                html += `<tr>
                    <td rowspan="${categoryServices.length}">${category}</td>
                    <td>${service.name}</td>
                    <td>${formatCurrency(service.basePrice)}</td>
                </tr>`;
            } else {
                html += `<tr>
                    <td>${service.name}</td>
                    <td>${formatCurrency(service.basePrice)}</td>
                </tr>`;
            }
        });
    });

    tbody.innerHTML = html || '<tr><td colspan="3" class="empty">Нет услуг</td></tr>';
}

// Пересчитать стоимость
function calculatePrice() {
    const selectedCheckboxes = document.querySelectorAll('#servicesList input[type="checkbox"]:checked');

    let servicesTotal = 0;
    selectedServices.clear();

    selectedCheckboxes.forEach(checkbox => {
        const price = parseFloat(checkbox.dataset.price) || 0;
        const name = checkbox.dataset.name;
        servicesTotal += price;
        selectedServices.set(checkbox.value, { name, price });
    });

    // Дополнительные начисления
    let additionalTotal = 0;
    if (document.getElementById('additionalWork').checked) additionalTotal += 500;
    if (document.getElementById('urgentOrder').checked)    additionalTotal += servicesTotal * 0.20;
    if (document.getElementById('warranty').checked)       additionalTotal += servicesTotal * 0.15;

    // Скидка
    const discountPercent = parseFloat(document.getElementById('discountPercent').value) || 0;
    const subtotal = servicesTotal + additionalTotal;
    const discountAmount = (subtotal * discountPercent) / 100;
    const finalTotal = subtotal - discountAmount;

    // Сохранить результат для createOrderFromCalculator
    currentCalcResult = { servicesTotal, additionalTotal, discountAmount, finalTotal };

    // Обновить отображение
    document.getElementById('servicesTotal').textContent = formatCurrency(servicesTotal);
    document.getElementById('additionalTotal').textContent = formatCurrency(additionalTotal);
    document.getElementById('discountTotal').textContent = '-' + formatCurrency(discountAmount);
    document.getElementById('finalTotal').innerHTML = `<strong>${formatCurrency(finalTotal)}</strong>`;
}

// Слушатели для дополнительных параметров (дублируют onchange в HTML, но для надёжности)
document.addEventListener('DOMContentLoaded', () => {
    ['additionalWork','urgentOrder','warranty'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.addEventListener('change', calculatePrice);
    });
    const disc = document.getElementById('discountPercent');
    if (disc) disc.addEventListener('input', calculatePrice);
});

// Создать заказ на основе расчета
async function createOrderFromCalculator() {
    if (selectedServices.size === 0) {
        showNotification('Пожалуйста, выберите хотя бы одну услугу', 'warning');
        return;
    }

    // Сформировать заметку из дополнительных опций
    const extras = [];
    if (document.getElementById('additionalWork').checked) extras.push('доп. диагностика (+500₽)');
    if (document.getElementById('urgentOrder').checked)    extras.push('срочный заказ (+20%)');
    if (document.getElementById('warranty').checked)       extras.push('гарантия 1 год (+15%)');
    const discountPercent = parseFloat(document.getElementById('discountPercent').value) || 0;
    if (discountPercent > 0) extras.push(`скидка ${discountPercent}%`);

    const calcData = {
        serviceIds:      Array.from(selectedServices.keys()),
        additionalWork:  document.getElementById('additionalWork').checked,
        urgentOrder:     document.getElementById('urgentOrder').checked,
        warranty:        document.getElementById('warranty').checked,
        discountPercent,
        extras,
        totalPrice:      currentCalcResult.finalTotal
    };

    sessionStorage.setItem('calculatorData', JSON.stringify(calcData));
    showNotification('Переходим к созданию заказа…', 'info');
    setTimeout(() => { window.location.href = '/orders?fromCalculator=1'; }, 600);
}

// Очистить калькулятор
function resetCalculator() {
    document.querySelectorAll('#servicesList input[type="checkbox"]').forEach(cb => cb.checked = false);
    document.getElementById('additionalWork').checked = false;
    document.getElementById('urgentOrder').checked = false;
    document.getElementById('warranty').checked = false;
    document.getElementById('discountPercent').value = 0;
    calculatePrice();
}

