package com.pluto.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.pluto.core.data.HistoryRepository
import com.pluto.core.database.entity.HistoryEntity
import com.pluto.core.designsystem.CosmicBackground
import com.pluto.core.designsystem.EmptyState
import com.pluto.core.designsystem.PLUTOColors
import com.pluto.core.designsystem.PLUTOIconCircle
import com.pluto.core.designsystem.PLUTOOutlinedButton
import com.pluto.core.designsystem.PLUTOShapes
import com.pluto.core.designsystem.PLUTOTypography
import com.pluto.core.designsystem.PlutoIcons
import com.pluto.core.designsystem.PlutoPosterCard
import com.pluto.core.model.PlaybackProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HistoryViewModel — drives [HistoryScreen].
 *
 * Surfaces two reactive slices of the user's watch history from
 * [HistoryRepository]:
 *   1. [continueWatching] — the most recent in-progress plays, observed
 *      from `playback_progress` so a user can resume where they left off.
 *   2. [recent] — the most recent rows in `history`, observed from the
 *      `history` table so a user can re-open anything they've watched.
 *
 * Both Flows start with `emptyList()` and re-emit on every Room write, so
 * the UI updates instantly when progress is saved or a new item is
 * marked viewed — no manual refresh, no spinner for local data.
 *
 * [clearAll] wipes both tables (history + playback_progress) via the
 * repository, used by the screen's "Clear" confirmation dialog.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val continueWatching: StateFlow<List<PlaybackProgress>> =
        historyRepository.observeContinueWatching(limit = 20)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val recent: StateFlow<List<HistoryEntity>> =
        historyRepository.observeRecent(limit = 30)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun clearAll() {
        viewModelScope.launch { historyRepository.clearAll() }
    }
}

/**
 * HistoryScreen — the user's full watch history.
 *
 * Layout (top to bottom):
 *   1. [CosmicBackground] — animated deep-space backdrop.
 *   2. Top bar — back button ([PLUTOIconCircle] with [PlutoIcons.Back]),
 *      "Watch History" title, and a "Clear" button (only when there is
 *      any history to clear).
 *   3. When both [continueWatching] and [recent] are empty → [EmptyState]
 *      inviting the user to start watching.
 *   4. Otherwise a single [LazyVerticalGrid] (2 columns) drives the whole
 *      body. Full-span rows are used for the "Continue Watching" section
 *      header + [LazyRow] of in-progress cards, and the "Recently Watched"
 *      header; grid cells below render [PlutoPosterCard]s for each
 *      [HistoryEntity].
 *
 * Interactions:
 *   - Tap a Continue Watching card → [onOpenMovie] / [onOpenSeries]
 *     based on [PlaybackProgress.contentType].
 *   - Tap a Recently Watched card → [onOpenMovie] / [onOpenSeries]
 *     based on [HistoryEntity.contentType].
 *   - Tap "Clear" → confirmation [AlertDialog] that calls
 *     [HistoryViewModel.clearAll].
 *
 * Bottom content padding is 120dp so the last grid row clears the
 * floating navigation bar (per the master spec's "FLOATING NAVIGATION"
 * section).
 */
@Composable
fun HistoryScreen(
    onOpenMovie: (Int) -> Unit,
    onOpenSeries: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val continueWatching by viewModel.continueWatching.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()

    var showClearAllDialog by remember { mutableStateOf(false) }

    val hasAnyHistory = continueWatching.isNotEmpty() || recent.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        CosmicBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            HistoryTopBar(
                onBack = onBack,
                onClearAll = { showClearAllDialog = true },
                clearAllEnabled = hasAnyHistory
            )

            if (!hasAnyHistory) {
                EmptyState(
                    title = "Nothing here yet",
                    message = "Movies and series you watch will appear here.",
                    icon = PlutoIcons.History
                )
            } else {
                HistoryBody(
                    continueWatching = continueWatching,
                    recent = recent,
                    onOpenMovie = onOpenMovie,
                    onOpenSeries = onOpenSeries
                )
            }
        }
    }

    // ── Clear all confirmation ─────────────────────────────────────────
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = {
                Text(
                    text = "Clear watch history?",
                    style = PLUTOTypography.headlineLarge,
                    color = PLUTOColors.FrostWhite
                )
            },
            text = {
                Text(
                    text = "This removes your full watch history and resume " +
                        "progress. This can't be undone.",
                    style = PLUTOTypography.bodyMedium,
                    color = PLUTOColors.IceBlue
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    showClearAllDialog = false
                }) {
                    Text(
                        text = "Clear All",
                        style = PLUTOTypography.bodyMedium,
                        color = PLUTOColors.Danger
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text(
                        text = "Cancel",
                        style = PLUTOTypography.bodyMedium,
                        color = PLUTOColors.IceBlue
                    )
                }
            }
        )
    }
}

// ── Top bar ──────────────────────────────────────────────────────────────

/**
 * HistoryTopBar — back button + "Watch History" title + clear-all button.
 *
 * The clear-all button only renders when there is any history to clear,
 * so the empty state never shows a dangling action. A spacer reserves
 * its width so the title stays centered when the action is hidden.
 */
