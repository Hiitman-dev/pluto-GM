package com.pluto.core.network

import com.pluto.core.model.FilterType
import com.pluto.core.model.Genre
import com.pluto.core.model.Movie
import com.pluto.core.model.Season
import com.pluto.core.model.Series
import com.pluto.core.model.SearchResult
import com.pluto.core.model.Country
import com.pluto.core.model.Poster
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * MovieRepository — movies endpoint.
 *
 * DIRECT PORT of CCloud's `data/repository/MovieRepository.kt`.
 *
 * Endpoint: GET {API_BASE_URL}/api/movie/by/filtres/{genreId}/{filterType}/{page}/{apiKey}
 */
@Singleton
class MovieRepository @Inject constructor(
    client: OkHttpClient,
    @Named("apiKey") apiKey: String,
    @Named("apiBaseUrl") apiBaseUrl: String,
    @Named("fallbackServer1") fallbackServer1: String,
    @Named("fallbackServer2") fallbackServer2: String
) : BaseRepository(client, apiKey, apiBaseUrl, fallbackServer1, fallbackServer2) {

    private val baseUrl: String = "$apiBaseUrl/api/movie/by/filtres"

    suspend fun getMovies(page: Int, genreId: Int, filterType: FilterType): List<Movie> {
        val url = buildUrl(baseUrl, genreId, filterType, page)
        val json = executeRequest(url) { Request.Builder().url(it).build() }
        return parseMovies(json)
    }

    private fun buildUrl(base: String, genreId: Int, filterType: FilterType, page: Int): String =
        when (filterType) {
            FilterType.DEFAULT -> "$base/$genreId/${filterType.urlSegment}/$page/$apiKey"
            FilterType.BY_YEAR -> "$base/$genreId/${filterType.urlSegment}/$page/$apiKey"
            FilterType.BY_IMDB -> "$base/$genreId/${filterType.urlSegment}/$page/$apiKey"
        }

    private fun parseMovies(json: String): List<Movie> {
        val arr = JSONArray(json)
        val movies = ArrayList<Movie>(arr.length())
        for (i in 0 until arr.length()) {
            try {
                movies.add(parseMovie(arr.getJSONObject(i)))
            } catch (_: Exception) {
                // Skip malformed items (matches CCloud behavior)
            }
        }
        return movies
    }

    private fun parseMovie(o: JSONObject): Movie = Movie(
        id = o.optInt("id", 0),
        type = o.optString("type", "movie"),
        title = o.optString("title", "Unknown Title"),
        description = o.optString("description", "No description available"),
        year = o.optInt("year", 0),
        imdb = o.optDouble("imdb", 0.0),
        rating = o.optDouble("rating", 0.0),
        duration = o.optString("duration", null)?.takeIf { it != "null" && it != "N/A" },
        image = o.optString("image", ""),
        cover = o.optString("cover", ""),
        genres = parseGenres(o.optJSONArray("genres")),
        sources = parseSources(o.optJSONArray("sources")),
        country = parseCountries(o.optJSONArray("country"))
    )

    private fun parseGenres(arr: JSONArray?): List<Genre> {
        if (arr == null) return emptyList()
        val list = ArrayList<Genre>(arr.length())
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                list.add(Genre(o.optInt("id", 0), o.optString("title", "Unknown")))
            } catch (_: Exception) {}
        }
        return list
    }

    private fun parseSources(arr: JSONArray?): List<com.pluto.core.model.Source> {
        if (arr == null) return emptyList()
        val list = ArrayList<com.pluto.core.model.Source>(arr.length())
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                list.add(
                    com.pluto.core.model.Source(
                        id = o.optInt("id", 0),
                        quality = o.optString("quality", "Unknown"),
                        type = o.optString("type", "Unknown"),
                        url = o.optString("url", "")
                    )
                )
            } catch (_: Exception) {}
        }
        return list
    }

    private fun parseCountries(arr: JSONArray?): List<Country> {
        if (arr == null) return emptyList()
        val list = ArrayList<Country>(arr.length())
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                list.add(Country(o.optInt("id", 0), o.optString("title", "Unknown"), o.optString("image", "")))
            } catch (_: Exception) {}
        }
        return list
    }
}

/**
 * SeriesRepository — series endpoint.
 *
 * DIRECT PORT of CCloud's `data/repository/SeriesRepository.kt`.
 *
 * Endpoint: GET {API_BASE_URL}/api/serie/by/filtres/{genreId}/{filterType}/{page}/{apiKey}
 */
