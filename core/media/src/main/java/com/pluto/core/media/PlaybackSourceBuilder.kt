package com.pluto.core.media

import com.pluto.core.network.SeriesNormalizer
import com.pluto.core.model.Episode
import com.pluto.core.model.Movie
import com.pluto.core.model.Quality
import com.pluto.core.model.Source
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PlaybackSourceBuilder — converts API models into playable [PlaybackSource]s.
 *
 * Per Section 43 ("PLAYER QUALITY") of the master spec: "Show only actual
 * available qualities. Do not invent unavailable options."
 *
 * Uses [SeriesNormalizer] to deduplicate and order quality tiers.
 */
@Singleton
class PlaybackSourceBuilder @Inject constructor() {

    /** Build a list of playable sources for a movie. */
    fun fromMovie(movie: Movie): List<PlaybackSource> {
        return movie.sources
            .filter { it.url.isNotBlank() }
            .map { it.toPlaybackSource() }
            .sortedByDescending { it.height }
    }

    /** Build a list of playable sources for an episode. */
    fun fromEpisode(episode: Episode): List<PlaybackSource> {
        return SeriesNormalizer.groupQualities(episode.sources)
            .map { quality -> quality.toPlaybackSource() }
            .sortedByDescending { it.height }
    }

    /** Build a list of playable sources from a raw [Source] list. */
    fun fromSources(sources: List<Source>): List<PlaybackSource> {
        return SeriesNormalizer.groupQualities(sources)
            .map { it.toPlaybackSource() }
            .sortedByDescending { it.height }
    }

    private fun Source.toPlaybackSource(): PlaybackSource {
        val height = SeriesNormalizer.normalizeQualityHeight(quality)
        return PlaybackSource(
            url = url,
            qualityLabel = SeriesNormalizer.canonicalQualityLabel(height, quality),
            height = height,
            originalSource = this
        )
    }

    private fun Quality.toPlaybackSource(): PlaybackSource {
        val best = SeriesNormalizer.pickBestSource(this)
        return PlaybackSource(
            url = best?.url ?: sources.firstOrNull()?.url ?: "",
            qualityLabel = label,
            height = height,
            originalSource = best
        )
    }
}
