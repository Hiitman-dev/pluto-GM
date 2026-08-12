package com.pluto.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.pluto.app.BuildConfig
import com.pluto.core.common.PlutoLogger
import com.pluto.core.notifications.EpisodeSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * PlutoApplication — app-level Hilt entry point.
 *
 * - Initializes Hilt
 * - Configures WorkManager with HiltWorkerFactory (for [EpisodeSyncWorker])
 * - Schedules periodic new-episode sync
 * - Sets log level based on build type (debug = VERBOSE, release = MINIMAL)
 */
@HiltAndroidApp
class PlutoApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(
                if (BuildConfig.DEBUG) android.util.Log.DEBUG
                else android.util.Log.WARN
            )
            .build()

    override fun onCreate() {
        super.onCreate()

        PlutoLogger.level = if (BuildConfig.DEBUG)
            com.pluto.core.common.LogLevel.VERBOSE
        else
            com.pluto.core.common.LogLevel.MINIMAL

        // Schedule periodic episode sync (every 6 hours)
        EpisodeSyncScheduler.schedule(this)
    }
}
