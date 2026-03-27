# 🔧 RemByte CRM - Система управления сервисом ремонта ПК

## 🆕 Актуальные изменения (27.03.2026)

- Единый логотип: `/images/logo.png` на всех страницах интерфейса.
- Переключение внешнего вида: `Системная / Светлая / Темная`.
- Тема сохраняется в браузере (`localStorage`, ключ `fixbyte-theme`).
- Авторизация и доступ к БД берутся из `.env` (`MARIADB_*`, `FIXBYTE_*`).

## 📋 Описание проекта

**RemByte CRM** - это веб-приложение для управления бизнесом по ремонту персональных компьютеров и ноутбуков, разработанное на **Spring Boot** с использованием **React-подобного** веб-интерфейса.

### Основные функции:

✅ **CRM система** - управление клиентами, контакты, история заказов  
✅ **Учет заказов** - создание, отслеживание статуса, управление платежами  
✅ **Калькулятор стоимости** - автоматический расчет с учетом скидок и дополнений  
✅ **Каталог услуг** - управление услугами и ценами  
✅ **Финансовая статистика** - анализ доходов и производительности  
✅ **REST API** - полный REST API для интеграции с другими системами  

---

## 🏗️ Архитектура проекта

```
RemByte/
├── src/
│   ├── main/
│   │   ├── java/com/rembyte/
│   │   │   ├── RemByteApplication.java          # Точка входа
│   │   │   ├── config/
│   │   │   │   └── ApplicationConfiguration.java # Конфигурация
│   │   │   ├── model/                           # JPA сущности
│   │   │   │   ├── Client.java
│   │   │   │   ├── Order.java
│   │   │   │   ├── RepairService.java
│   │   │   │   ├── Payment.java
│   │   │   │   ├── OrderStatus.java
│   │   │   │   └── PaymentMethod.java
│   │   │   ├── repository/                      # JPA репозитории
│   │   │   │   ├── ClientRepository.java
│   │   │   │   ├── OrderRepository.java
│   │   │   │   ├── RepairServiceRepository.java
│   │   │   │   └── PaymentRepository.java
│   │   │   ├── service/                         # Бизнес-логика
│   │   │   │   ├── ClientService.java
│   │   │   │   ├── OrderService.java
│   │   │   │   ├── RepairServiceService.java
│   │   │   │   ├── PaymentService.java
│   │   │   │   └── OrderStatistics.java
│   │   │   └── controller/                      # REST контроллеры
│   │   │       ├── ClientController.java
│   │   │       ├── OrderController.java
│   │   │       ├── RepairServiceController.java
│   │   │       └── WebController.java
│   │   ├── resources/
│   │   │   ├── application.properties
│   │   │   ├── static/
│   │   │   │   ├── css/
│   │   │   │   │   └── style.css
│   │   │   │   └── js/
│   │   │   │       ├── api.js
│   │   │   │       ├── clients.js
│   │   │   │       ├── orders.js
│   │   │   │       ├── services.js
│   │   │   │       ├── dashboard.js
│   │   │   │       └── calculator.js
│   │   │   └── templates/
│   │   │       ├── index.html
│   │   │       ├── dashboard.html
│   │   │       ├── clients.html
│   │   │       ├── orders.html
│   │   │       ├── services.html
│   │   │       └── calculator.html
│   └── test/
├── pom.xml                                      # Конфигурация Maven
└── README.md                                    # Документация
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
java -jar target/rembyte-crm-1.0.0.jar

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
java -jar target/rembyte-crm-1.0.0.jar
```

### На сервере Linux/Mac
```bash
# Предоставить права на выполнение
chmod +x rembyte-crm-1.0.0.jar

# Запустить в фоновом режиме
nohup java -jar rembyte-crm-1.0.0.jar &

# Или через systemd
sudo nano /etc/systemd/system/rembyte.service
# [Unit]
# Description=RemByte CRM Service
# After=network.target
# [Service]
# Type=simple
# ExecStart=/usr/bin/java -jar /path/to/rembyte-crm-1.0.0.jar
# Restart=on-failure
```

### Через Docker
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/rembyte-crm-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

```bash
docker build -t rembyte:1.0.0 .
docker run -p 9087:9087 rembyte:1.0.0
```

---

## 📚 Технологический стек

| Компонент | Версия | Назначение |
|-----------|--------|-----------|
| Java | 17 | Язык программирования |
| Spring Boot | 3.2.0 | Web framework |
| Spring Data JPA | 3.2.0 | ORM |
| MariaDB | 10.5+ | БД |
| Lombok | 1.18.30 | Code generation |
| Maven | 3.8+ | Build tool |
| HTML5/CSS3 | Latest | Frontend |
| JavaScript (ES6) | Latest | Frontend logic |

---

## 📞 Контакты и поддержка

Для вопросов и предложений обратитесь к разработчику проекта.

---

## 📄 Лицензия

Проект RemByte CRM создан для образовательных и коммерческих целей.

---

**RemByte CRM v1.0.0** ✨  
*Система управления сервисом по ремонту ПК и ноутбуков*

