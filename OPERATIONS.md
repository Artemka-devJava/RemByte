# OPERATIONS — эксплуатация FixByte CRM

Документ для администрирования, деплоя и восстановления системы.

## 1) Конфигурация окружения

Рекомендуется хранить секреты и prod-настройки в `.env`.

Минимальные переменные:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `FIXBYTE_ADMIN_USERNAME`
- `FIXBYTE_ADMIN_PASSWORD`
- `FIXBYTE_OPERATOR_USERNAME`
- `FIXBYTE_OPERATOR_PASSWORD`

Пример URL для локальной MariaDB:

- `jdbc:mariadb://localhost:9092/rembyte?useUnicode=true&characterEncoding=UTF-8`

## 2) Локальный запуск

```powershell
Set-Location "C:\JavaProject\RemByte"
.\run.ps1            # авто: MariaDB на :9092, иначе встроенная H2
.\run.ps1 -H2        # принудительно встроенная H2 (offline/demo, БД в ./data/)
.\run.ps1 -Build     # собрать jar перед запуском
```

Linux/macOS — `./run.sh` (`--h2`, `--build`). Без скрипта: `mvn spring-boot:run`
(нужна MariaDB) или `mvn spring-boot:run -Ph2 -Dspring-boot.run.profiles=h2`.

Проверка:

- Web: `http://localhost:9087`

## 3) Docker запуск

```powershell
Set-Location "C:\JavaProject\RemByte"
docker compose up -d --build
docker compose ps
```

Остановка:

```powershell
docker compose down
```

Полная остановка с удалением volume БД (осторожно, удалит данные):

```powershell
docker compose down -v
```

### 3.1) Режим сети macvlan (свой IP в локальной сети)

Для варианта, когда контейнер CRM получает **собственный IP в вашей LAN** и
телефоны/браузеры заходят по нему напрямую (`http://<CRM_IP>:9087`), без проброса
портов:

```bash
cp .env.macvlan .env          # отредактировать под свою сеть
docker compose -f docker-compose.macvlan.yml up -d --build
```

Полная инструкция, ограничения (нужен Linux-хост по кабелю; Docker Desktop
Windows/macOS не подойдёт), доступ с самого хоста через `host-shim.sh` и
разбор проблем — в **`docker/macvlan/README.md`**.

## 4) Бэкап и восстановление

Встроенный модуль в настройках CRM:

- Создание backup-архива
- Восстановление из backup
- Включает пользовательские данные и вложения

Рекомендации:

- Делайте бэкап перед обновлением версии
- Храните 2-3 последних архива локально + копию вне сервера
- Проверяйте восстановление на тестовой среде

## 5) Обновление приложения

1. Сделать backup из панели администратора
2. Обновить код/образ
3. Перезапустить сервис
4. Проверить: логин, заказы, вложения, канбан, чат, плагины

Для Docker:

```powershell
docker compose up -d --build
docker compose logs --tail=200 app
```

## 6) Частые проблемы и решения

### Порт занят

Симптом: `Port 8080/9087 was already in use`.

Решение:

- Освободить порт
- Или сменить `server.port`

### MariaDB недоступна

Симптом: `Connection refused`, `Socket fail to connect host=localhost port=9092`.

Проверка:

```powershell
docker ps
```

Убедитесь, что контейнер БД запущен и healthy.

### Ошибка загрузки больших файлов через reverse proxy

Симптом: `413 Request Entity Too Large`.

Для Nginx добавьте в server/location:

- `client_max_body_size 100M;` (или больше)

и перезагрузите Nginx.

### Ошибки шаблонов Thymeleaf

Симптом: ошибки `TemplateInputException`.

Решение:

- Проверить синтаксис fragment/`th:*`
- Проверить существование подключаемых fragment-файлов
- Перезапустить после очистки сборки

## 7) Минимальный post-deploy smoke-check

- Вход под админом
- Создание клиента
- Создание заказа
- Добавление вложения
- Перевод статуса заказа
- Открытие чата
- Открытие канбан-доски
- Создание/восстановление тестового backup

## 8) Миграция состава заказов на строки (order_lines)

Начиная с этой версии состав заказа хранится в таблице `order_lines`
(услуга/разовая работа, снимок названия, цена за единицу, количество) вместо
связи many-to-many `order_services`.

- **dev / docker** (`ddl-auto=update`): ничего делать не нужно. Таблица
  создаётся автоматически, перенос данных из `order_services` выполняет
  `OrderLineMigrationService` при первом старте (лог `OrderLine migration completed`).
  Отключается флагом `fixbyte.orderlines.migration.enabled=false`.
- **prod** (`ddl-auto=validate`): перед деплоем выполнить SQL-скрипт
  `sql/2026-09-06-order-lines.sql` (создание таблицы + перенос данных +
  пересчёт сумм). Сделать backup БД заранее.
- Старая таблица `order_services` не удаляется автоматически. После проверки
  корректности заказов её можно удалить вручную (`DROP TABLE order_services;`).
- Десктоп- и mobile-клиенты — обёртки над веб-интерфейсом, отдельных действий
  не требуют.

## 9) Механизм ведения клиентов (карточка, журнал, устройства, фото)

Добавлены поля в `clients` и таблицы `client_notes`, `client_devices`,
`client_photos`. Карточка клиента — страница `/clients/{id}`.

- **dev / docker / h2** (`ddl-auto=update`): создаётся автоматически.
- **prod** (`ddl-auto=validate`): перед деплоем выполнить
  `sql/2026-09-06-client-mechanism.sql` (ALTER `clients` + 3 новые таблицы).
  Сделать backup БД заранее.
- Телефоны нормализуются приложением к виду `+7XXXXXXXXXX`. Существующие
  записи в неедином формате лучше привести к нему вручную (не ломая
  уникальность `phone`) — тогда заработает поиск дублей.
- Фото клиента хранятся в БД (`client_photos.content`, LONGBLOB), как и
  вложения заказов. Эндпоинты `POST/GET/DELETE /api/clients/{id}/photos` —
  готовая точка интеграции для мобильного приложения (список клиентов
  `GET /api/clients`, карточка `GET /api/clients/{id}`, загрузка фото).

