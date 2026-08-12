package com.pluto.core.data

import com.pluto.core.common.ApiException
import com.pluto.core.common.DispatcherProvider
import com.pluto.core.common.Result
import com.pluto.core.model.FilterType
import com.pluto.core.model.Genre
import com.pluto.core.model.Movie
import com.pluto.core.model.Poster
import com.pluto.core.model.SearchResult
import com.pluto.core.model.Season
import com.pluto.core.model.Series
import com.pluto.core.network.CountryPostersRepository
import com.pluto.core.network.CountryRepository
import com.pluto.core.network.GenreRepository
import com.pluto.core.network.MovieRepository
import com.pluto.core.network.SearchRepository
import com.pluto.core.network.SeasonsRepository
import com.pluto.core.network.SeriesRepository
import com.pluto.core.network.SeriesNormalizer
import com.pluto.core.network.fromHttp
import com.pluto.core.model.Country
import com.pluto.core.model.NormalizedSeries
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ContentRepository — facade over the network repositories.
 *
 * Returns [Result] wrappers (never throws). Each method delegates to the
 * corresponding CCloud API repository, catches exceptions, and converts
 * them to typed [ApiException]s.
 *
 * Translation: CCloud's individual repositories return raw domain models
 * (Movie, Series, etc.) directly. PLUTO wraps them in Result<T> so the UI
 * can pattern-match on Loading / Success / Error without try/catch.
 */
@Singleton
class ContentRepository @Inject constructor(
    private val movies: MovieRepository,
    private val series: SeriesRepository,
    private val seasons: SeasonsRepository,
    private val search: SearchRepository,
    private val genres: GenreRepository,
    private val countries: CountryRepository,
    private val countryPosters: CountryPostersRepository,
    private val dispatchers: DispatcherProvider
) {
    suspend fun getMovies(page: Int, genreId: Int, filterType: FilterType): Result<List<Movie>> =
        withContext(dispatchers.io) {
            runCatching { movies.getMovies(page, genreId, filterType) }
                .map { Result.success(it.filter { m -> m.title.isNotBlank() }) }
                .getOrElse { Result.error(ApiException.fromHttp(it)) }
        }

    suspend fun getSeries(page: Int, genreId: Int, filterType: FilterType): Result<List<Series>> =
        withContext(dispatchers.io) {
            runCatching { series.getSeries(page, genreId, filterType) }
                .map { Result.success(it.filter { s -> s.title.isNotBlank() }) }
                .getOrElse { Result.error(ApiException.fromHttp(it)) }
        }

    suspend fun getSeasons(seriesId: Int): Result<List<Season>> =
        withContext(dispatchers.io) {
            runCatching { seasons.getSeasons(seriesId) }
                .map(Result.Companion::success)
                .getOrElse { Result.error(ApiException.fromHttp(it)) }
        }

    suspend fun getNormalizedSeries(series: Series): Result<NormalizedSeries> =
        withContext(dispatchers.io) {
            runCatching {
                val rawSeasons = seasons.getSeasons(series.id)
                SeriesNormalizer.normalize(series, rawSeasons)
            }.map(Result.Companion::success)
                .getOrElse { Result.error(ApiException.fromHttp(it)) }
        }

    suspend fun search(query: String): Result<SearchResult> =
        withContext(dispatchers.io) {
            runCatching { search.search(query) }
                .map(Result.Companion::success)
                .getOrElse { Result.error(ApiException.fromHttp(it)) }
        }

    suspend fun getGenres(): Result<List<Genre>> =
        withContext(dispatchers.io) {
            runCatching { genres.getGenres() }
                .map(Result.Companion::success)
                .getOrElse { Result.error(ApiException.fromHttp(it)) }
        }

    suspend fun getCountries(): Result<List<Country>> =
        withContext(dispatchers.io) {
            runCatching { countries.getAllCountries() }
                .map(Result.Companion::success)
                .getOrElse { Result.error(ApiException.fromHttp(it)) }
        }

    suspend fun getPostersByCountry(
        countryId: Int,
        page: Int,
        filterType: FilterType
    ): Result<List<Poster>> = withContext(dispatchers.io) {
        runCatching { countryPosters.getPostersByCountry(countryId, page, filterType) }
            .map(Result.Companion::success)
            .getOrElse { Result.error(ApiException.fromHttp(it)) }
    }
}
