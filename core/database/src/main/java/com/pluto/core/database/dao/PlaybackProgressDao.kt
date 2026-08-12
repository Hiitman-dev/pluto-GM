package com.pluto.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pluto.core.database.entity.PlaybackProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: PlaybackProgressEntity)

    @Query("DELETE FROM playback_progress WHERE contentType = :contentType AND itemId = :itemId AND (episodeId IS :episodeId OR episodeId = :episodeId)")
    suspend fun remove(contentType: String, itemId: Int, episodeId: Int?)

    @Query("DELETE FROM playback_progress")
    suspend fun clearAll()

    @Query("SELECT * FROM playback_progress WHERE contentType = :contentType AND itemId = :itemId AND (episodeId IS :episodeId OR episodeId = :episodeId) LIMIT 1")
    suspend fun find(contentType: String, itemId: Int, episodeId: Int?): PlaybackProgressEntity?

    @Query("SELECT * FROM playback_progress WHERE positionMs < durationMs * 9 / 10 ORDER BY updatedAt DESC LIMIT :limit")
    fun observeContinueWatching(limit: Int): Flow<List<PlaybackProgressEntity>>

    @Query("SELECT * FROM playback_progress ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PlaybackProgressEntity>>
}
