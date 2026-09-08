# Хранилище полных бэкапов

Сюда приложение складывает **полные** резервные копии (`rembyte_backup_*_full.zip`):
дамп БД со всеми BLOB (фото, картинки, вложения) + legacy-папка `uploads`.

Каталог монтируется в контейнер `app` как `/backups` (`FIXBYTE_BACKUP_DIR=/backups`).
Управление — в панели администратора → **База данных** → «Хранилище полных бэкапов»:
сохранить сейчас, скачать, восстановить, удалить. Старые архивы удаляются
автоматически, остаётся последних `FIXBYTE_BACKUP_KEEP` штук (по умолчанию 14).

## Вариант 1. Каталог на хосте (по умолчанию)

Ничего делать не нужно — бэкапы лягут в `deploy/backups/` на хосте
(`BACKUP_HOST_DIR=./backups` в `.env`). Забирайте их оттуда любым способом
(rsync, scp, ваш бэкап-агент).

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
bind-mount строкой `- crm_backups:/backups`:

```yaml
services:
  app:
    volumes:
      - crm_backups:/backups
volumes:
  crm_backups:
    driver_opts:
      type: cifs
      o: "username=USER,password=PASS,uid=1000,vers=3.0"
      device: "//NAS/backups"
```

## Плановый полный бэкап

`FIXBYTE_BACKUP_CRON` — cron из 6 полей (Spring). `-` = выключено (по умолчанию).
Пример каждую ночь в 03:30: `FIXBYTE_BACKUP_CRON="0 30 3 * * *"`.

---

Сам этот каталог в git не хранит архивы — только этот README
(см. `deploy/backups/.gitignore`).
