# FALLBACK ONLY - NOT used for production images.
# Production: `make image-build` (Paketo buildpacks via bootBuildImage in build.gradle.kts)
# This Dockerfile: local dev builds (compose.dev.yaml) and CI fallback only.

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

# Patch OS-level CVEs on every build
RUN apt-get update && apt-get upgrade -y && rm -rf /var/lib/apt/lists/*

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
