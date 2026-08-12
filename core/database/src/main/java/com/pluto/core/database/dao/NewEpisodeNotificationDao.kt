package com.pluto.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pluto.core.database.entity.NewEpisodeNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NewEpisodeNotificationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(notification: NewEpisodeNotificationEntity): Long

    @Query("UPDATE new_episode_notifications SET shownAt = :shownAt WHERE deduplicationKey = :key")
    suspend fun markShown(key: String, shownAt: Long)

    @Query("UPDATE new_episode_notifications SET openedAt = :openedAt WHERE deduplicationKey = :key")
    suspend fun markOpened(key: String, openedAt: Long)

    @Query("SELECT * FROM new_episode_notifications WHERE openedAt IS NULL ORDER BY detectedAt DESC")
    fun observeUnread(): Flow<List<NewEpisodeNotificationEntity>>

    @Query("SELECT * FROM new_episode_notifications ORDER BY detectedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<NewEpisodeNotificationEntity>>

    @Query("SELECT COUNT(*) FROM new_episode_notifications WHERE openedAt IS NULL")
    fun observeUnreadCount(): Flow<Int>

    @Query("DELETE FROM new_episode_notifications WHERE detectedAt < :olderThan")
    suspend fun purgeOlderThan(olderThan: Long)

    @Query("DELETE FROM new_episode_notifications")
    suspend fun clearAll()
}
