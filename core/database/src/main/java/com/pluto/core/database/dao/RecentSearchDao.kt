package com.pluto.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pluto.core.database.entity.RecentSearchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(search: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE query = :query")
    suspend fun remove(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clearAll()

    @Query("SELECT * FROM recent_searches ORDER BY searchedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RecentSearchEntity>>
}
