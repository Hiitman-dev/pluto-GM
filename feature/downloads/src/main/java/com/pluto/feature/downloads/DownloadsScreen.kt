package com.pluto.feature.downloads

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.pluto.core.data.SettingsRepository
import com.pluto.core.designsystem.CosmicBackground
import com.pluto.core.designsystem.EmptyState
import com.pluto.core.designsystem.GlassCard
import com.pluto.core.designsystem.GlassLevel
import com.pluto.core.designsystem.PLUTOColors
import com.pluto.core.designsystem.PLUTOIconCircle
import com.pluto.core.designsystem.PLUTOOutlinedButton
import com.pluto.core.designsystem.PLUTOTypography
import com.pluto.core.designsystem.PlutoIcons
import com.pluto.core.download.DownloadManager
import com.pluto.core.download.ExternalActionLauncher
import com.pluto.core.download.ExternalAppInfo
import com.pluto.core.model.DownloadSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * DownloadsViewModel — exposes download settings and the configured
 * external-downloader catalog to the [DownloadsScreen].
 *
 * Per Section 28 ("DOWNLOAD MANAGEMENT") of the master spec, PLUTO delegates
 * the actual file download to the user's preferred external downloader app
 * (ADM, 1DM+, etc.). The in-app [DownloadManager] (which schedules a
 * [com.pluto.core.download.VideoDownloadWorker] via WorkManager) is wired
 * up here so that future iterations — once a DownloadEntity / Room table
 * exists and the Details screen hands us a content URL — can enqueue real
 * background downloads through the same ViewModel.
 *
 * This iteration shows:
 *   - The user's download preferences (Wi-Fi only, simultaneous count,
 *     default quality, auto-retry) as read-only cards.
 *   - The list of installed external downloaders, each with an "Open"
 *     button. Tapping "Open" without a content URL just shows a toast
 *     telling the user to open a video first. The [openWithDownloader]
 *     and [openInBrowser] methods are wired through to
 *     [ExternalActionLauncher] for the future flow where a content URL is
 *     passed in from the Details screen.
 */
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    @Suppress("unused") // Wired for the future in-app download flow (see class kdoc).
    private val downloadManager: DownloadManager,
    private val externalActionLauncher: ExternalActionLauncher,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    /** Read-only download settings (DataStore-backed). */
    val downloadSettings: StateFlow<DownloadSettings> =
        settingsRepository.observeDownloadSettings()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = DownloadSettings()
            )

    /**
     * The full configured-downloader catalog (each entry carries an
     * `installed` flag). Resolved once in init; the UI filters to
     * installed-only for display.
     */
    private val _externalDownloaders = MutableStateFlow<List<ExternalAppInfo>>(emptyList())
    val externalDownloaders: StateFlow<List<ExternalAppInfo>> =
        _externalDownloaders.asStateFlow()

    init {
        // PackageManager queries are fast for ~4 packages; resolve once.
        _externalDownloaders.value = externalActionLauncher.getAllDownloaders()
    }

    /**
     * Open a content URL in the given external downloader. Called from the
     * Details screen's "Download with..." sheet once a content URL has
     * been resolved; in this iteration the DownloadsScreen itself doesn't
     * have a URL yet, so the row's "Open" button just shows a toast.
     */
    fun openWithDownloader(url: String, packageName: String) {
        externalActionLauncher.openWithDownloader(url, packageName)
    }

    /** Open a content URL in the system browser (fallback). */
    fun openInBrowser(url: String) {
        externalActionLauncher.openInBrowser(url)
    }
}

/**
 * DownloadsScreen — read-only view of download settings + installed
 * external downloaders.
 *
 * Layout (top to bottom, in a single LazyColumn so the whole screen
 * scrolls together):
 *   1. Top bar — back button + "Downloads" title (statusBarsPadding).
 *   2. "Download Settings" section — glass card with four rows:
 *        Wi-Fi only · Default quality · Simultaneous downloads · Auto retry
 *   3. "External Downloaders" section header.
 *      - If at least one downloader is installed: one glass row per
 *        app (icon, name, package name, "Open" button).
 *      - Else: `EmptyState` telling the user to install ADM / 1DM+.
 *   4. Info card — explains downloads are delegated to the external app.
 *
 * Bottom content padding = 120dp to clear the floating nav bar.
 */
