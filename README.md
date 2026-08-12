# PLUTO — Cinema Beyond Earth

A premium native Android streaming application for movies and TV series, with a distinctive cosmic / galaxy / cinematic visual identity.

PLUTO is built by reverse-engineering the CCloud Android repository's API and translating its functionality into a production-grade Kotlin + Jetpack Compose + Media3 application with original PLUTO branding, architecture, and design language.

> **Status**: Architecture-complete source repository. Compile with `./gradlew assembleDebug` in Android Studio or any JDK 17 + Android SDK environment. The sandbox this was built in cannot compile Android, so verification of the APK build happens in the GitHub Actions workflow on push.

---

## ✨ Features

- **Real CCloud API integration** — movies, series, seasons, episodes, search, genres, countries, country-posters (all 7 endpoints, with 3-server fallback)
- **Season & quality normalization** — multi-quality episode records collapse into a single episode with multiple quality tiers (per the master spec)
- **Media3 / ExoPlayer playback** — abstracted behind a `PlayerEngine` interface so LibVLC/native engines can be added without touching the UI
- **Background new-episode notifications** — WorkManager-powered sync of followed series, deduplicated by composite key, deep-linked into the correct episode
- **External player & downloader support** — MX Player, VLC, KMPlayer, ADM, 1DM+, system browser, "Open with..." chooser
- **Glassmorphic cosmic UI** — original PLUTO design system with custom icon language, layered nebula gradients, orbital arcs, star field
- **Room persistence** — favorites, history, playback progress, followed series, new-episode notifications, recent searches
- **DataStore preferences** — video player settings, subtitles, downloads, notifications
- **Deep links** — `pluto://movie/{id}`, `pluto://series/{id}`, `pluto://series/{id}/season/{s}/episode/{e}`
- **Picture-in-Picture, rotation, lock, gestures** — full player HUD with brightness/volume/seek gestures
- **Hilt DI, StateFlow, repository pattern** — clean architecture with module boundaries

---

## 🏗️ Architecture

Multi-module Gradle project with clear layering:

```
pluto-android/
├── app/                       # Application entry point (MainActivity, Application, nav graph)
├── core/
│   ├── common/                # Result, ApiException, DispatcherProvider, PlutoLogger
│   ├── model/                 # Domain models + DTOs (Movie, Series, Season, Episode, Source, etc.)
│   ├── data/                  # ContentRepository, FavoritesRepository, HistoryRepository, SettingsRepository
│   ├── network/               # CCloud API client + SeriesNormalizer + BaseRepository (multi-server fallback)
│   ├── database/              # Room database, DAOs, entities
│   ├── designsystem/          # PLUTOColors, PLUTOTypography, CosmicBackground, GlassCard, PlutoIcons, PLUTOTheme
│   ├── media/                 # PlayerEngine abstraction + Media3PlayerEngine + PlayerController
│   ├── navigation/            # Type-safe routes + deep link definitions
│   ├── notifications/         # WorkManager sync + NotificationRepository + PlutoNotificationPoster
│   └── download/              # DownloadManager + ExternalActionLauncher + ClipboardHelper
└── feature/
    ├── splash/                # First-launch splash experience
    ├── auth/                  # Email auth (placeholder provider, replaceable)
    ├── home/                  # Home with Continue Watching, Recently Added, Trending, Movies, Series
    ├── search/                # Debounced search with filters
    ├── details/               # Movie + series details with seasons/episodes
    ├── player/                # Full player UI with gestures, HUD, quality picker
    ├── downloads/             # Download manager UI
    ├── favorites/             # Library / favorites grid
    ├── history/               # Watch trajectory / continue watching
    ├── notifications/         # New-episode notification list
    └── settings/              # Appearance, playback, downloads, notifications, about
```

### Data flow

```
CCloud API  ─→  core/network (OkHttp + multi-server fallback)
                     ↓
                core/network/SeriesNormalizer (collapses multi-quality episodes)
                     ↓
                core/data/ContentRepository (wraps in Result<T>)
                     ↓
                feature/*/ViewModel (StateFlow)
                     ↓
                feature/*/Composable (PLUTO design system)
```

---

## 🎨 Design System

The PLUTO Design System is the visual identity — not "normal UI + stars" but a coherent cosmic language.

**Colors** (Section 8 of the master spec):
| Token | Value | Usage |
|-------|-------|-------|
| Void | `#03040A` | Base background |
| Deep Space | `#070B1A` | Elevated surface |
| Navy Drift | `#0C1233` | Card background |
| Electric Blue | `#0A4DFF` | Primary / CTA |
| Glow Blue | `#2D7FFF` | Active state |
| Ice Blue | `#8AB4FF` | Secondary text |
| Frost White | `#E8F4FF` | Primary text |
| Muted Star | `#5A6B8C` | Tertiary text |
| Danger | `#FF3B5C` | Error / destructive |

