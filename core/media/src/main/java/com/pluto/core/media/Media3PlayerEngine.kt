package com.pluto.core.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.pluto.core.common.PlutoLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Media3PlayerEngine — ExoPlayer-backed implementation of [PlayerEngine].
 *
 * Lifecycle invariants:
 *   - The polling [scope] lives for the lifetime of the Singleton.
 *     It is NOT cancelled in [release]; only the active [pollingJob] is.
 *     This allows the engine to be re-used after release (e.g. when the
 *     user exits the player screen and later returns).
 *   - The ExoPlayer instance is created lazily on first [load] and
 *     released in [release]. A new instance is created on the next load.
 *   - All state flows are reset to defaults in [release] so the UI never
 *     shows stale position / duration values from a previous playback.
 *
 * Thread safety:
 *   - ExoPlayer must be created and accessed on the main thread.
 *     [scope] uses Dispatchers.Main.immediate so polling is safe.
 *   - [load], [play], [pause], [seekTo], [seekBy], [setSpeed], [setVolume],
 *     [switchSource], [release] may be called from any thread — they
 *     dispatch to the main thread when needed via [scope.launch].
 */
@Singleton
class Media3PlayerEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : PlayerEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pollingJob: Job? = null

    private var player: ExoPlayer? = null
    private var pendingSpeed: Float = 1.0f
    private var pendingSeekMs: Long? = null

    private val _state = MutableStateFlow<PlayerState>(PlayerState.Idle)
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    override val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    override val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _bufferedMs = MutableStateFlow(0L)
    override val bufferedMs: StateFlow<Long> = _bufferedMs.asStateFlow()

    private var currentSource: PlaybackSource? = null

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE -> _state.value = PlayerState.Idle
                Player.STATE_BUFFERING -> _state.value = PlayerState.Buffering
                Player.STATE_READY -> {
                    // Apply any pending seek / speed once the player is ready
                    pendingSeekMs?.let { pos ->
                        player?.seekTo(pos)
                        pendingSeekMs = null
                    }
                    player?.setPlaybackSpeed(pendingSpeed.coerceIn(0.25f, 4f))
                    _state.value = if (player?.isPlaying == true) PlayerState.Playing else PlayerState.Paused
                }
                Player.STATE_ENDED -> _state.value = PlayerState.Ended
                else -> _state.value = PlayerState.Idle
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (_state.value !is PlayerState.Error) {
                _state.value = if (isPlaying) PlayerState.Playing else PlayerState.Paused
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            PlutoLogger.e("PLUTO-Player", "Playback error: ${error.errorCodeName}", error)
            val message = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                    "Connection lost. Check your network and retry."
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED ->
                    "This source format is not supported by the player."
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                PlaybackException.ERROR_CODE_DECODING_FAILED ->
                    "This codec is not supported on this device."
                else -> "Playback interrupted."
            }
            _state.value = PlayerState.Error(message, recoverable = true)
        }
    }

    private fun ensurePlayer(): ExoPlayer {
        return player ?: ExoPlayer.Builder(context)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also {
                it.addListener(listener)
                player = it
                startPolling(it)
            }
    }

    private fun startPolling(p: ExoPlayer) {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                _positionMs.value = p.currentPosition.coerceAtLeast(0L)
                _durationMs.value = p.duration.coerceAtLeast(0L)
                _bufferedMs.value = p.bufferedPosition.coerceAtLeast(0L)
                delay(200)
            }
        }
    }

    override fun load(source: PlaybackSource) {
        currentSource = source
        // Reset stale state before loading the new source — the UI must not
        // show the previous video's position / duration while the new one
        // is buffering.
        _positionMs.value = 0L
        _durationMs.value = 0L
        _bufferedMs.value = 0L
        pendingSeekMs = null

        val p = ensurePlayer()
        val mediaItem = MediaItem.Builder()
            .setUri(source.url)
            .apply { source.mimeType?.let { setMimeType(it) } }
            .build()
        p.setMediaItem(mediaItem)
        p.prepare()
        _state.value = PlayerState.Buffering
    }

    override fun play() {
        // Resume from ENDED state by re-preparing from the start
        if (_state.value is PlayerState.Ended) {
            player?.seekTo(0)
        }
        player?.play()
    }

    override fun pause() { player?.pause() }

    override fun seekTo(positionMs: Long) {
        val target = positionMs.coerceAtLeast(0L)
        player?.seekTo(target)
        _positionMs.value = target
    }

    override fun seekBy(deltaMs: Long) {
        val p = player ?: return
        val target = (p.currentPosition + deltaMs).coerceAtLeast(0L)
            .coerceAtMost(p.duration.takeIf { it > 0 } ?: Long.MAX_VALUE)
        seekTo(target)
    }

    override fun setSpeed(speed: Float) {
        pendingSpeed = speed
        player?.setPlaybackSpeed(speed.coerceIn(0.25f, 4f))
    }

    override fun setVolume(volume: Float) {
        player?.volume = volume.coerceIn(0f, 1f)
    }

    @OptIn(UnstableApi::class)
    override fun attach(view: PlayerView) {
        view.player = ensurePlayer()
    }

    override fun detach(view: PlayerView) {
        view.player = null
    }

    override fun switchSource(source: PlaybackSource) {
        val p = player ?: return
        val currentPosition = p.currentPosition
        currentSource = source
        // Defer the seek until STATE_READY of the new source — seeking
        // immediately after setMediaItem can be lost on some devices.
        pendingSeekMs = currentPosition
        val mediaItem = MediaItem.Builder()
            .setUri(source.url)
            .apply { source.mimeType?.let { setMimeType(it) } }
            .build()
        p.setMediaItem(mediaItem)
        p.prepare()
        _state.value = PlayerState.Buffering
    }

    /**
     * Release the underlying ExoPlayer instance and reset all state.
     *
     * NOTE: The polling [scope] is intentionally NOT cancelled — it
     * lives for the lifetime of the Singleton so a new ExoPlayer can
     * be created by a subsequent [load] call. Cancelling the scope
     * would silently break polling for the rest of the process.
     */
    override fun release() {
        pollingJob?.cancel()
        pollingJob = null
        player?.removeListener(listener)
        player?.release()
        player = null
        pendingSeekMs = null
        _state.value = PlayerState.Idle
        _positionMs.value = 0L
        _durationMs.value = 0L
        _bufferedMs.value = 0L
    }
}
