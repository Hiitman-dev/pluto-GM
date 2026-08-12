package com.pluto.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.pluto.core.common.ApiException
import com.pluto.core.common.Result
import com.pluto.core.data.ContentRepository
import com.pluto.core.data.RecentSearchRepository
import com.pluto.core.designsystem.PLUTOColors
import com.pluto.core.designsystem.PLUTOIconCircle
import com.pluto.core.designsystem.PLUTOOutlinedButton
import com.pluto.core.designsystem.PLUTOShimmer
import com.pluto.core.designsystem.PLUTOShapes
import com.pluto.core.designsystem.PLUTOTypography
import com.pluto.core.designsystem.CosmicBackground
import com.pluto.core.designsystem.EmptyState
import com.pluto.core.designsystem.PlutoIcons
import com.pluto.core.designsystem.PlutoPosterCard
import com.pluto.core.designsystem.SignalLostState
import com.pluto.core.model.Poster
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SearchViewModel — drives [SearchScreen].
 *
 * Responsibilities:
 *   1. Holds the current query as a [StateFlow] of [String].
 *   2. Debounces query changes (300ms) and cancels in-flight requests
 *      when the query changes via [flatMapLatest].
 *   3. Persists successful queries to [RecentSearchRepository] so they
 *      appear as "Recent Searches" chips the next time the user opens
 *      the screen.
 *   4. Exposes recent searches as a [StateFlow] of [List] of [String],
 *      observed from [RecentSearchRepository].
 *
 * The search flow is fully reactive: typing into the field updates
 * [_query]; the [results] flow debounces the change, fires a search
 * against [ContentRepository.search], and emits the typed [Result].
 * Errors thrown inside the search coroutine are caught by
 * [kotlinx.coroutines.flow.catch] and converted to [Result.Error].
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val recentSearchRepository: RecentSearchRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /**
     * Bumped by [retry] to re-fire the current query without retyping
     * it. The retry tick is combined with the *debounced* query so that
     * retries fire immediately instead of waiting another 300ms.
     */
    private val _retryTick = MutableStateFlow(0)

    /**
     * Recent searches observed from [RecentSearchRepository], mapped to
     * plain [String]s for the UI to render as chips.
     */
    val recentSearches: StateFlow<List<String>> =
        recentSearchRepository.observeRecent(limit = 10)
            .map { entities -> entities.map { it.query } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /**
     * Search results — debounced (300ms), distinct, blank-filtered, and
     * re-fired on [retry]. [flatMapLatest] cancels the previous in-flight
     * request whenever a new query arrives so we never render stale
     * results.
     *
     * Blank queries short-circuit to an empty success result so the UI
     * can render the "Recent Searches" section instead of a loading
     * state.
     */
    val results: StateFlow<Result<List<Poster>>> =
        combine(
            _query.debounce(QUERY_DEBOUNCE_MS).distinctUntilChanged(),
            _retryTick
        ) { q, _ -> q }
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    flowOf(Result.success(emptyList()))
                } else {
                    flow {
                        emit(Result.loading<List<Poster>>())
                        // Translate Result<SearchResult> -> Result<List<Poster>>
                        // and persist the query when the call succeeds.
                        val result = contentRepository.search(query)
                            .map { it.posters }
                        if (result is Result.Success) {
                            recentSearchRepository.add(query)
                        }
                        emit(result)
                    }.catch { e ->
                        emit(Result.error(ApiException.fromException(e)))
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = Result.success(emptyList())
            )

    fun setQuery(q: String) {
        _query.value = q
    }

    fun clearQuery() {
        _query.value = ""
    }

    /** Re-fires the search for the current query (used by error retry). */
    fun retry() {
        if (_query.value.isNotBlank()) _retryTick.value++
    }

    fun removeRecent(query: String) {
        viewModelScope.launch { recentSearchRepository.remove(query) }
    }

    fun clearRecents() {
        viewModelScope.launch { recentSearchRepository.clearAll() }
    }

    private companion object {
        const val QUERY_DEBOUNCE_MS = 300L
    }
}

/**
 * SearchScreen — discover movies and series across the PLUTO catalog.
 *
 * Layout (top to bottom):
 *   1. [CosmicBackground] — animated deep-space backdrop.
 *   2. Search top bar — back button + cosmic-styled [TextField] + clear.
 *   3. When the query is empty: "Recent Searches" chips (each removable).
 *   4. When the query is non-empty:
 *        - Loading   → 6 skeleton poster cards in a 2-column grid.
 *        - Error     → [SignalLostState] with retry / go-back actions.
 *        - Empty hit → [EmptyState] "No matches" message.
 *        - Success   → [LazyVerticalGrid] (2 columns) of [PlutoPosterCard].
 *
 * Tapping a result dispatches to [onOpenMovie] or [onOpenSeries] based
 * on [Poster.type] ("movie" vs "serie").
 */
