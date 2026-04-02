# 🚀 БЫСТРЫЙ СТАРТ FixByte CRM

## 🆕 Актуальные изменения (30.03.2026)

- Добавлен нативный модуль `🗂️ Канбан` (`/kanban`) с персональными досками по пользователям.
- В канбане поддерживаются: мульти-доски, переименование доски/колонок, drag-and-drop карточек.
- Для карточек доступны вложения (фото + текстовые файлы), фото выводится как превью.
- Шаблоны UI переведены на общие Thymeleaf fragments (`head`, `sidebar`, `scripts`).
- Sidebar-меню централизовано и автоматически подсвечивает активный пункт.
- Исправлены проблемы рендера `/dashboard` и `/login` после шаблонного рефакторинга.
- Создан десктопный клиент для Windows (`desktop-client/`) на Electron 35.
- Собраны дистрибутивы: NSIS-установщик и Portable .exe (~88 МБ).
- Иконка генерируется автоматически скриптом `make-icon.js` (sharp + to-ico).

## Изменения (29.03.2026)

- Поменяна иконка плагина Заметок: `📝` (блокнот вместо пазла).
- Проведена чистка кода и удалены устаревшие файлы.
- Оптимизирована сборка (jar: 52.72 MB).

## Изменения (28.03.2026)

- Раздел `Пользователи` перенесен в `Настройки` (`/admin#tabUsers`).
- Добавлена страница `Настройки` (`/admin`) с вкладками:
  - `🧾 Настройки чека`
  - `🗄️ База данных`
  - `🔑 Пользователи`
- В заказах при статусе `COMPLETED` появилась кнопка `🧾 Напечатать чек`.
- Кнопка `📎 Сохранить PDF и прикрепить` в чеке прикрепляет PDF к заказу.
- В `Настройки → База данных`: резервная копия + восстановление.
- Группы услуг в форме заказа свёрнуты по умолчанию.
- Добавлен встроенный модуль `Чат` (`/chat`).

## 🚢 Релизные документы

- [RELEASE_NOTES_2026-03-30.md](RELEASE_NOTES_2026-03-30.md) ← новый
- [RELEASE_NOTES_2026-03-29.md](RELEASE_NOTES_2026-03-29.md)
- [RELEASE_NOTES_2026-03-28.md](RELEASE_NOTES_2026-03-28.md)
- [KNOWN_ISSUES.md](KNOWN_ISSUES.md)

## 🆕 Что изменилось в UI

- Логотип загружается из `src/main/resources/static/images/logo.png` и используется на всех страницах.
- На рабочих экранах добавлен переключатель темы: `Системная`, `Светлая`, `Темная`.
- Выбор темы сохраняется в браузере (`localStorage`, ключ `fixbyte-theme`).
- Авторизация использует значения из `.env` (`FIXBYTE_ADMIN_USERNAME`, `FIXBYTE_ADMIN_PASSWORD`, `FIXBYTE_OPERATOR_USERNAME`, `FIXBYTE_OPERATOR_PASSWORD`).
- Добавлен встроенный плагин `🧩 Заметки` (`/notes`) с хранением данных в БД.
- В заметках доступны 2 режима работы с Markdown:
  - `✍️ Редактирование`
  - `👁️ Превью`
- В `Настройки -> Плагины` администратор может полностью отключить встроенный плагин заметок.
- Добавлен встроенный модуль `💬 Чат` с историей диалогов в MariaDB и внешним iframe-виджетом для сайта.
- Добавлен встроенный раздел `🗂️ Канбан` с селектором досок и карточками по колонкам.

## ✅ Что было создано

Полноценное веб-приложение для управления сервисом ремонта ПК на **Spring Boot 3.2.0** с REST API и веб-интерфейсом.

### 📂 Структура проекта создана полностью:

