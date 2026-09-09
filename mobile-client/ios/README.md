# FixByte CRM — iOS

Нативное приложение (SwiftUI, iOS 16+, **без внешних зависимостей** — только
Foundation/URLSession/PhotosUI). Функционал такой же, как у Android-клиента.

## Что делает

| Экран | Возможности |
|---|---|
| **Вход** | логин/пароль. Cookie сессии сохраняется между запусками — при валидной сессии форма не показывается. |
| **Настройки** (⚙, с экрана входа и из меню списка клиентов) | адрес сервера CRM — задаётся один раз. |
| **Клиенты** | список, поиск по имени/телефону/email, pull-to-refresh, **＋ Клиент** (имя + телефон + тип; при дубле телефона предлагает открыть существующего), выход. |
| **Карточка клиента** | контакты, тип, метки, заметка; сводка (заказов / оборот / долг); «Позвонить» и «Написать» (Telegram/WhatsApp/Email по предпочтительному каналу); **список заказов** клиента + **＋ Заказ** (описание + примерная цена). |
| **Заказ** | статус, что принято, примерная стоимость; «Изменить неисправность / оценку»; **Фото по заказу** — камера/галерея (ужимаются до 1600px), просмотр на весь экран, удаление; **Акт приёмки** — «Печать» (PDF с сервера → AirPrint через `UIPrintInteractionController`) и «Отправить PDF» (системный лист). |

Фото заказа уходят на `POST /api/orders/{id}/attachments` (поле `files`), видны в
веб-CRM в карточке заказа. Акт — `GET /api/orders/{id}/act` (PDF, сервер его же
сохраняет в заказ). Реквизиты и логотип в шапке акта настраиваются в веб-CRM
(«Настройки чека»), приложению для этого ничего делать не нужно.

> Съёмка фото перенесена с клиента на **заказ** (как в Android-клиенте). Старые
> фото клиентов остаются в БД CRM, но в приложении и на карточке клиента больше
> не показываются.

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

## Адрес сервера (экран «Настройки», ⚙)

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
  Views/      LoginView, SettingsView, ClientsView, CreateClientSheet, ClientDetailView,
              OrderDetailView (+ NewOrderSheet, EditOrderSheet), RemoteImage
              (авторизованная картинка), CameraPicker, ActivityView
  Resources/  Info.plist, Assets.xcassets
```
