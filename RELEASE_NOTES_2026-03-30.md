# Release Notes — 2026-03-30

## Версия
- Приложение: `1.0.3` (по `pom.xml`)
- Тип релиза: Desktop-клиент

---

## 🖥️ Что вошло в этот релиз

### 1) Нативный канбан в CRM

Добавлен встроенный раздел `🗂️ Канбан` (`/kanban`) для работы с задачами операторов.

#### Основные возможности:
- ✅ Персональные доски по пользователям (изоляция данных по владельцу)
- ✅ Несколько досок на пользователя: создание, выбор, переименование
- ✅ Переименование колонок под свой процесс (`К выполнению`, `В работе` и т.д.)
- ✅ Drag-and-drop перемещение карточек между колонками
- ✅ Вложения в карточках: фото и текстовые файлы (`.txt`, `.md`, `.csv`, `.log`)
- ✅ Первое изображение карточки показывается как превью на доске

#### API канбана:
- `GET /api/kanban/boards`
- `POST /api/kanban/boards`
- `PUT /api/kanban/boards/{boardId}`
- `GET /api/kanban/board?boardId={boardId}`
- `PUT /api/kanban/columns/{columnId}`
- `POST /api/kanban/columns/{columnId}/cards`
- `PUT /api/kanban/cards/{cardId}`
- `PUT /api/kanban/cards/{cardId}/move`
- `DELETE /api/kanban/cards/{cardId}`
- `POST /api/kanban/cards/{cardId}/attachments`
- `GET /api/kanban/cards/{cardId}/attachments`
- `DELETE /api/kanban/cards/{cardId}/attachments`

### 2) Десктопный клиент для Windows (Electron)

Добавлена папка `desktop-client/` с полноценным Windows-приложением на Electron.

| Параметр | Значение |
|----------|----------|
| Технология | Electron 35.7.5 |
| Целевая платформа | Windows 10/11 x64 |
| Размер установщика | ≈ 88 МБ |
| Целевой URL | `https://crm.fix-byte.ru` |

#### Функциональность клиента:
- ✅ Splash-экран при запуске (логотип + анимированные точки)
- ✅ Загружает `https://crm.fix-byte.ru` в нативном окне 1440×900
- ✅ Сохранение сессии между запусками (`persist:fixbyte-crm` partition)
- ✅ Иконка в системном трее — приложение сворачивается, а не закрывается
- ✅ Одна копия — запуск дополнительных окон заблокирован (`single instance lock`)
- ✅ Страница «Нет соединения» при недоступности сервера
- ✅ Меню навигации: Дашборд / Заказы / Клиенты / Услуги / Чат / Настройки
- ✅ Открытие внешних ссылок в браузере системы (не внутри приложения)

#### Файлы:
```
desktop-client/
├── main.js          — главный процесс Electron
├── preload.js       — безопасный мост Node.js → рендерер
├── splash.html      — анимированный экран загрузки
├── make-icon.js     — скрипт генерации icon.ico из logo.png
├── package.json     — конфигурация + параметры сборки
└── build/
    ├── icon.ico     — иконка (6 размеров: 16–256px, 350 КБ)
    └── logo.png     — исходный логотип
```

#### Результаты сборки (`npm run build:win`):
```
dist/
├── FixByte CRM Setup 1.0.3.exe      — NSIS-установщик
└── FixByte-CRM-Portable-1.0.3.exe   — Portable-версия
```

### 3) Генерация иконки

Создан скрипт `make-icon.js` — конвертирует `logo.png` в многоразмерный `icon.ico`:

```powershell
cd desktop-client
npm install
node make-icon.js
```

Использует библиотеки `sharp` (ресайзинг) + `to-ico` (упаковка в ICO-формат).  
Генерирует размеры: 16, 32, 48, 64, 128, 256 px.

### 4) Унификация шаблонов интерфейса (Thymeleaf fragments)

Для рабочих страниц CRM убрано дублирование типовых блоков и введены общие фрагменты:

- `templates/fragments/head-common.html`
- `templates/fragments/sidebar-nav.html`
- `templates/fragments/sidebar-shell.html`
- `templates/fragments/theme-switcher.html`
- `templates/fragments/sidebar-user.html`
- `templates/fragments/common-scripts.html`

Что это дало:
- единый источник sidebar-меню и подсветки active-пункта;
- более простую поддержку 11+ страниц;
- меньше риска рассинхронизации UI между модулями.

### 5) Стабилизационные фиксы после рефакторинга шаблонов

- Исправлен edge-case в `sidebar-nav`: null-safe обработка `#httpServletRequest`.
- Исправлен рендер `/login`: HTTP-сессия создается до отрисовки формы.
- После фиксов страница входа и базовая навигация работают без runtime-ошибок.

### 6) Проверка качества (30.03.2026)

Выполнен полный прогон тестов Maven:

- `com.rembyte.controller.WebRouteUniquenessTest`
- `com.rembyte.service.ChatServiceTest`
- `com.rembyte.service.LegacyAttachmentMigrationServiceTest`

Итог: `BUILD SUCCESS`, `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`.

---

## Структура проекта (полная)

```
RemByte/
├── src/main/java/com/rembyte/
│   ├── controller/      (10 контроллеров)
│   ├── model/           (13 моделей данных)
│   ├── service/         (12 сервисов)
│   ├── repository/      (12 репозиториев)
│   └── config/          (конфигурация безопасности)
├── src/main/resources/
│   ├── templates/       (13 HTML шаблонов Thymeleaf)
│   └── static/
│       ├── css/         (стили + темы)
│       ├── js/          (модули интерфейса + плагин-хост)
│       └── images/      (логотип, favicon)
├── desktop-client/      ← НОВОЕ: Electron Windows-приложение
│   ├── dist/            ← .exe файлы для дистрибуции
│   └── build/           ← иконки для сборки
├── docker-compose.yml
├── Dockerfile
├── .env
└── plugin-samples/      (3 примера плагинов)
```

---

## Статистика проекта

| Метрика | Значение |
|---------|----------|
| Java классов | 47 |
| HTML шаблонов | 13 |
| JavaScript модулей | 11+ |
| Таблиц в БД | 13 |
| API endpoints | 50+ |
| Встроенных плагинов | 1 (Заметки) |
| Примеров плагинов | 3 (Змейка, Тетрис, Текстовый редактор) |
| Минимум Java | 17 |
| Платформы десктопа | Windows 10/11 x64 |

---

## Итог

Приложение готово к:
- ✅ Локальной разработке
- ✅ Развертыванию в Docker
- ✅ Продакшену с MariaDB
- ✅ Расширению через плагины
- ✅ Использованию через браузер
- ✅ Использованию через Windows-приложение (Electron)
