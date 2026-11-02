# syntax=docker/dockerfile:1

FROM gradle:8.14.3-jdk17 AS build
WORKDIR /app

COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle/libs.versions.toml gradle/libs.versions.toml
COPY shared/build.gradle.kts shared/build.gradle.kts
COPY server/build.gradle.kts server/build.gradle.kts
RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle :server:dependencies --no-daemon -q > /dev/null || true

COPY shared shared
COPY server server
RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle :server:installDist --no-daemon -x test

FROM eclipse-temurin:17-jre
RUN groupadd --system app && useradd --system --gid app app \
    && apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/server/build/install/music-room-server ./
USER app
EXPOSE 8080
ENTRYPOINT ["./bin/music-room-server"]
