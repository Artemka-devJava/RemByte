# ============================================================
# FixByte CRM — Dockerfile (multi-stage build)
# Stage 1: сборка JAR через Maven
# Stage 2: минимальный runtime-образ (JRE Alpine)
# ============================================================

# ── Stage 1: Build ──────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /build

# Копируем pom.xml отдельно — слой кешируется пока pom.xml не меняется
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# Копируем исходный код и собираем JAR
COPY src ./src
RUN mvn clean package -DskipTests -B --no-transfer-progress

# ── Stage 2: Runtime ────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="FixByte CRM"
LABEL description="FixByte CRM — сервис управления ремонтом ПК"

WORKDIR /app

# Создаём непривилегированного пользователя
RUN addgroup -S fixbyte && adduser -S fixbyte -G fixbyte

# Копируем JAR из build-стадии
COPY --from=build /build/target/rembyte-crm-1.0.3.jar app.jar

# Назначаем владельца
RUN chown fixbyte:fixbyte app.jar

USER fixbyte

EXPOSE 9087

# Запуск с настройками JVM для контейнера
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]

