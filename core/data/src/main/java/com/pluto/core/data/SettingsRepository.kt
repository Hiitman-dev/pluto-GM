package com.pluto.core.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pluto.core.common.DispatcherProvider
import com.pluto.core.model.DownloadSettings
import com.pluto.core.model.NotificationSettings
import com.pluto.core.model.SubtitleSettings
import com.pluto.core.model.VideoPlayerSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "pluto_settings")

/**
 * SettingsRepository — DataStore-backed preferences.
 *
 * Mirrors CCloud's `data/repository/SettingsRepository.kt` (which used
 * JSON files in filesDir). PLUTO uses DataStore per Section 4 of the
 * master spec ("DataStore where preferences are appropriate").
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider
) {
    private object Keys {
        // Video player
        val SEEK_SECONDS = intPreferencesKey("seek_seconds")
        val DEFAULT_SPEED = floatPreferencesKey("default_speed")
        val DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        val AUTOPLAY_NEXT = booleanPreferencesKey("autoplay_next")

        // Subtitles
        val SUBTITLE_TEXT_COLOR = longPreferencesKey("subtitle_text_color")
        val SUBTITLE_BORDER_COLOR = longPreferencesKey("subtitle_border_color")
        val SUBTITLE_SIZE = floatPreferencesKey("subtitle_size")

        // Downloads
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val SIMULTANEOUS = intPreferencesKey("simultaneous_downloads")
        val DOWNLOAD_QUALITY = stringPreferencesKey("download_quality")
        val AUTO_RETRY = booleanPreferencesKey("auto_retry")

        // Notifications
        val NOTIF_NEW_EPISODES = booleanPreferencesKey("notif_new_episodes")
        val NOTIF_SOUND = booleanPreferencesKey("notif_sound")
        val NOTIF_VIBRATION = booleanPreferencesKey("notif_vibration")
        val NOTIF_GROUPING = booleanPreferencesKey("notif_grouping")

        // Welcome
        val WELCOME_COMPLETED = booleanPreferencesKey("welcome_completed")
    }

    fun observeVideoPlayerSettings(): Flow<VideoPlayerSettings> = context.dataStore.data.map { p ->
        VideoPlayerSettings(
            seekSeconds = p[Keys.SEEK_SECONDS] ?: 10,
            defaultSpeed = p[Keys.DEFAULT_SPEED] ?: 1.0f,
            defaultQuality = p[Keys.DEFAULT_QUALITY] ?: "auto",
            autoPlayNext = p[Keys.AUTOPLAY_NEXT] ?: true
        )
    }

    suspend fun saveVideoPlayerSettings(settings: VideoPlayerSettings) = withContext(dispatchers.io) {
        context.dataStore.edit { p ->
            p[Keys.SEEK_SECONDS] = settings.seekSeconds
            p[Keys.DEFAULT_SPEED] = settings.defaultSpeed
            p[Keys.DEFAULT_QUALITY] = settings.defaultQuality
            p[Keys.AUTOPLAY_NEXT] = settings.autoPlayNext
        }
    }

    fun observeSubtitleSettings(): Flow<SubtitleSettings> = context.dataStore.data.map { p ->
        SubtitleSettings(
            textColor = p[Keys.SUBTITLE_TEXT_COLOR] ?: 0xFFFFFF00L,
            borderColor = p[Keys.SUBTITLE_BORDER_COLOR] ?: 0x80000000L,
            textSize = p[Keys.SUBTITLE_SIZE] ?: 17f
        )
    }

    suspend fun saveSubtitleSettings(settings: SubtitleSettings) = withContext(dispatchers.io) {
        context.dataStore.edit { p ->
            p[Keys.SUBTITLE_TEXT_COLOR] = settings.textColor
            p[Keys.SUBTITLE_BORDER_COLOR] = settings.borderColor
            p[Keys.SUBTITLE_SIZE] = settings.textSize
        }
    }

    fun observeDownloadSettings(): Flow<DownloadSettings> = context.dataStore.data.map { p ->
        DownloadSettings(
            wifiOnly = p[Keys.WIFI_ONLY] ?: true,
            simultaneousDownloads = p[Keys.SIMULTANEOUS] ?: 2,
            defaultQuality = p[Keys.DOWNLOAD_QUALITY] ?: "720p",
            autoRetry = p[Keys.AUTO_RETRY] ?: true
        )
    }

    suspend fun saveDownloadSettings(settings: DownloadSettings) = withContext(dispatchers.io) {
        context.dataStore.edit { p ->
            p[Keys.WIFI_ONLY] = settings.wifiOnly
            p[Keys.SIMULTANEOUS] = settings.simultaneousDownloads
            p[Keys.DOWNLOAD_QUALITY] = settings.defaultQuality
            p[Keys.AUTO_RETRY] = settings.autoRetry
        }
    }

    fun observeNotificationSettings(): Flow<NotificationSettings> = context.dataStore.data.map { p ->
        NotificationSettings(
            newEpisodesEnabled = p[Keys.NOTIF_NEW_EPISODES] ?: true,
            sound = p[Keys.NOTIF_SOUND] ?: true,
            vibration = p[Keys.NOTIF_VIBRATION] ?: true,
            grouping = p[Keys.NOTIF_GROUPING] ?: true
        )
    }

    suspend fun saveNotificationSettings(settings: NotificationSettings) = withContext(dispatchers.io) {
        context.dataStore.edit { p ->
            p[Keys.NOTIF_NEW_EPISODES] = settings.newEpisodesEnabled
            p[Keys.NOTIF_SOUND] = settings.sound
            p[Keys.NOTIF_VIBRATION] = settings.vibration
            p[Keys.NOTIF_GROUPING] = settings.grouping
        }
    }

    fun observeWelcomeCompleted(): Flow<Boolean> = context.dataStore.data.map { p ->
        p[Keys.WELCOME_COMPLETED] ?: false
    }

    suspend fun markWelcomeCompleted() = withContext(dispatchers.io) {
        context.dataStore.edit { it[Keys.WELCOME_COMPLETED] = true }
    }
}
