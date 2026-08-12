package com.pluto.core.model

import kotlinx.serialization.Serializable

/**
 * FilterType — mirrors the CCloud API's three sort modes.
 *
 * Source: CCloud `data/model/FilterType.kt` (FilterType enum: DEFAULT / BY_YEAR / BY_IMDB).
 * Each enum value maps to a URL segment used by the CCloud API:
 *   DEFAULT -> "created"
 *   BY_YEAR -> "year"
 *   BY_IMDB -> "imdb"
 *
 * These are the ONLY sort modes the CCloud API exposes. There is no
 * "popularity" or "alphabetical" sort. PLUTO does not invent filters
 * that the backend does not support.
 */
enum class FilterType(val urlSegment: String, val label: String) {
    DEFAULT("created", "Latest"),
    BY_YEAR("year", "By Year"),
    BY_IMDB("imdb", "Top IMDb");

    companion object {
        fun fromSegment(segment: String): FilterType =
            entries.firstOrNull { it.urlSegment == segment } ?: DEFAULT
    }
}

@Serializable
data class Genre(
    val id: Int = 0,
    val title: String = ""
)

@Serializable
data class Country(
    val id: Int = 0,
    val title: String = "",
    val image: String = ""
)

/**
 * Source — a single playable / downloadable video URL with metadata.
 *
 * Mirrors CCloud `data/model/Movie.kt` nested `Source` data class.
 * The CCloud API returns one Source per quality tier per file.
 */
@Serializable
data class Source(
    val id: Int = 0,
    val quality: String = "Unknown",
    val type: String = "Unknown",
    val url: String = ""
)

/**
 * Movie — primary content unit returned by `/api/movie/by/filtres/...`.
 *
 * Mirrors CCloud `data/model/Movie.kt`. Movies carry their own sources
 * inline (unlike series, where sources live on each episode).
 */
@Serializable
data class Movie(
    val id: Int,
    val type: String = "movie",
    val title: String = "Unknown Title",
    val description: String = "No description available",
    val year: Int = 0,
    val imdb: Double = 0.0,
    val rating: Double = 0.0,
    val duration: String? = null,
    val image: String = "",
    val cover: String = "",
    val genres: List<Genre> = emptyList(),
    val sources: List<Source> = emptyList(),
    val country: List<Country> = emptyList()
)

/**
 * Series — primary content unit returned by `/api/serie/by/filtres/...`.
 *
 * Mirrors CCloud `data/model/Series.kt`. Series do NOT carry sources at
 * this level — sources come from each Episode inside each Season
 * (see [SeasonsRepository.getSeasons]).
 */
@Serializable
data class Series(
    val id: Int,
    val type: String = "serie",
    val title: String = "Unknown Title",
    val description: String = "No description available",
    val year: Int = 0,
    val imdb: Double = 0.0,
    val rating: Double = 0.0,
    val duration: String? = null,
    val image: String = "",
    val cover: String = "",
    val genres: List<Genre> = emptyList(),
    val country: List<Country> = emptyList()
)

@Serializable
data class Episode(
    val id: Int,
    val title: String = "Unknown Episode",
    val description: String = "",
    val duration: String? = null,
    val image: String = "",
    val sources: List<Source> = emptyList(),
    val episodeNumber: Int = 0,
    val seasonNumber: Int = 0
)

@Serializable
data class Season(
    val id: Int,
    val title: String = "Unknown Season",
    val seasonNumber: Int = 0,
    val episodes: List<Episode> = emptyList()
)

/**
 * Poster — search-result item.
 *
 * Mirrors CCloud `data/model/SearchResult.kt` `Poster` class. The CCloud
 * search endpoint returns a single `posters` array that mixes movies and
 * series. The `type` field ("movie" or "serie") distinguishes them.
 */
@Serializable
data class Poster(
    val id: Int,
    val title: String = "Unknown Title",
    val type: String = "",
    val description: String = "No description available",
    val year: Int = 0,
    val imdb: Double = 0.0,
    val rating: Double = 0.0,
    val duration: String? = null,
    val image: String = "",
    val cover: String = "",
    val genres: List<Genre> = emptyList(),
    val sources: List<Source> = emptyList(),
    val country: List<Country> = emptyList()
)

@Serializable
data class SearchResult(val posters: List<Poster> = emptyList())

