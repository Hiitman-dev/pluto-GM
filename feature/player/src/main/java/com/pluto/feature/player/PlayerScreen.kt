@file:OptIn(ExperimentalLayoutApi::class)

package com.pluto.feature.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.pluto.app.ui.PipController
import com.pluto.core.common.Result
import com.pluto.core.data.ContentRepository
import com.pluto.core.data.HistoryRepository
import com.pluto.core.data.Mappers
import com.pluto.core.designsystem.PLUTOButton
import com.pluto.core.designsystem.PLUTOColors
import com.pluto.core.designsystem.PLUTOIconCircle
import com.pluto.core.designsystem.PLUTOOutlinedButton
import com.pluto.core.designsystem.PLUTOShapes
import com.pluto.core.designsystem.PLUTOTypography
import com.pluto.core.designsystem.PlutoIcons
import com.pluto.core.designsystem.PlutoSpinner
import com.pluto.core.media.PlaybackSource
import com.pluto.core.media.PlaybackSourceBuilder
import com.pluto.core.media.PlayerController
import com.pluto.core.media.PlayerState
import com.pluto.core.model.Episode
import com.pluto.core.model.FilterType
import com.pluto.core.model.Movie
import com.pluto.core.model.PlaybackProgress
import com.pluto.core.model.Season
import com.pluto.core.model.Series
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PlayerViewModel — drives [PlayerScreen].
 *
 * Bridges the UI to the singleton [PlayerController] (which wraps the
 * Media3 engine) plus the content/history repositories for resolving
 * the playable sources and persisting playback progress.
 *
 * Per spec:
 *   - Section 5 ("PLAYER ABSTRACTION") — UI never touches ExoPlayer directly.
 *   - Section 39 ("PLAYER GESTURE")    — toggle controls, double-tap seek,
 *     double-tap play/pause, vertical swipe for volume/brightness.
 *   - Section 40 ("PLAYER LOCK")       — lock disables all gestures.
 *   - Section 41 ("PLAYER SMART RESUME") — resume from saved position.
 *   - Section 42 ("PLAYER SOURCE FALLBACK") — switch sources on failure.
 *   - Section 43 ("PLAYER QUALITY")    — show only actual available qualities.
 *
 * Lifecycle:
 *   - [initialize] is called from the screen's `LaunchedEffect(Unit)`.
 *   - [release] is called from the screen's `DisposableEffect(Unit)` and
 *     saves final playback progress via `historyRepository.saveProgress`
 *     using `NonCancellable` so the write survives viewModelScope
 *     cancellation during dispose.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val contentRepository: ContentRepository,
    private val historyRepository: HistoryRepository,
    private val playbackSourceBuilder: PlaybackSourceBuilder
) : ViewModel() {

    // ── Delegated player state ───────────────────────────────────────────
    val state: StateFlow<PlayerState> = playerController.engine.state
    val positionMs: StateFlow<Long> = playerController.engine.positionMs
    val durationMs: StateFlow<Long> = playerController.engine.durationMs
    val isLocked: StateFlow<Boolean> = playerController.isLocked
    val currentSpeed: StateFlow<Float> = playerController.currentSpeed
    val currentSource: StateFlow<PlaybackSource?> = playerController.currentSource
    val availableSources: StateFlow<List<PlaybackSource>> = playerController.availableSources

    // ── Player-screen-local UI state ─────────────────────────────────────
    private val _controlsVisible = MutableStateFlow(true)
    val controlsVisible: StateFlow<Boolean> = _controlsVisible.asStateFlow()

    private val _brightness = MutableStateFlow(-1f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _volume = MutableStateFlow(-1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private var hideControlsJob: Job? = null

    /**
     * The coroutine launched by [initialize]. Tracked so [release] can
     * cancel it if the user exits before the fetch completes — otherwise
     * the fetch would create an orphan ExoPlayer that plays audio in
     * the background with no UI.
     */
    private var initJob: Job? = null

    // Content metadata — captured during initialize() for progress saves.
    private var contentType: String = "movie"
    private var contentId: Int = 0
    private var episodeId: Int? = null
    private var seasonId: Int? = null
    private var title: String = ""
    private var image: String = ""
    private var genresJson: String = "[]"

    @Volatile private var initialized: Boolean = false

    /**
     * Resolve content + sources for the given content type / id, then hand
     * the playable source list to [PlayerController.initialize] with the
     * provided resume position. Also marks the title as viewed.
     *
     * Per spec Section 41 ("PLAYER SMART RESUME"): if [startPositionMs] is
     * non-zero, the controller seeks there after the source buffers.
     */
    fun initialize(
        contentType: String,
        contentId: Int,
        episodeId: Int?,
        seasonNumber: Int?,
        episodeNumber: Int?,
        startPositionMs: Long
    ) {
        if (initialized) return
        initialized = true
        this.contentType = contentType
        this.contentId = contentId
        this.episodeId = episodeId

        // Track the init job so release() can cancel it. If we don't, the
        // fetch coroutine keeps running after the user exits; when it
        // completes it would call engine.load -> ensurePlayer and create
        // an orphan ExoPlayer that plays audio in the background.
        initJob = viewModelScope.launch {
            if (contentType == "movie") {
                initializeMovie(contentId, startPositionMs)
            } else {
                initializeSeries(
                    seriesId = contentId,
                    episodeId = episodeId,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    startPositionMs = startPositionMs
                )
            }
        }
    }

    private suspend fun initializeMovie(movieId: Int, startPositionMs: Long) {
        val movie: Movie? = findMovie(movieId)
        if (movie == null) {
            // Nothing else we can do — the controller has no sources to load.
            return
        }
        title = movie.title
        image = movie.image
        genresJson = Mappers.encodeGenres(movie.genres)

        val sources = playbackSourceBuilder.fromMovie(movie)
        playerController.initialize(sources, startPositionMs)

        // Mark viewed (per spec Section 47 — "Recent History").
        historyRepository.markViewed(
            contentType = "movie",
            itemId = movieId,
            title = title,
            image = image,
            genresJson = genresJson
        )
    }

    private suspend fun initializeSeries(
        seriesId: Int,
        episodeId: Int?,
        seasonNumber: Int?,
        episodeNumber: Int?,
        startPositionMs: Long
    ) {
        val series: Series? = findSeries(seriesId)
        if (series != null) {
            title = series.title
            image = series.image
            genresJson = Mappers.encodeGenres(series.genres)
        }

        val seasonsResult = contentRepository.getSeasons(seriesId)
        val seasons: List<Season> = (seasonsResult as? Result.Success)?.data ?: emptyList()

        // Resolve the episode by episodeId, or fall back to season/episode
        // numbers when the API didn't return a usable id.
        val episode: Episode? = seasons
            .flatMap { it.seasonEpisodes() }
            .firstOrNull { ep -> episodeId != null && ep.id == episodeId }
            ?: seasons
                .flatMap { it.seasonEpisodes() }
                .firstOrNull { ep ->
                    seasonNumber != null && episodeNumber != null &&
                        ep.seasonNumber == seasonNumber &&
                        ep.episodeNumber == episodeNumber
                }
            ?: seasons.firstOrNull()?.episodes?.firstOrNull()

        if (episode == null) return

        // Find the parent season for the progress record.
        val parentSeason: Season? = seasons.firstOrNull { it.episodes.contains(episode) }
        seasonId = parentSeason?.id
        this.episodeId = episode.id

        val sources = playbackSourceBuilder.fromEpisode(episode)
        playerController.initialize(sources, startPositionMs)

        // Mark viewed (per spec Section 47 — "Recent History").
        historyRepository.markViewed(
            contentType = "series",
            itemId = seriesId,
            title = title.ifBlank { series?.title ?: "Series $seriesId" },
            image = image,
            genresJson = genresJson,
            episodeId = episode.id,
            seasonId = seasonId
        )
    }

    private suspend fun findMovie(id: Int): Movie? {
        for (page in 1..MAX_PAGES) {
            when (val result = contentRepository.getMovies(page, 0, FilterType.DEFAULT)) {
                is Result.Success -> {
                    if (result.data.isEmpty()) break
                    val match = result.data.firstOrNull { it.id == id }
                    if (match != null) return match
                }
                is Result.Error -> return null
                is Result.Loading -> continue
            }
        }
        return null
    }

    private suspend fun findSeries(id: Int): Series? {
        for (page in 1..MAX_PAGES) {
            when (val result = contentRepository.getSeries(page, 0, FilterType.DEFAULT)) {
                is Result.Success -> {
                    if (result.data.isEmpty()) break
                    val match = result.data.firstOrNull { it.id == id }
                    if (match != null) return match
                }
                is Result.Error -> return null
                is Result.Loading -> continue
            }
        }
        return null
    }

    /** Local helper — flatten a season's episodes (kept verbose for clarity). */
    private fun Season.seasonEpisodes(): List<Episode> = episodes

    // ── Player controls (delegate to PlayerController) ───────────────────

    fun togglePlayPause() {
        playerController.togglePlayPause()
        // If we just started playback, kick off the auto-hide.
        if (playerController.engine.state.value is PlayerState.Playing) {
            hideControlsDelayed()
        } else {
            showControls()
        }
    }

    fun seekBy(deltaMs: Long) {
        playerController.seekBy(deltaMs)
        hideControlsDelayed()
    }

    fun seekTo(positionMs: Long) {
        playerController.seekTo(positionMs)
    }

    fun toggleLock() {
        if (playerController.isLocked.value) {
            playerController.unlock()
            showControls()
        } else {
            playerController.lock()
            hideControlsImmediate()
        }
    }

    fun setSpeed(speed: Float) {
        playerController.setSpeed(speed)
        hideControlsDelayed()
    }

    fun switchSource(source: PlaybackSource) {
        playerController.switchSource(source)
        hideControlsDelayed()
    }

    fun retryOrFallback(): Boolean = playerController.retryOrFallback()

    /**
     * Attach the underlying media engine to a [PlayerView]. The engine
     * no-ops if already attached. Called from the screen's `AndroidView`.
     */
    @OptIn(UnstableApi::class)
    fun attachToView(view: PlayerView) {
        playerController.engine.attach(view)
    }

    // ── Controls visibility (auto-hide after 3s when playing) ────────────

    fun toggleControls() {
        if (_controlsVisible.value) hideControlsImmediate() else showControls()
    }

    fun showControls() {
        _controlsVisible.value = true
        hideControlsJob?.cancel()
        if (playerController.engine.state.value is PlayerState.Playing) {
            hideControlsDelayed()
        }
    }

    fun hideControlsImmediate() {
        _controlsVisible.value = false
        hideControlsJob?.cancel()
    }

    fun hideControlsDelayed() {
        hideControlsJob?.cancel()
        hideControlsJob = viewModelScope.launch {
            delay(CONTROLS_AUTO_HIDE_MS)
            // Auto-hide only while actively playing — paused users may want controls.
            if (playerController.engine.state.value is PlayerState.Playing) {
                _controlsVisible.value = false
            }
        }
    }

    // ── Brightness / volume (UI tracks current values; VM is just a holder) ─

    fun setBrightness(value: Float) {
        _brightness.value = value.coerceIn(0f, 1f)
    }

    fun setVolume(value: Float) {
        _volume.value = value.coerceIn(0f, 1f)
        playerController.setVolume(value)
    }

    // ── Progress saves ───────────────────────────────────────────────────

    /**
     * Periodic progress save. Called every 5s from the screen's
     * `LaunchedEffect`. Cheap (a single Room upsert) so we don't bother
     * gating on playback state.
     */
    fun saveProgressNow() {
        if (!initialized) return
        val progress = currentProgressSnapshot()
        viewModelScope.launch {
            runCatching { historyRepository.saveProgress(progress) }
        }
    }

    private fun currentProgressSnapshot(): PlaybackProgress = PlaybackProgress(
        contentId = contentId,
        contentType = contentType,
        episodeId = episodeId,
        seasonId = seasonId,
        positionMs = playerController.engine.positionMs.value,
        durationMs = playerController.engine.durationMs.value,
        updatedAt = System.currentTimeMillis()
    )

    /**
     * Final cleanup — save the final playback position then release the
     * controller. Runs on `NonCancellable` so the save survives
     * `viewModelScope` cancellation during screen dispose.
     */
    fun release() {
        // Cancel the in-flight init coroutine FIRST. If it completes after
        // release(), it would call engine.load() which creates a new
        // ExoPlayer that plays audio in the background with no UI.
        initJob?.cancel()
        initJob = null

        hideControlsJob?.cancel()
        if (!initialized) {
            playerController.release()
            return
        }
        val progress = currentProgressSnapshot()
        viewModelScope.launch(NonCancellable) {
            runCatching { historyRepository.saveProgress(progress) }
            playerController.release()
        }
    }

    private companion object {
        const val MAX_PAGES = 5
        const val CONTROLS_AUTO_HIDE_MS = 3000L
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Screen
// ────────────────────────────────────────────────────────────────────────────

/**
 * PlayerScreen — full-screen video player.
 *
 * Wires the singleton [PlayerController]'s Media3 engine to a Compose
 * [PlayerView], then layers PLUTO-themed controls on top:
 *   - Tap to toggle controls (auto-hide after 3s when playing)
 *   - Double-tap left/right thirds: seek -10s / +10s
 *   - Double-tap center: toggle play/pause
 *   - Vertical swipe left half: brightness; right half: volume
 *   - Lock button: disables all gestures except unlock
 *   - Bottom bar: seek + play/pause + speed chips + quality chips + PiP
 *   - State overlays: Buffering (spinner), Error (retry / back),
 *     Ended (replay)
 *
 * Per spec Section 38 ("PLAYER UI LAYOUT") — minimal, cinematic, all
 * controls drawn by us (PlayerView.useController = false).
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    contentType: String,
    contentId: Int,
    episodeId: Int?,
    seasonNumber: Int?,
    episodeNumber: Int?,
    startPositionMs: Long,
    pipController: PipController?,
    onExit: () -> Unit,
    onOpenSeries: (Int) -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current

    val state by viewModel.state.collectAsStateWithLifecycle()
    val positionMs by viewModel.positionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    val currentSpeed by viewModel.currentSpeed.collectAsStateWithLifecycle()
    val currentSource by viewModel.currentSource.collectAsStateWithLifecycle()
    val availableSources by viewModel.availableSources.collectAsStateWithLifecycle()
    val controlsVisible by viewModel.controlsVisible.collectAsStateWithLifecycle()

    // ── First-composition initialization ────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.initialize(
            contentType = contentType,
            contentId = contentId,
            episodeId = episodeId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            startPositionMs = startPositionMs
        )
    }

    // ── Release on dispose ──────────────────────────────────────────────
    // IMPORTANT: clear the PiP-wants flag SYNCHRONOUSLY before releasing
    // the player. If we clear it inside viewModel.release() (which is
    // suspended), MainActivity.onUserLeaveHint might still see wantsPip=true
    // and enter PiP with no player visible.
    DisposableEffect(Unit) {
        onDispose {
            pipController?.setWantsPip(false)
            viewModel.release()
        }
    }

    // ── Periodic progress save (every 5s while playing) ─────────────────
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            viewModel.saveProgressNow()
        }
    }

    // ── PiP intent: wants PiP while playing; clear on Idle/Ended ────────
    LaunchedEffect(state) {
        when (state) {
            is PlayerState.Playing -> pipController?.setWantsPip(true)
            is PlayerState.Idle, is PlayerState.Ended -> pipController?.setWantsPip(false)
            else -> { /* leave as-is */ }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PLUTOColors.Void)
    ) {
        // ── ExoPlayer surface ───────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setKeepContentOnPlayerReset(true)
                    viewModel.attachToView(this)
                }
            },
            update = { pv ->
                // Re-attach on every update — the engine no-ops if already attached.
                viewModel.attachToView(pv)
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Gesture overlay (tap/double-tap + vertical drag) ────────────
        PlayerGestureOverlay(
            isLocked = isLocked,
            onToggleControls = { viewModel.toggleControls() },
            onSeekBy = { delta -> viewModel.seekBy(delta) },
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onBrightnessChange = { delta ->
                applyBrightnessDelta(view.context, delta, viewModel)
            },
            onVolumeChange = { delta ->
                applyVolumeDelta(context, delta, viewModel)
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Top controls (back + lock) ──────────────────────────────────
        AnimatedVisibility(
            visible = controlsVisible && state !is PlayerState.Error,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PLUTOIconCircle(
                    icon = PlutoIcons.Back,
                    onClick = onExit,
                    contentDescription = "Back"
                )
                PLUTOIconCircle(
                    icon = if (isLocked) PlutoIcons.Lock else PlutoIcons.Unlock,
                    onClick = { viewModel.toggleLock() },
                    contentDescription = if (isLocked) "Unlock" else "Lock"
                )
            }
        }

        // ── Bottom controls (seek bar + play/pause + speed/quality/PiP) ─
        AnimatedVisibility(
            visible = controlsVisible && state !is PlayerState.Error,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BottomControlBar(
                positionMs = positionMs,
                durationMs = durationMs,
                isPlaying = state is PlayerState.Playing,
                currentSpeed = currentSpeed,
                availableSources = availableSources,
                currentSourceUrl = currentSource?.url,
                onSeekTo = { viewModel.seekTo(it) },
                onSeekFinished = { viewModel.seekTo(it); viewModel.hideControlsDelayed() },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSpeedSelected = { viewModel.setSpeed(it) },
                onSourceSelected = { viewModel.switchSource(it) },
                onEnterPip = { pipController?.enterPipNow() },
                pipAvailable = pipController != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .systemBarsPadding()
            )
        }

        // ── Lock indicator (visible whenever locked) ────────────────────
        if (isLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(20.dp)
                    .size(40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(PLUTOColors.Glass3)
                    .clickable { viewModel.toggleLock() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PlutoIcons.Lock,
                    contentDescription = "Locked — tap to unlock",
                    tint = PLUTOColors.FrostWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ── Buffering overlay ───────────────────────────────────────────
        if (state is PlayerState.Buffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                PlutoSpinner(size = 48)
            }
        }

        // ── Error overlay ───────────────────────────────────────────────
        if (state is PlayerState.Error) {
            PlayerErrorOverlay(
                message = (state as PlayerState.Error).message,
                onSwitchSource = { viewModel.retryOrFallback() },
                onExit = onExit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── Ended overlay ───────────────────────────────────────────────
        if (state is PlayerState.Ended) {
            PlayerEndedOverlay(
                onReplay = { viewModel.togglePlayPause() },
                onExit = onExit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Gesture overlay
// ────────────────────────────────────────────────────────────────────────────

/**
 * PlayerGestureOverlay — captures single taps (toggle controls),
 * double-taps (left third = -10s, right third = +10s, center = play/pause),
 * and vertical drag (left half = brightness, right half = volume).
 *
 * Per spec Section 39 ("PLAYER GESTURE").
 */
@Composable
private fun PlayerGestureOverlay(
    isLocked: Boolean,
    onToggleControls: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            // Tap + double-tap detection (skipped while locked — only the
            // unlock button stays interactive, drawn on top of this overlay).
            .pointerInput(isLocked) {
                if (isLocked) return@pointerInput
                detectTapGestures(
                    onTap = { /* any tap toggles controls */ onToggleControls() },
                    onDoubleTap = { offset ->
                        val w = size.width.toFloat()
                        when {
                            offset.x < w / 3f -> onSeekBy(-SEEK_STEP_MS)
                            offset.x > 2f * w / 3f -> onSeekBy(SEEK_STEP_MS)
                            else -> onTogglePlayPause()
                        }
                    }
                )
            }
            // Vertical drag for brightness (left half) / volume (right half).
            .pointerInput(isLocked) {
                if (isLocked) return@pointerInput
                var isBrightness = false
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        isBrightness = offset.x < size.width / 2f
                        totalDrag = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                        // Convert px → fraction of viewport height (negative = up = increase).
                        val fractionDelta = -totalDrag / size.height.toFloat()
                        if (isBrightness) onBrightnessChange(fractionDelta)
                        else onVolumeChange(fractionDelta)
                    }
                )
            }
    )
}

private const val SEEK_STEP_MS = 10_000L

// ────────────────────────────────────────────────────────────────────────────
// Brightness / volume helpers
// ────────────────────────────────────────────────────────────────────────────

/**
 * Apply a brightness delta by manipulating the host [Activity]'s
 * `window.attributes.screenBrightness`.
 *
 * Per spec Section 39 ("PLAYER GESTURE"): vertical swipe on left half
 * adjusts brightness; -1f means "use system default".
 */
private fun applyBrightnessDelta(
    context: Context,
    deltaFraction: Float,
    viewModel: PlayerViewModel
) {
    val activity = context as? Activity ?: return
    val window = activity.window
    val current = window.attributes.screenBrightness
    val base = if (current < 0f) 0.5f else current
    val next = (base + deltaFraction).coerceIn(0f, 1f)
    val params = window.attributes
    params.screenBrightness = next
    window.attributes = params
    viewModel.setBrightness(next)
}

/**
 * Apply a volume delta via [AudioManager] for [AudioManager.STREAM_MUSIC].
 */
private fun applyVolumeDelta(
    context: Context,
    deltaFraction: Float,
    viewModel: PlayerViewModel
) {
    val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
    val next = (current + deltaFraction * max).toInt().coerceIn(0, max)
    audio.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
    viewModel.setVolume(next.toFloat() / max.toFloat())
}

// ────────────────────────────────────────────────────────────────────────────
// Bottom control bar
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun BottomControlBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    currentSpeed: Float,
    availableSources: List<PlaybackSource>,
    currentSourceUrl: String?,
    onSeekTo: (Long) -> Unit,
    onSeekFinished: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onSourceSelected: (PlaybackSource) -> Unit,
    onEnterPip: () -> Unit,
    pipAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    var seekPreview by remember { mutableStateOf<Long?>(null) }
    val displayedPosition = seekPreview ?: positionMs
    val safeMax = durationMs.toFloat().coerceAtLeast(1f)

    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        PLUTOColors.Void.copy(alpha = 0f),
                        PLUTOColors.Void.copy(alpha = 0.7f),
                        PLUTOColors.Void.copy(alpha = 0.92f)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Position + duration row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(displayedPosition),
                style = PLUTOTypography.metadataMono,
                color = PLUTOColors.FrostWhite
            )
            Text(
                text = formatTime(durationMs),
                style = PLUTOTypography.metadataMono,
                color = PLUTOColors.IceBlue
            )
        }

        Slider(
            value = displayedPosition.toFloat().coerceIn(0f, safeMax),
            onValueChange = { v -> seekPreview = v.toLong() },
            onValueChangeFinished = {
                seekPreview?.let { onSeekFinished(it) }
                seekPreview = null
            },
            valueRange = 0f..safeMax,
            colors = SliderDefaults.colors(
                thumbColor = PLUTOColors.GlowBlue,
                activeTrackColor = PLUTOColors.GlowBlue,
                inactiveTrackColor = PLUTOColors.NavyDrift
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        // Buttons row: play/pause | spacer | speed chips | quality chips | pip
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PLUTOIconCircle(
                icon = if (isPlaying) PlutoIcons.Pause else PlutoIcons.Play,
                onClick = onTogglePlayPause,
                contentDescription = if (isPlaying) "Pause" else "Play",
                size = 44
            )

            // Speed chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SPEED_OPTIONS.forEach { speed ->
                    val selected = currentSpeed == speed
                    ChipPill(
                        text = speedLabel(speed),
                        selected = selected,
                        onClick = { onSpeedSelected(speed) }
                    )
                }
            }
        }

        // Quality chips + PiP (wrap if needed)
        if (availableSources.size > 1 || pipAvailable) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (availableSources.size > 1) {
                    availableSources.forEach { src ->
                        val selected = src.url == currentSourceUrl
                        ChipPill(
                            text = src.qualityLabel.ifBlank { "Source" },
                            selected = selected,
                            onClick = { onSourceSelected(src) }
                        )
                    }
                }
                if (pipAvailable) {
                    ChipPill(
                        text = "PiP",
                        selected = false,
                        onClick = onEnterPip
                    )
                }
            }
        }
    }
}

