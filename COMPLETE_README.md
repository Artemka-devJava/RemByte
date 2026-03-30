# 🔧 FixByte CRM — Полная документация

## 🆕 Актуальные изменения (30.03.2026)

- Добавлен нативный модуль `🗂️ Канбан` (`/kanban`) с персональными досками пользователей.
- Для канбана поддержаны мульти-доски, переименование досок/колонок и drag-and-drop карточек.
- Для карточек канбана добавлены вложения (изображения + текстовые файлы) и превью первого изображения.
- UI-шаблоны унифицированы через `templates/fragments/*` (единые head/sidebar/scripts).
- Sidebar-меню переведено на один источник с автоматической подсветкой active-пункта.
- Исправлены edge-case ошибки рендера `/dashboard` и `/login` после шаблонного рефакторинга.
- Создан десктопный клиент для Windows (`desktop-client/`) на Electron 35.
- Иконка генерируется автоматически скриптом `make-icon.js` (sharp + to-ico).
- В заказах группы услуг по умолчанию свёрнуты для компактного отображения.
- В форме заказа доступно быстрое добавление новой услуги.
- Авторизация и доступ к БД берутся из `.env`.
- Единый логотип на всех страницах: `static/images/logo.png`.

## 📋 Описание проекта

**FixByte CRM** — веб-приложение для управления бизнесом по ремонту ПК и ноутбуков на **Spring Boot 3.2.0**.

### Основные функции:

✅ **CRM система** — управление клиентами, контакты, история заказов  
✅ **Учет заказов** — создание, статусы, платежи, вложения (фото/видео/PDF)  
✅ **Калькулятор стоимости** — расчёт с учётом скидок и дополнений  
✅ **Каталог услуг** — управление услугами и ценами по категориям  
✅ **Настройки** — чек/квитанция, пользователи, бэкап и восстановление БД  
✅ **Плагины** — загрузка из файла, изоляция в iframe sandbox  
✅ **Заметки** — встроенный плагин с папками и Markdown-редактором  
✅ **Чат** — онлайн-виджет для сайта + операторский интерфейс  
✅ **Канбан** — персональные доски, колонки, карточки, вложения и превью  
✅ **REST API** — 50+ endpoints для интеграции  
✅ **Десктоп** — Windows-клиент на Electron 35  

---

## 🏗️ Архитектура проекта

