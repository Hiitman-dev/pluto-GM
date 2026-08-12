package com.pluto.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.pluto.core.database.entity.NewEpisodeNotificationEntity
import com.pluto.core.designsystem.CosmicBackground
import com.pluto.core.designsystem.EmptyState
import com.pluto.core.designsystem.GlassCard
import com.pluto.core.designsystem.GlassLevel
import com.pluto.core.designsystem.PLUTOButton
import com.pluto.core.designsystem.PLUTOColors
import com.pluto.core.designsystem.PLUTOIconCircle
import com.pluto.core.designsystem.PLUTOShapes
import com.pluto.core.designsystem.PLUTOTypography
import com.pluto.core.designsystem.PlutoIcons
import com.pluto.core.notifications.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * NotificationsViewModel — drives [NotificationsScreen].
 *
 * Exposes the most-recent new-episode notifications (capped at 50) and
 * the current unread badge count. Tapping a notification marks it
 * opened (clearing its unread glow + the badge) before navigating to
 * the series. The user can bulk-clear the entire list with [clearAll].
 *
 * Per Section 55 ("FUTURE FCM") of the master spec: this ViewModel is
 * intentionally unaware of *how* notifications arrive — the
 * [NotificationRepository] is the seam where polling could be swapped
 * for FCM without touching this screen.
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val notifications: StateFlow<List<NewEpisodeNotificationEntity>> =
        notificationRepository.observeRecentNotifications(50)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val unreadCount: StateFlow<Int> =
        notificationRepository.observeUnreadCount()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

    fun markOpened(entity: NewEpisodeNotificationEntity) {
        viewModelScope.launch {
            notificationRepository.markOpened(entity.deduplicationKey)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            notificationRepository.clearAllNotifications()
        }
    }
}

/**
 * NotificationsScreen — the center for "new episode" alerts.
 *
 * Layout (top to bottom):
 *   1. [CosmicBackground] — animated deep-space backdrop.
 *   2. Top bar — back button, "Notifications" title, clear-all button
 *      (only shown when there are notifications to clear).
 *   3. [LazyColumn] of glass cards, one per [NewEpisodeNotificationEntity].
 *
 * Card states:
 *   - Unread (openedAt == null): brighter glass ([GlassLevel.Glass3]),
 *     a glowing 4dp [PLUTOColors.GlowBlue] strip on the left edge, and
 *     full-opacity text.
 *   - Read (openedAt != null): dimmed to 55% alpha — visually quieter
 *     so the unread items win attention.
 *
 * Tapping anywhere on a card (or its "Watch" button) calls
 * [NotificationsViewModel.markOpened] and dispatches [onOpenSeries]
 * with the notification's seriesId. A confirmation [AlertDialog] guards
 * the "Clear all" action so an accidental tap can't wipe the list.
 *
 * The list has 120dp of bottom content padding so the last card clears
 * the floating navigation pill (Section 58 / PLUTOFloatingNavigation).
 */
@Composable
fun NotificationsScreen(
    onOpenSeries: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()

    var showClearDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        CosmicBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            NotificationsTopBar(
                onBack = onBack,
                onClearAll = { showClearDialog = true },
                showClearAll = notifications.isNotEmpty()
            )

            if (notifications.isEmpty()) {
                EmptyState(
                    title = "All caught up",
                    message = "New episode notifications for followed " +
                        "series will appear here.",
                    icon = PlutoIcons.Notification
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = notifications,
                        key = { it.deduplicationKey }
                    ) { entity ->
                        NotificationCard(
                            entity = entity,
                            onOpen = {
                                viewModel.markOpened(entity)
                                onOpenSeries(entity.seriesId)
                            }
                        )
                    }
                }
            }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = {
                    Text(
                        text = "Clear all notifications?",
                        style = PLUTOTypography.headlineLarge,
                        color = PLUTOColors.FrostWhite
                    )
                },
                text = {
                    Text(
                        text = "This removes every notification in the " +
                            "list. You can't undo this action.",
                        style = PLUTOTypography.bodyMedium,
                        color = PLUTOColors.IceBlue
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAll()
                            showClearDialog = false
                        }
                    ) {
                        Text(
                            text = "Clear All",
                            style = PLUTOTypography.bodyMedium,
                            color = PLUTOColors.Danger
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text(
                            text = "Keep",
                            style = PLUTOTypography.bodyMedium,
                            color = PLUTOColors.IceBlue
                        )
                    }
                },
                containerColor = PLUTOColors.DeepSpace,
                titleContentColor = PLUTOColors.FrostWhite,
                textContentColor = PLUTOColors.IceBlue
            )
        }
    }
}

