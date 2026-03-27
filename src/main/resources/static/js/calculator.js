/**
 * JavaScript код для калькулятора стоимости
 */

let selectedServices = new Map();

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

// Отобразить услуги в калькуляторе
function renderCalculatorServices(services) {
    const container = document.getElementById('servicesList');
    
    container.innerHTML = services.map(service => `
        <label class="service-item">
            <input type="checkbox" value="${service.id}" data-price="${service.basePrice}" 
                   data-name="${service.name}" onchange="updateCalculatorPrice()">
            <span><strong>${service.name}</strong></span>
            <span style="margin-left: auto;">${formatCurrency(service.basePrice)}</span>
        </label>
    `).join('');
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
        const price = parseFloat(checkbox.dataset.price);
        const name = checkbox.dataset.name;
        servicesTotal += price;
        selectedServices.set(checkbox.value, { name, price });
    });

    // Дополнительные услуги
    let additionalTotal = 0;
    
    if (document.getElementById('additionalWork').checked) {
        additionalTotal += 500;
    }

    // Скидка
    const discountPercent = parseFloat(document.getElementById('discountPercent').value) || 0;
    let subtotal = servicesTotal + additionalTotal;
    let discountAmount = (subtotal * discountPercent) / 100;

    // Срочность (применяется перед скидкой)
    if (document.getElementById('urgentOrder').checked) {
        additionalTotal += servicesTotal * 0.2; // 20% от услуг
    }

    // Гарантия (применяется перед скидкой)
    if (document.getElementById('warranty').checked) {
        additionalTotal += servicesTotal * 0.15; // 15% от услуг
    }

    // Пересчитать с учетом всех дополнений
    subtotal = servicesTotal + additionalTotal;
    discountAmount = (subtotal * discountPercent) / 100;
    const finalTotal = subtotal - discountAmount;

    // Обновить отображение
    document.getElementById('servicesTotal').textContent = formatCurrency(servicesTotal);
    document.getElementById('additionalTotal').textContent = formatCurrency(additionalTotal);
    document.getElementById('discountTotal').textContent = '-' + formatCurrency(discountAmount);
    document.getElementById('finalTotal').textContent = formatCurrency(finalTotal);
}

// Обновить цену калькулятора
function updateCalculatorPrice() {
    calculatePrice();
}

// Слушатели для дополнительных параметров
document.addEventListener('DOMContentLoaded', () => {
    const additionalWork = document.getElementById('additionalWork');
    const urgentOrder = document.getElementById('urgentOrder');
    const warranty = document.getElementById('warranty');
    const discountPercent = document.getElementById('discountPercent');

    if (additionalWork) additionalWork.addEventListener('change', calculatePrice);
    if (urgentOrder) urgentOrder.addEventListener('change', calculatePrice);
    if (warranty) warranty.addEventListener('change', calculatePrice);
    if (discountPercent) discountPercent.addEventListener('change', calculatePrice);
});

// Создать заказ на основе расчета
async function createOrderFromCalculator() {
    if (selectedServices.size === 0) {
        alert('Пожалуйста, выберите хотя бы одну услугу');
        return;
    }

    // Получить список выбранных услуг
    const serviceIds = Array.from(selectedServices.keys());
    
    alert(`Выбранные услуги:\n${Array.from(selectedServices.values()).map(s => `• ${s.name} - ${formatCurrency(s.price)}`).join('\n')}\n\nУ вас должен быть выбран клиент перед созданием заказа.\nПерейдите в раздел "Заказы" и создайте новый заказ.`);
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

