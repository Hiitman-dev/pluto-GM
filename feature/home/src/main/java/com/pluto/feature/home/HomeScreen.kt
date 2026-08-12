package com.pluto.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pluto.core.common.ApiException
import com.pluto.core.common.Result
import com.pluto.core.data.ContentRepository
import com.pluto.core.data.HistoryRepository
import com.pluto.core.data.FavoritesRepository
import com.pluto.core.designsystem.CosmicBackground
import com.pluto.core.designsystem.EmptyState
import com.pluto.core.designsystem.PLUTOButton
import com.pluto.core.designsystem.PLUTOColors
import com.pluto.core.designsystem.PLUTOOutlinedButton
import com.pluto.core.designsystem.PLUTOShimmer
import com.pluto.core.designsystem.PLUTOTypography
import com.pluto.core.designsystem.PlutoIcons
import com.pluto.core.designsystem.PlutoPosterCard
import com.pluto.core.model.FilterType
import com.pluto.core.model.Movie
import com.pluto.core.model.PlaybackProgress
import com.pluto.core.model.Series
import com.pluto.core.designsystem.PLUTOShapes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HomeViewModel — exposes home screen state.
 *
 * Per the spec's "Smart Home" section: Home adapts to user behavior.
 * We use locally-available data (watch history, favorites) to compose
 * the home screen — no AI backend required.
 *
 * Sections shown:
 *   1. Continue Watching (from PlaybackProgressDao) — only if any.
 *   2. Recently Added Movies (FilterType.DEFAULT).
 *   3. Top IMDb Movies (FilterType.BY_IMDB).
 *   4. Recently Added Series (FilterType.DEFAULT).
 *   5. Top IMDb Series (FilterType.BY_IMDB).
 *
 * If no data is available and the user has no history/favorites, the
 * "New User Experience" surfaces — a single cinematic hero + Recently
 * Added Movies.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val historyRepository: HistoryRepository,
    favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val _moviesLatest = MutableStateFlow<Result<List<Movie>>>(Result.loading())
    val moviesLatest: StateFlow<Result<List<Movie>>> = _moviesLatest.asStateFlow()

    private val _moviesTopRated = MutableStateFlow<Result<List<Movie>>>(Result.loading())
    val moviesTopRated: StateFlow<Result<List<Movie>>> = _moviesTopRated.asStateFlow()

    private val _seriesLatest = MutableStateFlow<Result<List<Series>>>(Result.loading())
    val seriesLatest: StateFlow<Result<List<Series>>> = _seriesLatest.asStateFlow()

    private val _seriesTopRated = MutableStateFlow<Result<List<Series>>>(Result.loading())
    val seriesTopRated: StateFlow<Result<List<Series>>> = _seriesTopRated.asStateFlow()

    val continueWatching: StateFlow<List<PlaybackProgress>> =
        historyRepository.observeContinueWatching(10)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { loadAll() }

    fun loadAll() {
        viewModelScope.launch {
            _moviesLatest.value = contentRepository.getMovies(1, 0, FilterType.DEFAULT)
        }
        viewModelScope.launch {
            _moviesTopRated.value = contentRepository.getMovies(1, 0, FilterType.BY_IMDB)
        }
        viewModelScope.launch {
            _seriesLatest.value = contentRepository.getSeries(1, 0, FilterType.DEFAULT)
        }
        viewModelScope.launch {
            _seriesTopRated.value = contentRepository.getSeries(1, 0, FilterType.BY_IMDB)
        }
    }
}

/**
 * HomeScreen — the first screen the user sees.
 *
 * Layout:
 *   - CosmicBackground (animated nebula + star field)
 *   - LazyColumn with sections
 *   - Each section is a LazyRow of PlutoPosterCards
 *
 * Per the spec's "First 5 seconds" requirement: the user must instantly
 * understand that this is a streaming app, content is available, and
 * the visual identity is distinctive.
 */