```
RemByte/
├── src/
│   ├── main/
│   │   ├── java/com/rembyte/
│   │   │   ├── RemByteApplication.java          # Точка входа
│   │   │   ├── config/
│   │   │   │   └── SecurityConfig.java           # Spring Security
│   │   │   ├── model/                            # JPA сущности (13)
│   │   │   │   ├── Client.java
│   │   │   │   ├── Order.java
│   │   │   │   ├── RepairService.java
│   │   │   │   ├── Payment.java
│   │   │   │   ├── OrderAttachment.java
│   │   │   │   ├── NoteFolder.java
│   │   │   │   ├── NoteItem.java
│   │   │   │   ├── ChatConversation.java
│   │   │   │   ├── ChatMessage.java
│   │   │   │   ├── Plugin.java
│   │   │   │   ├── PluginSetting.java
│   │   │   │   ├── ReceiptSettings.java
│   │   │   │   └── User.java
│   │   │   ├── repository/                       # JPA репозитории (12)
│   │   │   ├── service/                          # Бизнес-логика (12)
│   │   │   │   ├── ClientService.java
│   │   │   │   ├── OrderService.java
│   │   │   │   ├── RepairServiceService.java
│   │   │   │   ├── PaymentService.java
│   │   │   │   ├── FileStorageService.java
│   │   │   │   ├── NoteService.java
│   │   │   │   ├── ChatService.java
│   │   │   │   ├── PluginService.java
│   │   │   │   ├── ReceiptSettingsService.java
│   │   │   │   ├── UserService.java
│   │   │   │   ├── OrderStatistics.java
│   │   │   │   └── BackupService.java
│   │   │   └── controller/                       # REST + Web контроллеры (10)
│   │   │       ├── ClientController.java
│   │   │       ├── OrderController.java
│   │   │       ├── RepairServiceController.java
│   │   │       ├── AdminController.java
│   │   │       ├── PluginController.java
│   │   │       ├── NotePluginController.java
│   │   │       ├── ChatController.java
│   │   │       ├── ChatWidgetController.java
│   │   │       ├── FileController.java
│   │   │       └── WebController.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       ├── application-docker.properties
│   │       ├── static/
│   │       │   ├── css/style.css               # Стили + темы
│   │       │   ├── js/                         # 11+ JS модулей
│   │       │   │   ├── plugin-host.js
│   │       │   │   ├── notes.js
│   │       │   │   ├── chat.js
│   │       │   │   └── ...
│   │       │   └── images/
│   │       │       ├── logo.png
│   │       │       └── favicon.ico
│   │       └── templates/                      # 13 Thymeleaf шаблонов
│   │           ├── index.html
│   │           ├── dashboard.html
│   │           ├── clients.html
│   │           ├── orders.html
│   │           ├── services.html
│   │           ├── calculator.html
│   │           ├── admin.html
│   │           ├── plugins.html
│   │           ├── notes.html
│   │           ├── chat.html
│   │           ├── chat-widget-frame.html
│   │           ├── login.html
│   │           └── users.html
│   └── test/
│       └── java/com/rembyte/
│           └── RouteUniquenessTest.java          # Авто-тест уникальности маршрутов
├── desktop-client/                               # Electron Windows-клиент
│   ├── main.js
│   ├── preload.js
│   ├── splash.html
│   ├── make-icon.js
│   ├── package.json
│   └── build/
│       ├── icon.ico                              # Генерируется make-icon.js
│       └── logo.png
├── docker/
│   └── mariadb/init/01_init.sql
├── plugin-samples/
│   ├── snake-game-plugin.html
│   ├── tetris-game-plugin.html
│   └── text-editor-plugin.html
├── docker-compose.yml
├── Dockerfile
├── .env
└── pom.xml
```

---

## 🚀 Быстрый старт

### Требования

