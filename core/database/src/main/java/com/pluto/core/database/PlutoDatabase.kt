package com.pluto.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pluto.core.database.dao.FavoriteDao
import com.pluto.core.database.dao.FollowedSeriesDao
import com.pluto.core.database.dao.HistoryDao
import com.pluto.core.database.dao.NewEpisodeNotificationDao
import com.pluto.core.database.dao.PlaybackProgressDao
import com.pluto.core.database.dao.RecentSearchDao
import com.pluto.core.database.entity.FavoriteEntity
import com.pluto.core.database.entity.FollowedSeriesEntity
import com.pluto.core.database.entity.HistoryEntity
import com.pluto.core.database.entity.NewEpisodeNotificationEntity
import com.pluto.core.database.entity.PlaybackProgressEntity
import com.pluto.core.database.entity.RecentSearchEntity

/**
 * PlutoDatabase — single Room database for all local PLUTO data.
 *
 * Mirrors CCloud's filesDir JSON files (favorites.json, recently_viewed.json,
 * watched_episodes.json) but uses Room for structured, queryable persistence.
 *
 * Per Section 76 of the master spec ("SECURITY"): no credentials stored here.
 * Only favorites, history, progress, followed series, recent searches, and
 * new-episode notifications — all user data, no auth tokens.
 */
@Database(
    entities = [
        FavoriteEntity::class,
        HistoryEntity::class,
        PlaybackProgressEntity::class,
        FollowedSeriesEntity::class,
        NewEpisodeNotificationEntity::class,
        RecentSearchEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class PlutoDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun progressDao(): PlaybackProgressDao
    abstract fun followedSeriesDao(): FollowedSeriesDao
    abstract fun newEpisodeDao(): NewEpisodeNotificationDao
    abstract fun recentSearchDao(): RecentSearchDao

    companion object {
        const val DATABASE_NAME = "pluto.db"
    }
}
