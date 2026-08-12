package com.pluto.feature.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pluto.core.common.ApiException
import com.pluto.core.common.Result
import com.pluto.core.data.ContentRepository
import com.pluto.core.data.FavoritesRepository
import com.pluto.core.data.HistoryRepository
import com.pluto.core.data.Mappers
import com.pluto.core.designsystem.CosmicBackground
import com.pluto.core.designsystem.EmptyState
import com.pluto.core.designsystem.PLUTOButton
import com.pluto.core.designsystem.PLUTOColors
import com.pluto.core.designsystem.PLUTOIconCircle
import com.pluto.core.designsystem.PLUTOOutlinedButton
import com.pluto.core.designsystem.PLUTOShimmer
import com.pluto.core.designsystem.PLUTOShapes
import com.pluto.core.designsystem.PLUTOTypography
import com.pluto.core.designsystem.PlutoIcons
import com.pluto.core.designsystem.SignalLostState
import com.pluto.core.download.ExternalActionLauncher
import com.pluto.core.download.ExternalAppInfo
import com.pluto.core.model.FilterType
import com.pluto.core.model.Movie
import com.pluto.core.model.NormalizedEpisode
import com.pluto.core.model.NormalizedSeason
import com.pluto.core.model.NormalizedSeries
import com.pluto.core.model.PlaybackProgress
import com.pluto.core.model.Quality
import com.pluto.core.model.Series
import com.pluto.core.model.Source
import com.pluto.core.notifications.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * DetailsViewModel — drives [DetailsScreen].
 *
 * Responsibilities:
 *   1. Fetch a single [Movie] or [Series] by id (the CCloud API has no
 *      "get single movie" endpoint, so we page through the catalog list
 *      endpoint up to [MAX_PAGES] pages and filter by id).
 *   2. For series, normalize via [ContentRepository.getNormalizedSeries]
 *      so the UI sees clean seasons + per-episode quality tiers.
 *   3. Observe + mutate the favorite state via [FavoritesRepository].
 *   4. Observe + mutate the "follow series" state via [NotificationRepository].
 *   5. Surface the last known resume position from [HistoryRepository].
 *   6. Expose the list of installed external video players (MX / VLC / etc).
 *
 * The VM is loaded on first composition via [load] — see the
 * `LaunchedEffect(contentType, contentId)` block in [DetailsScreen].
 */
