#!/usr/bin/env sh
# Собирает jar приложения из исходников и кладёт в deploy/app/app.jar.
# Запускать один раз (на машине с Maven + JDK 17), потом папку deploy/
# можно копировать на сервер с одним лишь Docker.
#
#   ./build-jar.sh            исходники в ../  (корень репозитория)
#   ./build-jar.sh /path/src  другой путь к проекту с pom.xml
#
# Либо просто вручную: cp <ваш>.jar app/app.jar
set -eu
cd "$(dirname "$0")"

SRC="${1:-..}"
[ -f "$SRC/pom.xml" ] || { echo "Не найден $SRC/pom.xml"; exit 1; }

echo "==> mvn -f $SRC/pom.xml clean package -DskipTests"
mvn -f "$SRC/pom.xml" -q clean package -DskipTests

JAR="$(ls "$SRC"/target/rembyte-crm-*.jar 2>/dev/null | grep -vE 'sources|javadoc' | head -n1)"
[ -n "$JAR" ] || { echo "jar не найден в $SRC/target"; exit 1; }

mkdir -p app
cp "$JAR" app/app.jar
echo "OK: app/app.jar  ($(du -h app/app.jar | cut -f1))  <- $(basename "$JAR")"

# SQL-миграции для prod (ddl-auto=validate) — кладём рядом, чтобы папка была самодостаточной.
if [ -d "$SRC/sql" ]; then
    mkdir -p sql
    cp "$SRC"/sql/*.sql sql/ 2>/dev/null || true
    echo "OK: sql/  ($(ls sql/*.sql 2>/dev/null | wc -l | tr -d ' ') файлов)"
fi

echo "Дальше:  cp .env.example .env  &&  ./up.sh"
