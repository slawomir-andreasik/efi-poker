# FALLBACK Dockerfile - kept for fast local dev builds (compose.dev.yaml) and as a safety net.
# Production images are built via `make image-build` (Paketo buildpacks + AOT cache).
# See: backend/build.gradle.kts bootBuildImage task.

# Build stage
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY api ./api
COPY backend ./backend
RUN chmod +x gradlew && ./gradlew :backend:bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN addgroup --system --gid 1001 appgroup && \
    adduser --system --uid 1001 --ingroup appgroup appuser

COPY --from=build /app/backend/build/libs/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:8080/actuator/health || exit 1

EXPOSE 8080
USER appuser
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/urandom", \
  "-jar", "app.jar"]