@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val favoritesRepository: FavoritesRepository,
    private val historyRepository: HistoryRepository,
    private val notificationRepository: NotificationRepository,
    private val externalActionLauncher: ExternalActionLauncher
) : ViewModel() {

    private val _movie = MutableStateFlow<Result<Movie>>(Result.loading())
    val movie: StateFlow<Result<Movie>> = _movie.asStateFlow()

    private val _series = MutableStateFlow<Result<Series>>(Result.loading())
    val series: StateFlow<Result<Series>> = _series.asStateFlow()

    private val _normalizedSeries =
        MutableStateFlow<Result<NormalizedSeries>>(Result.loading())
    val normalizedSeries: StateFlow<Result<NormalizedSeries>> =
        _normalizedSeries.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    private val _resumePositionMs = MutableStateFlow(0L)
    val resumePositionMs: StateFlow<Long> = _resumePositionMs.asStateFlow()

    private val _selectedSeasonIndex = MutableStateFlow(0)
    val selectedSeasonIndex: StateFlow<Int> = _selectedSeasonIndex.asStateFlow()

    private val _externalPlayers = MutableStateFlow<List<ExternalAppInfo>>(emptyList())
    val externalPlayers: StateFlow<List<ExternalAppInfo>> = _externalPlayers.asStateFlow()

    private var loadedKey: Pair<String, Int>? = null
    private var contentType: String = "movie"
    private var contentId: Int = 0

    /**
     * Prime the ViewModel with the active [contentType] + [contentId].
     *
     * Idempotent — a re-call with the same pair is a no-op. [refresh]
     * resets the dedup key so the next [load] re-fetches.
     */
    fun load(contentType: String, contentId: Int) {
        if (loadedKey == contentType to contentId) return
        loadedKey = contentType to contentId
        this.contentType = contentType
        this.contentId = contentId

        // External players are independent of content — populate once.
        _externalPlayers.value = runCatching {
            externalActionLauncher.getAllVideoPlayers()
        }.getOrDefault(emptyList())

        // Observe favorite state for this content.
        viewModelScope.launch {
            favoritesRepository.observeIsFavorite(contentType, contentId)
                .collectLatest { _isFavorite.value = it }
        }

        // Follow state is series-only.
        if (contentType == "series") {
            viewModelScope.launch {
                notificationRepository.observeIsFollowing(contentId)
                    .collectLatest { _isFollowing.value = it }
            }
        }

        // Fetch content + resume position.
        if (contentType == "movie") {
            loadMovie(contentId)
        } else {
            loadSeries(contentId)
        }
        loadResumePosition()
    }

    private fun loadMovie(id: Int) {
        viewModelScope.launch {
            _movie.value = Result.loading()
            _movie.value = findMovie(id)
        }
    }

    private suspend fun findMovie(id: Int): Result<Movie> {
        // CCloud has no "get single movie" endpoint. Page through the
        // catalog list up to MAX_PAGES and look for a matching id.
        for (page in 1..MAX_PAGES) {
            when (val result = contentRepository.getMovies(page, 0, FilterType.DEFAULT)) {
                is Result.Success -> {
                    if (result.data.isEmpty()) break // end of catalog
                    val match = result.data.firstOrNull { it.id == id }
                    if (match != null) return Result.success(match)
                }
                is Result.Error -> return result
                is Result.Loading -> continue
            }
        }
        return Result.error(ApiException.NotFound())
    }

    private fun loadSeries(id: Int) {
        viewModelScope.launch {
            _series.value = Result.loading()
            _normalizedSeries.value = Result.loading()
            val seriesResult = findSeries(id)
            _series.value = seriesResult
            if (seriesResult is Result.Success) {
                _normalizedSeries.value =
                    contentRepository.getNormalizedSeries(seriesResult.data)
                // Default to the first season.
                _selectedSeasonIndex.value = 0
            }
        }
    }

    private suspend fun findSeries(id: Int): Result<Series> {
        for (page in 1..MAX_PAGES) {
            when (val result = contentRepository.getSeries(page, 0, FilterType.DEFAULT)) {
                is Result.Success -> {
                    if (result.data.isEmpty()) break
                    val match = result.data.firstOrNull { it.id == id }
                    if (match != null) return Result.success(match)
                }
                is Result.Error -> return result
                is Result.Loading -> continue
            }
        }
        return Result.error(ApiException.NotFound())
    }

    private fun loadResumePosition() {
        viewModelScope.launch {
            val progress: PlaybackProgress? = runCatching {
                historyRepository.getProgress(contentType, contentId, null)
            }.getOrNull()
            _resumePositionMs.value = progress?.positionMs ?: 0L
        }
    }

    /** Toggle the favorite state. Resolves the right "save" path by content type. */
    fun toggleFavorite() {
        viewModelScope.launch {
            if (_isFavorite.value) {
                favoritesRepository.remove(contentType, contentId)
            } else {
                when (contentType) {
                    "movie" -> _movie.value.getOrNull()?.let {
                        favoritesRepository.saveMovie(it)
                    }
                    "series" -> _series.value.getOrNull()?.let {
                        favoritesRepository.saveSeries(it)
                    }
                }
            }
        }
    }

    /**
     * Toggle the "follow series" state. Series-only.
     *
     * On first follow, seed `lastKnownSeason` / `lastKnownEpisode` with
     * the current max so the user isn't immediately spammed with old
     * episodes as "new" notifications (per Section 50 of the spec).
     */
    fun toggleFollow() {
        if (contentType != "series") return
        viewModelScope.launch {
            if (_isFollowing.value) {
                notificationRepository.unfollow(contentId)
            } else {
                val series = (_series.value as? Result.Success)?.data ?: return@launch
                val normalized = (_normalizedSeries.value as? Result.Success)?.data
                val lastSeason = normalized?.seasons?.maxOfOrNull { it.seasonNumber } ?: 0
                val lastEpisode = normalized?.seasons
                    ?.firstOrNull { it.seasonNumber == lastSeason }
                    ?.episodes?.maxOfOrNull { it.episodeNumber } ?: 0
                notificationRepository.follow(
                    seriesId = contentId,
                    title = series.title,
                    poster = series.image,
                    lastKnownSeason = lastSeason,
                    lastKnownEpisode = lastEpisode
                )
            }
        }
    }

    fun selectSeason(index: Int) {
        _selectedSeasonIndex.value = index
    }

    /** Open the system "open with…" chooser for [sourceUrl]. */
    fun playInExternalApp(sourceUrl: String) {
        runCatching { externalActionLauncher.openWithChooser(sourceUrl) }
    }

    /** Open [sourceUrl] in a specific external player (e.g. MX Player). */
    fun playInExternalApp(sourceUrl: String, packageName: String, mimeType: String) {
        runCatching {
            externalActionLauncher.openWithVideoPlayer(sourceUrl, packageName, mimeType)
        }
    }

    /** Force a fresh fetch (drops the dedup key + re-loads). */
    fun refresh() {
        loadedKey = null
        load(contentType, contentId)
    }

    /**
     * Hook called when the user taps the primary "Play" CTA on a movie.
     * Marks the title as viewed + seeds a playback-progress entry before
     * the caller invokes [onPlay]. The Player screen takes over actual
     * periodic progress saves.
     */
    fun onPlayTapped() {
        viewModelScope.launch {
            val title: String
            val image: String
            val genres: List<com.pluto.core.model.Genre>
            when (contentType) {
                "movie" -> {
                    val m = _movie.value.getOrNull() ?: return@launch
                    title = m.title
                    image = m.image
                    genres = m.genres
                }
                "series" -> {
                    val s = _series.value.getOrNull() ?: return@launch
                    title = s.title
                    image = s.image
                    genres = s.genres
                }
                else -> return@launch
            }
            historyRepository.markViewed(
                contentType = contentType,
                itemId = contentId,
                title = title,
                image = image,
                genresJson = Mappers.encodeGenres(genres)
            )
            historyRepository.saveProgress(
                PlaybackProgress(
                    contentId = contentId,
                    contentType = contentType,
                    episodeId = null,
                    seasonId = null,
                    positionMs = _resumePositionMs.value,
                    durationMs = 0L,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Hook called when the user taps an episode. Same pattern as
     * [onPlayTapped] — marks the series + episode as viewed, seeds
     * playback progress, then the caller invokes [onPlayEpisode].
     */
    fun onEpisodeTapped(episodeId: Int, seasonNumber: Int, episodeNumber: Int) {
        viewModelScope.launch {
            val series = (_series.value as? Result.Success)?.data ?: return@launch
            val normalized = (_normalizedSeries.value as? Result.Success)?.data
            val season: NormalizedSeason? = normalized?.seasons
                ?.firstOrNull { it.seasonNumber == seasonNumber }
            val episode: NormalizedEpisode? = season?.episodes
                ?.firstOrNull { it.episodeNumber == episodeNumber }
            val resolvedEpisodeId = episode?.id ?: episodeId
            val seasonId = season?.id

            historyRepository.markViewed(
                contentType = "series",
                itemId = contentId,
                title = series.title,
                image = series.image,
                genresJson = Mappers.encodeGenres(series.genres),
                episodeId = resolvedEpisodeId,
                seasonId = seasonId
            )
            historyRepository.saveProgress(
                PlaybackProgress(
                    contentId = contentId,
                    contentType = "series",
                    episodeId = resolvedEpisodeId,
                    seasonId = seasonId,
                    positionMs = 0L,
                    durationMs = 0L,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private companion object {
        const val MAX_PAGES = 5
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Screen
// ────────────────────────────────────────────────────────────────────────────

/**
 * DetailsScreen — full-featured details view for movies and series.
 *
 * Layout (top to bottom):
 *   - [CosmicBackground] backdrop (fixed).
 *   - Scrollable [Column]: 16:9 backdrop image with gradient overlay,
 *     title (displayLarge), year · duration · rating row, genre chips,
 *     synopsis, CTA row, and either the movie quality list or the
 *     series season/episode list. A bottom "Open in external player"
 *     section lists installed player apps.
 *   - Floating [PLUTOIconCircle] back + favorite buttons fixed at the
 *     top (overlay the backdrop).
 *
 * State routing:
 *   - Loading → [DetailsSkeleton].
 *   - Error (network) → [SignalLostState] with retry.
 *   - Error (NotFound) → [EmptyState] "Couldn't find this title".
 *   - Success → movie or series content.
 */
@Composable
fun DetailsScreen(
    contentType: String,
    contentId: Int,
    onPlay: (resumeMs: Long) -> Unit,
    onPlayEpisode: ((episodeId: Int, seasonNumber: Int, episodeNumber: Int) -> Unit)? = null,
    onBack: () -> Unit,
    viewModel: DetailsViewModel = hiltViewModel()
) {
    LaunchedEffect(contentType, contentId) {
        viewModel.load(contentType, contentId)
    }

    val movie by viewModel.movie.collectAsStateWithLifecycle()
    val series by viewModel.series.collectAsStateWithLifecycle()
    val normalizedSeries by viewModel.normalizedSeries.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val isFollowing by viewModel.isFollowing.collectAsStateWithLifecycle()
    val resumePositionMs by viewModel.resumePositionMs.collectAsStateWithLifecycle()
    val selectedSeasonIndex by viewModel.selectedSeasonIndex.collectAsStateWithLifecycle()
    val externalPlayers by viewModel.externalPlayers.collectAsStateWithLifecycle()

    val primaryState: Result<*> = when (contentType) {
        "series" -> series
        else -> movie
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CosmicBackground(modifier = Modifier.fillMaxSize())

        when (primaryState) {
            is Result.Loading -> DetailsSkeleton(
                onBack = onBack
            )

            is Result.Error -> {
                if (primaryState.exception is ApiException.NotFound) {
                    EmptyState(
                        title = "Couldn't find this title",
                        message = "This title isn't in the PLUTO catalog right now. " +
                            "It may have been removed or never existed.",
                        icon = PlutoIcons.Galaxy,
                        action = { PLUTOButton("Go Back", onClick = onBack, icon = PlutoIcons.Back) }
                    )
                } else {
                    SignalLostState(
                        message = "Couldn't load this title.",
                        onRetry = viewModel::refresh,
                        onGoBack = onBack
                    )
                }
            }

            is Result.Success -> {
                if (contentType == "series") {
                    SeriesDetailsContent(
                        series = (series as Result.Success).data,
                        normalized = normalizedSeries,
                        isFavorite = isFavorite,
                        isFollowing = isFollowing,
                        selectedSeasonIndex = selectedSeasonIndex,
                        externalPlayers = externalPlayers,
                        onBack = onBack,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onToggleFollow = viewModel::toggleFollow,
                        onSelectSeason = viewModel::selectSeason,
                        onPlayEpisode = onPlayEpisode,
                        onEpisodeTapped = viewModel::onEpisodeTapped,
                        onOpenWithChooser = viewModel::playInExternalApp,
                        onOpenWithPlayer = viewModel::playInExternalApp
                    )
                } else {
                    MovieDetailsContent(
                        movie = (movie as Result.Success).data,
                        isFavorite = isFavorite,
                        resumePositionMs = resumePositionMs,
                        externalPlayers = externalPlayers,
                        onBack = onBack,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onPlay = {
                            viewModel.onPlayTapped()
                            onPlay(resumePositionMs)
                        },
                        onOpenWithChooser = viewModel::playInExternalApp,
                        onOpenWithPlayer = viewModel::playInExternalApp
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Movie details
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun MovieDetailsContent(
    movie: Movie,
    isFavorite: Boolean,
    resumePositionMs: Long,
    externalPlayers: List<ExternalAppInfo>,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlay: () -> Unit,
    onOpenWithChooser: (String) -> Unit,
    onOpenWithPlayer: (String, String, String) -> Unit
) {
    val scrollState = rememberScrollState()
    val backdropUrl = movie.cover.ifBlank { movie.image }
    val bestSource = pickBestSourceFromMovie(movie)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Backdrop(url = backdropUrl, contentDescription = movie.title)

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = movie.title,
                style = PLUTOTypography.displayLarge,
                color = PLUTOColors.FrostWhite,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Year · Duration · Rating
            MetadataRow(
                year = movie.year,
                duration = movie.duration,
                rating = movie.imdb
            )

            // Genres
            if (movie.genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                GenresRow(genres = movie.genres.map { it.title })
            }

            // Synopsis
            if (movie.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = movie.description,
                    style = PLUTOTypography.bodyMedium,
                    color = PLUTOColors.IceBlue
                )
            }

            // CTA row
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PLUTOButton(
                    text = if (resumePositionMs > 0L) "Resume" else "Play",
                    onClick = onPlay,
                    icon = PlutoIcons.Play,
                    modifier = Modifier.weight(1f)
                )
                if (bestSource != null) {
                    PLUTOOutlinedButton(
                        text = "Open With",
                        onClick = { onOpenWithChooser(bestSource.url) },
                        icon = PlutoIcons.External
                    )
                }
            }

            // Quality selector — one chip per quality tier with external-play.
            if (movie.sources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(28.dp))
                SectionLabel("AVAILABLE QUALITIES")
                Spacer(modifier = Modifier.height(12.dp))
                val qualities = groupQualities(movie.sources)
                QualityChips(
                    qualities = qualities,
                    onOpenExternal = { url -> onOpenWithChooser(url) }
                )
            }

            // External players section
            if (externalPlayers.isNotEmpty() && bestSource != null) {
                Spacer(modifier = Modifier.height(28.dp))
                SectionLabel("OPEN IN EXTERNAL PLAYER")
                Spacer(modifier = Modifier.height(12.dp))
                ExternalPlayersRow(
                    players = externalPlayers,
                    url = bestSource.url,
                    onOpen = onOpenWithPlayer
                )
            }

            // Bottom spacing so content clears the floating nav bar.
            Spacer(modifier = Modifier.height(120.dp))
        }
    }

    FloatingTopButtons(
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite
    )
}

// ────────────────────────────────────────────────────────────────────────────
// Series details
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun SeriesDetailsContent(
    series: Series,
    normalized: Result<NormalizedSeries>,
    isFavorite: Boolean,
    isFollowing: Boolean,
    selectedSeasonIndex: Int,
    externalPlayers: List<ExternalAppInfo>,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleFollow: () -> Unit,
    onSelectSeason: (Int) -> Unit,
    onPlayEpisode: ((Int, Int, Int) -> Unit)?,
    onEpisodeTapped: (Int, Int, Int) -> Unit,
    onOpenWithChooser: (String) -> Unit,
    onOpenWithPlayer: (String, String, String) -> Unit
) {
    val scrollState = rememberScrollState()
    val backdropUrl = series.cover.ifBlank { series.image }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Backdrop(url = backdropUrl, contentDescription = series.title)

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = series.title,
                style = PLUTOTypography.displayLarge,
                color = PLUTOColors.FrostWhite,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            MetadataRow(
                year = series.year,
                duration = series.duration,
                rating = series.imdb
            )

            if (series.genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                GenresRow(genres = series.genres.map { it.title })
            }

            if (series.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = series.description,
                    style = PLUTOTypography.bodyMedium,
                    color = PLUTOColors.IceBlue
                )
            }

            // CTA row — Follow Series (toggle).
            Spacer(modifier = Modifier.height(20.dp))
            PLUTOOutlinedButton(
                text = if (isFollowing) "Following" else "Follow Series",
                onClick = onToggleFollow,
                icon = PlutoIcons.Notification,
                modifier = Modifier.fillMaxWidth()
            )

            // Season selector + episode list — only rendered once the
            // normalized series data is available.
            when (normalized) {
                is Result.Success -> {
                    val normalizedData = normalized.data
                    if (normalizedData.seasons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(28.dp))
                        SectionLabel("SEASONS")
                        Spacer(modifier = Modifier.height(12.dp))
                        SeasonSelector(
                            seasons = normalizedData.seasons,
                            selectedIndex = selectedSeasonIndex.coerceIn(
                                0,
                                (normalizedData.seasons.size - 1).coerceAtLeast(0)
                            ),
                            onSelect = onSelectSeason
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        val season = normalizedData.seasons.getOrNull(selectedSeasonIndex)
                        if (season != null) {
                            SectionLabel("EPISODES · ${season.episodes.size}")
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                season.episodes.forEach { episode ->
                                    EpisodeCard(
                                        episode = episode,
                                        onClick = {
                                            onEpisodeTapped(
                                                episode.id,
                                                season.seasonNumber,
                                                episode.episodeNumber
                                            )
                                            onPlayEpisode?.invoke(
                                                episode.id,
                                                season.seasonNumber,
                                                episode.episodeNumber
                                            )
                                        },
                                        onOpenExternal = { url -> onOpenWithChooser(url) }
                                    )
                                }
                            }
                        }

                        // External players section: launches the best source
                        // of the first episode of the selected season.
                        val firstEpisode = normalizedData.seasons
                            .getOrNull(selectedSeasonIndex)?.episodes?.firstOrNull()
                        val bestUrl = firstEpisode?.let { pickBestSourceFromEpisode(it)?.url }
                        if (externalPlayers.isNotEmpty() && bestUrl != null) {
                            Spacer(modifier = Modifier.height(28.dp))
                            SectionLabel("OPEN IN EXTERNAL PLAYER")
                            Spacer(modifier = Modifier.height(12.dp))
                            ExternalPlayersRow(
                                players = externalPlayers,
                                url = bestUrl,
                                onOpen = onOpenWithPlayer
                            )
                        }
                    }
                }
                is Result.Error -> {
                    Spacer(modifier = Modifier.height(28.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(PLUTOShapes.medium))
                            .background(PLUTOColors.NavyDrift.copy(alpha = 0.5f))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Couldn't load seasons.",
                                style = PLUTOTypography.bodyMedium,
                                color = PLUTOColors.FrostWhite
                            )
                            Text(
                                text = normalized.exception.message
                                    ?: "Please try again later.",
                                style = PLUTOTypography.bodySmall,
                                color = PLUTOColors.MutedStar,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                is Result.Loading -> {
                    Spacer(modifier = Modifier.height(28.dp))
                    SectionLabel("LOADING EPISODES…")
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(4) {
                            PLUTOShimmer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(PLUTOShapes.medium))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }

    FloatingTopButtons(
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite
    )
}

// ────────────────────────────────────────────────────────────────────────────
// Shared building blocks
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun Backdrop(url: String, contentDescription: String) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(url.ifBlank { null })
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Subtle darkening so floating buttons are legible.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PLUTOColors.Void.copy(alpha = 0.35f),
                            Color.Transparent,
                            PLUTOColors.Void.copy(alpha = 0.25f)
                        )
                    )
                )
        )
        // Bottom fade into the page background.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PLUTOColors.Void.copy(alpha = 0f),
                            PLUTOColors.Void.copy(alpha = 1f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun FloatingTopButtons(
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PLUTOIconCircle(
            icon = PlutoIcons.Back,
            onClick = onBack,
            contentDescription = "Back",
            tint = PLUTOColors.FrostWhite,
            background = PLUTOColors.Glass2
        )
        PLUTOIconCircle(
            icon = if (isFavorite) PlutoIcons.FavoriteFilled else PlutoIcons.Favorite,
            onClick = onToggleFavorite,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            tint = if (isFavorite) PLUTOColors.Danger else PLUTOColors.FrostWhite,
            background = PLUTOColors.Glass2
        )
    }
}

@Composable
private fun MetadataRow(year: Int, duration: String?, rating: Double) {
    val parts = buildList {
        if (year > 0) add(year.toString())
        if (!duration.isNullOrBlank()) add(duration)
        if (rating > 0) add("%.1f".format(rating))
    }
    if (parts.isEmpty()) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (rating > 0) {
            Icon(
                imageVector = PlutoIcons.Star,
                contentDescription = null,
                tint = PLUTOColors.Warning,
                modifier = Modifier.size(14.dp)
            )
        }
        parts.forEachIndexed { index, part ->
            if (index > 0) {
                Text(
                    text = "·",
                    style = PLUTOTypography.metadataMono,
                    color = PLUTOColors.MutedStar
                )
            }
            Text(
                text = part,
                style = PLUTOTypography.metadataMono,
                color = if (index == parts.lastIndex && rating > 0)
                    PLUTOColors.FrostWhite
                else PLUTOColors.IceBlue
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenresRow(genres: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        genres.forEach { genre ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(PLUTOShapes.pill))
                    .background(PLUTOColors.Glass2)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = genre,
                    style = PLUTOTypography.labelMono,
                    color = PLUTOColors.IceBlue
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = PLUTOTypography.labelMono,
        color = PLUTOColors.IceBlue
    )
}

@Composable
private fun SeasonSelector(
    seasons: List<NormalizedSeason>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = -24.dp)
    ) {
        items(items = seasons, key = { it.id }) { season ->
            val selected = seasons.indexOf(season) == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(PLUTOShapes.pill))
                    .background(
                        if (selected) PLUTOColors.Glass4 else PLUTOColors.Glass2
                    )
                    .clickable { onSelect(seasons.indexOf(season)) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Season ${season.seasonNumber}",
                    style = PLUTOTypography.bodySmall.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (selected) PLUTOColors.FrostWhite else PLUTOColors.IceBlue
                )
            }
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: NormalizedEpisode,
    onClick: () -> Unit,
    onOpenExternal: (String) -> Unit
) {
    val bestSource = pickBestSourceFromEpisode(episode)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PLUTOShapes.medium))
            .background(PLUTOColors.Glass2)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Episode number badge
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(50))
                .background(PLUTOColors.Glass4),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = episode.episodeNumber.toString(),
                style = PLUTOTypography.metadataMono.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = PLUTOColors.FrostWhite
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = episode.title.ifBlank { "Episode ${episode.episodeNumber}" },
                style = PLUTOTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = PLUTOColors.FrostWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val qualityLabel = episode.qualities.firstOrNull()?.label
            val meta = buildList {
                if (!episode.duration.isNullOrBlank()) add(episode.duration)
                if (qualityLabel != null) add(qualityLabel)
            }.joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = PLUTOTypography.metadataMono,
                    color = PLUTOColors.MutedStar,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // External-player icon (only if a source is available)
        if (bestSource != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(PLUTOColors.Glass4)
                    .clickable { onOpenExternal(bestSource.url) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PlutoIcons.External,
                    contentDescription = "Open in external player",
                    tint = PLUTOColors.IceBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Primary play icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(50))
                .background(PLUTOColors.ElectricBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = PlutoIcons.Play,
                contentDescription = "Play episode",
                tint = PLUTOColors.FrostWhite,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QualityChips(
    qualities: List<Quality>,
    onOpenExternal: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        qualities.forEach { quality ->
            val source = pickBestSource(quality.sources)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(PLUTOShapes.pill))
                    .background(PLUTOColors.Glass2)
                    .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = quality.label,
                    style = PLUTOTypography.labelMono,
                    color = PLUTOColors.FrostWhite
                )
                if (source != null) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(50))
                            .background(PLUTOColors.Glass4)
                            .clickable { onOpenExternal(source.url) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PlutoIcons.External,
                            contentDescription = "Open ${quality.label} externally",
                            tint = PLUTOColors.IceBlue,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExternalPlayersRow(
    players: List<ExternalAppInfo>,
    url: String,
    onOpen: (String, String, String) -> Unit
) {
    val installed = players.filter { it.installed }
    val visible = if (installed.isEmpty()) players else installed
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = -24.dp)
    ) {
        items(items = visible, key = { it.packageName }) { player ->
            val enabled = player.installed
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(PLUTOShapes.pill))
                    .background(if (enabled) PLUTOColors.Glass2 else PLUTOColors.NavyDrift.copy(alpha = 0.4f))
                    .clickable(enabled = enabled) {
                        onOpen(url, player.packageName, player.mimeType)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = player.name,
                    style = PLUTOTypography.labelMono,
                    color = if (enabled) PLUTOColors.IceBlue else PLUTOColors.MutedStar
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Skeleton
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun DetailsSkeleton(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Backdrop shimmer (16:9)
        PLUTOShimmer(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(20.dp))
            // Title
            PLUTOShimmer(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Metadata row
            PLUTOShimmer(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(14.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            // Genre chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    PLUTOShimmer(
                        modifier = Modifier
                            .width(72.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(PLUTOShapes.pill))
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            // Synopsis lines
            repeat(4) { index ->
                PLUTOShimmer(
                    modifier = Modifier
                        .fillMaxWidth(if (index == 3) 0.6f else 1f)
                        .height(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.height(28.dp))
            // CTA
            PLUTOShimmer(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(PLUTOShapes.pill))
            )
        }
    }

    // Back button remains interactive during load.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PLUTOIconCircle(
            icon = PlutoIcons.Back,
            onClick = onBack,
            contentDescription = "Back",
            tint = PLUTOColors.FrostWhite,
            background = PLUTOColors.Glass2
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Source / quality helpers (mirrors SeriesNormalizer.pickBestSource logic
// — duplicated here because :feature:details doesn't depend on :core:network).
// ────────────────────────────────────────────────────────────────────────────

private val SOURCE_TYPE_PREFERENCE = listOf("mp4", "mkv", "x265", "h265", "h264", "avi")

private fun pickBestSource(sources: List<Source>): Source? {
    if (sources.isEmpty()) return null
    for (type in SOURCE_TYPE_PREFERENCE) {
        val match = sources.firstOrNull { it.type.equals(type, ignoreCase = true) }
        if (match != null) return match
    }
    return sources.first()
}

/**
 * Group raw sources by normalized quality height (descending).
 * Mirrors [com.pluto.core.network.SeriesNormalizer.groupQualities] but
 * reimplemented locally so :feature:details doesn't have to depend on
 * :core:network.
 */
private fun groupQualities(sources: List<Source>): List<Quality> {
    return sources
        .filter { it.url.isNotBlank() }
        .groupBy { normalizeHeight(it.quality) }
        .map { (height, sourcesAtHeight) ->
            Quality(
                height = height,
                label = canonicalLabel(height, sourcesAtHeight.first().quality),
                sources = sourcesAtHeight
            )
        }
        .sortedByDescending { it.height }
}

private fun normalizeHeight(quality: String): Int {
    val q = quality.trim().uppercase()
    Regex("(\\d{3,4})").find(q)?.let { return it.groupValues[1].toInt() }
    return when {
        q.contains("4K") || q.contains("UHD") -> 2160
        q.contains("2K") || q.contains("QHD") || q.contains("1440") -> 1440
        q.contains("FULLHD") || q.contains("FHD") -> 1080
        q.contains("HD") -> 720
        q.contains("SD") -> 480
        else -> 0
    }
}

private fun canonicalLabel(height: Int, original: String): String = when {
    height >= 2160 -> "4K"
    height >= 1440 -> "1440p"
    height >= 1080 -> "1080p"
    height >= 720 -> "720p"
    height >= 480 -> "480p"
    height > 0 -> "${height}p"
    else -> original.ifBlank { "Unknown" }
}

private fun pickBestSourceFromMovie(movie: Movie): Source? =
    pickBestSource(movie.sources)

private fun pickBestSourceFromEpisode(episode: NormalizedEpisode): Source? {
    val bestQuality = episode.qualities.firstOrNull() ?: return null
    return pickBestSource(bestQuality.sources)
}
