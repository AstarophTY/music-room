# Bundled MusicKit Android SDK

| File | Version |
| --- | --- |
| `MusicKitAuth-release-1.1.5.aar` | MusicKit Authentication 1.1.5 |
| `MediaPlayback-release-1.1.4.aar` | MusicKit Media Playback 1.1.4 |

Apple does not publish these on Maven Central: they are downloaded from the Apple Developer portal
("Apple Music Android SDK") and committed here so the Android build works without a manual step.

The libraries carry no license file, and their terms come from the Apple Developer Program License
Agreement. **Redistribution has not been cleared**: before this repository becomes public, either
confirm the agreement allows committing them, or remove them and have CI download the SDK from a
private artifact store at build time (`androidApp/build.gradle.kts` reads them through `files(...)`,
so only the download step would change).

`androidApp/build.gradle.kts` also declares the runtime dependencies the SDK needs but does not pull
in itself (Picasso, Gson, Material, AppCompat, media). Their versions are the ones the SDK was built
against; raising them has to be tested against playback.
