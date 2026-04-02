# ✅ ЧЕКЛИСТ БЕЗОПАСНОСТИ - CSRF ЗАЩИТА

**Версия:** 1.0
**Дата:** 30 марта 2026

---

## 📋 ПРОВЕРКА РЕАЛИЗАЦИИ

### Этап 1: Проверка JavaScript

- [ ] **Откройте DevTools** (F12 в браузере)
- [ ] **Консоль:** Выполните команду:
  ```javascript
  getCsrfToken()
  ```
  - [ ] Должна вернуть строку с токеном (не пустую)

- [ ] **Консоль:** Выполните команду:
  ```javascript
  getCsrfHeaderName()
  ```
  - [ ] Должна вернуть `"X-CSRF-TOKEN"`

- [ ] **Консоль:** Выполните команду:
  ```javascript
  getSecureHeaders()
  ```
  - [ ] Должна вернуть объект с Content-Type и X-CSRF-TOKEN

---

### Этап 2: Проверка HTML meta-тегов

**Откройте исходный код страницы (Ctrl+U или F12 → Elements)**

- [ ] Найдите в `<head>`:
  ```html
  <meta name="_csrf" content="...">
  ```
  - [ ] Значение `content` должно быть непустым токеном

- [ ] Найдите в `<head>`:
  ```html
  <meta name="_csrf_parameter_name" content="...">
  ```
  - [ ] Обычно содержит `"_csrf"`

- [ ] Найдите в `<head>`:
  ```html
  <meta name="_csrf_header_name" content="X-CSRF-TOKEN">
  ```

---

### Этап 3: Проверка Network запросов

**DevTools → Network tab**

1. **Создайте/обновите/удалите** любой элемент (клиент, заказ, карточку и т.д.)
2. **Найдите POST/PUT/DELETE запрос** в списке (обычно к `/api/...`)
3. **Кликните на запрос**
4. **Перейдите на вкладку Headers**
5. **Проверьте Request Headers:**

   - [ ] **Content-Type:** `application/json`
   - [ ] **X-CSRF-TOKEN:** `xxxxxxxx...` (должен совпадать с токеном из meta)

**Пример хорошего запроса:**
```
POST /api/clients HTTP/1.1
Host: localhost:8080
Content-Type: application/json
X-CSRF-TOKEN: 2d3d4e5f-6g7h-8i9j-0k1l-2m3n4o5p6q7r
Content-Length: 120

{"name":"John Doe","phone":"+7..."}
```

---

### Этап 4: Проверка всех компонентов

#### Клиенты (Clients)
- [ ] **Создать** клиента → должен быть X-CSRF-TOKEN в POST
- [ ] **Обновить** клиента → должен быть X-CSRF-TOKEN в PUT
- [ ] **Удалить** клиента → должен быть X-CSRF-TOKEN в DELETE

#### Заказы (Orders)
- [ ] **Создать** заказ → POST с CSRF
- [ ] **Обновить** заказ → PUT с CSRF
- [ ] **Изменить статус** → PUT с CSRF
- [ ] **Добавить платеж** → POST с CSRF
- [ ] **Загрузить вложения** → POST с CSRF (проверьте в Headers)
- [ ] **Удалить вложение** → DELETE с CSRF
- [ ] **Удалить заказ** → DELETE с CSRF

#### Услуги (Services)
- [ ] **Создать** услугу → POST с CSRF
- [ ] **Обновить** услугу → PUT с CSRF
- [ ] **Удалить** услугу → DELETE с CSRF

#### Заметки (Notes)
- [ ] **Создать папку** → POST с CSRF
- [ ] **Создать заметку** → POST с CSRF
- [ ] **Обновить заметку** → PUT с CSRF
- [ ] **Удалить заметку** → DELETE с CSRF

#### Канбан (Kanban)
- [ ] **Создать доску** → POST с CSRF
- [ ] **Переименовать доску** → PUT с CSRF
- [ ] **Создать карточку** → POST с CSRF
- [ ] **Обновить карточку** → PUT с CSRF
- [ ] **Переместить карточку** → PUT с CSRF
- [ ] **Удалить карточку** → DELETE с CSRF
- [ ] **Загрузить вложение** → POST с CSRF (в Headers)
- [ ] **Удалить вложение** → DELETE с CSRF