```
C:\JavaProject\RemByte/
├── src/main/
│   ├── java/com/rembyte/
│   │   ├── RemByteApplication.java       ✅ Spring Boot приложение
│   │   ├── config/
│   │   │   └── ApplicationConfiguration.java
│   │   ├── model/                        ✅ 5 JPA сущностей
│   │   │   ├── Client.java
│   │   │   ├── Order.java
│   │   │   ├── RepairService.java
│   │   │   ├── Payment.java
│   │   │   ├── OrderStatus.java
│   │   │   └── PaymentMethod.java
│   │   ├── repository/                   ✅ 4 Spring Data JPA репозитория
│   │   ├── service/                      ✅ 5 сервис-классов с бизнес-логикой
│   │   └── controller/                   ✅ 4 REST контроллера
│   └── resources/
│       ├── application.properties        ✅ Конфигурация приложения
│       ├── static/
│       │   ├── css/style.css             ✅ Стили (300+ строк)
│       │   └── js/
│       │       ├── api.js                ✅ API клиент
│       │       ├── clients.js            ✅ Логика управления клиентами
│       │       ├── orders.js             ✅ Логика управления заказами
│       │       ├── services.js           ✅ Логика управления услугами
│       │       ├── dashboard.js          ✅ Логика панели управления
│       │       └── calculator.js         ✅ Логика калькулятора
│       └── templates/
│           ├── index.html                ✅ Главная страница
│           ├── dashboard.html            ✅ Панель управления
│           ├── clients.html              ✅ Управление клиентами
│           ├── orders.html               ✅ Управление заказами
│           ├── services.html             ✅ Управление услугами
│           └── calculator.html           ✅ Калькулятор стоимости
├── pom.xml                               ✅ Maven конфигурация
├── README.md                             ✅ Основная документация
├── COMPLETE_README.md                    ✅ Полная документация
└── KNOWN_ISSUES.md                       ✅ Известные ограничения и решения
```

---

## 🎯 Как запустить приложение

### Способ 1: Рекомендуемый (через spring-boot:run)

```bash
cd C:\JavaProject\RemByte
mvn spring-boot:run
```

**Преимущества:**
- ✅ Не требует полной сборки
- ✅ Автоматически обрабатывает Lombok аннотации
- ✅ Быстрее всего для разработки
- ✅ Код hot-reloadable

После запуска перейдите на: **http://localhost:9087**

### Способ 2: Через jar файл

```bash
cd C:\JavaProject\RemByte
mvn clean package -DskipTests
java -jar target/rembyte-crm-1.0.0.jar
```

### Способ 3: Из IntelliJ IDEA

1. Откройте проект в IntelliJ IDEA
2. File → Settings → Plugins → установить "Lombok"
3. File → Settings → Build → Compiler → Annotation Processors → ✅ Enable
4. Build → Rebuild Project
5. Run → Run 'RemByteApplication'

### Способ 4: Docker (app + MariaDB)

```powershell
cd C:\JavaProject\RemByte
docker compose up -d --build
```

Проверка логов приложения:

```powershell
docker compose logs -f app
```

Остановка:

```powershell
docker compose down
```

---

## 🖥️ Способ 5: Десктопный клиент (Windows)

Запустить CRM как нативное Windows-приложение (открывает `https://crm.fix-byte.ru`):

```powershell
cd C:\JavaProject\RemByte\desktop-client

# Установить зависимости (один раз)
npm install

# Сгенерировать иконку (один раз, если build/icon.ico отсутствует)
node make-icon.js

# Запустить
npm start
```

Собрать .exe установщик:
```powershell
npm run build:win
# Результат: dist/FixByte CRM Setup 1.0.0.exe
#            dist/FixByte-CRM-Portable-1.0.0.exe
```

Подробнее: [`desktop-client/README.md`](desktop-client/README.md)

---

## 🔧 Функциональность

### 1. CRM система (👥 Клиенты)
- ➕ Добавление клиентов с контактной информацией
- 🔍 Поиск по имени, телефону, email
- ✏️ Редактирование информации
- 📊 История заказов клиента
- 🗑️ Удаление и деактивация

### 2. Учет заказов (📋 Заказы)
- ➕ Создание новых заказов
- 📝 Добавление описания проблемы
- 🔧 Выбор услуг из каталога
- ⚡ Быстрое добавление новой услуги прямо в форме заказа
- 🗂️ Группы услуг свёрнуты по умолчанию для компактного отображения
- 💰 Автоматический расчет стоимости
- 📊 Отслеживание статуса (NEW → IN_PROGRESS → READY → COMPLETED)
- 💵 Управление платежами
- 🔍 Поиск и фильтрация

### 3. Управление услугами (🔧 Услуги)
- ➕ Добавление новых услуг
- 💰 Установка цен
- 🏷️ Группировка по категориям:
  - BGA (3500₽)
  - Экран (2000₽)
  - Батарея (1500₽)
  - Материнская плата (5000₽)
  - Обслуживание (500₽)
  - Хранилище (1200₽)
  - Восстановление данных (2500₽)
  - ПО (800₽)

### 4. Калькулятор стоимости (💰 Калькулятор)
- ✔️ Выбор услуг
- ➕ Дополнения (диагностика, срочность, гарантия)
- 💸 Скидки в процентах
- 📊 Итоговая стоимость в реальном времени
- 📋 Прайс-лист

### 5. Панель управления (📊 Dashboard)
- 📈 Общая статистика
- 📋 Последние заказы
- 🏆 Популярные услуги
- 💹 Финансовая статистика

