#!/usr/bin/env sh
# Остановка. Том с БД сохраняется.
#   ./down.sh            обычный режим
#   ./down.sh macvlan
#   ./down.sh bridge -v  удалить и данные БД (осторожно)
set -eu
cd "$(dirname "$0")"

MODE="${1:-bridge}"
shift 2>/dev/null || true
case "$MODE" in
    bridge)  FILE=docker-compose.yml ;;
    macvlan) FILE=docker-compose.macvlan.yml ;;
    *) echo "usage: $0 [bridge|macvlan] [-v]" >&2; exit 1 ;;
esac

docker compose -f "$FILE" down "$@"