// ── Top bar ──────────────────────────────────────────────────────────────

/**
 * NotificationsTopBar — back button, "Notifications" title, clear-all.
 *
 * The clear-all control only renders when there's something to clear;
 * an empty list leaves the bar quiet. Matches the cosmic-icon pattern
 * used by [com.pluto.feature.search.SearchScreen]'s top bar (circular
 * glass-backed icons, frosted-white tint for primary action, ice-blue
 * for secondary).
 */
@Composable
private fun NotificationsTopBar(
    onBack: () -> Unit,
    onClearAll: () -> Unit,
    showClearAll: Boolean,
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

        Text(
            text = "Notifications",
            style = PLUTOTypography.headlineLarge,
            color = PLUTOColors.FrostWhite,
            modifier = Modifier.weight(1f)
        )

        if (showClearAll) {
            PLUTOIconCircle(
                icon = PlutoIcons.Close,
                onClick = onClearAll,
                contentDescription = "Clear all notifications",
                tint = PLUTOColors.IceBlue,
                background = PLUTOColors.Glass2
            )
        }
    }
}

// ── Notification card ────────────────────────────────────────────────────

/**
 * NotificationCard — a single new-episode alert.
 *
 * Layout: an outer [GlassCard] wraps a Row whose first item is a 4dp
 * glowing strip (unread only), then a column of
 *   - series title ([PLUTOTypography.headlineMedium])
 *   - "Season X · Episode Y" ([PLUTOTypography.metadataMono] — the
 *     episode-code mono per Section 43)
 *   - "detected Xm ago" relative time
 * and a "Watch" [PLUTOButton] on the trailing edge.
 *
 * The whole card is clickable; the Watch button shares the same
 * [onOpen] handler so tapping either surface marks the notification
 * opened and navigates to the series.
 */
@Composable
private fun NotificationCard(
    entity: NewEpisodeNotificationEntity,
    onOpen: () -> Unit
) {
    val isUnread = entity.openedAt == null
    val cardAlpha = if (isUnread) 1f else 0.55f
    val relativeTime = remember(entity.detectedAt) {
        formatRelativeTime(entity.detectedAt)
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        glassLevel = if (isUnread) GlassLevel.Glass3 else GlassLevel.Glass2
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .alpha(cardAlpha)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isUnread) {
                // Unread indicator — 4dp glowing strip on the left edge.
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(PLUTOColors.GlowBlue)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entity.seriesTitle,
                    style = PLUTOTypography.headlineMedium,
                    color = PLUTOColors.FrostWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Season ${entity.seasonNumber} · " +
                        "Episode ${entity.episodeNumber}",
                    style = PLUTOTypography.metadataMono,
                    color = PLUTOColors.IceBlue,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "detected $relativeTime",
                    style = PLUTOTypography.bodySmall,
                    color = PLUTOColors.MutedStar,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            PLUTOButton(
                text = "Watch",
                onClick = onOpen,
                icon = PlutoIcons.Play
            )
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────

/**
 * Format the elapsed milliseconds since [detectedAt] as a short relative
 * time string ("Xm ago", "Xh ago", "Xd ago").
 *
 * Per the task spec, this uses
 * `TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - detectedAt)`
 * and buckets into minutes / hours / days. Sub-minute values clamp to
 * "0m ago"; anything past 24h shows in days. Intentionally simple — no
 * "just now" / "yesterday" fancy formatting, since these are background
 * notifications, not chat timestamps.
 */
private fun formatRelativeTime(detectedAt: Long): String {
    val elapsedMs = System.currentTimeMillis() - detectedAt
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMs).coerceAtLeast(0L)
    return when {
        minutes < 60L -> "${minutes}m ago"
        minutes < 24L * 60L -> "${minutes / 60L}h ago"
        else -> "${minutes / (24L * 60L)}d ago"
    }
}
