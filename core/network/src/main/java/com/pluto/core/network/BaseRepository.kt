package com.pluto.core.network

import com.pluto.core.model.FilterType
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * BaseRepository — multi-server fallback HTTP client.
 *
 * DIRECT PORT of CCloud's `data/repository/BaseRepository.kt` to PLUTO.
 *
 * Behavior (mirrors CCloud exactly):
 *   1. Try the primary server with the supplied URL.
 *   2. If it throws OR returns non-2xx, iterate the fallback servers,
 *      replacing the host but keeping the path/query.
 *   3. If all servers fail, re-throw the original primary exception.
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
     */
    protected suspend fun executeRequest(
        primaryUrl: String,
        requestBuilder: (String) -> Request
    ): String {
        return try {
            val primaryRequest = requestBuilder(primaryUrl)
            client.newCall(primaryRequest).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                        ?: throw IllegalStateException("Empty response body from primary server")
                } else {
                    throw IllegalStateException("Primary server returned HTTP ${response.code}")
                }
            }
        } catch (primaryException: Exception) {
            // 4xx errors are not retried on fallback servers (matches CCloud)
            if (primaryException is IllegalStateException &&
                primaryException.message?.contains("HTTP 4") == true
            ) {
                throw primaryException
            }
            // Try each helper server in turn
            for (helperServer in helperServers) {
                if (helperServer.isEmpty()) continue
                try {
                    val helperUrl = primaryUrl.replace(Regex("^https?://[^/]+"), helperServer)
                    val helperRequest = requestBuilder(helperUrl)
                    client.newCall(helperRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            return response.body?.string()
                                ?: throw IllegalStateException("Empty response body from helper")
                        }
                    }
                } catch (_: Exception) {
                    // Move to next helper
                    continue
                }
            }
            // All servers exhausted — re-throw the original error
            throw primaryException
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
 * Companion object with configuration constants used by Hilt module.
 */
object NetworkConfig {
    const val DEFAULT_TIMEOUT_SECONDS = 30L
    const val DEFAULT_API_BASE_URL = "https://server-hi-speed-iran.info"
    const val DEFAULT_FALLBACK_1 = "https://hostinnegar.com"
    const val DEFAULT_FALLBACK_2 = "https://windowsdiba.info"
}

/**
 * Filter type -> URL segment mapping (mirrors CCloud's buildUrl).
 */
fun FilterType.toUrlSegment(): String = urlSegment
