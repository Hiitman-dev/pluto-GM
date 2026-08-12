package com.pluto.core.notifications

import com.pluto.core.common.DispatcherProvider
import com.pluto.core.database.dao.FollowedSeriesDao
import com.pluto.core.database.dao.NewEpisodeNotificationDao
import com.pluto.core.database.entity.FollowedSeriesEntity
import com.pluto.core.database.entity.NewEpisodeNotificationEntity
import com.pluto.core.model.NewEpisodeNotification
import com.pluto.core.network.SeasonsRepository
import com.pluto.core.network.SeriesNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NotificationRepository — follows series, detects new episodes.
 *
 * Implements Sections 49-55 of the master spec:
 *   - Users follow series (Notify Me button)
 *   - WorkManager periodically checks for new episodes
 *   - Initial sync does NOT notify for existing episodes (Section 50)
 *   - Deduplication via composite key "${seriesId}-${season}-${episode}" (Section 51)
 *   - One notification per episode (even if multiple qualities exist)
 *
 * Per Section 55 ("FUTURE FCM"): the repository is the seam where a
 * future backend + FCM could replace the polling logic without
 * changing the UI.
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val followedDao: FollowedSeriesDao,
    private val newEpisodeDao: NewEpisodeNotificationDao,
    private val seasonsRepository: SeasonsRepository,
    private val dispatchers: DispatcherProvider
) {
    fun observeFollowed(): Flow<List<FollowedSeriesEntity>> = followedDao.observeAll()
    fun observeIsFollowing(seriesId: Int): Flow<Boolean> = followedDao.observeIsFollowing(seriesId)
    fun observeUnreadNotifications(): Flow<List<NewEpisodeNotificationEntity>> = newEpisodeDao.observeUnread()
    fun observeUnreadCount(): Flow<Int> = newEpisodeDao.observeUnreadCount()
    fun observeRecentNotifications(limit: Int = 50): Flow<List<NewEpisodeNotificationEntity>> =
        newEpisodeDao.observeRecent(limit)

    suspend fun follow(
        seriesId: Int,
        title: String,
        poster: String,
        lastKnownSeason: Int = 0,
        lastKnownEpisode: Int = 0
    ) = withContext(dispatchers.io) {
        followedDao.upsert(
            FollowedSeriesEntity(
                seriesId = seriesId,
                title = title,
                poster = poster,
                lastKnownSeason = lastKnownSeason,
                lastKnownEpisode = lastKnownEpisode,
                followedAt = System.currentTimeMillis(),
                notificationsEnabled = true
            )
        )
    }

    suspend fun unfollow(seriesId: Int) = withContext(dispatchers.io) {
        followedDao.remove(seriesId)
    }

    suspend fun setNotificationsEnabled(seriesId: Int, enabled: Boolean) = withContext(dispatchers.io) {
        followedDao.setNotificationsEnabled(seriesId, enabled)
    }

    /**
     * Sync a single followed series against the current API state.
     *
     * Per Section 50 ("NEW EPISODE DETECTION"):
     *   - First sync: do NOT notify for existing episodes. We update
     *     lastKnownSeason/Episode so the baseline is set.
     *   - Subsequent syncs: compare known state against current API state.
     *     If new episodes appear, insert notifications (deduplicated by
     *     composite key).
     *
     * Returns the list of NEW notifications created (empty if none).
     */
    suspend fun syncSeries(followed: FollowedSeriesEntity): List<NewEpisodeNotification> =
        withContext(dispatchers.io) {
            val seasons = runCatching { seasonsRepository.getSeasons(followed.seriesId) }
                .getOrNull() ?: return@withContext emptyList()

            val newNotifications = mutableListOf<NewEpisodeNotification>()
            for (season in seasons) {
                for (episode in season.episodes) {
                    val isNewEpisode = when {
                        // First sync — don't notify, just record baseline
                        followed.lastKnownSeason == 0 && followed.lastKnownEpisode == 0 -> false
                        // Same season, new episode number
                        season.seasonNumber == followed.lastKnownSeason &&
                            episode.episodeNumber > followed.lastKnownEpisode -> true
                        // New season entirely (any episode count)
                        season.seasonNumber > followed.lastKnownSeason -> true
                        else -> false
                    }

                    if (isNewEpisode) {
                        val key = "${followed.seriesId}-${season.seasonNumber}-${episode.episodeNumber}"
                        val entity = NewEpisodeNotificationEntity(
                            deduplicationKey = key,
                            seriesId = followed.seriesId,
                            seriesTitle = followed.title,
                            seasonNumber = season.seasonNumber,
                            episodeNumber = episode.episodeNumber,
                            detectedAt = System.currentTimeMillis(),
                            shownAt = null,
                            openedAt = null
                        )
                        val rowId = newEpisodeDao.insertIfNew(entity)
                        if (rowId > 0) {
                            newNotifications.add(
                                NewEpisodeNotification(
                                    id = key,
                                    seriesId = followed.seriesId,
                                    seriesTitle = followed.title,
                                    seasonNumber = season.seasonNumber,
                                    episodeNumber = episode.episodeNumber
                                )
                            )
                        }
                    }
                }
            }

            // Update last-known baseline to the latest season + episode
            val latestSeason = seasons.maxOfOrNull { it.seasonNumber } ?: 0
            val latestEpisode = seasons
                .filter { it.seasonNumber == latestSeason }
                .flatMap { it.episodes }
                .maxOfOrNull { it.episodeNumber } ?: 0
            if (latestSeason > 0) {
                followedDao.updateLastKnown(followed.seriesId, latestSeason, latestEpisode)
            }

            newNotifications
        }

    suspend fun markOpened(deduplicationKey: String) = withContext(dispatchers.io) {
        newEpisodeDao.markOpened(deduplicationKey, System.currentTimeMillis())
    }

    suspend fun purgeOlderThan(olderThan: Long) = withContext(dispatchers.io) {
        newEpisodeDao.purgeOlderThan(olderThan)
    }

    suspend fun clearAllNotifications() = withContext(dispatchers.io) {
        newEpisodeDao.clearAll()
    }
}