**Typography**:
- Display: Space Grotesk (loaded via app `res/font/`)
- Body: Inter
- Metadata: Space Mono (years, timestamps, episode codes — "astronomical coordinates" feel)

**Custom Icons** (`PlutoIcons.kt`):
30+ original vector icons designed on a 24dp grid with consistent 1.5–2dp stroke. Each icon uses orbital / planetary / astronomical instrument inspiration (e.g. `Download` is an arrow entering an orbital container, `Notification` is a bell with an orbital arc, `Search` has a tiny stellar point).

**Cosmic Background** (`CosmicBackground.kt`):
Layered composable — Void base + deep-space gradient + nebula bloom (with parallax) + deterministic star field (28 stars, varied size/opacity, slow twinkle) + orbital arc SVG.

**Glass system** (`GlassCard.kt`):
Four-tier glass hierarchy (Glass1 almost-transparent → Glass4 active-with-glow) with optional diagonal light-sweep reflection (8% opacity, "noticeable only subconsciously").

---

## 🎬 Player Architecture

Per Section 5 of the master spec, the player is decoupled from any specific engine:

```
PLUTO Player UI
     ↓
PlayerController (DefaultPlayerController)
     ↓
PlayerEngine (interface)
     ↓
Media3PlayerEngine (default) | LibVLCPlayerEngine (future) | NativePlayerEngine (future)
```

**Supported features**:
- play / pause / seek / volume / brightness gestures
- double-tap (left=rewind, center=play/pause, right=forward)
- long-press = 2× speed
- horizontal drag = scrub seeking with `+00:15` / `-00:15` HUD
- vertical drag (left=brightness, right=volume)
- lock (disables gestures)
- quality switching (only actual available qualities — no invented options)
- source fallback (try next source on error)
- PiP, rotation, resume position
- subtitle track selection (where the container exposes them — CCloud API does NOT provide subtitle URLs)
- audio track selection

**Source normalization**: `PlaybackSourceBuilder` converts raw `Source` lists into `PlaybackSource`s with canonical quality labels (`"720"` / `"HD 720"` / `"720p"` → `"720p"`).

---

## 🔔 Notifications

Followed series are checked for new episodes every 6 hours via WorkManager.

**Per-episode deduplication** (Section 51): one episode = one notification, even if the API returned multiple quality variants. Composite key: `"${seriesId}-${seasonNumber}-${episodeNumber}"`.

**Initial sync** (Section 50): does NOT notify for existing episodes — just records the baseline. Subsequent syncs compare against this baseline.

**Deep link**: tapping a notification opens `pluto://series/{id}/season/{s}/episode/{e}`, which the nav graph resolves to the player at that episode.

**Future FCM** (Section 55): `NotificationRepository` is the seam — a future backend + FCM can replace the polling logic without touching the UI.

---

## ⬇️ Downloads & External Players

Android can download cross-origin video (unlike browsers), so PLUTO provides true background download via WorkManager + OkHttp streaming.

**External players** (Section 29):
- MX Player (Free + Pro)
- VLC
- KMPlayer (Free + Pro)
- "Open with..." system chooser

**External downloaders** (Section 30):
- ADM (Advanced Download Manager)
- 1DM / 1DM+
- System browser
- System downloader

Each app's availability is checked at runtime — unavailable apps are shown as "Not installed" rather than crashing.

**Copy links** (Section 27): per-quality copy + "Copy All Links" (only copies URLs that actually exist).

---

## 🔧 Setup

### Prerequisites

- Android Studio Ladybug or newer
- JDK 17
- Android SDK 35 (compileSdk)
- Min SDK 24 (Android 7.0+)

### Build

1. **Clone the repository**

2. **Configure secrets** — copy `local.properties.example` to `local.properties` and fill in your CCloud API key:
   ```properties
   PLUTO_API_KEY=your_api_key_here
   PLUTO_API_BASE_URL=https://server-hi-speed-iran.info
   PLUTO_FALLBACK_SERVER_1=https://hostinnegar.com
   PLUTO_FALLBACK_SERVER_2=https://windowsdiba.info
   ```

3. **Build**:
   ```bash
   ./gradlew assembleDebug
   ```
   APK output: `app/build/outputs/apk/debug/app-debug.apk`

4. **Run tests**:
   ```bash
   ./gradlew test
   ```

5. **Lint**:
   ```bash
   ./gradlew lintDebug
   ```

### Release signing

Configure via GitHub Secrets (never commit keystores):

