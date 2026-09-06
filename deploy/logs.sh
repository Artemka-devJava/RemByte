#!/usr/bin/env sh
# Логи приложения.  ./logs.sh [bridge|macvlan] [db]
set -eu
cd "$(dirname "$0")"

MODE="${1:-bridge}"
SVC="${2:-app}"
case "$MODE" in
    bridge)  FILE=docker-compose.yml ;;
    macvlan) FILE=docker-compose.macvlan.yml ;;
    *) echo "usage: $0 [bridge|macvlan] [app|db]" >&2; exit 1 ;;
esac

docker compose -f "$FILE" logs -f --tail=200 "$SVC"
