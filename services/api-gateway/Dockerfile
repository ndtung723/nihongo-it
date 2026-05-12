# Build stage — context = services/ (root multi-module project)
FROM gradle:9.5-jdk21 AS builder
WORKDIR /app

COPY settings.gradle.kts build.gradle.kts ./
COPY common common
COPY api-gateway api-gateway

RUN gradle :api-gateway:bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/api-gateway/build/libs/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
