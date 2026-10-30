# Build context: backend/ (the whole Gradle multi-project, since media-service depends on media-api).

# --- Build stage: compile with the Gradle wrapper on a full JDK ---
FROM amazoncorretto:25-al2023-jdk AS build
# The Gradle wrapper script needs xargs, which the minimal Amazon Linux image doesn't include.
RUN dnf install -y findutils && dnf clean all
WORKDIR /workspace
COPY . .
# Cache Gradle's downloads between image builds.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :media:media-service:bootJar --no-daemon

# --- Runtime stage: headless JRE + the jar ---
FROM amazoncorretto:25-al2023-headless
# Standard gRPC health checker, used by docker compose's healthcheck.
COPY --from=ghcr.io/grpc-ecosystem/grpc-health-probe:v0.4.60 /ko-app/grpc-health-probe /usr/local/bin/grpc_health_probe
WORKDIR /app
# A new named volume copies the ownership of the folder it's mounted on, so create it owned by the app user.
RUN mkdir -p /data/media && chown 1000:1000 /data/media
COPY --from=build /workspace/media/media-service/build/libs/media-service-*.jar app.jar
# Non-root (numeric, since the minimal image has no useradd).
USER 1000:1000
EXPOSE 9092
ENTRYPOINT ["java", "-jar", "app.jar"]
