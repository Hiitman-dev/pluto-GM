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
 * ApiConfigModule — provides the named API config strings.
 *
 * These are populated by the app layer from BuildConfig (which in turn
 * reads from local.properties at build time). The app module supplies
 * the concrete values via a @Module that depends on BuildConfig — see
 * app/di/AppConfigModule.kt.
 *
 * This module provides sensible defaults so unit tests in core/network
 * can run without the app module present.
 */
@Module
@InstallIn(SingletonComponent::class)
object ApiConfigModule {

    @Provides
    @Named("apiKey")
    @Singleton
    fun provideApiKey(): String = "" // overridden by app module in production

    @Provides
    @Named("apiBaseUrl")
    @Singleton
    fun provideApiBaseUrl(): String = NetworkConfig.DEFAULT_API_BASE_URL

    @Provides
    @Named("fallbackServer1")
    @Singleton
    fun provideFallbackServer1(): String = NetworkConfig.DEFAULT_FALLBACK_1

    @Provides
    @Named("fallbackServer2")
    @Singleton
    fun provideFallbackServer2(): String = NetworkConfig.DEFAULT_FALLBACK_2
}
