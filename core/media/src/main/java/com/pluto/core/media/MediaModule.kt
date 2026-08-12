package com.pluto.core.media

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * MediaModule — Hilt DI for the player subsystem.
 *
 * Binds [PlayerEngine] -> [Media3PlayerEngine] and
 * [PlayerController] -> [DefaultPlayerController].
 *
 * Per Section 5 ("PLAYER ABSTRACTION") of the master spec: the engine
 * could be swapped for LibVLC or native without touching the UI.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {

    @Binds
    @Singleton
    abstract fun bindPlayerEngine(impl: Media3PlayerEngine): PlayerEngine

    @Binds
    @Singleton
    abstract fun bindPlayerController(impl: DefaultPlayerController): PlayerController
}
