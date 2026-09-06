#!/usr/bin/env bash
# FixByte CRM — запуск
#   ./run.sh          авто: MariaDB, иначе встроенная H2
#   ./run.sh --h2     принудительно H2
#   ./run.sh --build  собрать jar перед запуском
set -euo pipefail
cd "$(dirname "$0")"

FORCE_H2=0
BUILD=0
for a in "$@"; do
  case "$a" in
    --h2) FORCE_H2=1 ;;
    --build) BUILD=1 ;;
  esac
done

MVN="mvn"
[ -x ./mvnw ] && MVN="./mvnw"

if [ "$BUILD" = 1 ]; then
  echo "==> Сборка (mvn -q package -DskipTests)"
  $MVN -q package -DskipTests
fi

port_open() { (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null && exec 3>&- && return 0 || return 1; }

USE_H2=$FORCE_H2
if [ "$USE_H2" = 0 ]; then
  if port_open 9092; then
    echo "==> MariaDB найдена на 127.0.0.1:9092 — профиль dev"
  else
    echo "==> MariaDB на 127.0.0.1:9092 не отвечает — переключаюсь на встроенную H2"
    USE_H2=1
  fi
fi

echo
echo "FixByte CRM: http://localhost:9087   (admin / см. .env)"
echo "Остановка: Ctrl+C"
echo

if [ "$USE_H2" = 1 ]; then
  exec $MVN spring-boot:run -Ph2 -Dspring-boot.run.profiles=h2
else
  exec $MVN spring-boot:run
fi