- **Java 17+** (скачать с [Oracle JDK](https://www.oracle.com/java/technologies/downloads/))
- **Maven 3.8.1+** (скачать с [Apache Maven](https://maven.apache.org/download.cgi))
- **Git** (опционально)

### Сборка и запуск

```bash
# 1. Перейти в директорию проекта
cd C:\JavaProject\RemByte

# 2. Очистить и собрать проект
mvn clean package -DskipTests

# 3. Запустить приложение
java -jar target/rembyte-crm-3.0.jar

# Или используй spring-boot:run для разработки
mvn spring-boot:run
```

### Доступ к приложению

После запуска приложение будет доступно по адресу:
```
http://localhost:9087
```

---

## 📱 Основные модули

### 1. 🎯 Главная страница
- Обзор возможностей системы
- Быстрая навигация
- Статус приложения

### 2. 📊 Панель управления (Dashboard)
- **Общая статистика:**
  - Всего клиентов
  - Активные заказы
  - Выручка за текущий месяц
  - Оплачено в текущем месяце
  
- **Последние заказы** - таблица с последними заказами
- **Популярные услуги** - топ услуг по количеству заказов
- **Финансовая статистика** - доход по месяцам

### 3. 👥 Управление клиентами
**CRUD операции с клиентами:**
- ➕ Добавить нового клиента
- 🔍 Поиск по имени, телефону, email
- ✏️ Редактирование информации
- 🗑️ Удаление клиента
- 📊 Просмотр истории заказов

**Поля клиента:**
- ФИО / Название организации
- Номер телефона (уникальный)
- Email
- Адрес
- Заметки
- Статус (активный/неактивный)
- Дата добавления

### 4. 📋 Управление заказами
**Полный цикл жизни заказа:**

**Статусы заказа:**
- `NEW` - Новый заказ (создан)
- `IN_PROGRESS` - В работе (выполняется ремонт)
- `WAITING_FOR_PARTS` - Ожидание деталей
- `READY` - Готов к выдаче
- `COMPLETED` - Завершен и выдан
- `CANCELLED` - Отменен

**Функции:**
- ➕ Создание нового заказа
- 🔍 Поиск по номеру или клиенту
- 📊 Фильтрация по статусу
- 💰 Управление платежами
- 📝 Добавление услуг
- ⚡ Быстрое создание новой услуги прямо в форме заказа
- 🗂️ Компактные сворачиваемые группы услуг в форме заказа
- ✏️ Редактирование
- 🗑️ Удаление

### 5. 🔧 Управление услугами
**Каталог услуг ремонта:**

**Категории услуг:**
- **BGA** - Замена микросхем BGA (видеокарта, чипсет)
- **Экран** - Замена дисплеев и матриц
- **Батарея** - Замена аккумуляторов
- **Материнская плата** - Замена МП
- **Обслуживание** - Чистка, смена термопасты
- **Хранилище** - Замена HDD на SSD
- **Данные** - Восстановление данных
- **Программное обеспечение** - Переустановка ОС

**Функции:**
- ➕ Добавить услугу
- 💰 Установить цену
- 🏷️ Указать категорию
- 📝 Описание услуги
- ✏️ Редактирование
- 🗑️ Удаление

### 6. 💰 Калькулятор стоимости
**Расчет стоимости ремонта:**

- ✔️ Выбор услуг из каталога
- ➕ Добавление дополнений:
  - Диагностика (+500₽)
  - Срочность (+20% от услуг)
  - Гарантия на работу (+15% от услуг)
- 💸 Применение скидок (в процентах)
- 📊 Итоговая стоимость в реальном времени
- 📋 Прайс-лист всех услуг

---

## 🔐 REST API

### Клиенты

```
GET    /api/clients                          # Получить всех клиентов
GET    /api/clients/active                   # Получить активных
GET    /api/clients/{id}                     # Получить по ID
GET    /api/clients/search?name={name}       # Поиск по имени
POST   /api/clients                          # Создать клиента
PUT    /api/clients/{id}                     # Обновить
DELETE /api/clients/{id}                     # Удалить
```

### Заказы

```
GET    /api/orders                           # Все заказы
GET    /api/orders/{id}                      # По ID
GET    /api/orders/client/{clientId}         # Заказы клиента
GET    /api/orders/number/{orderNumber}      # По номеру
GET    /api/orders/statistics?from=...&to=...# Статистика
POST   /api/orders                           # Создать
PUT    /api/orders/{id}                      # Обновить
PUT    /api/orders/{id}/status?status=...    # Изменить статус
POST   /api/orders/{id}/payment?amount=...   # Добавить платеж
DELETE /api/orders/{id}                      # Удалить
```

### Услуги

```
GET    /api/services                         # Все услуги
GET    /api/services/active                  # Активные
GET    /api/services/{id}                    # По ID
GET    /api/services/category/{category}     # По категории
POST   /api/services                         # Создать
PUT    /api/services/{id}                    # Обновить
DELETE /api/services/{id}                    # Удалить
```

---

## 💾 База данных

Используется **MariaDB** с profile-based конфигурацией.

- `application.properties` содержит общие настройки и `spring.config.import=optional:file:.env[.properties]`
- `application-dev.properties` — локальный профиль (`ddl-auto=update`)
- `application-prod.properties` — production профиль (`ddl-auto=validate`)

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

## 📊 Пример использования

### Сценарий 1: Создание заказа

1. **Добавить клиента** (раздел "Клиенты")
   - Имя: "Иван Петров"
   - Телефон: "+7 (999) 123-45-67"
   - Email: "ivan@example.com"

2. **Создать заказ** (раздел "Заказы")
   - Выбрать клиента: "Иван Петров"
   - Описание: "Ноутбук не включается, вероятно проблема с видеокартой"
   - Выбрать услуги:
     - ☑️ Замена BGA микросхемы (3500₽)
     - ☑️ Чистка и смена термопасты (500₽)
   - **Итого: 4000₽**
   - Создать заказ

3. **Отслеживать статус**
   - Статус изменяется: NEW → IN_PROGRESS → READY → COMPLETED
   - Добавить платеж по мере выполнения

4. **Смотреть статистику** в панели управления

---

## 🛠️ Разработка

### Структура кода

**Models** (Entity classes):
- Все бизнес-сущности с JPA аннотациями
- Использует Lombok для генерации getter/setter

**Repositories** (Data Access Layer):
- Spring Data JPA репозитории
- Готовые методы для поиска и фильтрации

**Services** (Business Logic Layer):
- Вся бизнес-логика
- Транзакции и валидация
- Расчеты и преобразования

**Controllers** (REST API Layer):
- REST endpoints
- Обработка HTTP запросов
- Сериализация/десериализация JSON

**Frontend** (HTML/CSS/JS):
- Vanilla JavaScript (без framework)
- Fetch API для общения с REST API
- Модальные окна для форм
- Таблицы с фильтрацией

---

## 🐛 Решение проблем

### Ошибка: "Address already in use"
Другой процесс занимает порт 9087. Измените порт в `application.properties`:
```properties
server.port=9088
```

### Ошибка: "Access denied for user" / "Unknown database"
Проверьте значения в `.env` и доступность MariaDB:
```properties
MARIADB_URL=jdbc:mariadb://localhost:9092/rembyte?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8
MARIADB_USERNAME=root
MARIADB_PASSWORD=***
```

### Приложение медленно загружается
При первом запуске Maven загружает все зависимости. Это нормально.

---

## 📦 Развертывание

### На локальной машине
```bash
java -jar target/rembyte-crm-3.0.jar
```

### На сервере Linux/Mac
```bash
# Предоставить права на выполнение
chmod +x rembyte-crm-3.0.jar

# Запустить в фоновом режиме
nohup java -jar rembyte-crm-3.0.jar &

# Или через systemd
sudo nano /etc/systemd/system/rembyte.service
# [Unit]
# Description=RemByte CRM Service
# After=network.target
# [Service]
# Type=simple
# ExecStart=/usr/bin/java -jar /path/to/rembyte-crm-3.0.jar
# Restart=on-failure
```

### Через Docker
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/rembyte-crm-3.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

```bash
docker build -t rembyte:3.0 .
docker run -p 9087:9087 rembyte:3.0
```

---

## 📚 Технологический стек

| Компонент | Версия | Назначение |
|-----------|--------|-----------|
| Java | 17 LTS | Язык программирования |
| Spring Boot | 3.2.0 | Web framework |
| Spring Data JPA | 3.2.0 | ORM |
| Spring Security | 3.2.0 | Аутентификация / авторизация |
| Lombok | 1.18.30 | Генерация кода |
| MariaDB | 10.5+ / 11.4 Docker | База данных |
| Maven | 3.8+ | Сборка |
| Thymeleaf | 3.2.0 | Server-side шаблонизация |
| HTML5/CSS3/JS | ES6+ | Frontend |
| Electron | 35.x | Windows десктопный клиент |
| electron-builder | 26.x | Сборка .exe установщика |
| sharp + to-ico | latest | Генерация иконки ICO |

---

## 🖥️ Десктопный клиент (Windows)

Папка `desktop-client/` содержит Electron-приложение для Windows.

**Возможности:**
- Splash-экран при запуске
- Открывает `https://crm.fix-byte.ru` в нативном окне 1440×900
- Трей-иконка, single instance
- Сохранение сессии между запусками (`persist:fixbyte-crm`)
- Страница «Нет соединения» при недоступности сервера

**Сборка:**
```powershell
cd desktop-client
npm install
node make-icon.js      # сгенерировать иконку из logo.png
npm run build:win      # собрать .exe
```

**Результат:**
```
dist/
  FixByte CRM Setup 3.0.0.exe      ← NSIS установщик (88.3 МБ)
  FixByte-CRM-Portable-3.0.0.exe   ← Portable (88.1 МБ)
```

Подробнее: [`desktop-client/README.md`](desktop-client/README.md)

---

## 📞 Контакты и поддержка

Для вопросов и предложений обратитесь к разработчику проекта.

---

**FixByte CRM v3.0** ✨  
*Система управления сервисом по ремонту ПК и ноутбуков*

