# Хранилище полных бэкапов

Сюда приложение складывает **полные** резервные копии (`rembyte_backup_*_full.zip`):
дамп БД со всеми BLOB (фото, картинки, вложения) + legacy-папка `uploads`.

**По умолчанию контейнер `app` монтирует РЕАЛЬНЫЙ `/mnt` Linux-хоста**
(`docker-compose.yml`: `- /mnt:/mnt`) — то есть видит те же точки
монтирования (NAS, USB-диски и т.п.), что и сам хост под `/mnt`. Конкретную
папку внутри выбирайте прямо в панели администратора → **База данных** →
«Хранилище полных бэкапов»: кнопка **«Обзор»** показывает реальное дерево
папок хоста, путь обязан лежать внутри `/mnt`, иначе сервер откажет. Там же:
сохранить сейчас, скачать, восстановить, удалить. Старые архивы удаляются
автоматически, остаётся последних `FIXBYTE_BACKUP_KEEP` штук (по умолчанию 14).

Этот каталог (`deploy/backups/`) в такой схеме **не используется** — это
просто одна из папок хоста, ничем не выделенная. Он остаётся полезен только
для варианта 1 ниже.

## Вариант 1. Одна конкретная папка на хосте вместо всей /mnt

Если не хотите отдавать контейнеру весь `/mnt` хоста, в `docker-compose.yml`
замените volume `- /mnt:/mnt` на `- ${BACKUP_HOST_DIR:-./backups}:/mnt` —
тогда в контейнер попадёт только один каталог, по умолчанию этот самый
`deploy/backups/` (`BACKUP_HOST_DIR=./backups` в `.env`). Заберите бэкапы
оттуда любым способом (rsync, scp, ваш бэкап-агент), либо укажите свою
папку в `BACKUP_HOST_DIR`.

## Вариант 2. Сохранение через Samba/CIFS

### 2а. Монтирование шары на хосте

```bash
sudo mkdir -p /mnt/crm-backups
sudo mount -t cifs //NAS/backups /mnt/crm-backups \
     -o username=USER,password=PASS,uid=$(id -u),gid=$(id -g),vers=3.0
# затем в deploy/.env:
#   BACKUP_HOST_DIR=/mnt/crm-backups
```

Постоянное монтирование — строка в `/etc/fstab`:

```
//NAS/backups  /mnt/crm-backups  cifs  credentials=/etc/crm-backups.cred,uid=1000,gid=1000,vers=3.0,_netdev  0  0
```

(`/etc/crm-backups.cred` — файл с `username=` и `password=`, права `600`.)

### 2б. CIFS-volume прямо в docker-compose

В `docker-compose.yml` (и `docker-compose.macvlan.yml`) раскомментируйте volume
`crm_backups` и подставьте свои данные, затем в сервисе `app` замените
bind-mount строкой `- crm_backups:/mnt`:

```yaml
services:
  app:
    volumes:
      - crm_backups:/mnt
volumes:
  crm_backups:
    driver_opts:
      type: cifs
      o: "username=USER,password=PASS,uid=1000,vers=3.0"
      device: "//NAS/backups"
```

## Плановый полный бэкап

Периодичность задаётся в панели администратора → **База данных** →
«Автоматический бэкап»: выключено / раз в день / месяц / квартал / год.
Проверка срока — каждую ночь в 03:30. Настройка хранится в БД, отдельная
переменная окружения не нужна.

---

Сам этот каталог в git не хранит архивы — только этот README
(см. `deploy/backups/.gitignore`).
