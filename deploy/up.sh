#!/usr/bin/env sh
# FixByte CRM — запуск через Docker
#   ./up.sh            обычный режим (порт ${HOST_HTTP_PORT:-9087})
#   ./up.sh macvlan    свой IP в локальной сети (только Linux)
set -eu
cd "$(dirname "$0")"

if [ ! -s app/app.jar ]; then
    echo "Нет app/app.jar. Сначала соберите его:  ./build-jar.sh"
    echo "(или вручную: cp <ваш>.jar app/app.jar)"
    exit 1
fi

if [ ! -f .env ]; then
    cp .env.example .env
    echo "Создан deploy/.env — отредактируйте пароли (и для macvlan сеть), затем запустите снова."
    exit 1
fi

MODE="${1:-bridge}"
case "$MODE" in
    bridge)  FILE=docker-compose.yml ;;
    macvlan) FILE=docker-compose.macvlan.yml ;;
    *) echo "usage: $0 [bridge|macvlan]" >&2; exit 1 ;;
esac

echo "==> docker compose -f $FILE up -d --build"
docker compose -f "$FILE" up -d --build
docker compose -f "$FILE" ps

if [ "$MODE" = "macvlan" ]; then
    CRM_IP="$(grep -E '^CRM_IP=' .env | cut -d= -f2)"
    echo
    echo "CRM доступна в сети:  http://${CRM_IP:-<CRM_IP>}:9087"
    echo "Доступ с этого хоста: sudo ./macvlan/host-shim.sh up"
else
    PORT="$(grep -E '^HOST_HTTP_PORT=' .env | cut -d= -f2)"
    echo
    echo "CRM:  http://localhost:${PORT:-9087}"
fi
