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

Всё для Docker вынесено в отдельную папку **`deploy/`** — самодостаточную:
compose-файлы, `.env.example`, скрипты, SQL инициализации БД, гайд по macvlan.

```bash
cd deploy
cp .env.example .env      # смените пароли (и сеть для macvlan)
./up.sh                   # обычный режим; Windows: .\up.ps1
./logs.sh                 # логи
./down.sh                 # остановить (данные БД сохраняются)
```

Веб: `http://localhost:${HOST_HTTP_PORT:-9087}`.

### 3.1) Режим сети macvlan (свой IP в локальной сети)

Контейнер CRM получает **собственный IP в вашей LAN**, телефоны/браузеры
заходят по нему напрямую (`http://<CRM_IP>:9087`) без проброса портов:

```bash
cd deploy
cp .env.example .env      # MACVLAN_PARENT, LAN_SUBNET, LAN_GATEWAY, LAN_IP_RANGE, CRM_IP, HOST_SHIM_IP
./up.sh macvlan
sudo ./macvlan/host-shim.sh up     # доступ с самого хоста
```

Только Linux-хост по кабелю (Docker Desktop Windows/macOS не подойдёт).
Полная инструкция и разбор проблем — **`deploy/macvlan/README.md`**.

## 4) Бэкап и восстановление

Панель администратора → вкладка **База данных**.

### Два уровня бэкапа

| | Что внутри | Когда |
|---|---|---|
| **Лёгкий** (`/admin/backup?level=light`) | только записи БД; таблицы с фото/вложениями выгружаются структурой без данных; папка `uploads` не включается | частый быстрый бэкап настроек и записей |
| **Полный** (`/admin/backup?level=full`) | всё целиком: дамп БД с BLOB (все фото, картинки, вложения) + legacy-папка `uploads` | перед обновлением, для полного архива |

> Восстановление **лёгкого** бэкапа очищает таблицы вложений/фото. Для полного
> отката данных нужен **полный** бэкап.

Восстановление: перетащить `.zip`/`.sql` в зону «Восстановление» (`/admin/restore`).
Оба уровня и «сырой» `.sql` понимаются автоматически.

### Хранилище полных бэкапов (в т.ч. Samba)

Если задан `FIXBYTE_BACKUP_DIR` — в панели появляется блок «Хранилище полных
бэкапов»: кнопка «Сохранить полный бэкап в хранилище», список сохранённых
архивов со скачиванием / восстановлением / удалением, авто-ротация
(`FIXBYTE_BACKUP_KEEP`, по умолчанию 14).

`FIXBYTE_BACKUP_DIR` может быть **смонтированной шарой Samba/CIFS** — тогда
полные бэкапы «сохраняются через samba» без доп. кода:

```bash
# Linux-хост
sudo mount -t cifs //NAS/backups /mnt/crm-backups \
     -o username=USER,password=PASS,uid=$(id -u),vers=3.0
export FIXBYTE_BACKUP_DIR=/mnt/crm-backups
```

```yaml
# docker-compose: volume с драйвером cifs
services:
  app:
    environment:
      FIXBYTE_BACKUP_DIR: /backups
    volumes:
      - crm_backups:/backups
volumes:
  crm_backups:
    driver_opts:
      type: cifs
      o: "username=USER,password=PASS,uid=1000,vers=3.0"
      device: "//NAS/backups"
```

Плановый полный бэкап в хранилище — cron из 6 полей в `FIXBYTE_BACKUP_CRON`
(`-` = выключено, по умолчанию). Пример каждую ночь в 03:30: `0 30 3 * * *`.

### Рекомендации

- Полный бэкап перед каждым обновлением версии.
- Храните 2-3 полных архива в хранилище + копию вне сервера.
- Проверяйте восстановление на тестовой среде.

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

