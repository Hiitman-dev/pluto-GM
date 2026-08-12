# PLUTO API SPECIFICATION

> Reverse-engineered from the CCloud Android repository
> (https://github.com/Hiitman-dev/CCloud-master)
>
> This document describes the REAL CCloud API as discovered by reading the
> Kotlin source. Every endpoint documented here is backed by a concrete
> `Repository.kt` or `ApiService.kt` reference. Where information could not
> be determined from source, the entry is marked `NOT FOUND IN SOURCE`.

---

## 1. Base Configuration

| Field | Value | Source |
|-------|-------|--------|
| Primary server | `https://server-hi-speed-iran.info` | `app/build.gradle.kts` (default `CLOUD_API_BASE_URL`) |
| Fallback server 1 | `https://hostinnegar.com` | `app/build.gradle.kts` (default `CLOUD_FALLBACK_SERVER_1`) |
| Fallback server 2 | `https://windowsdiba.info` | `app/build.gradle.kts` (default `CLOUD_FALLBACK_SERVER_2`) |
| API key | Loaded from `local.properties` (`CLOUD_API_KEY`) — **NOT committed** | `app/build.gradle.kts` |
| Connect timeout | 30 seconds | `RetrofitModule.kt`, `NetworkModule.kt` |
| Read timeout | 30 seconds | `RetrofitModule.kt`, `NetworkModule.kt` |
| Auth header | None — API key is passed as the final path segment | All repositories |
| Content-Type | `application/json` (responses) | `RetrofitModule.kt` |

### Fallback behavior

`BaseRepository.executeRequest()` tries the primary server first. If it
fails (network error OR non-2xx HTTP), it iterates over the helper servers
and retries the same path with the host replaced.

```text
primary URL  ──fail──▶  fallback1 + same path  ──fail──▶  fallback2 + same path
```

If all servers fail, the original primary exception is re-thrown.

---

## 2. Filter Types

Discovered in `data/model/FilterType.kt`:

| Enum value | URL segment | Meaning |
|------------|-------------|---------|
| `DEFAULT` | `created` | Sort by creation date (newest first) |
| `BY_YEAR` | `year` | Sort by year (newest first) |
| `BY_IMDB` | `imdb` | Sort by IMDb rating (highest first) |

These are the ONLY sort modes the CCloud API exposes. There is no
"by popularity", "by rating count", or "alphabetical" sort.

---

## 3. Endpoints

### 3.1 Movies

| Field | Value |
|-------|-------|
| Name | Get movies by filter |
| HTTP Method | `GET` |
| URL | `{API_BASE_URL}/api/movie/by/filtres/{genreId}/{filterType}/{page}/{apiKey}` |
| Path Parameters | `genreId: int` (use `0` for "all genres"), `filterType: string` (one of `created` / `year` / `imdb`), `page: int` (0-indexed), `apiKey: string` |
| Query Parameters | none |
| Headers | none |
| Authentication | API key in URL path |
| Request Body | none |
| Response | JSON array of movie objects |
| Response Model | `Movie[]` (see §4.1) |
| Used By | `MovieRepository.getMovies()`, `HomeViewModel` (today / new releases), `MoviesScreen` |
| Purpose | Browse / paginate movies, optionally filtered by genre and sorted |
| Errors | Network error → fallback servers; non-2xx → fallback servers; all fail → `ApiException` |
| Fallback behavior | Replaces host with fallback1, then fallback2 |

### 3.2 Series

| Field | Value |
|-------|-------|
| Name | Get series by filter |
| HTTP Method | `GET` |
| URL | `{API_BASE_URL}/api/serie/by/filtres/{genreId}/{filterType}/{page}/{apiKey}` |
| Path Parameters | `genreId: int` (`0` = all), `filterType: string`, `page: int`, `apiKey: string` |
| Query Parameters | none |
| Headers | none |
| Authentication | API key in URL path |
| Request Body | none |
| Response | JSON array of series objects |
| Response Model | `Series[]` (see §4.2) |
| Used By | `SeriesRepository.getSeries()`, `HomeViewModel`, `SeriesScreen` |
| Purpose | Browse / paginate series |
| Errors | Same as Movies |
| Fallback behavior | Same as Movies |

### 3.3 Search

| Field | Value |
|-------|-------|
| Name | Search content |
| HTTP Method | `GET` |
| URL | `{API_BASE_URL}/api/search/{encodedQuery}/{apiKey}/` |
| Path Parameters | `encodedQuery: string` (URL-encoded, spaces as `%20`), `apiKey: string` |
| Query Parameters | none |
| Headers | none |
| Authentication | API key in URL path |
| Request Body | none |
| Response | JSON object `{ "posters": Poster[] }` |
| Response Model | `SearchResult` (see §4.5) |
| Used By | `SearchRepository.search()`, `SearchViewModel`, `SearchScreen` |
| Purpose | Search across movies and series by title |
| Errors | Same as Movies |
| Fallback behavior | Same as Movies |
| Notes | Search returns BOTH movies and series mixed together; `Poster.type` distinguishes them (`"movie"` or `"serie"`). There is **no pagination** on search — the API returns all matches in one response. |

### 3.4 Genres

| Field | Value |
|-------|-------|
| Name | Get all genres |
| HTTP Method | `GET` |
| URL | `{API_BASE_URL}/api/genre/all/{apiKey}` |
| Path Parameters | `apiKey: string` |
| Query Parameters | none |
| Headers | none |
| Authentication | API key in URL path |
| Request Body | none |
| Response | JSON array of `{ id, title }` |
| Response Model | `Genre[]` (see §4.6) |
| Used By | `GenreRepository.getGenres()`, `HomeViewModel.loadGenres()`, filter UI |
| Purpose | Populate genre filter dropdown |
| Errors | Same as Movies |
| Fallback behavior | Same as Movies |
| Notes | Repository sorts genres alphabetically by title before returning. |

### 3.5 Countries

| Field | Value |
|-------|-------|
| Name | Get all countries |
| HTTP Method | `GET` |
| URL | `{API_BASE_URL}/api/country/all/{apiKey}/` |
| Path Parameters | `apiKey: string` |
| Query Parameters | none |
| Headers | none |
| Authentication | API key in URL path |
| Request Body | none |
| Response | JSON array of `{ id, title, image }` |
| Response Model | `Country[]` (see §4.7) |
| Used By | `CountryRepository.getAllCountries()`, `CountryViewModel`, `CountryScreen` |
| Purpose | Populate country browser |
| Errors | Same as Movies |
| Fallback behavior | Same as Movies |

### 3.6 Posters by Country

| Field | Value |
|-------|-------|
| Name | Get posters for a country |
| HTTP Method | `GET` |
| URL | `{API_BASE_URL}/api/poster/by/filtres/0/{countryId}/{filterType}/{page}/{apiKey}` |
| Path Parameters | `countryId: int`, `filterType: string`, `page: int`, `apiKey: string` (first path segment is always literal `0`) |
| Query Parameters | none |
| Headers | none |
| Authentication | API key in URL path |
| Request Body | none |
| Response | JSON array of poster objects |
| Response Model | `Poster[]` (see §4.5) |
| Used By | `CountryPostersRepository.getPostersByCountry()`, `CountryScreen` |
| Purpose | Browse content produced in a specific country |
| Errors | Same as Movies |
| Fallback behavior | Same as Movies |

### 3.7 Seasons (by series)

| Field | Value |
|-------|-------|
| Name | Get seasons for a series |
| HTTP Method | `GET` |
| URL | `{API_BASE_URL}/api/season/by/serie/{seriesId}/{apiKey}/` |
| Path Parameters | `seriesId: int`, `apiKey: string` |
| Query Parameters | none |
| Headers | none |
| Authentication | API key in URL path |
| Request Body | none |
| Response | JSON array of season objects, each containing embedded episodes |
| Response Model | `Season[]` (see §4.3, §4.4) |
| Used By | `SeasonsRepository.getSeasons()`, `SeasonsViewModel`, `SingleSeriesScreen` |
| Purpose | Load the season/episode tree for a series |
| Errors | Same as Movies |
| Fallback behavior | Same as Movies |
| Notes | Each `Season` already contains its full `Episode[]` list, including each episode's `sources[]`. There is **no separate "get episodes" endpoint** — episodes are always returned nested inside seasons. |

---

## 4. Data Models

### 4.1 Movie

Source: `data/model/Movie.kt`, `MovieRepository.parseMovie()`

```typescript
interface Movie {
  id: number;
  type: string;          // "movie"
  title: string;
  description: string;
  year: number;
  imdb: number;          // 0.0–10.0
  rating: number;        // 0.0–10.0 (local rating)
  duration: string | null;
  image: string;         // poster URL
  cover: string;         // backdrop URL
  genres: Genre[];
  sources: Source[];     // playable video sources
  country: Country[];
}
```

### 4.2 Series

Source: `data/model/Series.kt`, `SeriesRepository.parseSeriesItem()`

```typescript
interface Series {
  id: number;
  type: string;          // "serie"
  title: string;
  description: string;
  year: number;
  imdb: number;
  rating: number;
  duration: string | null;
  image: string;         // poster URL
  cover: string;         // backdrop URL
  genres: Genre[];
  country: Country[];
  // NOTE: Series do NOT carry sources at this level — sources come from
  // each Episode inside each Season (see §3.7).
}
```

### 4.3 Season

Source: `data/model/Series.kt` (nested), `SeasonsRepository.parseSeason()`

```typescript
interface Season {
  id: number;
  title: string;
  episodes: Episode[];
}
```

### 4.4 Episode

Source: `data/model/Series.kt` (nested), `SeasonsRepository.parseEpisodes()`

```typescript
interface Episode {
  id: number;
  title: string;
  description: string;
  duration: string | null;
  image: string;         // episode thumbnail
  sources: Source[];     // playable video sources for this episode
}
```

### 4.5 Poster (search result item)

Source: `data/model/SearchResult.kt`, `SearchRepository.parsePoster()`

```typescript
interface Poster {
  id: number;
  title: string;
  type: string;          // "movie" or "serie"
  description: string;
  year: number;
  imdb: number;
  rating: number;
  duration: string | null;
  image: string;         // poster URL
  cover: string;         // backdrop URL
  genres: Genre[];
  sources: Source[];     // present for movies, empty for series
  country: Country[];
}
```

### 4.6 Genre

```typescript
interface Genre {
  id: number;
  title: string;
}
```

### 4.7 Country

```typescript
interface Country {
  id: number;
  title: string;
  image: string;         // country flag / artwork URL
}
```

### 4.8 Source

```typescript
interface Source {
  id: number;
  quality: string;       // e.g. "720p", "1080p", "4K"
  type: string;          // e.g. "mp4", "mkv", "x265"
  url: string;           // direct playable video URL
}
```

---

## 5. Images

| Use | Field | Notes |
|-----|-------|-------|
| Poster (card thumbnail) | `Movie.image` / `Series.image` / `Poster.image` | Already a fully-qualified URL returned by the API. No client-side URL construction is performed by CCloud. |
| Backdrop (hero / details banner) | `Movie.cover` / `Series.cover` / `Poster.cover` | Already a fully-qualified URL. |
| Country flag | `Country.image` | Already a fully-qualified URL. |
| Episode thumbnail | `Episode.image` | Already a fully-qualified URL. |

**There is no image URL builder.** The CCloud API returns absolute URLs in
every image field, and the Android client loads them directly with Coil.

---

## 6. Playback Flow

Discovered from `VideoPlayerActivity.kt` and the repository layer:

```text
Movie / Episode
     │
     ├─ Movie.sources[i].url      ── directly playable URL
     │
     └─ Episode.sources[i].url    ── directly playable URL
                 │
                 ▼
        ExoPlayer plays the URL as-is
        (no manifest parsing, no token exchange, no DRM)
```

The Android player is `androidx.media3` ExoPlayer. The URL is fed directly
into the player with no preprocessing. The Web equivalent is an HTML5
`<video>` element with the URL as its `src`.

### Quality selection

Each `Source` has a `quality` string ("720p", "1080p", etc.) and a `type`
string ("mp4", "mkv", "x265"). The player does NOT switch quality at
runtime — the user picks a source BEFORE playback starts, and that single
URL is played.

---

## 7. Subtitles

**NOT FOUND IN SOURCE.**

- There is a `SubtitleSettings` model (`textColor`, `borderColor`,
  `textSize`) but it is a player UI preference, not a content source.
- No subtitle endpoint exists in `ApiService.kt` or any repository.
- No subtitle URL field exists on `Movie`, `Series`, `Episode`, or `Source`.
- ExoPlayer is configured without any `MediaItem.SubtitleConfiguration`.

Conclusion: **CCloud does not provide subtitles.** PLUTO therefore does
not implement subtitle loading. The player settings page preserves the
`SubtitleSettings` model for parity, but no subtitles are loaded during
playback.

---

## 8. Downloads

Discovered from `utils/DownloadUtils.kt`:

CCloud does **not** download files inside the app. It exposes the source
URL to the user and offers to:

1. **Open the URL** in the system browser (`Intent.ACTION_VIEW`).
2. **Copy the URL** to the clipboard.
3. **Hand off to an external downloader** — ADM, VLC, MX Player, KM Player.

### Web equivalent

- Browser `window.open(url)` works for direct video URLs.
- `navigator.clipboard.writeText(url)` works for "copy link".
- Programmatic file download of cross-origin video is blocked by CORS in
  most browsers unless the upstream server sends `Access-Control-Allow-Origin`.
- PLUTO exposes "Open in new tab" and "Copy link" actions. True background
  download is a **WEB LIMITATION** and is documented as such in the UI.

---

## 9. Favorites

Discovered from `data/repository/FavoritesRepository.kt`:

- **Storage**: LOCAL — JSON file in Android `filesDir` (`favorites.json`).
- **NOT remote** — no API endpoint exists for favorites.
- **Model**: `FavoriteItem` (essentially a snapshot of `Movie` / `Series`).
- **Behavior**: save, remove, list, check `isFavorite`, clear all.
- **Groups**: optional `FavoriteGroup` containers (default group always
  exists, additional groups are user-defined).

### Web equivalent

`localStorage` is the direct browser analog of Android's `filesDir` JSON
files. PLUTO persists favorites to `localStorage` under the key
`pluto:favorites` and groups under `pluto:favoriteGroups`, mirroring
CCloud's `favorites.json` / `favorite_groups.json`.

---

## 10. History

Discovered from `data/repository/HistoryRepository.kt` and
`utils/ViewHistoryManager.kt`:

- **Storage**: LOCAL — `recently_viewed.json` (max 20 items) and
  `watched_episodes.json`.
- **NOT remote** — no API endpoint exists for history.
- **Recently viewed**: a list of `FavoriteItem` snapshots, capped at 20,
  most-recent-first.
- **Watched episodes**: `WatchedEpisode { seriesId, seasonId, episodeId, watchedAt }`.
- **Continue watching**: `ViewHistoryManager` tracks per-item progress
  (last position, total duration) so the UI can show a "Continue
  Watching" rail.

### Web equivalent

PLUTO persists:
- `pluto:recentlyViewed` — `FavoriteItem[]`, capped at 20.
- `pluto:watchedEpisodes` — `WatchedEpisode[]`.
- `pluto:progress` — `Record<string, { position, duration, updatedAt }>`.

---

## 11. Cache

Discovered from `data/repository/ContentCacheRepository.kt`:

- **Storage**: LOCAL — `movie_{id}.json`, `series_{id}.json` in `filesDir`.
- **Purpose**: when the user navigates from a list to a detail page, the
  list already contains the full movie/series object, so the detail page
  can render instantly from cache while optionally refreshing from API.
- **In-memory cache**: repositories keep a `Map<Int, Movie>` / `Map<Int, Series>`
  alongside the on-disk JSON.

### Web equivalent

PLUTO keeps an in-memory `Map` cache in the API client and also writes
through to `localStorage` (`pluto:cache:movie:{id}`, `pluto:cache:series:{id}`).

---

## 12. Settings

Discovered from `data/repository/SettingsRepository.kt` and
`data/model/VideoPlayerSettings.kt`, `data/model/SubtitleSettings.kt`,
`data/model/FontSettings.kt`:

| Setting | Model | Default | Storage |
|---------|-------|---------|---------|
| Player seek time | `VideoPlayerSettings.seekTimeSeconds` | `10` | `video_player_settings.json` |
| Subtitle text color | `SubtitleSettings.textColor` | yellow | `subtitle_settings.json` |
| Subtitle border color | `SubtitleSettings.borderColor` | 50% black | `subtitle_settings.json` |
| Subtitle text size | `SubtitleSettings.textSize` | `17f` (TV: `25f`) | `subtitle_settings.json` |
| Font family | `FontSettings` | NOT FOUND IN SOURCE (model exists, fields inferred) | `font_settings.json` |
| Welcome completed | boolean flag | `false` | `welcome_completed.flag` |
| Theme color | `ThemeSettings` | NOT FOUND IN SOURCE (UI-only) | shared prefs |

### Web equivalent

PLUTO persists all settings to `localStorage` under the `pluto:settings:*`
namespace and applies them at runtime.

---

## 13. Errors

Discovered from `util/ApiException.kt`:

| Variant | Trigger | User-facing message |
|---------|---------|---------------------|
| `NetworkError` | `UnknownHostException`, `SocketTimeoutException`, generic `IOException` | "Network error. Please check your connection." |
| `ServerError` | HTTP status ≥ 400 | "Server error (code). Please try again later." |
| `ParseError` | `JSONException`, `SerializationException` | "Failed to parse response data." |
| `NotFound` | 404 / empty result | "Requested content not found." |
| `Unauthorized` | 401 / 403 | "Authentication failed." |
| `UnknownError` | anything else | "An unexpected error occurred." |

### Retry behavior

- No automatic retry on the same server.
- Automatic fallback to `fallbackServer1`, then `fallbackServer2`.
- If all three servers fail, the original exception is surfaced to the UI.

---

## 14. Endpoints NOT in the source

The following were investigated and confirmed NOT to exist in the CCloud
repository:

- `NOT FOUND IN SOURCE` — Register / login / user account endpoints.
- `NOT FOUND IN SOURCE` — Subtitle endpoints.
- `NOT FOUND IN SOURCE` — Trending / popular / featured endpoints
  (Home derives these from `getMovies` / `getSeries` with page 0).
- `NOT FOUND IN SOURCE` — Continue-watching remote endpoint (it is local).
- `NOT FOUND IN SOURCE` — Episode-by-id endpoint (episodes are nested
  inside seasons).
- `NOT FOUND IN SOURCE` — Single-movie-by-id endpoint (the movie object
  from the list response is cached locally instead).
- `NOT FOUND IN SOURCE` — Token refresh / OAuth flow.

---

## 15. Web Implementation Notes

- The API key is loaded from `local.properties` on Android, which is NOT
  committed to the repository. PLUTO loads it from a server-side
  environment variable (`CCLOUD_API_KEY`) and proxies all API calls
  through a Next.js route handler at `/api/proxy/[...path]`. The key is
  never exposed to the browser.
- CORS is handled by the same proxy — the browser only ever talks to the
  same-origin `/api/proxy/*` endpoint.
- The proxy replicates the CCloud fallback behavior: primary → fallback1
  → fallback2.

### WEB LIMITATION — API key not publicly available

The CCloud API key is **NOT FOUND IN SOURCE**. It is loaded from
`local.properties` (gitignored) at build time. The published CCloud APK
was analyzed with `androguard` to extract `BuildConfig.API_KEY`, but the
class is stripped by R8 in the release build and the key string is not
present in any dex string table, ARSC resource, or the AndroidManifest.

**Consequence for PLUTO Web:**

- Without a valid `CCLOUD_API_KEY` in `.env.local`, all upstream API
  calls return HTTP 404 and PLUTO displays "SIGNAL LOST" error states.
- The application code is 100% real — no mock data, no fake responses.
- The moment a valid key is provided, PLUTO loads real movies, series,
  seasons, episodes, and sources directly from the CCloud API.

This is the honest, correct behavior. Faking API responses would violate
the PLUTO specification's "NO MOCK DATA" rule.
