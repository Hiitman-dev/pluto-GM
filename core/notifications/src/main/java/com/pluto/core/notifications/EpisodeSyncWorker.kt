package com.pluto.core.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pluto.core.common.PlutoLogger
import com.pluto.core.data.SettingsRepository
import com.pluto.core.database.dao.FollowedSeriesDao
import com.pluto.core.model.NewEpisodeNotification
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * EpisodeSyncWorker — periodic WorkManager job that checks followed
 * series for new episodes.
 *
 * Implements Section 54 ("BACKGROUND SYNC") of the master spec:
 *   - Use WorkManager. Do NOT use a permanent background service.
 *   - Constraints: network connected, battery conscious, retry on temp failure.
 *   - Do not corrupt state on API failure.
 *
 * Per Section 119 ("BATTERY"): avoid excessive WorkManager frequency.
 * Default schedule: every 6 hours, only when network is connected and
 * battery is not low.
 */
@HiltWorker
class EpisodeSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val notificationRepository: NotificationRepository,
    private val followedDao: FollowedSeriesDao,
    private val settingsRepository: SettingsRepository,
    private val poster: PlutoNotificationPoster
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val settings = settingsRepository.observeNotificationSettings().first()
            if (!settings.newEpisodesEnabled) {
                PlutoLogger.d("PLUTO-Sync", "Notifications disabled, skipping sync")
                return Result.success()
            }

            val followed = followedDao.getAllActive()
            if (followed.isEmpty()) {
                PlutoLogger.d("PLUTO-Sync", "No followed series, skipping sync")
                return Result.success()
            }

            var totalNew = 0
            for (series in followed) {
                try {
                    val newEps = notificationRepository.syncSeries(series)
                    totalNew += newEps.size
                    for (ep in newEps) {
                        poster.postNewEpisode(
                            NewEpisodeNotification(
                                id = "${ep.seriesId}-${ep.seasonNumber}-${ep.episodeNumber}",
                                seriesId = ep.seriesId,
                                seriesTitle = ep.seriesTitle,
                                seasonNumber = ep.seasonNumber,
                                episodeNumber = ep.episodeNumber
                            ),
                            settings
                        )
                    }
                } catch (e: Exception) {
                    PlutoLogger.w("PLUTO-Sync", "Failed to sync series ${series.seriesId}: ${e.message}")
                    // Continue to next series — don't fail the whole job
                }
            }

            PlutoLogger.i("PLUTO-Sync", "Sync complete: $totalNew new episodes across ${followed.size} series")
            Result.success()
        } catch (e: Exception) {
            PlutoLogger.e("PLUTO-Sync", "Sync failed: ${e.message}", e)
            // Retry on temp failure (network/timeout) — WorkManager backs off automatically
            Result.retry()
        }
    }
}

/**
 * EpisodeSyncScheduler — schedules the periodic sync work.
 *
 * Default: every 6 hours, network required, battery not low.
 */
object EpisodeSyncScheduler {
    const val WORK_NAME = "pluto_episode_sync"

    fun schedule(context: Context, intervalHours: Long = 6) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<EpisodeSyncWorker>(
            intervalHours, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
