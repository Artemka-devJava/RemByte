# FixByte CRM — iOS

Нативное приложение (SwiftUI, iOS 16+, **без внешних зависимостей** — только
Foundation/URLSession/PhotosUI). Функционал такой же, как у Android-клиента.

## Что делает

| Экран | Возможности |
|---|---|
| **Вход** | адрес сервера + логин/пароль. Cookie сессии сохраняется между запусками. |
| **Клиенты** | список, поиск по имени/телефону/email, pull-to-refresh, **＋ Клиент** (имя + телефон + тип; при дубле телефона предлагает открыть существующего), выход. |
| **Карточка клиента** | контакты, тип, метки, заметка; сводка (заказов / оборот / долг); «Позвонить» и «Написать» (Telegram/WhatsApp/Email по предпочтительному каналу); **сетка фото**: добавить с камеры или из галереи (с подписью), просмотр на весь экран, **«Поделиться»** в соцсети/мессенджеры (системный лист — WhatsApp, Telegram, VK, почта…), удаление. |

Фото уходят на `POST /api/clients/{id}/photos` (в БД CRM, таблица `client_photos`),
перед отправкой ужимаются до 1600px. «Поделиться» скачивает фото из CRM,
кладёт во временный файл и отдаёт в `UIActivityViewController`.

## Сборка на macOS

Нужен **Xcode 15+**. Проектный файл (`.xcodeproj`) в репозиторий не кладётся —
он генерируется из `project.yml`.

### Вариант 1 — XcodeGen (рекомендуется, одна команда)

```bash
brew install xcodegen
cd mobile-client/ios
xcodegen generate
open FixByteCRM.xcodeproj
```

Дальше в Xcode: выбрать target **FixByteCRM** → вкладка **Signing & Capabilities**
→ указать свою команду (Team) → **Run** на симуляторе или устройстве.

### Вариант 2 — вручную, без XcodeGen

1. Xcode → **File ▸ New ▸ Project… ▸ iOS ▸ App**, имя `FixByteCRM`,
   Interface **SwiftUI**, Language **Swift**, снять галку с тестов.
2. Удалить созданные Xcode `ContentView.swift` и `FixByteCRMApp.swift`.
3. Перетащить в проект папки `App/`, `Model/`, `Net/`, `Store/`, `Views/`
   из `mobile-client/ios/FixByteCRM/` (Create groups).
4. Заменить сгенерированный `Info.plist` содержимым
   `mobile-client/ios/FixByteCRM/Resources/Info.plist` (или добавить из него
   ключи: `NSAppTransportSecurity`, `NSCameraUsageDescription`,
   `NSPhotoLibraryUsageDescription`).
5. Deployment Target → **iOS 16.0**.

## Адрес сервера (экран входа)

- **Симулятор iOS** → `http://localhost:9087` (симулятор видит localhost Mac напрямую). Значение по умолчанию.
- **iPhone в той же сети** → `http://<IP-Mac>:9087` (например `http://192.168.1.50:9087`).
- **Продакшн** → `https://crm.fix-byte.ru`.

HTTP разрешён через `NSAllowsArbitraryLoads` в `Info.plist`. Для публикации в
App Store замени на список конкретных доменов с HTTPS.

## Требования к серверу

Эндпоинт входа `POST /api/auth/login` (добавлен в CRM). Аутентификация — по
cookie сессии, картинки грузятся тем же `URLSession` (с этой же cookie).

## Структура

```
FixByteCRM/
  App/        FixByteCRMApp.swift, RootView
  Model/      Models.swift, UIImage+Resize
  Net/        Api.swift            — URLSession + все эндпоинты + хранение cookie
  Store/      Session.swift        — ObservableObject: вход/выход
  Views/      LoginView, ClientsView, CreateClientSheet, ClientDetailView,
              RemoteImage (авторизованная картинка), CameraPicker, ActivityView
  Resources/  Info.plist, Assets.xcassets
```
