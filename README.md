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
- Emails (Mailpit): http://localhost:8025

On Windows/WSL, Docker Desktop must have WSL integration enabled for your distro (Settings > Resources > WSL integration).

### Try the user system

From Swagger (http://localhost:8080/swagger) or with curl:

```bash
# Sign up (returns an access token + a refresh token, and emails a 6-digit code)
curl -s -X POST localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"hunter42abc","displayName":"Alice","acceptedTerms":true}'

# Read the code in Mailpit (http://localhost:8025), then verify the email
curl -s -X POST localhost:8080/api/v1/auth/email/verify -H 'Authorization: Bearer <ACCESS_TOKEN>' \
  -H 'Content-Type: application/json' -d '{"code":"123456"}'

# Log in
curl -s -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"hunter42abc"}'

# Profile (replace <ACCESS_TOKEN>)
curl -s localhost:8080/api/v1/users/me -H 'Authorization: Bearer <ACCESS_TOKEN>'

# Check that the password is hashed in the database
make psql   # then: SELECT email, password_hash FROM users;
```

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
| `make linux`       | Run the desktop app (on WSL it opens through WSLg)                                |
| `make windows`     | Build the Windows app with its own Java in `build/windows/MusicRoom` and start it |
| `make web`         | Run the web app on http://localhost:3000                                          |
| `make fclean`      | Remove containers, volumes (data) and build outputs                               |
| `make re`          | `fclean` then `up`                                                                |

## Structure

```
shared/       DTOs, API contract, Ktor HTTP client (shared by client and server)
server/       Ktor REST + WebSocket API, Exposed, Flyway migrations
androidApp/   Android application entry point
composeApp/   Shared Compose UI + desktop (desktopMain) and web (wasmJsMain) entry points
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
│   ├── security/           # PasswordHasher (bcrypt)
│   ├── throttler/          # rate limiting (global + authThrottle { })
│   ├── serialization/
│   └── websocket/
└── modules/
    ├── system/             # /health, /api/v1/info
    ├── auth/               # register, login, refresh, logout, change password
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

The backend URL comes from `BACKEND_URL` in `.env` and is baked into the app at build time (Gradle task `generateAppEnv` creates `AppEnv.BACKEND_URL`). Rebuild the app after changing it.

| Where the app runs | `BACKEND_URL`                            |
|--------------------|------------------------------------------|
| Android emulator   | `http://10.0.2.2:8080` (default)         |
| iOS simulator      | `http://localhost:8080`                  |
| Real phone         | `http://<your computer's local IP>:8080` |

## User system

- Sign up in the app in 3 pages: email, password + confirmation, display name + terms of use (required, checked by the server too).
- **Email verification**: a 6-digit code is emailed after sign-up. It is stored hashed, works once, expires after 15 min and allows 5 tries; a new one can be requested once a minute. The app shows the code screen until the email is verified.
- In development (`SMTP_HOST` empty), emails go to **Mailpit**: open http://localhost:8025 to read them. Set `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD` and `SMTP_FROM` to use a real SMTP server.
- Sign up / log in with email + password. Passwords are hashed with **bcrypt** (cost 12, random salt) and never stored or logged in clear text.
- **Access token**: JWT valid 15 min (`JWT_ACCESS_TTL_MINUTES`), sent as `Authorization: Bearer`.
- **Refresh token**: random value valid 30 days (`JWT_REFRESH_TTL_DAYS`), stored **hashed** (SHA-256) in `refresh_tokens`. Each one works once (rotation); reusing an old one revokes every session of the user.
- Changing the password signs out every other device and **emails a security alert**. Logging out revokes the refresh token.
- **Forgot password**: "Forgot password?" on the login screen emails a 6-digit code (same rules as the verification code), then the user sets a new password. The server answers the same way whether the email exists or not.
- **Two-factor authentication** (Profile > Security), off until the user turns it on, with one method at a time:
  - **Authenticator app**: TOTP codes (QR code or key), RFC 6238 implemented in `common/security/Totp.kt`. Secrets are encrypted in the database (AES-GCM, key derived from `JWT_SECRET`), each code works once, 10 single-use recovery codes (hashed), 5 wrong codes lock 2FA for 15 min.
  - **Email**: turned on with a code sent by `POST /api/v1/auth/2fa/email/code` then `POST /api/v1/auth/2fa/email/enable`. At each login a 6-digit code is emailed (15 min, single use, new one after 1 min with `POST /api/v1/auth/2fa/email/resend`). A password reset counts as the email step.
  - With 2FA on, login / Google / password reset answer `202` with a 5-minute challenge (`method`: `app` or `email`), then `POST /api/v1/auth/2fa/verify`. Turning 2FA on or off emails an alert.
- **Profile picture**: a photo (Android picker, file dialog on desktop and web; not on iOS yet) re-encoded by the server as a 256x256 JPEG, or initials on one of 6 Radix colors.
- **Social networks**: usernames for Instagram, TikTok, X, YouTube, Twitch, Discord, Spotify and SoundCloud shown on the profile (links open the network's page). Pasted links and "@name" are cleaned; each network's username format is checked (`SocialLinks` in `shared`).
- Errors carry a machine-readable `error` code (`ApiErrors`, `ValidationError` in `shared`): the app translates it.
- Auth routes are limited to 10 requests/min per IP (brute force protection).
- Validation rules live in `shared` (`AuthValidation`): the app uses them for instant feedback, the server enforces them.
- The app keeps only the refresh token on the device and refreshes the access token automatically.

### Google and Apple sign-in

The app gets an ID token from Google/Apple and sends it to `POST /api/v1/auth/oauth`. The server checks its signature with the provider's public keys, its issuer, audience and expiry: the app is never trusted. A new account must accept the terms (the server answers `403 terms_required`, the app shows the terms). An identity whose email matches an account is linked to it; if that account never verified its email, its password is removed (someone else may have created it).

**Google (Android):**
1. In [Google Cloud Console](https://console.cloud.google.com/apis/credentials), create an OAuth consent screen, then two OAuth client IDs:
   - **Android**: package `com.musicroom.app`, SHA-1 of the debug key (`keytool -list -v -keystore ~/.android/debug.keystore -storepass android`);
   - **Web application**: its client ID is the one to use below.
2. Set `GOOGLE_CLIENT_ID=<web client ID>` in `.env`, then `make up` (server) and `make android` (the app bakes it in).
3. The emulator needs a Google account (Settings > Passwords & accounts).

Without `GOOGLE_CLIENT_ID`, the button shows an error message and the server refuses Google tokens.

**Apple:** the server already verifies Apple ID tokens (`APPLE_CLIENT_ID` = the iOS bundle ID). The button is not in the app yet: Sign in with Apple has no Android SDK, and on iOS it needs a paid Apple Developer account and a Mac with Xcode.

## App design and languages

- Navigation: avatar (top left) opens the profile and settings; bottom bar with Home, Search, Events (middle), Playlists, Create. Search, events, playlists and create show placeholders until the matching services exist on the server.
- Colors: [Radix Colors](https://www.radix-ui.com/colors) yellow + sand, light and dark themes, all in `theme/color/Palette.kt`. The theme (system, light, dark) is chosen in Profile > Appearance and saved on the device.
- Icons: [Lucide](https://lucide.dev) (`com.composables:icons-lucide-cmp`).
- Languages: English and French, from `composeApp/src/commonMain/composeResources/values*/strings.xml`. The app follows the phone's language and sends it as `Accept-Language`, so server emails use it too.

## Secrets

All secrets live in `.env`, which is git-ignored. Only `.env.example` is committed. **Never commit a secret.**

## iOS app

Open `iosApp/iosApp.xcodeproj` in Xcode and run the `iosApp` scheme on an Apple Silicon simulator or a signed device. The Xcode build invokes Gradle to build the shared Compose framework and copy its resources. The Android SDK must also be configured because `composeApp` is included only when an Android SDK is available. The project supports iOS 15 and later; Intel simulators are not supported by the current Compose dependencies.

The default server URL is `http://localhost:8080`, which reaches the host from the simulator. Set a reachable server URL on a physical device. The app currently permits HTTP connections, matching the Android client; use HTTPS for production.
