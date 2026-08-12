package com.pluto.core.network

import com.pluto.core.common.PlutoLogger
import com.pluto.core.model.FilterType
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * NetworkModule — Hilt DI for OkHttp.
 *
 * Provides only the OkHttpClient. The named API config strings
 * (`apiKey`, `apiBaseUrl`, `fallbackServer1`, `fallbackServer2`)
 * are provided by the **app module's** `AppConfigModule` so they can
 * read from `BuildConfig` (which is populated from `local.properties`
 * at build time).
 *
 * IMPORTANT — Hilt does NOT support duplicate `@Named` bindings across
 * modules. Therefore the core/network module MUST NOT declare these
 * bindings, even as defaults. Tests in core/network that need a fake
 * API key construct the repositories directly (no Hilt) — see
 * `SeriesNormalizerTest`.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor { msg ->
            PlutoLogger.d("PLUTO-Net", msg)
        }.apply {
            // PlutoLogger.level is set in PlutoApplication.onCreate() before any
            // network call is made. We re-read it on every log call so the level
            // can be changed at runtime (e.g. by a debug panel).
            level = if (PlutoLogger.level == com.pluto.core.common.LogLevel.VERBOSE)
                HttpLoggingInterceptor.Level.BASIC
            else
                HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .connectTimeout(NetworkConfig.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NetworkConfig.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NetworkConfig.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .retryOnConnectionFailure(true)
            .build()
    }
}

/**
 * NetworkConfig — configuration constants.
 *
 * These are the fallback defaults used when `local.properties` is absent
 * (e.g. running unit tests, CI builds without secrets). The app module
 * overrides them at runtime via `BuildConfig` -> Hilt `@Named` bindings.
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