### 6. Онлайн-чат (💬 Чат)
- 💬 Прием сообщений с внешнего сайта прямо в CRM
- 👨‍🔧 Ответы оператора из CRM в реальном времени через polling
- 🔔 Счетчик непрочитанных диалогов в левом меню
- 🔒 Белый список разрешенных `origin` для безопасного встраивания виджета
- ⚙️ Генерация готового script-кода в `Настройки -> Плагины`

### 7. Канбан (🗂️ Канбан)
- 🗂️ Несколько досок на одного пользователя (персональные доски)
- ✏️ Переименование доски и колонок
- 🧲 Drag-and-drop перемещение карточек между колонками
- 📎 Вложения карточек: изображения и текстовые файлы (`.txt`, `.md`, `.csv`, `.log`)
- 🖼️ Превью первого изображения прямо на карточке
- 💾 Хранение данных в MariaDB (`kanban_boards`, `kanban_columns`, `kanban_cards`, `kanban_card_attachments`)

---

## 📡 REST API

Все операции доступны через REST API:

```
GET    /api/clients              # Получить всех клиентов
POST   /api/clients              # Создать клиента
PUT    /api/clients/{id}         # Обновить клиента

GET    /api/orders               # Все заказы
POST   /api/orders               # Создать заказ
PUT    /api/orders/{id}/status   # Изменить статус
POST   /api/orders/{id}/attachments # Прикрепить фото/видео/документы (в т.ч. PDF-чек)

GET    /api/services             # Все услуги
POST   /api/services             # Создать услугу

GET    /api/notes-plugin/folders                   # Папки заметок
POST   /api/notes-plugin/folders                   # Создать папку
GET    /api/notes-plugin/folders/{folderId}/notes  # Заметки папки
POST   /api/notes-plugin/folders/{folderId}/notes  # Создать заметку
PUT    /api/notes-plugin/notes/{noteId}            # Обновить заметку
DELETE /api/notes-plugin/notes/{noteId}            # Удалить заметку
GET    /api/notes-plugin/notes/{noteId}/download   # Скачать заметку .txt

GET    /api/plugin-settings/notes   # Статус встроенного плагина заметок
PUT    /api/plugin-settings/notes   # Вкл/выкл плагин заметок (ADMIN)

GET    /api/kanban/boards                         # Список досок текущего пользователя
POST   /api/kanban/boards                         # Создать доску
PUT    /api/kanban/boards/{boardId}               # Переименовать доску
GET    /api/kanban/board?boardId={boardId}        # Получить доску с колонками/карточками
PUT    /api/kanban/columns/{columnId}             # Переименовать колонку
POST   /api/kanban/columns/{columnId}/cards       # Создать карточку
PUT    /api/kanban/cards/{cardId}                 # Обновить карточку
PUT    /api/kanban/cards/{cardId}/move            # Переместить карточку
DELETE /api/kanban/cards/{cardId}                 # Удалить карточку
POST   /api/kanban/cards/{cardId}/attachments     # Загрузить вложения карточки
GET    /api/kanban/cards/{cardId}/attachments     # Получить вложения карточки
DELETE /api/kanban/cards/{cardId}/attachments     # Удалить вложение карточки

GET    /api/chat/conversations             # Все диалоги чата
GET    /api/chat/conversations/{id}        # Диалог и сообщения
POST   /api/chat/conversations/{id}/messages # Ответ оператора
PUT    /api/chat/conversations/{id}/status # Открыть/закрыть диалог
GET    /api/chat/summary                   # Сводка по непрочитанным диалогам
GET    /api/chat/widget-site               # Настройки виджета чата (ADMIN)
PUT    /api/chat/widget-site               # Сохранить настройки виджета (ADMIN)

GET    /public/chat/site/{siteKey}                    # Публичная конфигурация виджета
POST   /public/chat/conversations                     # Создать новый диалог с сайта
GET    /public/chat/conversations/{publicToken}       # Получить диалог по токену
POST   /public/chat/conversations/{publicToken}/messages # Сообщение посетителя

GET    /admin/backup?mode=full   # Скачать полный ZIP-бэкап (БД + legacy uploads) (ADMIN)
GET    /admin/backup?mode=db     # Скачать ZIP-бэкап только БД (ADMIN)
POST   /admin/restore            # Восстановить из ZIP или SQL (ADMIN)
```

---

## ⚠️ Если возникла ошибка при сборке

### Проблема: "cannot find symbol: method setName()"

Это означает, что Lombok не генерирует методы. **Решения:**

**1. Используйте spring-boot:run** (Рекомендуется):
```bash
mvn spring-boot:run
```

**2. Установите Lombok plugin в IntelliJ IDEA**:
- File → Settings → Plugins → "Lombok" → Install
- File → Settings → Build → Compiler → Annotation Processors → ✅ Enable
- Build → Rebuild Project