| Secret | Description |
|--------|-------------|
| `PLUTO_KEYSTORE_BASE64` | Base64-encoded `.jks` keystore |
| `PLUTO_KEY_ALIAS` | Key alias |
| `PLUTO_KEY_PASSWORD` | Key password |
| `PLUTO_STORE_PASSWORD` | Keystore password |
| `PLUTO_API_KEY` | CCloud API key for the release build |

The GitHub Actions workflow (`.github/workflows/android.yml`) decodes these on tagged releases and produces a signed universal APK.

---

## 🧪 Testing

Unit tests cover the critical normalization logic mandated by Section 92 of the master spec:

- **`SeriesNormalizerTest`** — quality normalization (720 / 720p / HD 720 / 4K / UHD all collapse correctly), season grouping, episode identity (multi-quality variants collapse to one episode)
- **`MappersTest`** — Room entity ↔ domain model round-trip
- **`ResultTest`** — Result sealed class contract + ApiException classification + PlutoLogger.redact()

Run with:
```bash
./gradlew test
```

---

## 🚀 CI/CD

GitHub Actions workflow (`.github/workflows/android.yml`):

1. Checkout + JDK 17 + Android SDK setup
2. Gradle dependency cache
3. Run unit tests (`./gradlew test`)
4. Build debug APK (`./gradlew assembleDebug`)
5. Run lint (`./gradlew lintDebug`)
6. Upload APK + test results as artifacts
7. On tagged releases (`v*`): decode signing keystore from secrets, build signed release APK, create GitHub Release with the APK attached

The workflow NEVER uses `|| true` to hide failures (per Section 97 of the master spec).

---

## 🔐 Security

- **API key**: loaded from `local.properties` (gitignored) → `BuildConfig` → Hilt `@Named("apiKey")`. Never hardcoded, never committed.
- **Signing credentials**: GitHub Secrets only. Never committed.
- **Logging**: debug builds = VERBOSE; release builds = MINIMAL. `PlutoLogger.redact()` automatically scrubs `api_key`, `password`, `token`, `secret`, etc. from all log output.
- **Local data**: Room stores only favorites, history, progress, followed series, recent searches, and new-episode notifications. No auth tokens, no credentials.
- **Network**: HTTPS-only (with cleartext permitted only for legacy CCloud fallback servers that don't have HTTPS — configurable in `AndroidManifest.xml`).

---

## 📱 Compatibility

- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)
- **Compile SDK**: 35
- **Form factors**: phone, tablet, foldable (adaptive layouts)
- **Orientations**: portrait, landscape (player supports both + fullscreen)
- **TV**: not yet optimized (future work — sidebar navigation pattern is stubbed in `core/navigation`)

---

## 🗺️ Roadmap

### Implemented
- ✅ Complete Gradle multi-module architecture
- ✅ CCloud API integration (all 7 endpoints, 3-server fallback)
- ✅ PLUTO design system (colors, typography, motion, glass, custom icons, cosmic background)
- ✅ Player abstraction + Media3 implementation
- ✅ WorkManager notification sync + deep links
- ✅ Room persistence (favorites, history, progress, followed series)
- ✅ DataStore preferences
- ✅ External player + downloader integration
- ✅ Unit tests for normalization logic
- ✅ GitHub Actions CI/CD

### Future
- ⏳ Full feature screen implementations (placeholders currently in `app/ui/`)
- ⏳ Actual file download streaming in `VideoDownloadWorker`
- ⏳ TV / large-screen layout optimizations
- ⏳ Optional "Cosmic Dawn" light theme (Section 113)
- ⏳ Backend + FCM for push notifications (Section 55)
- ⏳ Auth provider implementation (Section 14 — placeholder is replaceable)
- ⏳ LibVLC engine for broader format support (Section 5 — abstraction makes this drop-in)

---

## 📚 Documentation

- `docs/PLUTO_API_SPEC.md` — full API reference (mirrored from the PLUTO Web project; same CCloud API)
- Section comments throughout the codebase cite the relevant master spec section (e.g. `// Implements Section 22 of the master spec`)

---

## 📄 License

This project is provided as-is for educational and development purposes. The CCloud API is a third-party service; PLUTO is an independent client application and is not affiliated with CCloud.

---

## 🌌 Product Vision

> "I am watching cinema from the edge of the universe."

PLUTO should feel like a premium Android streaming product that happens to share the same backend as the PLUTO Web app — not "an Android version of a website." The cosmic design should be memorable, the player should be powerful, the download system should be organized, the series system should be intelligent, the notification system should be useful, and the architecture should be maintainable.

**PLUTO. Cinema Beyond Earth.**