@Singleton
class SeriesRepository @Inject constructor(
    client: OkHttpClient,
    @Named("apiKey") apiKey: String,
    @Named("apiBaseUrl") apiBaseUrl: String,
    @Named("fallbackServer1") fallbackServer1: String,
    @Named("fallbackServer2") fallbackServer2: String
) : BaseRepository(client, apiKey, apiBaseUrl, fallbackServer1, fallbackServer2) {

    private val baseUrl: String = "$apiBaseUrl/api/serie/by/filtres"

    suspend fun getSeries(page: Int, genreId: Int, filterType: FilterType): List<Series> {
        val url = "$baseUrl/$genreId/${filterType.urlSegment}/$page/$apiKey"
        val json = executeRequest(url) { Request.Builder().url(it).build() }
        return parseSeries(json)
    }

    private fun parseSeries(json: String): List<Series> {
        val arr = JSONArray(json)
        val list = ArrayList<Series>(arr.length())
        for (i in 0 until arr.length()) {
            try {
                list.add(parseSeriesItem(arr.getJSONObject(i)))
            } catch (_: Exception) {}
        }
        return list
    }

    private fun parseSeriesItem(o: JSONObject): Series = Series(
        id = o.optInt("id", 0),
        type = o.optString("type", "serie"),
        title = o.optString("title", "Unknown Title"),
        description = o.optString("description", "No description available"),
        year = o.optInt("year", 0),
        imdb = o.optDouble("imdb", 0.0),
        rating = o.optDouble("rating", 0.0),
        duration = o.optString("duration", null)?.takeIf { it != "null" && it != "N/A" },
        image = o.optString("image", ""),
        cover = o.optString("cover", ""),
        genres = parseGenresArr(o.optJSONArray("genres")),
        country = parseCountriesArr(o.optJSONArray("country"))
    )

    private fun parseGenresArr(arr: JSONArray?): List<Genre> {
        if (arr == null) return emptyList()
        val list = ArrayList<Genre>(arr.length())
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                list.add(Genre(o.optInt("id", 0), o.optString("title", "Unknown")))
            } catch (_: Exception) {}
        }
        return list
    }

    private fun parseCountriesArr(arr: JSONArray?): List<Country> {
        if (arr == null) return emptyList()
        val list = ArrayList<Country>(arr.length())
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                list.add(Country(o.optInt("id", 0), o.optString("title", "Unknown"), o.optString("image", "")))
            } catch (_: Exception) {}
        }
        return list
    }
}

/**
 * SeasonsRepository — seasons + episodes for a series.
 *
 * DIRECT PORT of CCloud's `data/repository/SeasonsRepository.kt`.
 *
 * Endpoint: GET {API_BASE_URL}/api/season/by/serie/{seriesId}/{apiKey}/
 * Response: array of seasons, each containing its full episodes list
 *           (including each episode's sources).
 */
@Singleton
class SeasonsRepository @Inject constructor(
    client: OkHttpClient,
    @Named("apiKey") apiKey: String,
    @Named("apiBaseUrl") apiBaseUrl: String,
    @Named("fallbackServer1") fallbackServer1: String,
    @Named("fallbackServer2") fallbackServer2: String
) : BaseRepository(client, apiKey, apiBaseUrl, fallbackServer1, fallbackServer2) {

    private val baseUrl: String = "$apiBaseUrl/api/season/by/serie"

    suspend fun getSeasons(seriesId: Int): List<Season> {
        val url = "$baseUrl/$seriesId/$apiKey/"
        val json = executeRequest(url) { Request.Builder().url(it).build() }
        return parseSeasons(json)
    }

    private fun parseSeasons(json: String): List<Season> {
        val arr = JSONArray(json)
        val list = ArrayList<Season>(arr.length())
        for (i in 0 until arr.length()) {
            try {
                list.add(parseSeason(arr.getJSONObject(i), i))
            } catch (_: Exception) {}
        }
        return list
    }

    private fun parseSeason(o: JSONObject, seasonIndex: Int): Season = Season(
        id = o.optInt("id", 0),
        title = o.optString("title", "Season ${seasonIndex + 1}"),
        seasonNumber = seasonIndex + 1,
        episodes = parseEpisodes(o.optJSONArray("episodes"))
    )

    private fun parseEpisodes(arr: JSONArray?): List<com.pluto.core.model.Episode> {
        if (arr == null) return emptyList()
        val list = ArrayList<com.pluto.core.model.Episode>(arr.length())
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                list.add(
                    com.pluto.core.model.Episode(
                        id = o.optInt("id", 0),
                        title = o.optString("title", "Episode ${i + 1}"),
                        description = o.optString("description", ""),
                        duration = o.optString("duration", null)?.takeIf { it != "null" },
                        image = o.optString("image", ""),
                        sources = parseSources(o.optJSONArray("sources")),
                        episodeNumber = i + 1
                    )
                )
            } catch (_: Exception) {}
        }
        return list
    }

    private fun parseSources(arr: JSONArray?): List<com.pluto.core.model.Source> {
        if (arr == null) return emptyList()
        val list = ArrayList<com.pluto.core.model.Source>(arr.length())
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                list.add(
                    com.pluto.core.model.Source(
                        id = o.optInt("id", 0),
                        quality = o.optString("quality", "Unknown"),
                        type = o.optString("type", "Unknown"),
                        url = o.optString("url", "")
                    )
                )
            } catch (_: Exception) {}
        }
        return list
    }
}

