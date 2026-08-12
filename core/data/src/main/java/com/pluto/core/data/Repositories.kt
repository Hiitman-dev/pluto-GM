package com.pluto.core.data

import com.pluto.core.common.Result
import com.pluto.core.common.ApiException
import com.pluto.core.common.DispatcherProvider
import com.pluto.core.database.dao.FavoriteDao
import com.pluto.core.database.dao.HistoryDao
import com.pluto.core.database.dao.PlaybackProgressDao
import com.pluto.core.database.dao.RecentSearchDao
import com.pluto.core.database.entity.RecentSearchEntity
import com.pluto.core.model.FavoriteItem
import com.pluto.core.model.Movie
import com.pluto.core.model.PlaybackProgress
import com.pluto.core.model.Series
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FavoritesRepository — wraps FavoriteDao.
 *
 * Mirrors CCloud's `data/repository/FavoritesRepository.kt` behavior:
 *   - save / remove / isFavorite / loadAllFavorites / clearAll
 * CCloud stores favorites in `favorites.json` (filesDir). PLUTO uses
 * Room (per Section 6 of the master spec: "Room where structured
 * persistence is required").
 */
@Singleton
class FavoritesRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val dispatchers: DispatcherProvider
) {
    fun observeAll(): Flow<List<FavoriteItem>> =
        favoriteDao.observeAll().map { it.map(Mappers::favoriteEntityToItem) }

    fun observeMovies(): Flow<List<FavoriteItem>> =
        favoriteDao.observeByType("movie").map { it.map(Mappers::favoriteEntityToItem) }

    fun observeSeries(): Flow<List<FavoriteItem>> =
        favoriteDao.observeByType("series").map { it.map(Mappers::favoriteEntityToItem) }

    fun observeIsFavorite(contentType: String, itemId: Int): Flow<Boolean> =
        favoriteDao.observeIsFavorite(contentType, itemId)

    suspend fun isFavorite(contentType: String, itemId: Int): Boolean = withContext(dispatchers.io) {
        favoriteDao.find(contentType, itemId) != null
    }

    suspend fun saveMovie(movie: Movie) = withContext(dispatchers.io) {
        favoriteDao.upsert(Mappers.movieToFavoriteEntity(movie))
    }

    suspend fun saveSeries(series: Series) = withContext(dispatchers.io) {
        favoriteDao.upsert(Mappers.seriesToFavoriteEntity(series))
    }

    suspend fun remove(contentType: String, itemId: Int) = withContext(dispatchers.io) {
        favoriteDao.remove(contentType, itemId)
    }

    suspend fun clearAll() = withContext(dispatchers.io) {
        favoriteDao.clearAll()
    }
}

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao,
    private val progressDao: PlaybackProgressDao,
    private val dispatchers: DispatcherProvider
) {
    fun observeRecent(limit: Int = 20) =
        historyDao.observeRecent(limit)

    fun observeContinueWatching(limit: Int = 12) =
        progressDao.observeContinueWatching(limit)

    suspend fun markViewed(
        contentType: String,
        itemId: Int,
        title: String,
        image: String,
        genresJson: String,
        episodeId: Int? = null,
        seasonId: Int? = null
    ) = withContext(dispatchers.io) {
        historyDao.upsert(
            com.pluto.core.database.entity.HistoryEntity(
                contentType = contentType,
                itemId = itemId,
                episodeId = episodeId,
                seasonId = seasonId,
                title = title,
                image = image,
                genresJson = genresJson,
                watchedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveProgress(progress: PlaybackProgress) = withContext(dispatchers.io) {
        progressDao.upsert(Mappers.progressToEntity(progress))
    }

    suspend fun getProgress(contentType: String, itemId: Int, episodeId: Int?): PlaybackProgress? =
        withContext(dispatchers.io) {
            progressDao.find(contentType, itemId, episodeId)?.let(Mappers::entityToProgress)
        }

    suspend fun clearAll() = withContext(dispatchers.io) {
        historyDao.clearAll()
        progressDao.clearAll()
    }
}

@Singleton
class RecentSearchRepository @Inject constructor(
    private val recentSearchDao: RecentSearchDao,
    private val dispatchers: DispatcherProvider
) {
    fun observeRecent(limit: Int = 10) = recentSearchDao.observeRecent(limit)

    suspend fun add(query: String) = withContext(dispatchers.io) {
        if (query.isBlank()) return@withContext
        recentSearchDao.upsert(RecentSearchEntity(query = query.trim(), searchedAt = System.currentTimeMillis()))
    }

    suspend fun remove(query: String) = withContext(dispatchers.io) {
        recentSearchDao.remove(query)
    }

    suspend fun clearAll() = withContext(dispatchers.io) {
        recentSearchDao.clearAll()
    }
}
