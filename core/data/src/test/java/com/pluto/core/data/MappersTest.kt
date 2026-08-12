package com.pluto.core.data

import com.google.common.truth.Truth.assertThat
import com.pluto.core.model.Genre
import com.pluto.core.model.Country
import com.pluto.core.model.Movie
import com.pluto.core.model.Source
import org.junit.Test

/**
 * MappersTest — verifies Room entity <-> domain model conversion.
 *
 * Per Section 92 ("TESTING") of the master spec: test API mapping,
 * history calculations, and data layer correctness.
 */
class MappersTest {

    @Test
    fun `movie to favorite entity preserves all fields`() {
        val movie = Movie(
            id = 42,
            type = "movie",
            title = "Inception",
            description = "A dream within a dream",
            year = 2010,
            imdb = 8.8,
            rating = 9.0,
            duration = "148 min",
            image = "img",
            cover = "cover",
            genres = listOf(Genre(1, "Sci-Fi"), Genre(2, "Action")),
            sources = listOf(Source(1, "1080p", "mp4", "url")),
            country = listOf(Country(1, "USA", "flag"))
        )

        val entity = Mappers.movieToFavoriteEntity(movie)
        assertThat(entity.contentType).isEqualTo("movie")
        assertThat(entity.itemId).isEqualTo(42)
        assertThat(entity.title).isEqualTo("Inception")
        assertThat(entity.year).isEqualTo(2010)
        assertThat(entity.imdb).isEqualTo(8.8)

        // Round-trip
        val back = Mappers.favoriteEntityToItem(entity)
        assertThat(back.id).isEqualTo(42)
        assertThat(back.title).isEqualTo("Inception")
        assertThat(back.genres).hasSize(2)
        assertThat(back.genres[0].title).isEqualTo("Sci-Fi")
        assertThat(back.sources).hasSize(1)
        assertThat(back.sources[0].url).isEqualTo("url")
    }

    @Test
    fun `genre list round-trips through JSON`() {
        val genres = listOf(Genre(1, "Drama"), Genre(2, "Action"), Genre(3, "Sci-Fi"))
        val json = Mappers.encodeGenres(genres)
        val back = Mappers.decodeGenres(json)
        assertThat(back).hasSize(3)
        assertThat(back[0].title).isEqualTo("Drama")
    }

    @Test
    fun `decodeGenres handles null and invalid input`() {
        assertThat(Mappers.decodeGenres(null)).isEmpty()
        assertThat(Mappers.decodeGenres("")).isEmpty()
        assertThat(Mappers.decodeGenres("not json")).isEmpty()
    }

    @Test
    fun `decodeSources handles null and invalid input`() {
        assertThat(Mappers.decodeSources(null)).isEmpty()
        assertThat(Mappers.decodeSources("invalid")).isEmpty()
    }

    @Test
    fun `series to favorite entity stores empty sources list`() {
        val series = com.pluto.core.model.Series(
            id = 7,
            title = "Test Series",
            year = 2024,
            genres = listOf(Genre(1, "Drama"))
        )
        val entity = Mappers.seriesToFavoriteEntity(series)
        assertThat(entity.contentType).isEqualTo("series")
        assertThat(entity.sourcesJson).isEqualTo("[]")
    }
}