@Composable
private fun HistoryTopBar(
    onBack: () -> Unit,
    onClearAll: () -> Unit,
    clearAllEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PLUTOIconCircle(
            icon = PlutoIcons.Back,
            onClick = onBack,
            contentDescription = "Back",
            tint = PLUTOColors.FrostWhite,
            background = PLUTOColors.Glass2
        )

        Text(
            text = "Watch History",
            style = PLUTOTypography.headlineLarge,
            color = PLUTOColors.FrostWhite
        )

        if (clearAllEnabled) {
            PLUTOOutlinedButton(
                text = "Clear",
                onClick = onClearAll
            )
        } else {
            // Reserve space so the title stays centered when the action is hidden.
            Box(modifier = Modifier)
        }
    }
}

// ── Body (single scrollable grid) ────────────────────────────────────────

/**
 * HistoryBody — the full scrollable body of [HistoryScreen].
 *
 * A single [LazyVerticalGrid] drives the whole body so both sections
 * share one scroll axis. Full-span rows are used for:
 *   - "Continue Watching" header + horizontal [LazyRow] of cards.
 *   - "Recently Watched" header.
 *
 * Below the Recently Watched header, each [HistoryEntity] renders in a
 * 2-column grid cell as a [PlutoPosterCard].
 *
 * [BoxWithConstraints] computes the exact column width so each card
 * fills its cell — [PlutoPosterCard] takes a fixed [Int] width rather
 * than `fillMaxWidth`, so we calculate it from the available space
 * minus padding and spacing.
 */
@Composable
private fun HistoryBody(
    continueWatching: List<PlaybackProgress>,
    recent: List<HistoryEntity>,
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
                start = horizontalPadding,
                end = horizontalPadding,
                top = 8.dp,
                bottom = 120.dp // clears the floating nav bar
            ),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Section 1: Continue Watching ───────────────────────────
            if (continueWatching.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(
                        title = "Continue Watching",
                        subtitle = "Pick up where you left off"
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 0.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        lazyRowItems(
                            items = continueWatching,
                            key = { progress ->
                                "${progress.contentType}-${progress.contentId}-" +
                                    "${progress.episodeId ?: 0}"
                            }
                        ) { progress ->
                            ContinueWatchingCard(
                                progress = progress,
                                onClick = {
                                    if (progress.contentType == "movie") {
                                        onOpenMovie(progress.contentId)
                                    } else {
                                        onOpenSeries(progress.contentId)
                                    }
                                }
                            )
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // ── Section 2: Recently Watched ────────────────────────────
            if (recent.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(
                        title = "Recently Watched",
                        subtitle = "${recent.size} item${if (recent.size == 1) "" else "s"}"
                    )
                }
                items(
                    items = recent,
                    key = { entity ->
                        "${entity.contentType}-${entity.itemId}-" +
                            "${entity.episodeId ?: 0}-${entity.rowId}"
                    }
                ) { entity ->
                    PlutoPosterCard(
                        title = entity.title,
                        imageUrl = entity.image,
                        isSeries = entity.contentType == "series",
                        onClick = {
                            if (entity.contentType == "movie") {
                                onOpenMovie(entity.itemId)
                            } else {
                                onOpenSeries(entity.itemId)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        cardWidth = cardWidth
                    )
                }
            }
        }
    }
}

// ── Section header ───────────────────────────────────────────────────────

/**
 * SectionHeader — title + optional subtitle, matching the design
 * language of [com.pluto.feature.home.HomeScreen]'s section headers.
 */
@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = PLUTOTypography.headlineLarge,
            color = PLUTOColors.FrostWhite
        )
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

// ── Continue Watching card ───────────────────────────────────────────────

/**
 * ContinueWatchingCard — a 200dp × 112dp landscape resume card.
 *
 * Mirrors the [com.pluto.feature.home.HomeScreen]'s `ContinueWatchingCard`:
 * a NavyDrift surface with an ElectricBlue→DeepSpace gradient backdrop, a
 * centered play glyph, a 3dp progress bar along the bottom, and a two-line
 * caption (label + "X% watched") below the thumbnail.
 *
 * The progress entity doesn't carry a poster image, so the card uses the
 * cosmic gradient as its visual surface — the percent + play glyph carry
 * enough meaning for the user to recognize a resume affordance.
 */
@Composable
private fun ContinueWatchingCard(
    progress: PlaybackProgress,
    onClick: () -> Unit
) {
    val percent = if (progress.durationMs > 0) {
        (progress.positionMs.toFloat() / progress.durationMs).coerceIn(0f, 1f)
    } else 0f

    val label = if (progress.contentType == "movie") {
        "Movie"
    } else {
        "Episode ${progress.episodeId ?: ""}".trim()
    }

    Column(
        modifier = Modifier
            .width(200.dp)
            .clip(RoundedCornerShape(PLUTOShapes.medium))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(PLUTOShapes.medium))
                .background(PLUTOColors.NavyDrift)
        ) {
            // Cosmic gradient backdrop (the progress entity has no image).
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
            // Progress bar at the bottom of the thumbnail.
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
            text = label,
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
