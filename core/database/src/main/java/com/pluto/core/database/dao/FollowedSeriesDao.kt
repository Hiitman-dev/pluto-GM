package com.pluto.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pluto.core.database.entity.FollowedSeriesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowedSeriesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(followed: FollowedSeriesEntity)

    @Query("DELETE FROM followed_series WHERE seriesId = :seriesId")
    suspend fun remove(seriesId: Int)

    @Query("UPDATE followed_series SET notificationsEnabled = :enabled WHERE seriesId = :seriesId")
    suspend fun setNotificationsEnabled(seriesId: Int, enabled: Boolean)

    @Query("UPDATE followed_series SET lastKnownSeason = :season, lastKnownEpisode = :episode WHERE seriesId = :seriesId")
    suspend fun updateLastKnown(seriesId: Int, season: Int, episode: Int)

    @Query("SELECT * FROM followed_series WHERE seriesId = :seriesId LIMIT 1")
    suspend fun find(seriesId: Int): FollowedSeriesEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM followed_series WHERE seriesId = :seriesId AND notificationsEnabled = 1)")
    fun observeIsFollowing(seriesId: Int): Flow<Boolean>

    @Query("SELECT * FROM followed_series WHERE notificationsEnabled = 1")
    suspend fun getAllActive(): List<FollowedSeriesEntity>

    @Query("SELECT * FROM followed_series ORDER BY followedAt DESC")
    fun observeAll(): Flow<List<FollowedSeriesEntity>>
}
