# Deploy & Upgrade Guide

## 1. Подготовка
- Проверьте наличие `.env` в корне проекта.
- Убедитесь, что заполнены переменные `MARIADB_*` и `FIXBYTE_*`.
- Для обновления действующей инсталляции сделайте backup:
  - через `/admin/backup`
  - или через `mysqldump`.

## 2. Обновление через Docker (рекомендуется)

### Шаги
```powershell
Set-Location "C:\JavaProject\RemByte"
docker compose pull
docker compose up -d --build
docker compose logs -f app
```

### Проверить после старта
- Приложение: `http://localhost:9087`
- Логи приложения: нет ошибок `Application failed to start`
- Миграция вложений: в логах есть запись о `Legacy migration completed`

## 3. Локальный запуск без Docker

```powershell
Set-Location "C:\JavaProject\RemByte"
mvn clean package
mvn spring-boot:run
```

## 4. Параметры миграции вложений

```properties
FIXBYTE_UPLOAD_MIGRATION_ENABLED=true
FIXBYTE_UPLOAD_MIGRATION_DELETE_LEGACY=false
```

- `...ENABLED=true` - включить перенос файлов `uploads/orders/**` в таблицу `order_attachments`.
- `...DELETE_LEGACY=false` - не удалять старые файлы после переноса.

## 5. Upgrade без простоя (минимальный)
1. Поднять новый контейнер приложения с тем же volume БД.
2. Проверить health и логин.
3. Переключить трафик на новый контейнер.
4. Остановить старый контейнер.

## 6. Откат (rollback)
1. Остановить текущую версию приложения.
2. Развернуть предыдущий образ приложения.
3. Восстановить БД из backup (если были несовместимые изменения данных).

## 7. Минимальная post-deploy проверка
- Логин под `ADMIN` и `OPERATOR`.
- Создание заказа.
- В форме заказа группы услуг стартуют в свернутом состоянии.
- Проверка быстрого добавления услуги из формы заказа (услуга сразу выбирается в заказе).
- Прикрепление файла к заказу.
- Открытие вложения по URL `/uploads/orders/{orderId}/{storedName}`.
- Удаление вложения.
- Проверка разделов `/admin` и `/plugins`.
- Проверка встроенного плагина заметок:
  - открыть `/notes`
  - создать папку и заметку
  - проверить Markdown-режимы `редактирование/превью`
  - скачать заметку `.txt`
  - в `/admin#tabPlugins` отключить `Плагин «Заметки»` и убедиться, что пункт меню скрыт

Подробный функциональный прогон: `SMOKE_TEST_CHECKLIST.md`.

