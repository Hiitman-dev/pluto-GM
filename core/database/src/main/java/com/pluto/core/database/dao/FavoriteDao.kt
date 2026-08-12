package com.pluto.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pluto.core.database.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE contentType = :contentType AND itemId = :itemId")
    suspend fun remove(contentType: String, itemId: Int)

    @Query("DELETE FROM favorites")
    suspend fun clearAll()

    @Query("SELECT * FROM favorites WHERE contentType = :contentType AND itemId = :itemId LIMIT 1")
    suspend fun find(contentType: String, itemId: Int): FavoriteEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE contentType = :contentType AND itemId = :itemId)")
    fun observeIsFavorite(contentType: String, itemId: Int): Flow<Boolean>

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE contentType = :type ORDER BY addedAt DESC")
    fun observeByType(type: String): Flow<List<FavoriteEntity>>
}
