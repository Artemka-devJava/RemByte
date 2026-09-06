# deploy/ — запуск FixByte CRM через Docker

Самодостаточная папка: содержит **всё для запуска**. Соберите один раз jar
(нужны Maven + JDK 17), дальше папку `deploy/` можно целиком скопировать на
любой сервер с одним лишь Docker — исходники и Maven там уже не нужны.

```
deploy/
  Dockerfile                    runtime-образ из app/app.jar (без Maven)
  app/app.jar                   ← сюда кладётся собранное приложение
  build-jar.sh / build-jar.ps1  собрать app/app.jar из исходников
  .env.example                  шаблон конфигурации
  docker-compose.yml            обычный режим (порт на хосте)
  docker-compose.macvlan.yml    свой IP в локальной сети (Linux)
  up.sh / down.sh / logs.sh     обёртки        up.ps1  (Windows, bridge)
  mariadb/init/01_init.sql      инициализация БД
  macvlan/                      host-shim.sh + systemd-юнит + гайд
```

## Шаг 1. Собрать jar (один раз)

На машине с Maven и JDK 17 (обычно там же, где исходники):

```bash
cd deploy
./build-jar.sh            # Windows: .\build-jar.ps1
```

Появится `app/app.jar` (~55 МБ). Если jar уже собран где-то ещё —
просто положите его: `cp путь/к/rembyte-crm-*.jar app/app.jar`.

## Шаг 2. Запуск (нужен только Docker)

Скопируйте папку `deploy/` на сервер и:

```bash
cd deploy
cp .env.example .env      # ОБЯЗАТЕЛЬНО смените пароли
nano .env
./up.sh                   # Windows: .\up.ps1
```

Готово: `http://localhost:9087` (или `http://<IP-сервера>:9087` из сети).
Логин — из `.env` (`FIXBYTE_ADMIN_*`).

Управление:

```bash
./logs.sh            # логи приложения (./logs.sh bridge db — логи БД)
./down.sh            # остановить (данные БД сохраняются)
./down.sh bridge -v  # остановить и УДАЛИТЬ данные БД
```

Обновление новой версии: пересоберите jar (`./build-jar.sh`) и `./up.sh`
(пересоберёт образ и перезапустит).

## Режим macvlan (свой IP в сети — для телефонов)

Контейнер приложения появляется в вашей LAN как отдельное устройство,
приложения на телефонах ходят прямо на `http://<CRM_IP>:9087`.

```bash
cd deploy
cp .env.example .env      # заполнить MACVLAN_PARENT, LAN_SUBNET, LAN_GATEWAY,
                          # LAN_IP_RANGE, CRM_IP, HOST_SHIM_IP
./up.sh macvlan
sudo ./macvlan/host-shim.sh up     # чтобы сам сервер тоже видел контейнер
```

Только Linux-хост, подключённый к сети кабелем. Docker Desktop
(Windows/macOS) — используйте обычный режим. Подробности — `macvlan/README.md`.

## Доступ к БД

MariaDB наружу не проброшена. Консоль:

```bash
docker exec -it fixbyte-db mariadb -uroot -p rembyte
```

Бэкап тома:

```bash
docker run --rm -v fixbyte-crm_fixbyte-db-data:/data -v "$PWD":/backup alpine \
  tar czf /backup/db-$(date +%F).tar.gz -C /data .
```

## Профиль приложения

Контейнер запускается с `SPRING_PROFILES_ACTIVE=docker`
(`application-docker.properties`, `ddl-auto=update` — схема
создаётся/мигрируется автоматически). Для prod с `validate` выполните SQL
из `sql/` репозитория до первого запуска новой версии.