/**
 * SearchRepository — debounced search across movies + series.
 *
 * DIRECT PORT of CCloud's `data/repository/SearchRepository.kt`.
 *
 * Endpoint: GET {API_BASE_URL}/api/search/{encodedQuery}/{apiKey}/
 * Response: { posters: [...] } — movies AND series mixed, distinguished by `type`.
 */
@Singleton
class SearchRepository @Inject constructor(
    client: OkHttpClient,
    @Named("apiKey") apiKey: String,
    @Named("apiBaseUrl") apiBaseUrl: String,
    @Named("fallbackServer1") fallbackServer1: String,
    @Named("fallbackServer2") fallbackServer2: String
) : BaseRepository(client, apiKey, apiBaseUrl, fallbackServer1, fallbackServer2) {

    private val baseUrl: String = "$apiBaseUrl/api/search"

    suspend fun search(query: String): SearchResult {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8").replace("+", "%20")
        val url = "$baseUrl/$encoded/$apiKey/"
        val json = executeRequest(url) { Request.Builder().url(it).build() }
        return parseSearchResult(json)
    }

    private fun parseSearchResult(json: String): SearchResult {
        val obj = JSONObject(json)
        val arr = obj.optJSONArray("posters")
        val posters = ArrayList<Poster>(arr?.length() ?: 0)
        if (arr != null) {
            for (i in 0 until arr.length()) {
                try {
                    posters.add(parsePoster(arr.getJSONObject(i)))
                } catch (_: Exception) {}
            }
        }
        return SearchResult(posters)
    }

    private fun parsePoster(o: JSONObject): Poster = Poster(
        id = o.optInt("id", 0),
        title = o.optString("title", "Unknown Title"),
        type = o.optString("type", ""),
        description = o.optString("description", "No description available"),
        year = o.optInt("year", 0),
        imdb = o.optDouble("imdb", 0.0),
        rating = o.optDouble("rating", 0.0),
        duration = o.optString("duration", null)?.takeIf { it != "null" && it != "N/A" },
        image = o.optString("image", ""),
        cover = o.optString("cover", ""),
        genres = parseGenres(o.optJSONArray("genres")),
        sources = parseSources(o.optJSONArray("sources")),
        country = parseCountries(o.optJSONArray("country"))
    )

    private fun parseGenres(arr: JSONArray?): List<Genre> {
        if (arr == null) return emptyList()
        val list = ArrayList<Genre>(arr.length())
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                list.add(Genre(o.optInt("id", 0), o.optString("title", "Unknown")))
            } catch (_: Exception) {}
        }
        return list
    }

    private fun parseSources(arr: JSONArray?): List<com.pluto.core.model.Source> {
        if (arr == null) return emptyList()
        val list = ArrayList<com.pluto.core.model.Source>(arr.length())
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                list.add(
                    com.pluto.core.model.Source(
                        id = o.optInt("id", 0),
                        quality = o.optString("quality", "Unknown"),
                        type = o.optString("type", "Unknown"),
                        url = o.optString("url", "")
                    )
                )
            } catch (_: Exception) {}
        }
        return list
    }

    private fun parseCountries(arr: JSONArray?): List<Country> {
        if (arr == null) return emptyList()
        val list = ArrayList<Country>(arr.length())
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                list.add(Country(o.optInt("id", 0), o.optString("title", "Unknown"), o.optString("image", "")))
            } catch (_: Exception) {}
        }
        return list
    }
}

