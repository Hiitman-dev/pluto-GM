package com.pluto.core.network

import com.pluto.core.common.PlutoLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * NetworkModule — Hilt DI for OkHttp + named API config strings.
 *
 * DIRECT PORT of CCloud's `di/NetworkModule.kt` + `di/RetrofitModule.kt`,
 * adapted for PLUTO (no Retrofit — we use OkHttp directly to match CCloud's
 * existing repository pattern, which uses OkHttp + org.json).
 *
 * Secrets are loaded from BuildConfig at the app layer (see app/PlutoApplication.kt)
 * and passed down via Hilt @Named qualifiers. They are NEVER hardcoded here.
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
            .build()
    }
}

/**
 * ApiConfigModule (removed)
 *
 * The four `@Named` API config strings (`apiKey`, `apiBaseUrl`,
 * `fallbackServer1`, `fallbackServer2`) are now provided solely by the
 * app layer's `AppConfigModule` (`app/.../di/AppConfigModule.kt`), which
 * reads them from `BuildConfig` (sourced from `local.properties` at build
 * time per the spec's "secrets → BuildConfig → Hilt" rule).
 *
 * Having a second module here install the same qualified `String` bindings
 * into `SingletonComponent` caused a Hilt/Dagger `DuplicateBindings`
 * compile error. The defaults previously provided here duplicated the
 * BuildConfig defaults verbatim (same hosts), so removing them does not
 * change production behavior. `SeriesNormalizerTest` — the only unit
 * test in this module — needs no Hilt graph.
 *
 * Default API host values live on as compile-time constants in
 * `NetworkConfig` (`BaseRepository.kt`) for any non-DI caller that wants
 * them; they are no longer exposed as Hilt bindings.
 */
