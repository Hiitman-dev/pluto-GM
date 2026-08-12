package com.pluto.feature.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.pluto.core.data.FavoritesRepository
import com.pluto.core.designsystem.CosmicBackground
import com.pluto.core.designsystem.EmptyState
import com.pluto.core.designsystem.PLUTOColors
import com.pluto.core.designsystem.PLUTOIconCircle
import com.pluto.core.designsystem.PLUTOOutlinedButton
import com.pluto.core.designsystem.PLUTOTypography
import com.pluto.core.designsystem.PlutoIcons
import com.pluto.core.designsystem.PlutoPosterCard
import com.pluto.core.model.FavoriteItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * FavoritesViewModel — drives [FavoritesScreen].
 *
 * Exposes the user's saved favorites reactively from
 * [FavoritesRepository.observeAll]. Because the underlying Flow re-emits
 * whenever the Room `favorites` table changes, the UI updates instantly
 * when a favorite is added (from DetailsScreen) or removed (from here).
 *
 * Mutations:
 *   - [removeFavorite] drops a single [FavoriteItem].
 *   - [clearAll] wipes the whole library.
 *
 * Both are fire-and-forget — the Flow re-emits the new state and the UI
 * re-renders. No separate "loading" state is needed.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    val favorites: StateFlow<List<FavoriteItem>> =
        favoritesRepository.observeAll()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun removeFavorite(item: FavoriteItem) {
        viewModelScope.launch { favoritesRepository.remove(item.type, item.id) }
    }

    fun clearAll() {
        viewModelScope.launch { favoritesRepository.clearAll() }
    }
}

/**
 * FavoritesScreen — the user's saved library.
 *
 * Layout (top to bottom):
 *   1. [CosmicBackground] — animated deep-space backdrop.
 *   2. Top bar — back button ([PLUTOIconCircle] with [PlutoIcons.Back]),
 *      "Library" title, and a "Clear" button (only when favorites exist).
 *   3. When empty → [EmptyState] inviting the user to save favorites.
 *   4. When non-empty → [LazyVerticalGrid] (2 columns) of [PlutoPosterCard].
 *
 * Interactions:
 *   - Tap a card → [onOpenMovie] / [onOpenSeries] based on [FavoriteItem.type].
 *   - Long-press a card → confirmation [AlertDialog] to remove that favorite.
 *   - Tap "Clear" → confirmation [AlertDialog] to clear the whole library.
 *
 * No loading state is rendered — the [favorites] Flow is reactive and
 * starts with `emptyList()`, so the empty state briefly shows on first
 * composition before Room emits the cached rows. This is intentional and
 * matches the spec's "no spinner for local data" guidance.
 */
@Composable
fun FavoritesScreen(
    onOpenMovie: (Int) -> Unit,
    onOpenSeries: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    var itemToRemove by remember { mutableStateOf<FavoriteItem?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        CosmicBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            FavoritesTopBar(
                onBack = onBack,
                onClearAll = { showClearAllDialog = true },
                clearAllEnabled = favorites.isNotEmpty()
            )

            if (favorites.isEmpty()) {
                EmptyState(
                    title = "No favorites yet",
                    message = "Save movies you love\nand we'll keep them here.",
                    icon = PlutoIcons.Favorite
                )
            } else {
                FavoritesGrid(
                    favorites = favorites,
                    onOpenMovie = onOpenMovie,
                    onOpenSeries = onOpenSeries,
                    onLongPress = { item -> itemToRemove = item }
                )
            }
        }
    }

    // ── Remove single favorite confirmation ────────────────────────────
    itemToRemove?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToRemove = null },
            title = {
                Text(
                    text = "Remove favorite?",
                    style = PLUTOTypography.headlineLarge,
                    color = PLUTOColors.FrostWhite
                )
            },
            text = {
                Text(
                    text = "“${item.title}” will be removed from your library. " +
                        "You can always favorite it again from its detail page.",
                    style = PLUTOTypography.bodyMedium,
                    color = PLUTOColors.IceBlue
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeFavorite(item)
                    itemToRemove = null
                }) {
                    Text(
                        text = "Remove",
                        style = PLUTOTypography.bodyMedium,
                        color = PLUTOColors.Danger
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRemove = null }) {
                    Text(
                        text = "Cancel",
                        style = PLUTOTypography.bodyMedium,
                        color = PLUTOColors.IceBlue
                    )
                }
            }
        )
    }

    // ── Clear all confirmation ─────────────────────────────────────────
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = {
                Text(
                    text = "Clear all favorites?",
                    style = PLUTOTypography.headlineLarge,
                    color = PLUTOColors.FrostWhite
                )
            },
            text = {
                Text(
                    text = "This removes all ${favorites.size} favorites from " +
                        "your library. This can't be undone.",
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
 * FavoritesTopBar — back button + "Library" title + clear-all button.
 *
 * The clear-all button only renders when there is at least one favorite
 * to clear, so the empty state never shows a dangling action.
 */
@Composable
private fun FavoritesTopBar(
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
            text = "Library",
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

// ── Grid ─────────────────────────────────────────────────────────────────

/**
 * FavoritesGrid — 2-column [LazyVerticalGrid] of [PlutoPosterCard].
 *
 * [BoxWithConstraints] computes the exact column width so each card fills
 * its cell — [PlutoPosterCard] takes a fixed [Int] width rather than
 * `fillMaxWidth`, so we calculate it from the available space minus
 * padding and spacing.
 *
 * Bottom content padding is 120dp so the last row clears the floating
 * navigation bar (per the master spec's "FLOATING NAVIGATION" section).
 */
@Composable
private fun FavoritesGrid(
    favorites: List<FavoriteItem>,
    onOpenMovie: (Int) -> Unit,
    onOpenSeries: (Int) -> Unit,
    onLongPress: (FavoriteItem) -> Unit,
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
            items(
                items = favorites,
                key = { item -> "${item.type}-${item.id}" }
            ) { item ->
                PlutoPosterCard(
                    title = item.title,
                    imageUrl = item.image,
                    year = item.year,
                    rating = item.imdb,
                    isSeries = item.type == "series",
                    qualityLabel = item.sources.firstOrNull()?.quality,
                    onClick = {
                        if (item.type == "movie") onOpenMovie(item.id)
                        else onOpenSeries(item.id)
                    },
                    onLongClick = { onLongPress(item) },
                    modifier = Modifier.fillMaxWidth(),
                    cardWidth = cardWidth
                )
            }
        }
    }
}