@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val settings by viewModel.downloadSettings.collectAsStateWithLifecycle()
    val downloaders by viewModel.externalDownloaders.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val installedDownloaders = downloaders.filter { it.installed }

    Box(modifier = Modifier.fillMaxSize()) {
        CosmicBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Top bar ────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PLUTOIconCircle(
                        icon = PlutoIcons.Back,
                        onClick = onBack,
                        contentDescription = "Back"
                    )
                    Text(
                        text = "Downloads",
                        style = PLUTOTypography.displayMedium,
                        color = PLUTOColors.FrostWhite
                    )
                }
            }

            // ── Section 1: Download Settings ──────────────────────────
            item {
                SectionHeader(
                    title = "Download Settings",
                    subtitle = "Configured defaults for in-app downloads"
                )
            }
            item {
                SettingsCard(settings = settings)
            }

            // ── Section 2: External Downloaders ───────────────────────
            item {
                SectionHeader(
                    title = "External Downloaders",
                    subtitle = "Tap a downloader to use it for a video URL"
                )
            }
            if (installedDownloaders.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(0.55f)) {
                        EmptyState(
                            title = "No downloaders installed",
                            message = "Install ADM, 1DM+, or another downloader app to enable downloads.",
                            icon = PlutoIcons.Download
                        )
                    }
                }
            } else {
                items(
                    items = installedDownloaders,
                    key = { it.packageName }
                ) { app ->
                    DownloaderRow(
                        app = app,
                        onOpen = {
                            Toast.makeText(
                                context,
                                "Open a video and tap Download to use this app",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }

            // ── Info card ─────────────────────────────────────────────
            item {
                InfoCard()
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
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

@Composable
private fun SettingsCard(settings: DownloadSettings) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        glassLevel = GlassLevel.Glass2,
        blurEnabled = false
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            SettingRow(label = "Wi-Fi only", value = if (settings.wifiOnly) "Yes" else "No")
            SettingDivider()
            SettingRow(label = "Default quality", value = settings.defaultQuality)
            SettingDivider()
            SettingRow(
                label = "Simultaneous downloads",
                value = settings.simultaneousDownloads.toString()
            )
            SettingDivider()
            SettingRow(label = "Auto retry", value = if (settings.autoRetry) "On" else "Off")
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = PLUTOTypography.bodyMedium,
            color = PLUTOColors.IceBlue
        )
        Text(
            text = value,
            style = PLUTOTypography.bodyMedium,
            color = PLUTOColors.FrostWhite,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SettingDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(PLUTOColors.GlassBorder)
    )
}

@Composable
private fun DownloaderRow(app: ExternalAppInfo, onOpen: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        glassLevel = GlassLevel.Glass2,
        blurEnabled = false
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(PLUTOColors.Glass3),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PlutoIcons.Download,
                    contentDescription = null,
                    tint = PLUTOColors.IceBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = PLUTOTypography.bodyMedium,
                    color = PLUTOColors.FrostWhite,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = app.packageName,
                    style = PLUTOTypography.bodySmall,
                    color = PLUTOColors.MutedStar,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            PLUTOOutlinedButton(text = "Open", onClick = onOpen)
        }
    }
}

@Composable
private fun InfoCard() {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        glassLevel = GlassLevel.Glass1,
        blurEnabled = false
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = PlutoIcons.Galaxy,
                contentDescription = null,
                tint = PLUTOColors.IceBlue,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Downloads are delegated to your preferred external app. " +
                    "PLUTO does not host downloaded files in this version.",
                style = PLUTOTypography.bodySmall,
                color = PLUTOColors.MutedStar
            )
        }
    }
}
