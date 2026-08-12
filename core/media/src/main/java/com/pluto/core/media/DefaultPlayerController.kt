package com.pluto.core.media

import com.pluto.core.common.DispatcherProvider
import com.pluto.core.model.VideoPlayerSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DefaultPlayerController — concrete implementation of [PlayerController].
 *
 * Coordinates [PlayerEngine] with gesture state, lock state, and source
 * fallback. Used by the Player screen in feature:player.
 *
 * Per Section 40 ("PLAYER LOCK"): lock disables accidental gestures.
 * Per Section 42 ("PLAYER SOURCE FALLBACK"): if a source fails, try the
 * next legitimate source (don't invent URLs).
 */
@Singleton
class DefaultPlayerController @Inject constructor(
    override val engine: PlayerEngine,
    private val dispatchers: DispatcherProvider
) : PlayerController {

    private val _isLocked = MutableStateFlow(false)
    override val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _currentSpeed = MutableStateFlow(1.0f)
    override val currentSpeed: StateFlow<Float> = _currentSpeed.asStateFlow()

    private val _currentSource = MutableStateFlow<PlaybackSource?>(null)
    override val currentSource: StateFlow<PlaybackSource?> = _currentSource.asStateFlow()

    private val _availableSources = MutableStateFlow<List<PlaybackSource>>(emptyList())
    override val availableSources: StateFlow<List<PlaybackSource>> = _availableSources.asStateFlow()

    private var settings: VideoPlayerSettings = VideoPlayerSettings()
    private var failedSources: MutableSet<String> = mutableSetOf()

    override fun initialize(sources: List<PlaybackSource>, startPositionMs: Long) {
        _availableSources.value = sources
        failedSources.clear()
        val first = sources.firstOrNull() ?: return
        loadSource(first, startPositionMs)
    }

    private fun loadSource(source: PlaybackSource, startPositionMs: Long = 0L) {
        _currentSource.value = source
        engine.load(source)
        if (startPositionMs > 0) {
            engine.seekTo(startPositionMs)
        }
        engine.setSpeed(_currentSpeed.value)
    }

    override fun togglePlayPause() {
        when (engine.state.value) {
            PlayerState.Playing -> engine.pause()
            PlayerState.Paused, PlayerState.Ready -> engine.play()
            is PlayerState.Error -> retryOrFallback()
            else -> engine.play()
        }
    }

    override fun seekBy(deltaMs: Long) {
        if (_isLocked.value) return
        engine.seekBy(deltaMs)
    }

    override fun seekTo(positionMs: Long) {
        if (_isLocked.value) return
        engine.seekTo(positionMs)
    }

    override fun setSpeed(speed: Float) {
        _currentSpeed.value = speed
        engine.setSpeed(speed)
    }

    override fun setVolume(volume: Float) {
        engine.setVolume(volume)
    }

    override fun switchSource(source: PlaybackSource) {
        loadSource(source, engine.positionMs.value)
    }

    override fun lock() { _isLocked.value = true }
    override fun unlock() { _isLocked.value = false }

    override fun release() {
        engine.release()
        _currentSource.value = null
        _availableSources.value = emptyList()
        failedSources.clear()
        _isLocked.value = false
    }

    /**
     * Retry the current source, or fall back to the next available one
     * if the current source has failed repeatedly.
     *
     * Per Section 42: "If CCloud provides multiple legitimate sources,
     * attempt appropriate fallback. Do not invent fallback URLs."
     */
    fun retryOrFallback() {
        val current = _currentSource.value ?: return
        failedSources.add(current.url)

        val candidates = _availableSources.value.filter { it.url !in failedSources }
        if (candidates.isEmpty()) {
            // All sources exhausted — let the UI show terminal error
            return
        }
        loadSource(candidates.first(), engine.positionMs.value)
    }
}
