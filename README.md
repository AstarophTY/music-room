# Music Room

Collaborative mobile music application.

**Stack:** Kotlin Multiplatform, Compose Multiplatform (Android), Ktor (server), Exposed, PostgreSQL, Flyway.

## Requirements

- Docker + Docker Compose
- JDK 17 (for `make dev`, `make test`, `make build`)
- Android Studio or an Android SDK (mobile app only, or use `make android-sdk`)

## Quick start

```bash
make env   # create .env with generated secrets
make up    # start PostgreSQL + the server in Docker
```

- API: http://localhost:8080/health
- Swagger: http://localhost:8080/swagger

## Commands

| Command            | Description                                                                       |
|--------------------|-----------------------------------------------------------------------------------|
| `make env`         | Create `.env` from `.env.example` (generates JWT_SECRET and the DB password)      |
| `make up`          | Build and start PostgreSQL + server in Docker, wait until healthy                 |
| `make down`        | Stop containers                                                                   |
| `make restart`     | Restart containers                                                                |
| `make logs`        | Follow server logs                                                                |
| `make ps`          | List containers                                                                   |
| `make dev`         | PostgreSQL in Docker, server running locally with Gradle (faster for development) |
| `make db`          | Start PostgreSQL only                                                             |
| `make psql`        | Open a `psql` shell on the database                                               |
| `make tools`       | Start pgAdmin on http://localhost:5050                                            |
| `make test`        | Run all tests (Docker required for Testcontainers)                                |
| `make build`       | Compile all modules                                                               |
| `make android-sdk` | Install the Android SDK + an emulator (no root needed)                            |
| `make emulator`    | Start the Android emulator                                                        |
| `make android`     | Install and launch the app on the connected emulator/device                       |
| `make fclean`      | Remove containers, volumes (data) and build outputs                               |
| `make re`          | `fclean` then `up`                                                                |

## Structure

```
shared/       DTOs, API contract, Ktor HTTP client (shared by client and server)
server/       Ktor REST + WebSocket API, Exposed, Flyway migrations
androidApp/   Android application entry point
composeApp/   Shared Compose UI
```

### Backend (NestJS-like structure)

```
server/src/main/kotlin/com/musicroom/server/
├── Application.kt          # main.ts: reads config, connects the database, bootstraps
├── AppModule.kt            # app.module.ts: list of modules + global plugins
├── config/                 # typed config (ConfigModule)
├── database/               # DatabaseModule: Hikari pool, Flyway, Exposed transactions
├── common/
│   ├── Module.kt           # Module / Controller interfaces
│   ├── exceptions/         # NotFoundException, ConflictException...
│   ├── filters/            # ExceptionFilter: exception -> JSON response
│   ├── guards/             # jwtGuard { }, call.currentUserId()
│   ├── interceptors/       # request logs with X-Platform / X-Device / X-App-Version
│   ├── jwt/                # JwtService (token creation/verification)
│   ├── middleware/         # CORS, security headers
│   ├── throttler/          # rate limiting (global + authThrottle { })
│   ├── serialization/
│   └── websocket/
└── modules/
    ├── system/             # /health, /api/v1/info
    └── users/
        ├── UsersModule.kt       # wires repository, service, controller
        ├── UsersController.kt   # HTTP routes
        ├── UsersService.kt      # business logic
        ├── UsersRepository.kt   # database access (Exposed)
        └── entities/            # Exposed tables + models
```

To add a feature: create `modules/<feature>/` following the same pattern, then add the module to the list in `AppModule`.

- SQL migrations: `server/src/main/resources/db/migration` (`V2__xxx.sql`, `V3__xxx.sql`...). Never edit a migration that has already been applied.
- OpenAPI docs: `server/src/main/resources/openapi/documentation.yaml`.
- The client sends `X-Platform`, `X-Device` and `X-App-Version` on every request; the server writes them on every log line.

## Android app

Open the project in Android Studio (it creates `local.properties` with the SDK path) and run the `androidApp` configuration, or use `make android-sdk`, `make emulator` then `make android`.

The `androidApp` and `composeApp` modules are only included in the build when an Android SDK is found (`local.properties`, `ANDROID_HOME` or `ANDROID_SDK_ROOT`), so the server builds without an SDK, including in Docker.

The backend URL is set in the app. It defaults to `http://10.0.2.2:8080`, which points to the host machine from the emulator. On a real phone, use your computer's local IP.

## Secrets

All secrets live in `.env`, which is git-ignored. Only `.env.example` is committed. **Never commit a secret.**

## iOS app

Open `iosApp/iosApp.xcodeproj` in Xcode and run the `iosApp` scheme on an Apple Silicon simulator or a signed device. The Xcode build invokes Gradle to build the shared Compose framework and copy its resources. The Android SDK must also be configured because `composeApp` is included only when an Android SDK is available. The project supports iOS 15 and later; Intel simulators are not supported by the current Compose dependencies.

The default server URL is `http://localhost:8080`, which reaches the host from the simulator. Set a reachable server URL on a physical device. The app currently permits HTTP connections, matching the Android client; use HTTPS for production.
