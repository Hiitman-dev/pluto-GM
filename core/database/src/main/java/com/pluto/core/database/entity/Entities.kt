package com.pluto.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * FavoriteEntity — locally stored favorite movie or series.
 *
 * Mirrors CCloud's `favorites.json` schema. `contentType` is "movie" or
 * "series". `itemId` is the CCloud API id. The composite index ensures
 * a single favorite per (contentType, itemId).
 */
@Entity(
    tableName = "favorites",
    indices = [Index(value = ["contentType", "itemId"], unique = true)]
)
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val contentType: String,
    val itemId: Int,
    val title: String,
    val description: String,
    val year: Int,
    val imdb: Double,
    val rating: Double,
    val duration: String?,
    val image: String,
    val cover: String,
    val genresJson: String, // serialized List<Genre>
    val countryJson: String, // serialized List<Country>
    val sourcesJson: String, // serialized List<Source> (movies only)
    val addedAt: Long
)

@Entity(
    tableName = "history",
    indices = [Index(value = ["contentType", "itemId", "episodeId"], unique = true)]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val contentType: String,
    val itemId: Int,
    val episodeId: Int?,
    val seasonId: Int?,
    val title: String,
    val image: String,
    val genresJson: String,
    val watchedAt: Long
)

@Entity(
    tableName = "playback_progress",
    indices = [Index(value = ["contentType", "itemId", "episodeId"], unique = true)]
)
data class PlaybackProgressEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val contentType: String,
    val itemId: Int,
    val episodeId: Int?,
    val seasonId: Int?,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "followed_series",
    indices = [Index(value = ["seriesId"], unique = true)]
)
data class FollowedSeriesEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val seriesId: Int,
    val title: String,
    val poster: String,
    val lastKnownSeason: Int,
    val lastKnownEpisode: Int,
    val followedAt: Long,
    val notificationsEnabled: Boolean
)

@Entity(
    tableName = "new_episode_notifications",
    indices = [Index(value = ["deduplicationKey"], unique = true)]
)
data class NewEpisodeNotificationEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val deduplicationKey: String,
    val seriesId: Int,
    val seriesTitle: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val detectedAt: Long,
    val shownAt: Long?,
    val openedAt: Long?
)

@Entity(
    tableName = "recent_searches",
    indices = [Index(value = ["query"], unique = true)]
)
data class RecentSearchEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val query: String,
    val searchedAt: Long
)
