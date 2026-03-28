# 🚀 БЫСТРЫЙ СТАРТ RemByte CRM

## 🆕 Актуальные изменения (28.03.2026)

- Раздел `Пользователи` перенесен в `Настройки` (`/admin#tabUsers`).
- Добавлена страница `Настройки` (`/admin`) с вкладками:
  - `🧾 Настройки чека`
  - `🗄️ База данных`
  - `🔑 Пользователи`
- В заказах при статусе `COMPLETED` появилась кнопка `🧾 Напечатать чек`.
- Кнопка `📎 Сохранить PDF и прикрепить` в чеке:
  - генерирует PDF
  - прикрепляет его к текущему заказу
  - не выполняет автоматическую загрузку файла на локальный ПК
- В `Настройки → База данных` добавлены:
  - `⬇️ Скачать резервную копию (.sql)`
  - `⬆️ Загрузить и восстановить` (полная замена текущих данных)

## 🚢 Релизные документы

- [RELEASE_NOTES_2026-03-28.md](RELEASE_NOTES_2026-03-28.md)
- [DEPLOY_UPGRADE.md](DEPLOY_UPGRADE.md)
- [KNOWN_ISSUES.md](KNOWN_ISSUES.md)
- [SMOKE_TEST_CHECKLIST.md](SMOKE_TEST_CHECKLIST.md)

## 🆕 Что изменилось в UI

- Логотип загружается из `src/main/resources/static/images/logo.png` и используется на всех страницах.
- На рабочих экранах добавлен переключатель темы: `Системная`, `Светлая`, `Темная`.
- Выбор темы сохраняется в браузере (`localStorage`, ключ `fixbyte-theme`).
- Авторизация использует значения из `.env` (`FIXBYTE_ADMIN_USERNAME`, `FIXBYTE_ADMIN_PASSWORD`, `FIXBYTE_OPERATOR_USERNAME`, `FIXBYTE_OPERATOR_PASSWORD`).

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
└── FIX_LOMBOK_ISSUE.md                   ✅ Инструкция по решению проблем
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

GET    /admin/backup             # Скачать SQL-бэкап (ADMIN)
POST   /admin/restore            # Восстановить БД из SQL (ADMIN)
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

Подробнее см. **FIX_LOMBOK_ISSUE.md**

---

## 💾 База данных

**По умолчанию:** MariaDB (`localhost:9092`) + переменные из `.env`.

- Вложения заказов (фото/видео/документы/PDF-чек) сохраняются в MariaDB, таблица `order_attachments` (`LONGBLOB`).
- Для Docker это означает, что отдельный том для папки `uploads` не обязателен: достаточно volume MariaDB.
- При старте выполняется автоперенос legacy-файлов из `uploads/orders/**` в БД (без дублей).
- Управление автопереносом: `FIXBYTE_UPLOAD_MIGRATION_ENABLED` и `FIXBYTE_UPLOAD_MIGRATION_DELETE_LEGACY`.

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
- 📖 **FIX_LOMBOK_ISSUE.md** - Решение проблем с Lombok

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
