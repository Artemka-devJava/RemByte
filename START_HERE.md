# 🎉 FIXBYTE CRM — ГОТОВО К РАБОТЕ!

## 🆕 Последние обновления (30.03.2026)

- 🖥️ **Десктопный клиент для Windows** (`desktop-client/`) — Electron 35, NSIS + Portable .exe
- База данных: MariaDB (`localhost:9092`) с настройками через `.env`.
- Учетные записи: `FIXBYTE_ADMIN_*` и `FIXBYTE_OPERATOR_*` из `.env`.
- Логотип: единое изображение `/images/logo.png` на всех страницах.
- Внешний вид: переключатель темы (`system`/`light`/`dark`/`fixbyte`) с сохранением выбора.

**FixByte CRM** — Система управления сервисом по ремонту ПК и ноутбуков

---

## ✨ ЧТО ЕСТЬ В ПРОЕКТЕ

### 📁 Spring Boot 3.2.0 веб-приложение

```
✅ 47 Java классов
✅ 13 HTML шаблонов (Thymeleaf)
✅ 11+ JavaScript модулей
✅ 50+ REST API endpoints
✅ 13 таблиц в MariaDB
✅ 3 примера плагинов
✅ Десктопный Electron-клиент для Windows
```

### 🏗️ Технологии

- **Backend:** Spring Boot 3.2.0, Spring Data JPA, Spring Security, Lombok
- **Frontend:** HTML5, CSS3, Thymeleaf, Vanilla JavaScript
- **Database:** MariaDB 11.4 (Docker) / любая MariaDB 10.5+
- **Docker:** Dockerfile + docker-compose.yml
- **Desktop:** Electron 35 (Windows 10/11 x64)

### 🎯 Компоненты системы

1. **CRM** — управление клиентами, заказами, услугами
2. **Калькулятор** — расчёт стоимости ремонта
3. **Настройки** — чек/квитанция, пользователи, бэкап БД
4. **Плагины** — загрузка из файла, изоляция в iframe sandbox
5. **Заметки** — встроенный плагин с папками и Markdown
6. **Чат** — онлайн-виджет для внешнего сайта + операторский интерфейс
7. **Десктоп** — Windows-клиент, открывающий crm.fix-byte.ru

---

## 📚 ДОКУМЕНТАЦИЯ

| Файл | Назначение |
|------|-----------|
| **INDEX.md** | 👈 Индекс всей документации |
| **QUICKSTART.md** | Быстрый старт (5 минут) |
| **desktop-client/README.md** | Десктопный клиент |
| **README.md** | Основная документация |
| **COMPLETE_README.md** | Полная документация |
| **PROJECT_REPORT.md** | Отчет о проекте |
| **FIX_LOMBOK_ISSUE.md** | Решение проблем |

---

## 🚀 БЫСТРЫЙ СТАРТ

### Вариант 1 — Браузер (локально)

```powershell
cd C:\JavaProject\RemByte
mvn spring-boot:run
# Открыть: http://localhost:9087
```

### Вариант 2 — Docker

```powershell
cd C:\JavaProject\RemByte
docker-compose up -d
# Открыть: http://localhost:9087
```

### Вариант 3 — Windows-клиент (Electron)

```powershell
cd C:\JavaProject\RemByte\desktop-client
npm install
node make-icon.js   # один раз
npm start           # открывает https://crm.fix-byte.ru
```

Собрать .exe:
```powershell
npm run build:win
# dist/FixByte CRM Setup 3.0.0.exe
```

---

## ✅ ФУНКЦИОНАЛЬНОСТЬ

### 👥 CRM система
- [x] Управление клиентами (поиск, история заказов)
- [x] Управление заказами (статусы, платежи, вложения)
- [x] Вложения к заказам: фото, видео, документы, PDF-чек
- [x] Быстрое добавление услуги прямо при создании заказа
- [x] Группы услуг (свёрнуты по умолчанию)

