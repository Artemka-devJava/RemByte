#!/usr/bin/env sh
# ============================================================
# host-shim.sh — доступ Docker-хоста к CRM-контейнеру в сети macvlan
#
# Ядро Linux изолирует хост и его macvlan-контейнеры друг от друга.
# Скрипт создаёт на хосте отдельный macvlan-интерфейс ("crm-shim")
# в той же физической сети и маршрут к диапазону контейнеров.
#
#   sudo ./host-shim.sh up      создать shim и маршрут
#   sudo ./host-shim.sh down    удалить
#   sudo ./host-shim.sh status  показать состояние
#
# Параметры берутся из ../.env (MACVLAN_PARENT, HOST_SHIM_IP, LAN_IP_RANGE).
# Другим устройствам сети (телефонам) shim НЕ нужен — только самому хосту.
# ============================================================
set -eu

DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="${ENV_FILE:-$DIR/../.env}"
if [ -f "$ENV_FILE" ]; then
    set -a
    . "$ENV_FILE"
    set +a
fi

PARENT="${MACVLAN_PARENT:?MACVLAN_PARENT не задан (см. deploy/.env)}"
SHIM_IP="${HOST_SHIM_IP:?HOST_SHIM_IP не задан (см. deploy/.env)}"
RANGE="${LAN_IP_RANGE:?LAN_IP_RANGE не задан (см. deploy/.env)}"
SHIM="crm-shim"

need_root() { [ "$(id -u)" = "0" ] || { echo "Нужны права root (sudo)"; exit 1; }; }

case "${1:-}" in
  up)
    need_root
    ip link show "$SHIM" >/dev/null 2>&1 || \
        ip link add "$SHIM" link "$PARENT" type macvlan mode bridge
    ip addr show dev "$SHIM" 2>/dev/null | grep -q "inet ${SHIM_IP}/" || \
        ip addr add "${SHIM_IP}/32" dev "$SHIM"
    ip link set "$SHIM" up
    ip route replace "$RANGE" dev "$SHIM"
    echo "OK: '$SHIM' поднят (parent=$PARENT, ip=$SHIM_IP), маршрут $RANGE -> $SHIM"
    ;;
  down)
    need_root
    ip link del "$SHIM" 2>/dev/null || true
    echo "OK: '$SHIM' удалён"
    ;;
  status)
    if ip link show "$SHIM" >/dev/null 2>&1; then
        ip -br addr show dev "$SHIM"
        ip route show "$RANGE" || true
    else
        echo "'$SHIM' не создан"
    fi
    ;;
  *)
    echo "usage: $0 up|down|status" >&2
    exit 1
    ;;
esac
