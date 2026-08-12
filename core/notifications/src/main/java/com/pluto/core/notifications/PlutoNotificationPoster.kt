package com.pluto.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pluto.core.common.PlutoLogger
import com.pluto.core.model.NewEpisodeNotification
import com.pluto.core.model.NotificationSettings
import com.pluto.core.model.PlutoDeepLinks
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PlutoNotificationPoster — renders new-episode notifications to the system.
 *
 * Implements Section 52 ("NOTIFICATION CONTENT") of the master spec:
 *
 *   Example:
 *     PLUTO
 *     New episode available
 *     The Last of Us
 *     Season 2 · Episode 5
 *     Tap to watch
 *
 * Per Section 53 ("NOTIFICATION DEEP LINK"): tapping the notification
 * deep-links into PLUTO at the correct series/season/episode.
 */
@Singleton
class PlutoNotificationPoster @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manager = NotificationManagerCompat.from(context)

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "New Episodes",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when followed series release new episodes."
                enableVibration(true)
                enableLights(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun postNewEpisode(
        notification: NewEpisodeNotification,
        settings: NotificationSettings
    ) {
        if (!settings.newEpisodesEnabled) return

        ensureChannel()
        val deepLink = PlutoDeepLinks.episode(
            notification.seriesId,
            notification.seasonNumber,
            notification.episodeNumber
        )

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
            setPackage(context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("PLUTO")
            .setContentText("New episode available")
            .setSubText("Season ${notification.seasonNumber} · Episode ${notification.episodeNumber}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("PLUTO")
                    .bigText("${notification.seriesTitle}\nSeason ${notification.seasonNumber} · Episode ${notification.episodeNumber}\nTap to watch")
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(if (settings.grouping) GROUP_KEY else null)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (!settings.sound) builder.setSilent(true)
        if (!settings.vibration) builder.setVibrate(longArrayOf(0L))

        try {
            manager.notify(notification.id.hashCode(), builder.build())
        } catch (se: SecurityException) {
            PlutoLogger.w("PLUTO-Notif", "POST_NOTIFICATIONS not granted: ${se.message}")
        }
    }

    companion object {
        const val CHANNEL_ID = "pluto_new_episodes"
        const val GROUP_KEY = "pluto_new_episodes_group"
    }
}
