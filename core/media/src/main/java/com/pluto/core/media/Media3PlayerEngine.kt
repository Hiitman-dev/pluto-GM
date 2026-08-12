package com.pluto.core.media

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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
 * Per Section 4 ("TECHNOLOGY STACK") of the master spec:
 *   "Preferred starting point: Kotlin + Compose + Media3. But Media3 is
 *    NOT an absolute restriction."
 *
 * Media3 is chosen because:
 *   1. Best-in-class format support (MP4, MKV, HLS, DASH)
 *   2. First-party AndroidX library — guaranteed long-term support
 *   3. Native PiP, subtitle, audio-track APIs
 *   4. No NDK required
 *
 * LibVLC would be added as a separate Engine if broader format support
 * is needed in the future (the [PlayerEngine] abstraction makes this
 * a drop-in replacement).
 */
@Singleton
class Media3PlayerEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : PlayerEngine {

    private var player: ExoPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var pollingJob: Job? = null

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
            _state.value = when (playbackState) {
                Player.STATE_IDLE -> PlayerState.Idle
                Player.STATE_BUFFERING -> PlayerState.Buffering
                Player.STATE_READY -> if (player?.isPlaying == true) PlayerState.Playing else PlayerState.Paused
                Player.STATE_ENDED -> PlayerState.Ended
                else -> PlayerState.Idle
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
        val p = ensurePlayer()
        val mediaItem = MediaItem.Builder()
            .setUri(source.url)
            .apply { source.mimeType?.let { setMimeType(it) } }
            .build()
        p.setMediaItem(mediaItem)
        p.prepare()
        _state.value = PlayerState.Buffering
    }

    override fun play() { player?.play() }
    override fun pause() { player?.pause() }

    override fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs.coerceAtLeast(0L))
        _positionMs.value = positionMs
    }

    override fun seekBy(deltaMs: Long) {
        val p = player ?: return
        val target = (p.currentPosition + deltaMs).coerceAtLeast(0L)
            .coerceAtMost(p.duration.takeIf { it > 0 } ?: Long.MAX_VALUE)
        seekTo(target)
    }

    override fun setSpeed(speed: Float) {
        player?.setPlaybackSpeed(speed.coerceIn(0.25f, 4f))
    }

    override fun setVolume(volume: Float) {
        player?.volume = volume.coerceIn(0f, 1f)
    }

    override fun switchSource(source: PlaybackSource) {
        val p = player ?: return
        val currentPosition = p.currentPosition
        currentSource = source
        val mediaItem = MediaItem.Builder()
            .setUri(source.url)
            .apply { source.mimeType?.let { setMimeType(it) } }
            .build()
        p.setMediaItem(mediaItem)
        p.prepare()
        p.seekTo(currentPosition)
        p.play()
    }

    override fun release() {
        pollingJob?.cancel()
        pollingJob = null
        player?.removeListener(listener)
        player?.release()
        player = null
        scope.cancel()
        _state.value = PlayerState.Idle
        _positionMs.value = 0L
        _durationMs.value = 0L
        _bufferedMs.value = 0L
    }
}
