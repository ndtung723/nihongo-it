# Build stage — context = services/ (root multi-module project)
FROM gradle:8.14-jdk21 AS builder
WORKDIR /app

COPY settings.gradle.kts build.gradle.kts ./
COPY common common
COPY user-service user-service

RUN gradle :user-service:bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/user-service/build/libs/*-SNAPSHOT.jar app.jar

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8086/actuator/health || exit 1

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]
