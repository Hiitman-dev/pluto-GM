package com.pluto.core.network

import com.pluto.core.model.Country
import com.pluto.core.model.FilterType
import com.pluto.core.model.Genre
import com.pluto.core.model.Movie
import com.pluto.core.model.Poster
import com.pluto.core.model.Season
import com.pluto.core.model.Series
import kotlinx.serialization.Serializable

/**
 * CCloud API DTOs — exact response shapes returned by the upstream API.
 *
 * Translation map (Kotlin -> these DTOs):
 *   CCloud `data/remote/response/MovieResponse.kt`  -> MovieDto
 *   CCloud `data/remote/response/SeriesResponse.kt` -> SeriesDto
 *   CCloud `data/remote/response/SearchResponse.kt` -> SearchResponse
 *
 * All fields are nullable with safe defaults because CCloud's JSON is
 * non-strict and may omit fields per content type.
 */

@Serializable
data class GenreDto(val id: Int = 0, val title: String = "")

@Serializable
data class CountryDto(val id: Int = 0, val title: String = "", val image: String = "")

@Serializable
data class SourceDto(
    val id: Int = 0,
    val quality: String = "Unknown",
    val type: String = "Unknown",
    val url: String = ""
)

@Serializable
data class MovieDto(
    val id: Int = 0,
    val type: String = "movie",
    val title: String = "Unknown Title",
    val description: String = "No description available",
    val year: Int = 0,
    val imdb: Double = 0.0,
    val rating: Double = 0.0,
    val duration: String? = null,
    val image: String = "",
    val cover: String = "",
    val genres: List<GenreDto> = emptyList(),
    val sources: List<SourceDto> = emptyList(),
    val country: List<CountryDto> = emptyList()
)

@Serializable
data class SeriesDto(
    val id: Int = 0,
    val type: String = "serie",
    val title: String = "Unknown Title",
    val description: String = "No description available",
    val year: Int = 0,
    val imdb: Double = 0.0,
    val rating: Double = 0.0,
    val duration: String? = null,
    val image: String = "",
    val cover: String = "",
    val genres: List<GenreDto> = emptyList(),
    val country: List<CountryDto> = emptyList()
)

@Serializable
data class EpisodeDto(
    val id: Int = 0,
    val title: String = "Unknown Episode",
    val description: String = "",
    val duration: String? = null,
    val image: String = "",
    val sources: List<SourceDto> = emptyList()
)

@Serializable
data class SeasonDto(
    val id: Int = 0,
    val title: String = "Unknown Season",
    val episodes: List<EpisodeDto> = emptyList()
)

@Serializable
data class SearchResponse(val posters: List<PosterDto> = emptyList())

@Serializable
data class PosterDto(
    val id: Int = 0,
    val title: String = "Unknown Title",
    val type: String = "",
    val description: String = "No description available",
    val year: Int = 0,
    val imdb: Double = 0.0,
    val rating: Double = 0.0,
    val duration: String? = null,
    val image: String = "",
    val cover: String = "",
    val genres: List<GenreDto> = emptyList(),
    val sources: List<SourceDto> = emptyList(),
    val country: List<CountryDto> = emptyList()
)

// ── DTO -> Domain mappers ────────────────────────────────────────────────

fun GenreDto.toDomain() = Genre(id, title)
fun CountryDto.toDomain() = Country(id, title, image)
fun SourceDto.toDomain() = com.pluto.core.model.Source(id, quality, type, url)

fun MovieDto.toDomain() = Movie(
    id = id,
    type = type,
    title = title,
    description = description,
    year = year,
    imdb = imdb,
    rating = rating,
    duration = duration?.takeIf { it != "null" && it != "N/A" },
    image = image,
    cover = cover,
    genres = genres.map { it.toDomain() },
    sources = sources.map { it.toDomain() },
    country = country.map { it.toDomain() }
)

fun SeriesDto.toDomain() = Series(
    id = id,
    type = type,
    title = title,
    description = description,
    year = year,
    imdb = imdb,
    rating = rating,
    duration = duration?.takeIf { it != "null" && it != "N/A" },
    image = image,
    cover = cover,
    genres = genres.map { it.toDomain() },
    country = country.map { it.toDomain() }
)

fun EpisodeDto.toDomain(index: Int) = com.pluto.core.model.Episode(
    id = id,
    title = title,
    description = description,
    duration = duration?.takeIf { it != "null" },
    image = image,
    sources = sources.map { it.toDomain() },
    episodeNumber = index + 1
)

fun SeasonDto.toDomain() = Season(
    id = id,
    title = title,
    episodes = episodes.mapIndexed { i, e -> e.toDomain(i) }
)

fun PosterDto.toDomain() = Poster(
    id = id,
    title = title,
    type = type,
    description = description,
    year = year,
    imdb = imdb,
    rating = rating,
    duration = duration?.takeIf { it != "null" && it != "N/A" },
    image = image,
    cover = cover,
    genres = genres.map { it.toDomain() },
    sources = sources.map { it.toDomain() },
    country = country.map { it.toDomain() }
)
