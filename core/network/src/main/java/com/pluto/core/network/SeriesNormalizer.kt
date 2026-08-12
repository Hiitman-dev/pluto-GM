package com.pluto.core.network

import com.pluto.core.model.Episode
import com.pluto.core.model.NormalizedEpisode
import com.pluto.core.model.NormalizedSeason
import com.pluto.core.model.NormalizedSeries
import com.pluto.core.model.Quality
import com.pluto.core.model.Season
import com.pluto.core.model.Series
import com.pluto.core.model.Source

/**
 * SeriesNormalizer — decouples raw API shapes from the UI.
 *
 * Implements Sections 22-24, 74 of the master PLUTO specification:
 *
 *   SEASON NORMALIZATION (Section 23):
 *     Raw API:  S01 / 480, S01 / 720, S01 / 1080, S02 / 480, S02 / 720
 *     Normalized:
 *       Season 1 -> [480, 720, 1080]
 *       Season 2 -> [480, 720]
 *
 *   QUALITY NORMALIZATION (Section 24):
 *     "720", "720p", "HD 720", "720P" all collapse to Quality(720, "720p").
 *     "4K", "2160p", "2160P", "UHD" all collapse to Quality(2160, "4K").
 *
 *   EPISODE IDENTITY (Section 51):
 *     Multiple records for the same episode (S01E05 at 480/720/1080)
 *     become ONE NormalizedEpisode with multiple Qualities.
 *
 * The normalizer is a pure object — no Android dependencies — so it
 * can be unit-tested without Robolectric.
 */
object SeriesNormalizer {

    /**
     * Normalize a [Series] + its raw [Season] list into a [NormalizedSeries].
     */
    fun normalize(series: Series, seasons: List<Season>): NormalizedSeries {
        val normalizedSeasons = seasons.mapIndexed { index, season ->
            normalizeSeason(season, index + 1)
        }
        return NormalizedSeries(
            id = series.id,
            title = series.title,
            description = series.description,
            year = series.year,
            imdb = series.imdb,
            rating = series.rating,
            duration = series.duration,
            image = series.image,
            cover = series.cover,
            genres = series.genres,
            country = series.country,
            seasons = normalizedSeasons
        )
    }

    fun normalizeSeason(season: Season, seasonNumber: Int): NormalizedSeason {
        // Group episodes by their identity key (title or episodeNumber).
        // The API may return the same episode multiple times with different
        // qualities — we collapse them into a single NormalizedEpisode.
        val grouped = season.episodes.groupBy { episodeIdentityKey(it) }
        val episodes = grouped.values.map { group ->
            normalizeEpisodeGroup(group)
        }.sortedBy { it.episodeNumber }
        return NormalizedSeason(
            id = season.id,
            title = season.title,
            seasonNumber = seasonNumber,
            episodes = episodes
        )
    }

    /**
     * Identity key for an episode — used to detect that two API records
     * describe the same episode at different qualities.
     *
     * We prefer the title (most stable) and fall back to episode number.
     */
    private fun episodeIdentityKey(episode: Episode): String {
        val titleKey = episode.title.trim().lowercase()
            .replace(Regex("\\s+"), " ")
            // Strip trailing quality markers like "(720p)" from titles
            .replace(Regex("\\s*\\(?\\d{3,4}p\\)?\\s*$", RegexOption.IGNORE_CASE), "")
            .trim()
        return if (titleKey.isNotEmpty()) "t:$titleKey" else "n:${episode.episodeNumber}"
    }

    private fun normalizeEpisodeGroup(group: List<Episode>): NormalizedEpisode {
        val first = group.first()
        val allSources = group.flatMap { it.sources }
        val qualities = groupQualities(allSources).sorted()
        return NormalizedEpisode(
            id = first.id,
            title = cleanEpisodeTitle(first.title),
            description = first.description,
            duration = first.duration,
            image = first.image,
            episodeNumber = first.episodeNumber,
            qualities = qualities
        )
    }

    /**
     * Strip trailing quality markers from an episode title.
     * "Pilot (720p)" -> "Pilot"
     */
    private fun cleanEpisodeTitle(title: String): String {
        return title.replace(Regex("\\s*\\(?\\d{3,4}p\\)?\\s*$", RegexOption.IGNORE_CASE), "").trim()
    }

    /**
     * Group raw sources by normalized quality tier.
     *
     * Each output [Quality] contains ALL sources that produced that tier.
     */
    fun groupQualities(sources: List<Source>): List<Quality> {
        return sources
            .filter { it.url.isNotBlank() }
            .groupBy { normalizeQualityHeight(it.quality) }
            .map { (height, sourcesAtHeight) ->
                Quality(
                    height = height,
                    label = canonicalQualityLabel(height, sourcesAtHeight.first().quality),
                    sources = sourcesAtHeight
                )
            }
            .sorted() // descending by height
    }

    /**
     * Extract a canonical integer height (in pixels) from a quality string.
     *
     * Handles:
     *   "720", "720p", "720P", "HD 720", "720x384", "1280x720"
     *   "4K", "2160p", "2160P", "UHD", "4K UHD"
     *   "1080", "1080p", "1080P", "FullHD", "FHD"
     *   "480", "480p", "SD"
     *   "1440", "1440p", "2K"
     *
     * For "WIDTHxHEIGHT" patterns (e.g. "1280x720"), returns the HEIGHT
     * (the second number) — not the first.
     */
    fun normalizeQualityHeight(quality: String): Int {
        val q = quality.trim().uppercase()

        // Word-based aliases first (before numeric extraction, so "4K" wins
        // over a stray "4" digit and "FullHD" wins over substring digits).
        when {
            q.contains("4K") || q.contains("UHD") -> return 2160
            q.contains("2K") || q.contains("QHD") -> return 1440
            q.contains("FULLHD") || q.contains("FHD") -> return 1080
        }

        // "WIDTHxHEIGHT" — return the HEIGHT (second number)
        val resolutionPair = Regex("(\\d{3,4})[xX](\\d{3,4})").find(q)
        if (resolutionPair != null) {
            return resolutionPair.groupValues[2].toInt()
        }

        // Plain numeric extraction (e.g. "720", "720p", "720P", "HD 720")
        val match = Regex("(\\d{3,4})").find(q)
        if (match != null) {
            return match.groupValues[1].toInt()
        }

        // Remaining word-based aliases
        return when {
            q.contains("HD") -> 720
            q.contains("SD") -> 480
            else -> 0
        }
    }

    /**
     * Canonical label for a quality tier.
     *
     * 2160 -> "4K"
     * 1440 -> "1440p"
     * 1080 -> "1080p"
     * 720  -> "720p"
     * 480  -> "480p"
     * 0    -> "Unknown"
     */
    fun canonicalQualityLabel(height: Int, originalQuality: String): String {
        return when {
            height >= 2160 -> "4K"
            height >= 1440 -> "1440p"
            height >= 1080 -> "1080p"
            height >= 720 -> "720p"
            height >= 480 -> "480p"
            height > 0 -> "${height}p"
            else -> originalQuality.ifBlank { "Unknown" }
        }
    }

    /**
     * Pick the best source for a given quality tier.
     *
     * Heuristic: prefer mp4 > mkv > x265 > others (most compatible first).
     */
    fun pickBestSource(quality: Quality): Source? {
        val typePreference = listOf("mp4", "mkv", "x265", "h265", "h264", "avi")
        for (type in typePreference) {
            val match = quality.sources.firstOrNull { it.type.equals(type, ignoreCase = true) }
            if (match != null) return match
        }
        return quality.sources.firstOrNull()
    }
}
