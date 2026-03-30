# Release Notes - 2026-03-28

## Версия
- Приложение: `3.0` (по `pom.xml`)
- Тип релиза: функциональное обновление + стабилизация

## Что вошло в релиз

### 1) Вложения заказов: хранение в MariaDB
- Вложения (фото, видео, документы, PDF-чек) сохраняются в таблицу `order_attachments` как `LONGBLOB`.
- Добавлены:
  - `src/main/java/com/rembyte/model/OrderAttachment.java`
  - `src/main/java/com/rembyte/repository/OrderAttachmentRepository.java`
  - `src/main/java/com/rembyte/controller/UploadController.java`
- Сервис `FileStorageService` переведен на БД-хранилище.
- Для удаления вложений добавлена транзакция (фикс ошибки `No EntityManager with actual transaction available...`).

### 2) Совместимость со старыми ссылками
- Сохранен формат URL вложений: `/uploads/orders/{orderId}/{storedName}`.
- Выдача контента идет через `UploadController` из БД.
- Для старых файлов на диске есть fallback-чтение.

### 3) Автомиграция legacy-файлов
- Добавлен `LegacyAttachmentMigrationService`.
- При старте приложение переносит файлы из `uploads/orders/**` в таблицу `order_attachments`.
- Миграция идемпотентна: дубликаты не создаются.
- Добавлены флаги:
  - `FIXBYTE_UPLOAD_MIGRATION_ENABLED`
  - `FIXBYTE_UPLOAD_MIGRATION_DELETE_LEGACY`

### 4) Админ-панель и бизнес-функции
- Централизация настроек в `/admin` (пользователи, чек, резервное копирование/восстановление БД).
- Поддержка ролей `ADMIN`/`OPERATOR`.
- Расширения интерфейса: темы, плагины, работа с вложениями в заказах.

### 5) Плагины
- Поддержка загрузки локальных HTML-плагинов через интерфейс.
- Изолированный запуск через `plugin-host` (плагин не должен влиять на основную CRM-логику).
- Добавлены примеры в `plugin-samples/`.

### 6) Встроенный плагин заметок
- Добавлена страница `/notes` и пункт меню `🧩 Заметки`.
- Добавлены сущности и таблицы БД:
  - `note_folders`
  - `note_items`
  - `plugin_settings` (глобальные флаги встроенных плагинов)
- Для заметок реализовано:
  - хранение в MariaDB
  - папки и заметки
  - скачивание заметки в `.txt`
  - 2 режима Markdown: `редактирование` и `превью`
- Администратор может полностью отключить встроенный плагин заметок в `Настройки -> Плагины`:
  - пункт `Заметки` исчезает из меню
  - `/notes` редиректит на `/dashboard`
  - API заметок возвращает `403`

### 7) Улучшения формы заказов
- В форме создания/редактирования заказа добавлен блок быстрого добавления услуги:
  - создание услуги по месту (название, цена, категория, описание)
  - новая услуга сразу попадает в справочник и автоматически выбирается в текущем заказе
- Группы услуг в форме заказа по умолчанию свёрнуты, чтобы экономить место при большом количестве услуг.

## Изменения конфигурации

### Новые/важные env-переменные
```properties
SPRING_PROFILES_ACTIVE=dev
MARIADB_URL=jdbc:mariadb://localhost:9092/rembyte?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8
MARIADB_USERNAME=root
MARIADB_PASSWORD=***

FIXBYTE_ADMIN_USERNAME=admin
FIXBYTE_ADMIN_PASSWORD=***
FIXBYTE_OPERATOR_USERNAME=operator
FIXBYTE_OPERATOR_PASSWORD=***

FIXBYTE_UPLOAD_MIGRATION_ENABLED=true
FIXBYTE_UPLOAD_MIGRATION_DELETE_LEGACY=false
```

## Проверка качества
- Unit/интеграционные проверки маршрутов и миграции:
  - `WebRouteUniquenessTest`
  - `LegacyAttachmentMigrationServiceTest`

## Важные замечания
- Для Docker хранение вложений теперь опирается на volume MariaDB, отдельный volume для `uploads` не обязателен.
- При включении `FIXBYTE_UPLOAD_MIGRATION_DELETE_LEGACY=true` legacy-файлы удаляются после успешного переноса.

## Рекомендованный порядок после обновления
1. Сделать резервную копию БД.
2. Обновить `.env` (особенно `MARIADB_*`, `FIXBYTE_*`, `FIXBYTE_UPLOAD_MIGRATION_*`).
3. Перезапустить приложение/контейнеры.
4. Выполнить smoke-checklist: `SMOKE_TEST_CHECKLIST.md`.