#### Чат (Chat)
- [ ] **Отправить сообщение** → POST с CSRF
- [ ] **Изменить статус** → PUT с CSRF
- [ ] **Отметить как прочитано** → POST с CSRF

---

## 🧪 ТЕСТ НА УЯЗВИМОСТЬ (НЕ ВЫПОЛНЯТЬ В PRODUCTION!)

**Этот тест демонстрирует, что теперь защита работает:**

### Способ 1: Консоль JavaScript

```javascript
// Попробуйте отправить запрос БЕЗ правильного CSRF токена
fetch('/api/clients', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'X-CSRF-TOKEN': 'wrong-token-12345'  // ❌ Неправильный токен
    },
    body: JSON.stringify({ name: 'Test' })
}).then(r => r.json()).then(console.log)

// Ожидаемый результат: 403 Forbidden (CSRF validation failed)
```

### Способ 2: curl команда

```bash
curl -X POST http://localhost:8080/api/clients \
  -H "Content-Type: application/json" \
  -H "X-CSRF-TOKEN: wrong-token-12345" \
  -d '{"name":"Test"}'

# Ожидаемый результат: 403 Forbidden
```

---

## ⚠️ ЧАСТЫЕ ПРОБЛЕМЫ И РЕШЕНИЯ

### Проблема 1: "Invalid CSRF token"

**Причины:**
- [ ] CSRF токен истёк (обновите страницу)
- [ ] Токен передан в неправильном заголовке
- [ ] Meta-теги не добавлены в HTML

**Решение:**
```javascript
// Убедитесь, что используете getSecureHeaders()
headers: getSecureHeaders()  // ✅ Правильно
headers: { 'Content-Type': 'application/json' }  // ❌ Неправильно
```

### Проблема 2: "getCsrfToken is not defined"

**Причины:**
- [ ] api.js не загружен
- [ ] Скрипты загружаются в неправильном порядке

**Решение:**
```html
<!-- В конце body, в правильном порядке: -->
<script src="/js/api.js"></script>      <!-- Сначала api.js -->
<script src="/js/clients.js"></script>  <!-- Потом остальные -->
```

### Проблема 3: FormData запросы не отправляют CSRF

**Причина:** Забыли добавить CSRF токен в заголовки

**Решение:**
```javascript
// ✅ Правильно: добавляем CSRF в headers
const headers = {};
const csrf = getCsrfToken();
if (csrf) {
    headers[getCsrfHeaderName()] = csrf;
}
const response = await fetch(url, {
    method: 'POST',
    headers: headers,  // ✅ CSRF в headers
    body: formData
});
```

---

## 📊 РЕЗУЛЬТАТЫ ПРОВЕРКИ

### Заполните эту таблицу после проверки:

| Компонент | GET | POST | PUT | DELETE | Статус |
|-----------|-----|------|-----|--------|--------|
| Clients | ✅ | ✅ | ✅ | ✅ | |
| Orders | ✅ | ✅ | ✅ | ✅ | |
| Services | ✅ | ✅ | ✅ | ✅ | |
| Notes | ✅ | ✅ | ✅ | ✅ | |
| Kanban | ✅ | ✅ | ✅ | ✅ | |
| Chat | ✅ | ✅ | ✅ | ✅ | |

---

## ✅ ФИНАЛЬНАЯ ПРОВЕРКА

Когда все чекпоинты пройдены, заполните:

- [ ] **Дата проверки:** ____________________
- [ ] **Проверяющий:** ____________________
- [ ] **Все запросы отправляют CSRF токены:** ДА / НЕТ
- [ ] **Все meta-теги присутствуют:** ДА / НЕТ
- [ ] **Функции getCsrfToken() и getSecureHeaders() работают:** ДА / НЕТ
- [ ] **Тесты безопасности пройдены:** ДА / НЕТ

---

**СТАТУС БЕЗОПАСНОСТИ:** 🟢 **ЗАЩИЩЕНО** / 🟡 **ТРЕБУЕТ ВНИМАНИЯ** / 🔴 **УЯЗВИМО**

---

**Спасибо за проверку безопасности приложения! 🛡️**

