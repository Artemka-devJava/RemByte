# Режим macvlan — свой IP в локальной сети

Контейнер приложения получает **собственный IP в вашей LAN** и виден как
отдельное устройство. Телефоны с приложениями (Android/iOS) и любой браузер
в сети заходят напрямую: `http://<CRM_IP>:9087` — без проброса портов и без
привязки к IP хоста.

```
[ роутер 192.168.1.1 ]
        │  LAN
        ├── ноутбук  192.168.1.10
        ├── телефон  192.168.1.33
        └── fixbyte-app 192.168.1.242   ← контейнер, свой MAC/IP (macvlan)
                 │ crm_internal (private)
                 └── fixbyte-db          ← наружу не виден
```

## Когда работает

| Хост | macvlan даёт LAN-IP контейнеру? |
|---|---|
| **Linux, проводное подключение** | ✅ да — целевой сценарий |
| Linux, Wi-Fi | ⚠️ обычно нет: точки доступа режут кадры с «чужим» MAC |
| **Docker Desktop (Windows / macOS)** | ❌ нет: Docker в NAT-ВМ. Используйте `../docker-compose.yml` |

## Подготовка

1. **Имя интерфейса хоста:**
   ```bash
   ip -o link show        # eth0 / enp3s0 / eno1 / end0 ...
   ```
2. **Зарезервировать адреса.** На роутере сузьте DHCP-пул так, чтобы блок
   `LAN_IP_RANGE` (по умолчанию `192.168.1.240/28`) роутер **не раздавал**.
   В нём живут `CRM_IP` и `HOST_SHIM_IP`.
3. **Промиск-режим** (если контейнер не отвечает по сети):
   ```bash
   sudo ip link set <MACVLAN_PARENT> promisc on
   ```

## Запуск

```bash
cd deploy
cp .env.example .env
nano .env        # MACVLAN_PARENT, LAN_SUBNET, LAN_GATEWAY,
                 # LAN_IP_RANGE, CRM_IP, HOST_SHIM_IP, пароли

./up.sh macvlan
./logs.sh macvlan
```

Проверка:

```bash
docker exec fixbyte-app wget -qO- http://localhost:9087/login | head -c 60   # изнутри
curl -I http://192.168.1.242:9087/login                                      # с телефона/ПК: 200
```

В приложении на телефоне на экране входа: `http://<CRM_IP>:9087`.

## Доступ с самого Docker-хоста

Хост по умолчанию **не видит** свои macvlan-контейнеры:

```bash
sudo ./macvlan/host-shim.sh up       # создаёт интерфейс crm-shim + маршрут
sudo ./macvlan/host-shim.sh status
curl -I http://192.168.1.242:9087/login
sudo ./macvlan/host-shim.sh down     # убрать
```

Автозапуск shim после перезагрузки — юнит `fixbyte-macvlan-shim.service`
(инструкция внутри файла; предполагает проект в `/opt/fixbyte-crm`).

## Если не работает

| Симптом | Причина / решение |
|---|---|
| `parent interface ... not found` | неверный `MACVLAN_PARENT` (см. `ip -o link show`) |
| контейнер поднялся, но с телефона `curl <CRM_IP>:9087` молчит | Wi-Fi у контейнера/телефона; промиск выключен; `CRM_IP` вне `LAN_SUBNET`; firewall хоста |
| `Address already in use` / конфликт IP | `CRM_IP` попал в DHCP-пул — сузьте пул или смените адрес |
| с хоста не открывается, с телефона — да | норма для macvlan → `host-shim.sh up` |
| `Communications link failure` при старте app | БД ещё поднимается (`start_period` 40 c) — подождите, смотрите `./logs.sh macvlan db` |
| сменить IP контейнера | поправьте `CRM_IP` в `.env` → `docker compose -f docker-compose.macvlan.yml up -d --force-recreate app` |