/**
 * GenreRepository — list of all genres.
 *
 * DIRECT PORT of CCloud's `data/repository/GenreRepository.kt`.
 *
 * Endpoint: GET {API_BASE_URL}/api/genre/all/{apiKey}
 * Response: array of { id, title }
 */
@Singleton
class GenreRepository @Inject constructor(
    client: OkHttpClient,
    @Named("apiKey") apiKey: String,
    @Named("apiBaseUrl") apiBaseUrl: String,
    @Named("fallbackServer1") fallbackServer1: String,
    @Named("fallbackServer2") fallbackServer2: String
) : BaseRepository(client, apiKey, apiBaseUrl, fallbackServer1, fallbackServer2) {

    private val url: String = "$apiBaseUrl/api/genre/all"

    suspend fun getGenres(): List<Genre> {
        val json = executeRequest("$url/$apiKey") { Request.Builder().url(it).build() }
        val arr = JSONArray(json)
        val list = ArrayList<Genre>(arr.length())
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                list.add(Genre(o.optInt("id", 0), o.optString("title", "Unknown")))
            } catch (_: Exception) {}
        }
        return list.sortedBy { it.title }
    }
}

/**
 * CountryRepository — list of all countries.
 *
 * DIRECT PORT of CCloud's `data/repository/CountryRepository.kt`.
 */
@Singleton
class CountryRepository @Inject constructor(
    client: OkHttpClient,
    @Named("apiKey") apiKey: String,
    @Named("apiBaseUrl") apiBaseUrl: String,
    @Named("fallbackServer1") fallbackServer1: String,
    @Named("fallbackServer2") fallbackServer2: String
) : BaseRepository(client, apiKey, apiBaseUrl, fallbackServer1, fallbackServer2) {

    private val url: String = "$apiBaseUrl/api/country/all"

    suspend fun getAllCountries(): List<Country> {
        val json = executeRequest("$url/$apiKey/") { Request.Builder().url(it).build() }
        val arr = JSONArray(json)
        val list = ArrayList<Country>(arr.length())
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                list.add(Country(o.optInt("id", 0), o.optString("title", "Unknown"), o.optString("image", "")))
            } catch (_: Exception) {}
        }
        return list
    }
}

/**
 * CountryPostersRepository — posters (movies + series) by country.
 *
 * DIRECT PORT of CCloud's `data/repository/CountryPostersRepository.kt`.
 *
 * Endpoint: GET {API_BASE_URL}/api/poster/by/filtres/0/{countryId}/{filterType}/{page}/{apiKey}
 */
@Singleton
class CountryPostersRepository @Inject constructor(
    client: OkHttpClient,
    @Named("apiKey") apiKey: String,
    @Named("apiBaseUrl") apiBaseUrl: String,
    @Named("fallbackServer1") fallbackServer1: String,
    @Named("fallbackServer2") fallbackServer2: String
) : BaseRepository(client, apiKey, apiBaseUrl, fallbackServer1, fallbackServer2) {

    private val baseUrl: String = "$apiBaseUrl/api/poster/by/filtres"

    suspend fun getPostersByCountry(
        countryId: Int,
        page: Int,
        filterType: FilterType
    ): List<Poster> {
        val url = "$baseUrl/0/$countryId/${filterType.urlSegment}/$page/$apiKey"
        val json = executeRequest(url) { Request.Builder().url(it).build() }
        val arr = JSONArray(json)
        val list = ArrayList<Poster>(arr.length())
        for (i in 0 until arr.length()) {
            try {
                list.add(parsePoster(arr.getJSONObject(i)))
            } catch (_: Exception) {}
        }
        return list
    }

    private fun parsePoster(o: JSONObject): Poster = Poster(
        id = o.optInt("id", 0),
        title = o.optString("title", "Unknown Title"),
        type = o.optString("type", ""),
        description = o.optString("description", "No description available"),
        year = o.optInt("year", 0),
        imdb = o.optDouble("imdb", 0.0),
        rating = o.optDouble("rating", 0.0),
        duration = o.optString("duration", null)?.takeIf { it != "null" && it != "N/A" },
        image = o.optString("image", ""),
        cover = o.optString("cover", ""),
        genres = emptyList(),
        sources = emptyList(),
        country = emptyList()
    )
}
