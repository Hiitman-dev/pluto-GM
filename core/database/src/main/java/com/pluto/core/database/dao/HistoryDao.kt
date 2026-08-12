package com.pluto.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pluto.core.database.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(history: HistoryEntity)

    @Query("DELETE FROM history WHERE contentType = :contentType AND itemId = :itemId AND (episodeId IS :episodeId OR episodeId = :episodeId)")
    suspend fun remove(contentType: String, itemId: Int, episodeId: Int?)

    @Query("DELETE FROM history")
    suspend fun clearAll()

    @Query("SELECT * FROM history ORDER BY watchedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE contentType = :contentType AND itemId = :itemId AND episodeId IS :episodeId LIMIT 1")
    suspend fun find(contentType: String, itemId: Int, episodeId: Int?): HistoryEntity?
}
