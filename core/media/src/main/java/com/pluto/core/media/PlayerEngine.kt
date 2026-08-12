package com.pluto.core.media

import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.pluto.core.model.Source
import kotlinx.coroutines.flow.StateFlow

/**
 * PlayerEngine — abstraction over the underlying media playback engine.
 *
 * Implements Section 5 ("PLAYER ABSTRACTION") of the master spec:
 *   "Do not couple the entire UI to ExoPlayer."
 *
 * The UI talks to [PlayerController], which delegates to a PlayerEngine.
 * The engine could be Media3, LibVLC, native, or another implementation.
 *
 * Currently implemented: [Media3PlayerEngine] (default).
 * Future engines can be added without touching the UI.
 */
interface PlayerEngine {
    val state: StateFlow<PlayerState>
    val positionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>
    val bufferedMs: StateFlow<Long>

    /** Load and prepare a source. Does not start playback. */
    fun load(source: PlaybackSource)

    /** Start playback from current position. */
    fun play()

    /** Pause playback. */
    fun pause()

    /** Seek to absolute position in milliseconds. */
    fun seekTo(positionMs: Long)

    /** Seek by a delta (positive = forward, negative = rewind). */
    fun seekBy(deltaMs: Long)

    /** Set playback speed (0.5 to 3.0). */
    fun setSpeed(speed: Float)

    /** Set volume (0.0 to 1.0). */
    fun setVolume(volume: Float)

    /**
     * Attach this engine to a [PlayerView] so its surface renders the
     * engine's underlying player. Safe to call repeatedly — the engine
     * creates its underlying player lazily on first attach if needed.
     */
    @androidx.annotation.OptIn(UnstableApi::class)
    fun attach(view: PlayerView)

    /** Detach the engine from a [PlayerView]. */
    fun detach(view: PlayerView)

    /** Release all resources. Call when the player is destroyed. */
    fun release()

    /** Select a different source for the current content (quality switch). */
    fun switchSource(source: PlaybackSource)
}

/**
 * PlaybackSource — a single playable URL with metadata.
 *
 * Wraps [Source] with normalized quality info so the player UI can show
 * "1080p" rather than the raw API string ("1080", "HD 1080", etc.).
 */
data class PlaybackSource(
    val url: String,
    val qualityLabel: String,
    val height: Int,
    val mimeType: String? = null,
    val originalSource: Source? = null
)

/**
 * PlayerState — high-level player state machine.
 */
sealed class PlayerState {
    data object Idle : PlayerState()
    data object Buffering : PlayerState()
    data object Ready : PlayerState()
    data object Playing : PlayerState()
    data object Paused : PlayerState()
    data object Ended : PlayerState()
    data class Error(val message: String, val recoverable: Boolean = true) : PlayerState()
}

/**
 * PlayerController — the API the UI consumes.
 *
 * Wraps a [PlayerEngine] with:
 *   - gesture integration (double-tap seek, long-press speed)
 *   - lock state (disables gestures)
 *   - resume position tracking
 *   - source fallback (try next source if current fails)
 */
interface PlayerController {
    val engine: PlayerEngine
    val isLocked: StateFlow<Boolean>
    val currentSpeed: StateFlow<Float>
    val currentSource: StateFlow<PlaybackSource?>
    val availableSources: StateFlow<List<PlaybackSource>>

    fun initialize(sources: List<PlaybackSource>, startPositionMs: Long = 0L)
    fun togglePlayPause()
    fun seekBy(deltaMs: Long)
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun setVolume(volume: Float)
    fun switchSource(source: PlaybackSource)
    fun lock()
    fun unlock()

    /**
     * Retry the current source, or fall back to the next available source
     * if the current one has failed. Returns true if a fallback source
     * was loaded; false if all sources have been exhausted (UI should
     * show terminal error).
     *
     * Per Section 42 ("PLAYER SOURCE FALLBACK"): "If multiple legitimate
     * sources are provided, attempt appropriate fallback. Do not invent
     * fallback URLs."
     */
    fun retryOrFallback(): Boolean

    fun release()
}

/**
 * PlaybackTrack — selectable audio / subtitle track.
 *
 * Per Section 44 ("PLAYER SUBTITLE") + Section 45 ("PLAYER AUDIO"):
 * support track selection where the engine exposes it.
 *
 * NOTE: CCloud API does NOT return subtitle URLs. Subtitles only appear
 * if the video container itself contains them (e.g. MKV with embedded
 * subs). The Media3 engine exposes them via this model.
 */
data class PlaybackTrack(
    val id: String,
    val language: String,
    val label: String,
    val kind: TrackKind
)

enum class TrackKind { Audio, Subtitle, Video }

/**
 * VideoQuality — normalized quality tier presented in the player UI.
 *
 * Per Section 43 ("PLAYER QUALITY"): "Show only actual available
 * qualities. Do not invent unavailable options."
 */
data class VideoQuality(
    val height: Int,
    val label: String,
    val source: PlaybackSource
)