### 🧾 Чек/квитанция
- [x] Генерация PDF-чека при статусе `COMPLETED`
- [x] Прикрепление PDF к заказу одной кнопкой
- [x] Настройка полей чека (включать/выключать любое поле)
- [x] Настройка реквизитов в `Настройки → Чек`

### ⚙️ Настройки (только ADMIN)
- [x] Управление пользователями и правами
- [x] Резервная копия БД (скачать .sql)
- [x] Восстановление БД из .sql файла
- [x] Управление плагинами (вкл/выкл)

### 🧩 Плагины
- [x] Загрузка .html-плагина из файла (Admin → Настройки)
- [x] Изоляция в iframe sandbox
- [x] Встроенный плагин **Заметки** (папки, Markdown, скачивание)
- [x] Примеры: Змейка, Тетрис, Текстовый редактор

### 💬 Онлайн-чат
- [x] Операторский интерфейс `/chat`
- [x] Виджет для вставки на внешний сайт (script-тег)
- [x] История диалогов в MariaDB
- [x] Счётчик непрочитанных в меню

### 🖥️ Десктопный клиент (Windows)
- [x] Electron 35, Windows 10/11 x64
- [x] Splash-экран, трей-иконка, single instance
- [x] Сохранение сессии (логин между перезапусками)
- [x] NSIS-установщик + Portable .exe
- [x] Страница «Нет соединения»

### 💰 Калькулятор
- [x] Выбор услуг, скидки, дополнения
- [x] Расчёт в реальном времени

### 📊 Дашборд
- [x] Статистика: клиенты, заказы, выручка
- [x] Последние заказы, популярные услуги

### 📡 REST API
- [x] 50+ endpoints
- [x] Полный CRUD + поиск, фильтрация, статистика

---

## 📊 ТЕХНОЛОГИЧЕСКИЙ СТЕК

| Компонент | Версия |
|-----------|--------|
| Java | 17 LTS |
| Spring Boot | 3.2.0 |
| Spring Data JPA | 3.2.0 |
| Lombok | 1.18.30 |
| MariaDB | 10.5+ / 11.4 (Docker) |
| Maven | 3.8.1+ |
| Electron | 35.x (десктоп) |
| HTML5/CSS3/JS | ES6+ |

---

## ⚡ КОМАНДЫ

```powershell
# Запустить (рекомендуется)
mvn spring-boot:run

# Собрать JAR
mvn clean package -DskipTests

# Запустить JAR напрямую
java -jar target/rembyte-crm-3.0.jar

# Docker (запуск)
docker-compose up -d

# Docker (остановка)
docker-compose down

# Десктопный клиент (dev-запуск)
cd desktop-client ; npm start

# Десктопный клиент (собрать .exe)
cd desktop-client ; npm run build:win
```

---

## ⚠️ ЕСЛИ ВОЗНИКЛИ ПРОБЛЕМЫ

| Проблема | Документ |
|----------|----------|
| Ошибка сборки Maven / Lombok | [QUICKSTART.md](QUICKSTART.md) |
| Не запускается Docker | [KNOWN_ISSUES.md](KNOWN_ISSUES.md) |
| Нет `build/icon.ico` в Electron | `cd desktop-client && node make-icon.js` |
| Нужна полная документация | [COMPLETE_README.md](COMPLETE_README.md) |

---

## 🎉 ИТОГО

**FixByte CRM v3.0** — готовое к продакшену веб-приложение для управления сервисом ремонта ПК:

✅ Spring Boot 3.2.0 + MariaDB + Docker  
✅ Авторизация (ADMIN / OPERATOR)  
✅ Плагины, Заметки (Markdown), Онлайн-чат  
✅ PDF-чек, бэкап БД  
✅ Десктопный клиент для Windows (Electron)  
✅ Полная документация  

---

## 📚 НАЧНИТЕ С ЭТОГО

### 👉 [INDEX.md](INDEX.md) — полная навигация по документации

### Или сразу к QUICKSTART:

```
C:\JavaProject\RemByte\QUICKSTART.md
```

---

**FixByte CRM v3.0 — Удачи в работе! 🔧**

