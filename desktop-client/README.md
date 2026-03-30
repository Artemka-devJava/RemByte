# FixByte CRM — Десктопный клиент (Electron)

Десктопное приложение для Windows на основе Electron.  
Открывает `https://crm.fix-byte.ru` как нативное окно с трей-иконкой, меню и splash-экраном.

## Требования

- Node.js 18+ ([nodejs.org](https://nodejs.org))
- npm (идёт вместе с Node.js)
- Windows 10/11 x64

## Быстрый старт

```powershell
cd C:\JavaProject\RemByte\desktop-client

# 1. Установить зависимости
npm install

# 2. Сгенерировать иконку (если build/icon.ico отсутствует)
node make-icon.js

# 3. Запустить для теста
npm start
```

## Иконка

Иконка генерируется **автоматически** из `logo.png` скриптом `make-icon.js`.

```powershell
# Если build/logo.png ещё нет — скопировать из ресурсов CRM:
Copy-Item "..\src\main\resources\static\images\logo.png" "build\"

# Сгенерировать build/icon.ico (размеры: 16, 32, 48, 64, 128, 256 px)
node make-icon.js
```

Скрипт использует `sharp` (ресайзинг) и `to-ico` (упаковка в ICO).  
Готовый файл: `build/icon.ico` (~350 КБ, 6 размеров).

> **Примечание:** `build/icon.ico` — бинарный файл, его нет в git.  
> При первом клонировании нужно запустить `node make-icon.js`.

## Сборка .exe установщика

```powershell
npm run build:win
```

Готовые файлы появятся в `dist/`:

```
dist/
  FixByte CRM Setup 3.0.0.exe     ← установщик NSIS (88.3 МБ)
  FixByte-CRM-Portable-3.0.0.exe  ← portable версия (88.1 МБ)
```

## Возможности приложения

- ✅ Splash-экран при запуске (логотип + анимированные точки)
- ✅ Загрузка `https://crm.fix-byte.ru` в нативном окне 1440×900
- ✅ Сохранение сессии (логин сохраняется между запусками)
- ✅ Трей-иконка (сворачивается в трей, не закрывается)
- ✅ Блокировка повторного запуска (только одно окно)
- ✅ Страница «Нет соединения» при недоступности сервера
- ✅ Меню с быстрой навигацией (Дашборд, Заказы, Клиенты, Настройки…)
- ✅ Внешние ссылки открываются в браузере системы

## Горячие клавиши

| Клавиша | Действие |
|---------|----------|
| F5 | Обновить страницу |
| F11 | Полный экран |
| F12 | Инструменты разработчика |
| Alt+Left | Назад |
| Alt+Right | Вперёд |
| Ctrl+= | Увеличить масштаб |
| Ctrl+- | Уменьшить масштаб |
| Ctrl+0 | Сбросить масштаб |
| Alt+F4 | Выход |

## Структура файлов

```
desktop-client/
├── main.js          — главный процесс Electron
├── preload.js       — безопасный мост Node.js → рендерер
├── splash.html      — анимированный splash-экран
├── make-icon.js     — скрипт: logo.png → build/icon.ico
├── package.json     — зависимости + настройки electron-builder
└── build/
    ├── icon.ico     — иконка (генерируется, не в git)
    └── logo.png     — исходный логотип
```

## npm-скрипты

| Команда | Описание |
|---------|----------|
| `npm start` | Запустить в dev-режиме |
| `npm run build:win` | Собрать x64: установщик + portable |
| `npm run build:win32` | Собрать ia32 (32-bit) вариант |
| `node make-icon.js` | Сгенерировать/пересоздать icon.ico |

## Изменить URL CRM

Откройте `main.js`, строка 8:

```javascript
const CRM_URL = 'https://crm.fix-byte.ru';
```

Замените на нужный адрес, например `http://localhost:9087` для локальной разработки.
