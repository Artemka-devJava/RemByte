# ============================================================
# FixByte CRM — runtime-образ для папки deploy/
# Собирается из ГОТОВОГО jar (app/app.jar), Maven/JDK не нужны.
# Положить jar:  ./build-jar.sh   (или вручную скопировать в app/app.jar)
# ============================================================
FROM eclipse-temurin:17-jre-alpine

LABEL description="FixByte CRM — сервис управления ремонтом ПК"

WORKDIR /app

RUN addgroup -S fixbyte && adduser -S fixbyte -G fixbyte

COPY app/app.jar app.jar
RUN chown fixbyte:fixbyte app.jar

USER fixbyte
EXPOSE 9087

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=5 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:9087/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