private val SPEED_OPTIONS = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)

private fun speedLabel(speed: Float): String =
    if (speed == 1.0f) "1x" else "${speed}x"

// ────────────────────────────────────────────────────────────────────────────
// Chip pill (speed / quality selector)
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChipPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) PLUTOColors.Glass4 else PLUTOColors.Glass2
    val border = if (selected) PLUTOColors.GlassBorderActive else PLUTOColors.GlassBorder
    val tint = if (selected) PLUTOColors.FrostWhite else PLUTOColors.IceBlue
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PLUTOShapes.pill))
            .background(bg)
            .border(BorderStroke(1.dp, border), RoundedCornerShape(PLUTOShapes.pill))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = PLUTOTypography.bodySmall,
            color = tint
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Error overlay
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlayerErrorOverlay(
    message: String,
    onSwitchSource: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(PLUTOColors.Void.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                imageVector = PlutoIcons.Signal,
                contentDescription = null,
                tint = PLUTOColors.Danger,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = "Playback interrupted",
                style = PLUTOTypography.displaySmall,
                color = PLUTOColors.FrostWhite
            )
            Text(
                text = message,
                style = PLUTOTypography.bodyMedium,
                color = PLUTOColors.MutedStar,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PLUTOOutlinedButton(
                    text = "Back",
                    onClick = onExit,
                    icon = PlutoIcons.Back
                )
                PLUTOButton(
                    text = "Switch source",
                    onClick = onSwitchSource,
                    icon = PlutoIcons.Refresh
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Ended overlay
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlayerEndedOverlay(
    onReplay: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(PLUTOColors.Void.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "You've reached the end",
                style = PLUTOTypography.displaySmall,
                color = PLUTOColors.FrostWhite
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PLUTOOutlinedButton(
                    text = "Back",
                    onClick = onExit,
                    icon = PlutoIcons.Back
                )
                PLUTOButton(
                    text = "Replay",
                    onClick = onReplay,
                    icon = PlutoIcons.Play
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Time formatting
// ────────────────────────────────────────────────────────────────────────────

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
