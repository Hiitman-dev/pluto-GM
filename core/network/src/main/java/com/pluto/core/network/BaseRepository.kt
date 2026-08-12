package com.pluto.core.network

import com.pluto.core.common.PlutoLogger
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * BaseRepository — multi-server fallback HTTP client.
 *
 * Behavior:
 *   1. Try the primary server with the supplied URL.
 *   2. If it throws OR returns non-2xx, iterate the fallback servers,
 *      replacing the host but keeping the path/query.
 *   3. If all servers fail, re-throw the original primary exception.
 *
 * 4xx responses are NOT retried on fallback servers — they indicate a
 * client-side issue (bad URL, invalid API key, etc.) that fallback
 * servers will also reject. 5xx responses and network errors ARE
 * retried on fallback servers.
 *
 * The CCloud API key is appended as the final path segment, exactly as
 * CCloud does: `/api/<path>/<apiKey>`.
 */
@Singleton
open class BaseRepository @Inject constructor(
    protected val client: OkHttpClient,
    @Named("apiKey") protected val apiKey: String,
    @Named("apiBaseUrl") protected val apiBaseUrl: String,
    @Named("fallbackServer1") private val fallbackServer1: String,
    @Named("fallbackServer2") private val fallbackServer2: String
) {
    protected val helperServers: Array<String> = arrayOf(fallbackServer1, fallbackServer2)

    /**
     * Execute a request against the primary server, then fall back to the
     * helper servers if the primary fails. Returns the raw JSON body on
     * success, throws on total failure.
     *
     * Failure modes:
     *   - [HttpClientException] (4xx): NOT retried — re-thrown immediately.
     *   - [HttpServerException] (5xx): retried on fallback servers.
     *   - [IOException] (network/timeout): retried on fallback servers.
     *   - Empty body: treated as a server error, retried on fallback.
     */
    protected suspend fun executeRequest(
        primaryUrl: String,
        requestBuilder: (String) -> Request
    ): String {
        val primaryResponse = runCatching {
            executeOnce(primaryUrl, requestBuilder)
        }

        // 4xx — re-throw immediately, no fallback
        primaryResponse.exceptionOrNull()?.let { e ->
            if (e is HttpClientException) throw e
        }
        if (primaryResponse.isSuccess) {
            return primaryResponse.getOrThrow()
        }

        // Primary failed (5xx or network) — try each helper server
        val primaryError = primaryResponse.exceptionOrNull()
            ?: IllegalStateException("Unknown primary failure")

        PlutoLogger.w("PLUTO-Net", "Primary failed (${primaryError.message}); trying fallbacks")

        for (helperServer in helperServers) {
            if (helperServer.isBlank()) continue
            val helperUrl = primaryUrl.replace(Regex("^https?://[^/]+"), helperServer)
            val helperResult = runCatching { executeOnce(helperUrl, requestBuilder) }
            if (helperResult.isSuccess) {
                return helperResult.getOrThrow()
            }
            helperResult.exceptionOrNull()?.let { e ->
                if (e is HttpClientException) throw e // 4xx on helper — stop
            }
        }

        // All servers exhausted — re-throw the original primary error
        throw primaryError
    }

    /**
     * Execute a single HTTP request. Throws:
     *   - [HttpClientException] for 4xx responses (caller decides whether to retry).
     *   - [HttpServerException] for 5xx responses.
     *   - The original [IOException] for network failures.
     *   - [IllegalStateException] for empty response bodies.
     */
    private fun executeOnce(url: String, requestBuilder: (String) -> Request): String {
        val request = requestBuilder(url)
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                return response.body?.string()
                    ?: throw IllegalStateException("Empty response body from $url")
            }
            if (response.code in 400..499) {
                throw HttpClientException(response.code, url)
            }
            throw HttpServerException(response.code, url)
        }
    }

    /** Build a CCloud API URL by concatenating base + path segments + API key. */
    protected fun buildApiUrl(vararg segments: Any): String {
        val path = segments.joinToString("/") { it.toString() }
        return "$apiBaseUrl/api/$path/$apiKey"
    }

    protected fun buildApiUrlWithTrailingSlash(vararg segments: Any): String {
        return "${buildApiUrl(*segments)}/"
    }
}

/**
 * 4xx HTTP response — client-side error, do NOT retry on fallback servers.
 */
class HttpClientException(val code: Int, val url: String) : Exception("HTTP $code at $url")

/**
 * 5xx HTTP response — server-side error, retry on fallback servers.
 */
class HttpServerException(val code: Int, val url: String) : Exception("HTTP $code at $url")

// NetworkConfig and FilterType.toUrlSegment() are declared in NetworkModule.kt.