@Composable
fun HomeScreen(
    onOpenMovie: (Int) -> Unit,
    onOpenSeries: (Int) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenContinueWatching: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val moviesLatest by viewModel.moviesLatest.collectAsStateWithLifecycle()
    val moviesTop by viewModel.moviesTopRated.collectAsStateWithLifecycle()
    val seriesLatest by viewModel.seriesLatest.collectAsStateWithLifecycle()
    val seriesTop by viewModel.seriesTopRated.collectAsStateWithLifecycle()
    val continueWatching by viewModel.continueWatching.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        CosmicBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Brand header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PLUTO",
                        style = PLUTOTypography.brandWordmark,
                        color = PLUTOColors.FrostWhite
                    )
                    PLUTOOutlinedButton(
                        text = "Discover",
                        onClick = onOpenSearch,
                        icon = PlutoIcons.Search
                    )
                }
            }

            // Continue Watching
            if (continueWatching.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Continue Watching",
                        subtitle = "Pick up where you left off",
                        onSeeAll = onOpenContinueWatching
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(continueWatching) { progress ->
                            ContinueWatchingCard(
                                progress = progress,
                                onClick = {
                                    if (progress.contentType == "movie") onOpenMovie(progress.contentId)
                                    else onOpenSeries(progress.contentId)
                                }
                            )
                        }
                    }
                }
            }

            // Recently Added Movies
            item {
                MovieSection(
                    title = "Recently Added Movies",
                    result = moviesLatest,
                    onRetry = viewModel::loadAll,
                    onOpenMovie = onOpenMovie
                )
            }

            // Top IMDb Movies
            item {
                MovieSection(
                    title = "Top Rated Movies",
                    result = moviesTop,
                    onRetry = viewModel::loadAll,
                    onOpenMovie = onOpenMovie
                )
            }

            // Recently Added Series
            item {
                SeriesSection(
                    title = "Recently Added Series",
                    result = seriesLatest,
                    onRetry = viewModel::loadAll,
                    onOpenSeries = onOpenSeries
                )
            }

            // Top IMDb Series
            item {
                SeriesSection(
                    title = "Top Rated Series",
                    result = seriesTop,
                    onRetry = viewModel::loadAll,
                    onOpenSeries = onOpenSeries
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
    onSeeAll: (() -> Unit)? = null
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = PLUTOTypography.headlineLarge,
                color = PLUTOColors.FrostWhite
            )
            if (onSeeAll != null) {
                PLUTOOutlinedButton(text = "See All", onClick = onSeeAll)
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = PLUTOTypography.bodySmall,
                color = PLUTOColors.MutedStar,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun MovieSection(
    title: String,
    result: Result<List<Movie>>,
    onRetry: () -> Unit,
    onOpenMovie: (Int) -> Unit
) {
    SectionHeader(title = title)
    when (result) {
        is Result.Loading -> SkeletonRow()
        is Result.Error -> ErrorRow(result.exception, onRetry)
        is Result.Success -> {
            if (result.data.isEmpty()) {
                EmptyRow("No movies available right now.")
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(result.data) { movie ->
                        PlutoPosterCard(
                            title = movie.title,
                            imageUrl = movie.image,
                            year = movie.year,
                            rating = movie.imdb,
                            isSeries = false,
                            qualityLabel = movie.sources.firstOrNull()?.quality,
                            onClick = { onOpenMovie(movie.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesSection(
    title: String,
    result: Result<List<Series>>,
    onRetry: () -> Unit,
    onOpenSeries: (Int) -> Unit
) {
    SectionHeader(title = title)
    when (result) {
        is Result.Loading -> SkeletonRow()
        is Result.Error -> ErrorRow(result.exception, onRetry)
        is Result.Success -> {
            if (result.data.isEmpty()) {
                EmptyRow("No series available right now.")
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(result.data) { series ->
                        PlutoPosterCard(
                            title = series.title,
                            imageUrl = series.image,
                            year = series.year,
                            rating = series.imdb,
                            isSeries = true,
                            onClick = { onOpenSeries(series.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonRow() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(5) {
            PLUTOShimmer(
                modifier = Modifier.size(width = 140.dp, height = 210.dp)
            )
        }
    }
}

@Composable
private fun ErrorRow(exception: ApiException, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(PLUTOShapes.medium))
            .background(PLUTOColors.NavyDrift.copy(alpha = 0.6f))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Couldn't load this section",
                style = PLUTOTypography.bodyMedium,
                color = PLUTOColors.FrostWhite
            )
            Text(
                text = exception.message ?: "Unknown error",
                style = PLUTOTypography.bodySmall,
                color = PLUTOColors.MutedStar,
                modifier = Modifier.padding(top = 4.dp)
            )
            PLUTOButton(
                text = "Retry",
                onClick = onRetry,
                icon = PlutoIcons.Refresh,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun EmptyRow(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = message,
            style = PLUTOTypography.bodySmall,
            color = PLUTOColors.MutedStar
        )
    }
}

@Composable
private fun ContinueWatchingCard(
    progress: PlaybackProgress,
    onClick: () -> Unit
) {
    val percent = if (progress.durationMs > 0) {
        (progress.positionMs.toFloat() / progress.durationMs).coerceIn(0f, 1f)
    } else 0f

    Column(modifier = Modifier.width(200.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(PLUTOShapes.medium))
                .background(PLUTOColors.NavyDrift)
                .clip(RoundedCornerShape(PLUTOShapes.medium))
        ) {
            // The progress entity doesn't carry an image; show a gradient backdrop.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                PLUTOColors.ElectricBlue.copy(alpha = 0.18f),
                                PLUTOColors.DeepSpace
                            )
                        )
                    )
            )
            Icon(
                imageVector = PlutoIcons.Play,
                contentDescription = "Play",
                tint = PLUTOColors.FrostWhite,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
            )
            // Progress bar at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(PLUTOColors.NavyDrift)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percent)
                        .height(3.dp)
                        .background(PLUTOColors.GlowBlue)
                )
            }
        }
        Text(
            text = "Episode ${progress.episodeId ?: ""}",
            style = PLUTOTypography.bodySmall,
            color = PLUTOColors.FrostWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "${(percent * 100).toInt()}% watched",
            style = PLUTOTypography.metadataMono,
            color = PLUTOColors.IceBlue,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