**3. Полностью очистите кэш**:
```bash
mvn clean
rmdir %USERPROFILE%\.m2\repository\org\projectlombok /s
mvn compile
```

Если проблема сохраняется, проверьте раздел ограничений и диагностики в **KNOWN_ISSUES.md**.

---

## ✅ Проверка тестов (рекомендуется перед коммитом)

```bash
cd C:\JavaProject\RemByte
mvn test
```

На текущем состоянии проекта проходят:
- `com.rembyte.controller.WebRouteUniquenessTest`
- `com.rembyte.service.ChatServiceTest`
- `com.rembyte.service.LegacyAttachmentMigrationServiceTest`

---

## 💾 База данных

**По умолчанию:** MariaDB (`localhost:9092`) + переменные из `.env`.

- Вложения заказов (фото/видео/документы/PDF-чек) сохраняются в MariaDB, таблица `order_attachments` (`LONGBLOB`).
- Для Docker это означает, что отдельный том для папки `uploads` не обязателен: достаточно volume MariaDB.
- При старте выполняется автоперенос legacy-файлов из `uploads/orders/**` в БД (без дублей).
- Управление автопереносом: `FIXBYTE_UPLOAD_MIGRATION_ENABLED` и `FIXBYTE_UPLOAD_MIGRATION_DELETE_LEGACY`.
- Диалоги чата и сообщения виджета тоже сохраняются в MariaDB.

### Вставка виджета чата на сайт

Готовый код можно скопировать в `Настройки -> Плагины -> Код вставки на сайт`.

Пример:

```html
<script
  src="https://ваш-домен/js/chat-widget-loader.js"
  data-chat-site-key="main-site"
  data-chat-base-url="https://ваш-домен">
</script>
```

⚠️ Восстановление через `/admin/restore` перезаписывает текущие данные полностью.

Пример `.env`:

```properties
SPRING_PROFILES_ACTIVE=dev
MARIADB_URL=jdbc:mariadb://localhost:9092/rembyte?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8
MARIADB_USERNAME=root
MARIADB_PASSWORD=***
FIXBYTE_ADMIN_USERNAME=admin
FIXBYTE_ADMIN_PASSWORD=***
FIXBYTE_OPERATOR_USERNAME=operator
FIXBYTE_OPERATOR_PASSWORD=***
```

---

## 🎓 Примеры использования

### Пример 1: Создание заказа

1. **Главная страница** → "Клиенты"
2. Нажать "+ Создать нового клиента"
3. Заполнить:
   - Имя: "Иван Иванов"
   - Телефон: "+7 (999) 123-45-67"
   - Email: "ivan@example.com"
4. Нажать "Создать клиента"

5. **Главная страница** → "Заказы"
6. Нажать "+ Создать новый заказ"
7. Выбрать клиента "Иван Иванов"
8. Описание: "Ноутбук не включается"
9. Выбрать услуги:
   - ☑️ Замена BGA микросхемы (3500₽)
   - ☑️ Чистка и смена термопасты (500₽)
10. **Итого: 4000₽**
11. Нажать "Создать заказ"

### Пример 2: Расчет стоимости

1. **Главная страница** → "Калькулятор"
2. Выбрать услуги из списка
3. Отметить опции:
   - ☑️ Срочность (+20%)
   - ☑️ Гарантия (+15%)
4. Введить скидку (например, 10%)
5. **Видеть итоговую стоимость в реальном времени**

---

## 📊 Технологический стек

| Компонент | Версия |
|-----------|--------|
| Java | 17 |
| Spring Boot | 3.2.0 |
| Spring Data JPA | 3.2.0 |
| Lombok | 1.18.30 |
| MariaDB | 10.5+ |
| Maven | 3.8+ |
| HTML5/CSS3 | Latest |
| JavaScript ES6+ | Latest |

---

## 📄 Документация

- 📖 **README.md** - Основная документация
- 📖 **COMPLETE_README.md** - Полная документация со всеми деталями
- 📖 **KNOWN_ISSUES.md** - Известные ограничения и диагностика

---

## ✨ Заключение

Приложение **RemByte CRM** полностью готово к использованию! 

### Быстро проверить:

```bash
# 1. Перейти в директорию
cd C:\JavaProject\RemByte

# 2. Запустить приложение
mvn spring-boot:run

# 3. Открыть браузер
# http://localhost:9087
```

### Приложение предоставляет:

✅ Полный REST API  
✅ Веб-интерфейс на HTML/CSS/JS  
✅ Управление клиентами  
✅ Учет заказов  
✅ Калькулятор стоимости  
✅ Финансовую статистику  
✅ Автоматическую инициализацию данных  

---

**Удачи в использовании RemByte CRM! 🚀**
