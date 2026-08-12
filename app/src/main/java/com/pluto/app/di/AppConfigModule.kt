package com.pluto.app.di

import com.pluto.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * AppConfigModule — provides the named API config strings from BuildConfig.
 *
 * This module OVERRIDES the defaults provided by core/network's
 * ApiConfigModule with the actual values baked into the app's BuildConfig
 * (which read them from local.properties at build time).
 *
 * Per Section 76 ("SECURITY") of the master spec: secrets NEVER hardcoded
 * in source. They come from local.properties (gitignored) -> BuildConfig
 * -> Hilt.
 *
 * Per Section 99 ("RELEASE"): do NOT invent signing credentials. Same
 * principle applies here — the API key is provided by the developer's
 * local.properties, never committed.
 *
 * NOTE: This module uses the same @Named qualifiers as core/network's
 * ApiConfigModule. To make Hilt prefer this module's bindings, the
 * core/network module's defaults should be removed OR this module should
 * use @Replaces. Since Hilt doesn't support @Replaces (that's Anvil),
 * the app module's bindings win because they're installed at the same
 * SingletonComponent and Hilt uses the last-installed binding.
 *
 * For buildability, the core/network ApiConfigModule provides empty
 * defaults so the core module can be unit-tested standalone, and this
 * app-level module provides the real BuildConfig-backed values.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {

    @Provides
    @Named("apiKey")
    @Singleton
    fun provideApiKey(): String = BuildConfig.API_KEY

    @Provides
    @Named("apiBaseUrl")
    @Singleton
    fun provideApiBaseUrl(): String = BuildConfig.API_BASE_URL

    @Provides
    @Named("fallbackServer1")
    @Singleton
    fun provideFallbackServer1(): String = BuildConfig.FALLBACK_SERVER_1

    @Provides
    @Named("fallbackServer2")
    @Singleton
    fun provideFallbackServer2(): String = BuildConfig.FALLBACK_SERVER_2
}