@Composable
fun SearchScreen(
    onOpenMovie: (Int) -> Unit,
    onOpenSeries: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        CosmicBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            SearchTopBar(
                query = query,
                onQueryChange = viewModel::setQuery,
                onClearQuery = viewModel::clearQuery,
                onBack = onBack
            )

            if (query.isBlank()) {
                RecentSearchesSection(
                    recents = recentSearches,
                    onTapRecent = viewModel::setQuery,
                    onRemoveRecent = viewModel::removeRecent,
                    onClearAll = viewModel::clearRecents,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    when (val state = results) {
                        is Result.Loading -> SearchSkeletonGrid()

                        is Result.Error -> SignalLostState(
                            message = "Couldn't search the PLUTO network.",
                            onRetry = viewModel::retry,
                            onGoBack = onBack
                        )

                        is Result.Success -> {
                            if (state.data.isEmpty()) {
                                EmptyState(
                                    title = "No matches",
                                    message = "Nothing in the PLUTO catalog matches " +
                                        "“$query”. Try a different title.",
                                    icon = PlutoIcons.Search
                                )
                            } else {
                                SearchResultsGrid(
                                    results = state.data,
                                    onOpenMovie = onOpenMovie,
                                    onOpenSeries = onOpenSeries
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Top bar ──────────────────────────────────────────────────────────────

/**
 * SearchTopBar — back button + cosmic-styled search [TextField] + clear.
 *
 * The text field uses a frosted-glass container ([PLUTOColors.Glass2]),
 * frosted-white text ([PLUTOColors.FrostWhite]), a transparent indicator
 * (no underline — cosmic surfaces don't use material underlines), and an
 * electric-blue cursor for the "stellar point" accent.
 */
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PLUTOIconCircle(
            icon = PlutoIcons.Back,
            onClick = onBack,
            contentDescription = "Back",
            tint = PLUTOColors.FrostWhite,
            background = PLUTOColors.Glass2
        )

        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = "Search PLUTO…",
                    style = PLUTOTypography.bodyMedium,
                    color = PLUTOColors.MutedStar
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = PlutoIcons.Search,
                    contentDescription = null,
                    tint = PLUTOColors.IceBlue
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onClearQuery),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PlutoIcons.Close,
                            contentDescription = "Clear search",
                            tint = PLUTOColors.IceBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(PLUTOShapes.pill),
            textStyle = PLUTOTypography.bodyMedium.copy(color = PLUTOColors.FrostWhite),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = PLUTOColors.Glass2,
                unfocusedContainerColor = PLUTOColors.Glass2,
                disabledContainerColor = PLUTOColors.Glass2,
                focusedTextColor = PLUTOColors.FrostWhite,
                unfocusedTextColor = PLUTOColors.FrostWhite,
                disabledTextColor = PLUTOColors.FrostWhite,
                cursorColor = PLUTOColors.GlowBlue,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedLeadingIconColor = PLUTOColors.IceBlue,
                unfocusedLeadingIconColor = PLUTOColors.IceBlue,
                focusedTrailingIconColor = PLUTOColors.IceBlue,
                unfocusedTrailingIconColor = PLUTOColors.IceBlue
            )
        )
    }
}

// ── Recent searches ──────────────────────────────────────────────────────

/**
 * RecentSearchesSection — shown when the query is blank.
 *
 * If the user has no recent searches, an [EmptyState] invite to start
 * searching is shown. Otherwise a section header with a "Clear" action
 * precedes a [FlowRow] of removable chips. Tapping a chip runs that
 * query again.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentSearchesSection(
    recents: List<String>,
    onTapRecent: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (recents.isEmpty()) {
        EmptyState(
            title = "Find your next watch",
            message = "Search across every movie and series in the PLUTO " +
                "catalog. Your recent searches will appear here.",
            icon = PlutoIcons.Search,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT SEARCHES",
                style = PLUTOTypography.labelMono,
                color = PLUTOColors.IceBlue
            )
            PLUTOOutlinedButton(text = "Clear", onClick = onClearAll)
        }

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            recents.forEach { term ->
                RecentSearchChip(
                    text = term,
                    onClick = { onTapRecent(term) },
                    onRemove = { onRemoveRecent(term) }
                )
            }
        }
    }
}

/**
 * RecentSearchChip — a frosted-glass pill with the query text and a
 * small "remove" button. Tapping the chip (not the X) re-runs the query.
 */
@Composable
private fun RecentSearchChip(
    text: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(PLUTOShapes.pill))
            .background(PLUTOColors.Glass2)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = text,
            style = PLUTOTypography.bodySmall,
            color = PLUTOColors.FrostWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(50))
                .background(PLUTOColors.Glass4)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = PlutoIcons.Close,
                contentDescription = "Remove \"$text\" from recent searches",
                tint = PLUTOColors.IceBlue,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

// ── Loading / results grids ──────────────────────────────────────────────

/**
 * SearchSkeletonGrid — 6 [PLUTOShimmer] placeholders laid out in the
 * same 2-column grid as real results so the transition to loaded
 * content is visually seamless.
 */
@Composable
private fun SearchSkeletonGrid(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false
    ) {
        items(count = 6) {
            PLUTOShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            )
        }
    }
}

/**
 * SearchResultsGrid — 2-column [LazyVerticalGrid] of [PlutoPosterCard].
 *
 * [BoxWithConstraints] is used to compute the exact column width so each
 * card fills its cell — [PlutoPosterCard] takes a fixed [Int] width
 * rather than `fillMaxWidth`, so we calculate it from the available
 * space minus padding and spacing.
 */
@Composable
private fun SearchResultsGrid(
    results: List<Poster>,
    onOpenMovie: (Int) -> Unit,
    onOpenSeries: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalPadding = 16.dp
        val horizontalSpacing = 12.dp
        val cardWidthDp = (maxWidth - horizontalPadding * 2 - horizontalSpacing) / 2
        val cardWidth = cardWidthDp.value.toInt()

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = horizontalPadding,
                vertical = 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items = results, key = { poster -> "${poster.type}-${poster.id}" }) { poster ->
                PlutoPosterCard(
                    title = poster.title,
                    imageUrl = poster.image,
                    year = poster.year,
                    rating = poster.imdb,
                    isSeries = poster.type == "serie",
                    qualityLabel = poster.sources.firstOrNull()?.quality,
                    onClick = {
                        if (poster.type == "movie") onOpenMovie(poster.id)
                        else onOpenSeries(poster.id)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    cardWidth = cardWidth
                )
            }
        }
    }
}
