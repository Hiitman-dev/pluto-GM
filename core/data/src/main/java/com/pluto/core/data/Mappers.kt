package com.pluto.core.data

import com.pluto.core.database.entity.FavoriteEntity
import com.pluto.core.database.entity.HistoryEntity
import com.pluto.core.database.entity.PlaybackProgressEntity
import com.pluto.core.model.Country
import com.pluto.core.model.FavoriteItem
import com.pluto.core.model.Genre
import com.pluto.core.model.Movie
import com.pluto.core.model.PlaybackProgress
import com.pluto.core.model.Series
import com.pluto.core.model.Source
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Mappers between Room entities and domain models.
 *
 * The Room layer stores genre / country / source lists as JSON strings
 * (because Room doesn't natively support List<T> on a non-related entity
 * without TypeConverters — JSON strings keep the schema simple and
 * migration-friendly).
 */

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

private val genreListSerializer = ListSerializer(Genre.serializer())
private val countryListSerializer = ListSerializer(Country.serializer())
private val sourceListSerializer = ListSerializer(Source.serializer())

object Mappers {
    fun encodeGenres(genres: List<Genre>): String = json.encodeToString(genreListSerializer, genres)
    fun decodeGenres(s: String?): List<Genre> = s?.let { runCatching { json.decodeFromString(genreListSerializer, it) }.getOrNull() } ?: emptyList()

    fun encodeCountries(countries: List<Country>): String = json.encodeToString(countryListSerializer, countries)
    fun decodeCountries(s: String?): List<Country> = s?.let { runCatching { json.decodeFromString(countryListSerializer, it) }.getOrNull() } ?: emptyList()

    fun encodeSources(sources: List<Source>): String = json.encodeToString(sourceListSerializer, sources)
    fun decodeSources(s: String?): List<Source> = s?.let { runCatching { json.decodeFromString(sourceListSerializer, it) }.getOrNull() } ?: emptyList()

    fun movieToFavoriteEntity(movie: Movie): FavoriteEntity = FavoriteEntity(
        contentType = "movie",
        itemId = movie.id,
        title = movie.title,
        description = movie.description,
        year = movie.year,
        imdb = movie.imdb,
        rating = movie.rating,
        duration = movie.duration,
        image = movie.image,
        cover = movie.cover,
        genresJson = encodeGenres(movie.genres),
        countryJson = encodeCountries(movie.country),
        sourcesJson = encodeSources(movie.sources),
        addedAt = System.currentTimeMillis()
    )

    fun seriesToFavoriteEntity(series: Series): FavoriteEntity = FavoriteEntity(
        contentType = "series",
        itemId = series.id,
        title = series.title,
        description = series.description,
        year = series.year,
        imdb = series.imdb,
        rating = series.rating,
        duration = series.duration,
        image = series.image,
        cover = series.cover,
        genresJson = encodeGenres(series.genres),
        countryJson = encodeCountries(series.country),
        sourcesJson = "[]",
        addedAt = System.currentTimeMillis()
    )

    fun favoriteEntityToItem(e: FavoriteEntity): FavoriteItem = FavoriteItem(
        id = e.itemId,
        type = e.contentType,
        title = e.title,
        description = e.description,
        year = e.year,
        imdb = e.imdb,
        rating = e.rating,
        duration = e.duration,
        image = e.image,
        cover = e.cover,
        genres = decodeGenres(e.genresJson),
        country = decodeCountries(e.countryJson),
        sources = decodeSources(e.sourcesJson),
        addedAt = e.addedAt
    )

    fun favoriteToHistoryEntity(fav: FavoriteItem, episodeId: Int? = null, seasonId: Int? = null): HistoryEntity =
        HistoryEntity(
            contentType = fav.type,
            itemId = fav.id,
            episodeId = episodeId,
            seasonId = seasonId,
            title = fav.title,
            image = fav.image,
            genresJson = fav.genres.let { encodeGenres(it) },
            watchedAt = System.currentTimeMillis()
        )

    fun progressToEntity(progress: PlaybackProgress): PlaybackProgressEntity = PlaybackProgressEntity(
        contentType = progress.contentType,
        itemId = progress.contentId,
        episodeId = progress.episodeId,
        seasonId = progress.seasonId,
        positionMs = progress.positionMs,
        durationMs = progress.durationMs,
        updatedAt = progress.updatedAt
    )

    fun entityToProgress(e: PlaybackProgressEntity): PlaybackProgress = PlaybackProgress(
        contentId = e.itemId,
        contentType = e.contentType,
        episodeId = e.episodeId,
        seasonId = e.seasonId,
        positionMs = e.positionMs,
        durationMs = e.durationMs,
        updatedAt = e.updatedAt
    )
}
