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

### Сборка на JDK 17 (обязательно)

Проект компилируется и тестируется на **JDK 17** через Maven toolchains —
даже если Maven запущен на более новой JVM. Нужна запись в
`~/.m2/toolchains.xml`:

```xml
<toolchains xmlns="http://maven.apache.org/TOOLCHAINS/1.1.0">
  <toolchain>
    <type>jdk</type>
    <provides><version>17</version></provides>
    <configuration><jdkHome>/путь/к/jdk-17</jdkHome></configuration>
  </toolchain>
</toolchains>
```

Без неё `mvn` упадёт на фазе `validate` (`maven-toolchains-plugin`: "Cannot
find matching toolchain"). Так же это чинит Mockito — его inline-mock-maker
не работает на новых JDK (25), на 17 всё ок.

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

Панель администратора → вкладка **База данных**. Бэкап только один — **полный**:
дамп БД с BLOB (все фото, картинки, вложения) + legacy-папка `uploads`. Из него
восстанавливается вообще всё.

### Автоматический бэкап (расписание)

Блок «Автоматический бэкап» — периодичность: **выключено / раз в день / раз в
месяц / раз в квартал / раз в год**. Каждую ночь в **03:30** сервер проверяет,
наступил ли срок, и, если задан `FIXBYTE_BACKUP_DIR`, кладёт полный бэкап в
хранилище. Ориентир — момент последнего успешного бэкапа: если сервер в 03:30
был выключен, бэкап сделается при следующей ночной проверке.

Настройка хранится в БД (таблица `backup_schedule`, одна строка). На **prod**
(`ddl-auto=validate`) перед деплоем выполнить `sql/2026-09-09-backup-schedule.sql`
и (для этой сборки) `sql/2026-09-11-backup-dir.sql`.

### Хранилище полных бэкапов (в т.ч. Samba)

Контейнер монтирует `/mnt` на каталог хоста, заданный `BACKUP_HOST_DIR`
(docker-compose). Если задан `FIXBYTE_BACKUP_DIR` (по умолчанию `/mnt`) —
в панели работает блок «Хранилище полных бэкапов»: кнопка «Сохранить полный
бэкап в хранилище», список сохранённых архивов со скачиванием /
восстановлением / удалением, авто-ротация (`FIXBYTE_BACKUP_KEEP`, по
умолчанию 14). Восстановление — кнопкой «Восстановить» у нужного архива
в списке.

**Путь можно выбрать прямо в панели** (поле «Каталог для бэкапов» в том же
блоке) — например, указать конкретную подпапку внутри `/mnt`, если туда
смонтировано несколько шар/дисков. Путь обязан лежать внутри `/mnt`
(`fixbyte.backup.base-dir`, см. `BackupStorageService`) — сервер вернёт
ошибку на любой путь снаружи, поскольку только `/mnt` гарантированно
примонтирован в контейнер. Выбор хранится в БД (`backup_schedule.backup_dir`)
и имеет приоритет над `FIXBYTE_BACKUP_DIR`; пустое значение в панели —
откат на переменную окружения.

`/mnt` может быть **смонтированной шарой Samba/CIFS** — тогда полные бэкапы
«сохраняются через samba» без доп. кода:

```bash
# Linux-хост
sudo mount -t cifs //NAS/backups /mnt/crm-backups \
     -o username=USER,password=PASS,uid=$(id -u),vers=3.0
# в deploy/.env:  BACKUP_HOST_DIR=/mnt/crm-backups
```

```yaml
# docker-compose: volume с драйвером cifs
services:
  app:
    environment:
      FIXBYTE_BACKUP_DIR: /mnt
    volumes:
      - crm_backups:/mnt
volumes:
  crm_backups:
    driver_opts:
      type: cifs
      o: "username=USER,password=PASS,uid=1000,vers=3.0"
      device: "//NAS/backups"
```

Периодичность планового бэкапа задаётся в панели администратора (см. выше),
а не переменной окружения.

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

## 10) Акт приёма оборудования в ремонт (PDF)

`GET /api/orders/{id}/act` — формирует минимальный акт (шапка с логотипом и
реквизитами; что приняли, от кого, примерная стоимость, срок «по
договорённости», строки для подписей), отдаёт `application/pdf` и
**сохраняет копию в заказ** как вложение `attachment_type = ACT`
(`stored_name = acceptance-act.pdf`, одна копия на заказ, перезаписывается).
Попадает в полный бэкап. В веб-CRM — кнопка «📄 Акт приёмки (PDF)» в окне
просмотра заказа.

- **Шапка (логотип + реквизиты)** — общая с чеком/квитанцией. Реквизиты
  редактируются в панели администратора → «Настройки чека» (название,
  подзаголовок, адрес, телефон, email, сотрудник) и хранятся в БД
  (`company_settings`, одна строка), эндпоинт `GET/PUT /api/settings/company`
  (PUT — только ADMIN). Начальные значения при первом старте —
  `FIXBYTE_COMPANY_{NAME,SUBTITLE,ADDRESS,PHONE,EMAIL}` (env; кириллицу в
  `application.properties` держать нельзя — ISO-8859-1). Логотип —
  `static/images/logo.png`.
- Шрифт PDF — `src/main/resources/fonts/DroidSans.ttf` (Apache 2.0,
  кириллица + №; знак ₽ отсутствует, используется «руб.»).
- **prod** (`ddl-auto=validate`): выполнить `sql/2026-09-09-company-settings.sql`
  (таблица `company_settings`). И, если у Hibernate ранее было создано
  CHECK-ограничение `attachment_type IN ('PHOTO','VIDEO','FILE')` — снять его
  (см. `sql/2026-09-09-acceptance-act.sql`), иначе вставка `ACT` упадёт. Тип
  вложения теперь хранится через `AttributeConverter`, новых CHECK не создаётся.
- Мобильное приложение (Android): на карточке клиента — список заказов и
  кнопка «+ Заказ»; экран заказа — кнопка «Акт приёмки — печать» (системный
  диалог печати Android → сетевой принтер по Mopria/IPP) и «Отправить PDF».
- Видимость отдельных полей чека (галочки в «Настройках чека») по-прежнему
  хранится локально в браузере — это настройка интерфейса, не реквизиты.

