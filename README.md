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
- Emails: printed in the server logs (`make logs`) while `SMTP_HOST` is empty

On Windows/WSL, Docker Desktop must have WSL integration enabled for your distro (Settings > Resources > WSL integration).

### Try the user system

From Swagger (http://localhost:8080/swagger) or with curl:

```bash
# Sign up (returns an access token + a refresh token, and emails a 6-digit code)
curl -s -X POST localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"hunter42abc","displayName":"Alice","acceptedTerms":true}'

# Read the code in the server logs (make logs), then verify the email
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
| `make ios`         | Build and launch the app on an iOS simulator (macOS + Xcode)                      |
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
- In development (`SMTP_HOST` empty), emails are not sent: they are written to the server logs (`make logs`, or the Gradle output with `make dev`). Set `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD` and `SMTP_FROM` to use a real SMTP server.
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

### Spotify: playback and recommendations

1. Create an app on the [Spotify dashboard](https://developer.spotify.com/dashboard), add the redirect URI `http://127.0.0.1:8080/api/v1/spotify/callback` (or your `SPOTIFY_REDIRECT_URI`), and add each tester's Spotify email under *User Management* (development mode allows 5 users and needs a Premium owner).
2. Set `SPOTIFY_CLIENT_ID` and `SPOTIFY_CLIENT_SECRET` in `.env`, then `make up`.
3. In the app: Home, Search or Profile > Spotify > *Connect Spotify*. The Spotify login opens in the browser, and the app picks up the account once it is linked.

- **Playback** uses Spotify Connect: the app is a remote, the music plays in the Spotify app on one of the user's devices (phone, computer, speaker, or the web player at open.spotify.com). It needs **Spotify Premium** and Spotify open somewhere; otherwise the player bar says so (`spotify_no_device`, `spotify_premium_required`). Routes: `GET /api/v1/music/player`, `PUT .../player/play|pause|seek`, `POST .../player/next|previous`.
- **Recommendations** (`GET /api/v1/music/me/recommendations`): Spotify's own recommendations endpoint is closed to new apps, so the server builds them from the user's top tracks and more tracks from their top artists (search). Cached 10 minutes per user to save the Spotify quota.
- Search asks Spotify for 10 results per page, the development-mode maximum.

## Music Room Premium (Stripe)

A monthly subscription that lets users upload their own songs (MP3, 20 MB at most, 200 per user). Their songs show on their profile (*Profile > My songs*, and on their public page) and can be added to playlists. The app plays them itself, next to Spotify.

1. In the [Stripe dashboard](https://dashboard.stripe.com) (test mode is fine), create a product with a monthly recurring price, and copy the price ID (`price_...`) and the secret key (`sk_...`) into `STRIPE_PRICE_ID` and `STRIPE_SECRET_KEY` in `.env`.
2. Optional: add a webhook endpoint `<server>/api/v1/billing/webhook` with the events `checkout.session.completed` and `customer.subscription.*`, and put its signing secret in `STRIPE_WEBHOOK_SECRET` (in development: `stripe listen --forward-to localhost:8080/api/v1/billing/webhook`). Without a webhook, the server reads the subscription from Stripe when the user comes back from the payment page and every few minutes after.
3. Dashboard settings (no code): turn on the customer portal (*Settings > Billing > Customer portal*, cancel at period end), Smart Retries and failed-payment emails (*Billing > Revenue recovery*), and invoice and receipt emails. For VAT: turn on Stripe Tax, set the product tax code (electronically supplied services), add the French registration, then set `STRIPE_AUTOMATIC_TAX=true`.
4. In the app: *Profile > Music Room Premium > Subscribe* opens Stripe's payment page in the browser; Premium turns on in the app right after paying. *Manage my subscription* opens Stripe's page to change the card or cancel.

- **Stripe calls**: API version pinned (`2025-09-30.clover`), subscriptions in flexible billing mode, one customer per user (idempotency key). Webhook events only say what changed: the server reads the subscription again from Stripe, as events can arrive out of order.
- **Tables** (`V13__premium.sql`): `subscriptions` (Stripe customer and subscription status per user), `hosted_tracks` and `hosted_track_audio` (the MP3 files, kept apart so lists stay light).
- **Routes**: `GET /api/v1/billing`, `POST /api/v1/billing/checkout|portal`, `POST /api/v1/billing/webhook` (Stripe, signed), `GET|POST /api/v1/tracks`, `DELETE /api/v1/tracks/{id}`, `GET /api/v1/tracks/{id}/audio`, `GET /api/v1/users/{id}/tracks`.
- **Playback**: uploaded songs have the `MUSIC_ROOM` source and an id starting with `mr-`. A list with uploaded songs is played by the app song after song: Spotify songs on the user's Spotify device, uploaded ones in the app (Spotify is paused meanwhile). An uploaded song queued during a Spotify album or playlist pauses it, which goes on afterwards. Uploaded songs play without a Spotify account, but cannot be proposed to events (they play on Spotify).
- **Platforms**: web (`<audio>`), Android (`MediaPlayer`), iOS (`AVAudioPlayer`), desktop (Java Sound with mp3spi). Uploading needs a file chooser, not wired on iOS yet.

## Playlists

Playlists made in Music Room (`/api/v1/playlists`), stored in 4 tables:

- `playlists`: name, owner and **type**: `personal` (only the owner adds songs) or `shared` (members with write access add songs too).
- `playlist_members`: the users added by email, each with **read** (listen) or **write** (add / remove songs, change the photo) access. Write access only works in a shared playlist; turning a playlist personal makes every member read-only.
- `playlist_tracks`: the songs in order, with the Spotify track details saved when the song was added.
- `playlist_covers`: the photo, re-encoded as a 512x512 JPEG.

Only the owner renames, changes the type, manages members and deletes the playlist. Users who are neither the owner nor a member get 404. In the app, the Create button opens a card from the bottom; songs are added from Spotify search and the playlist plays on the user's Spotify device.

## App design and languages

- Navigation: avatar (top left) opens the profile and settings; bottom bar with Home, Search, Events (middle), Playlists, Create. Events, playlists and create show placeholders until the matching services exist on the server. A player bar sits above the bottom bar (under the content on large screens) while Spotify plays.
- Colors: [Radix Colors](https://www.radix-ui.com/colors) yellow + sand, light and dark themes, all in `theme/color/Palette.kt`. The theme (system, light, dark) is chosen in Profile > Appearance and saved on the device.
- Icons: [Lucide](https://lucide.dev) (`com.composables:icons-lucide-cmp`).
- Languages: English and French, from `composeApp/src/commonMain/composeResources/values*/strings.xml`. The app follows the phone's language and sends it as `Accept-Language`, so server emails use it too.

## Secrets

All secrets live in `.env`, which is git-ignored. Only `.env.example` is committed. **Never commit a secret.**

## iOS app

`make ios` builds the app and launches it on an iOS simulator (the booted one, otherwise the first available iPhone; pick another with `make ios IOS_SIMULATOR="iPhone 18 Pro"`). Build products go to `build/ios`.

Alternatively, open `iosApp/iosApp.xcodeproj` in Xcode and run the `iosApp` scheme on an Apple Silicon simulator or a signed device. The Xcode build invokes Gradle to build the shared Compose framework and copy its resources. The Android SDK must also be configured because `composeApp` is included only when an Android SDK is available. The project supports iOS 15 and later; Intel simulators are not supported by the current Compose dependencies.

The default server URL is `http://localhost:8080`, which reaches the host from the simulator. Set a reachable server URL on a physical device. The app currently permits HTTP connections, matching the Android client; use HTTPS for production.