// ── Normalized domain models (UI-facing) ─────────────────────────────────
// These are the models the UI consumes. They decouple the UI from raw API
// shapes — see Section 74 of the master spec ("API NORMALIZATION").

/**
 * Quality — normalized quality tier extracted from one or more Sources.
 *
 * Example raw API output:
 *   Source(quality="720p", type="mp4", url="...")
 *   Source(quality="720", type="x265", url="...")
 *   Source(quality="HD 720", type="mkv", url="...")
 * All three normalize to Quality(height=720, label="720p").
 */
data class Quality(
    val height: Int,
    val label: String,
    val sources: List<Source>
) : Comparable<Quality> {
    override fun compareTo(other: Quality): Int = other.height.compareTo(height)
    val isFourK: Boolean get() = height >= 2160
}

/**
 * NormalizedEpisode — an episode with its sources grouped by quality.
 *
 * Per Section 22-24 of the master spec: the API may return multiple
 * records for the same episode (one per quality). PLUTO normalizes them
 * into a single NormalizedEpisode with a list of Qualities, each
 * containing the raw Sources that produced that quality tier.
 */
data class NormalizedEpisode(
    val id: Int,
    val title: String,
    val description: String,
    val duration: String?,
    val image: String,
    val episodeNumber: Int,
    val qualities: List<Quality>
)

data class NormalizedSeason(
    val id: Int,
    val title: String,
    val seasonNumber: Int,
    val episodes: List<NormalizedEpisode>
)

/**
 * NormalizedSeries — the canonical series shape the UI consumes.
 *
 * Produced by [SeriesNormalizer] from a raw [Series] + its [Season] list.
 */
data class NormalizedSeries(
    val id: Int,
    val title: String,
    val description: String,
    val year: Int,
    val imdb: Double,
    val rating: Double,
    val duration: String?,
    val image: String,
    val cover: String,
    val genres: List<Genre>,
    val country: List<Country>,
    val seasons: List<NormalizedSeason>
)

// ── Favorite / History / Watched ─────────────────────────────────────────

@Serializable
data class FavoriteItem(
    val id: Int,
    val type: String, // "movie" | "series"
    val title: String,
    val description: String,
    val year: Int,
    val imdb: Double,
    val rating: Double,
    val duration: String?,
    val image: String,
    val cover: String,
    val genres: List<Genre>,
    val country: List<Country>,
    val sources: List<Source> = emptyList(),
    val addedAt: Long = System.currentTimeMillis()
)

@Serializable
data class WatchedEpisode(
    val seriesId: Int,
    val seasonId: Int,
    val episodeId: Int,
    val watchedAt: Long = System.currentTimeMillis()
)

@Serializable
data class PlaybackProgress(
    val contentId: Int,
    val contentType: String, // "movie" | "series"
    val episodeId: Int? = null,
    val seasonId: Int? = null,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long = System.currentTimeMillis()
)

// ── Notification / Followed Series ───────────────────────────────────────

@Serializable
data class FollowedSeries(
    val seriesId: Int,
    val title: String,
    val poster: String,
    val lastKnownSeason: Int = 0,
    val lastKnownEpisode: Int = 0,
    val followedAt: Long = System.currentTimeMillis(),
    val notificationsEnabled: Boolean = true
)

@Serializable
data class NewEpisodeNotification(
    val id: String, // "${seriesId}-${seasonNumber}-${episodeNumber}"
    val seriesId: Int,
    val seriesTitle: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val detectedAt: Long = System.currentTimeMillis()
)

// ── Settings ─────────────────────────────────────────────────────────────

data class VideoPlayerSettings(
    val seekSeconds: Int = 10,
    val defaultSpeed: Float = 1.0f,
    val defaultQuality: String = "auto",
    val autoPlayNext: Boolean = true
)

data class SubtitleSettings(
    val textColor: Long = 0xFFFFFF00, // yellow
    val borderColor: Long = 0x80000000, // 50% black
    val textSize: Float = 17f
)

data class DownloadSettings(
    val wifiOnly: Boolean = true,
    val simultaneousDownloads: Int = 2,
    val defaultQuality: String = "720p",
    val autoRetry: Boolean = true
)

data class NotificationSettings(
    val newEpisodesEnabled: Boolean = true,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val grouping: Boolean = true
)

// ── Auth (placeholder for future backend) ────────────────────────────────

data class AuthState(
    val isLoggedIn: Boolean = false,
    val userId: String? = null,
    val email: String? = null,
    val token: String? = null
)
