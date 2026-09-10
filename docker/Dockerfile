FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY wallet-common ./wallet-common
COPY wallet-core ./wallet-core
COPY wallet-service ./wallet-service
COPY wallet-api ./wallet-api
RUN mvn -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --home-dir /home/appuser appuser

COPY --from=build /workspace/wallet-api/target/wallet-api-1.0.0-SNAPSHOT.jar /app/app.jar
RUN chown -R appuser:appuser /app

USER appuser
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
