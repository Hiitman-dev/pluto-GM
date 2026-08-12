package com.pluto.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.pluto.core.data.SettingsRepository
import com.pluto.core.designsystem.CosmicBackground
import com.pluto.core.designsystem.GlassCard
import com.pluto.core.designsystem.GlassLevel
import com.pluto.core.designsystem.PLUTOColors
import com.pluto.core.designsystem.PLUTOIconCircle
import com.pluto.core.designsystem.PLUTOShapes
import com.pluto.core.designsystem.PLUTOTypography
import com.pluto.core.designsystem.PlutoIcons
import com.pluto.core.model.DownloadSettings
import com.pluto.core.model.NotificationSettings
import com.pluto.core.model.SubtitleSettings
import com.pluto.core.model.VideoPlayerSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Quality options for the playback default-quality dropdown.
 * Stored value (first) is what SettingsRepository persists.
 */
private val PLAYBACK_QUALITY_OPTIONS = listOf(
    "auto" to "Auto",
    "1080p" to "1080p",
    "720p" to "720p",
    "480p" to "480p"
)

/** Quality options for the downloads default-quality dropdown. */
private val DOWNLOAD_QUALITY_OPTIONS = PLAYBACK_QUALITY_OPTIONS

/** Speed options for the playback default-speed dropdown. */
private val SPEED_OPTIONS = listOf(
    0.5f to "0.5x",
    1.0f to "1x",
    1.25f to "1.25x",
    1.5f to "1.5x",
    2.0f to "2x"
)

/**
 * SettingsViewModel — exposes PLUTO preferences as StateFlows.
 *
 * Each settings group is observed from [SettingsRepository] and shared
 * via [stateIn] with [SharingStarted.WhileSubscribed] (5s grace period so
 * brief configuration changes don't cancel the upstream Flow). Saves
 * are fire-and-forget on [viewModelScope] — DataStore serializes writes,
 * so the UI simply re-collects the new value after the write lands.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val videoPlayerSettings: StateFlow<VideoPlayerSettings> =
        settingsRepository.observeVideoPlayerSettings()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                VideoPlayerSettings()
            )

    val subtitleSettings: StateFlow<SubtitleSettings> =
        settingsRepository.observeSubtitleSettings()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                SubtitleSettings()
            )

    val downloadSettings: StateFlow<DownloadSettings> =
        settingsRepository.observeDownloadSettings()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                DownloadSettings()
            )

    val notificationSettings: StateFlow<NotificationSettings> =
        settingsRepository.observeNotificationSettings()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                NotificationSettings()
            )

    fun saveVideoPlayerSettings(settings: VideoPlayerSettings) {
        viewModelScope.launch { settingsRepository.saveVideoPlayerSettings(settings) }
    }

    fun saveSubtitleSettings(settings: SubtitleSettings) {
        viewModelScope.launch { settingsRepository.saveSubtitleSettings(settings) }
    }

    fun saveDownloadSettings(settings: DownloadSettings) {
        viewModelScope.launch { settingsRepository.saveDownloadSettings(settings) }
    }

    fun saveNotificationSettings(settings: NotificationSettings) {
        viewModelScope.launch { settingsRepository.saveNotificationSettings(settings) }
    }
}

/**
 * SettingsScreen — PLUTO preferences.
 *
 * Layout:
 *   - CosmicBackground backdrop
 *   - LazyColumn (statusBarsPadding, bottom 120dp to clear nav bar)
 *   - Sticky-style top bar with PLUTOIconCircle back button + "Settings" title
 *   - Five sections, each a GlassCard:
 *       1. Playback  — autoplay toggle, default quality, seek seconds, default speed
 *       2. Subtitles — text size, text/border color chips
 *       3. Downloads — Wi-Fi only, default quality, simultaneous, auto retry
 *       4. Notifications — new episodes / sound / vibration / grouping
 *       5. About — version, tagline, tech stack
 *
 * All changes are reactive: there is no save button. Each control writes
 * through to DataStore immediately, and the UI re-collects the new state.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val videoPlayer by viewModel.videoPlayerSettings.collectAsStateWithLifecycle()
    val subtitle by viewModel.subtitleSettings.collectAsStateWithLifecycle()
    val downloads by viewModel.downloadSettings.collectAsStateWithLifecycle()
    val notifications by viewModel.notificationSettings.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        CosmicBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top bar
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
                        text = "Settings",
                        style = PLUTOTypography.displaySmall,
                        color = PLUTOColors.FrostWhite
                    )
                }
            }

            // ── Playback ────────────────────────────────────────────────
            item {
                SettingsSection(
                    title = "Playback",
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    ToggleRow(
                        label = "Autoplay Next Episode",
                        checked = videoPlayer.autoPlayNext,
                        onCheckedChange = { v ->
                            viewModel.saveVideoPlayerSettings(
                                videoPlayer.copy(autoPlayNext = v)
                            )
                        }
                    )
                    DropdownRow(
                        label = "Default Quality",
                        options = PLAYBACK_QUALITY_OPTIONS,
                        currentValue = videoPlayer.defaultQuality,
                        onSelect = { v ->
                            viewModel.saveVideoPlayerSettings(
                                videoPlayer.copy(defaultQuality = v)
                            )
                        }
                    )
                    SliderRow(
                        label = "Seek Seconds",
                        value = videoPlayer.seekSeconds.toFloat(),
                        valueRange = 5f..30f,
                        steps = 24,
                        valueFormatter = { "${it.toInt()}s" },
                        onValueChangeFinished = { v ->
                            viewModel.saveVideoPlayerSettings(
                                videoPlayer.copy(seekSeconds = v.toInt())
                            )
                        }
                    )
                    DropdownRow(
                        label = "Default Speed",
                        options = SPEED_OPTIONS,
                        currentValue = videoPlayer.defaultSpeed,
                        onSelect = { v ->
                            viewModel.saveVideoPlayerSettings(
                                videoPlayer.copy(defaultSpeed = v)
                            )
                        }
                    )
                }
            }

            // ── Subtitles ───────────────────────────────────────────────
            item {
                SettingsSection(
                    title = "Subtitles",
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    SliderRow(
                        label = "Text Size",
                        value = subtitle.textSize,
                        valueRange = 12f..24f,
                        steps = 11,
                        valueFormatter = { "${it.toInt()}sp" },
                        onValueChangeFinished = { v ->
                            viewModel.saveSubtitleSettings(
                                subtitle.copy(textSize = v)
                            )
                        }
                    )
                    ColorChipRow(
                        label = "Text Color",
                        colorLong = subtitle.textColor
                    )
                    ColorChipRow(
                        label = "Border Color",
                        colorLong = subtitle.borderColor,
                        isLast = true
                    )
                }
            }

            // ── Downloads ───────────────────────────────────────────────
            item {
                SettingsSection(
                    title = "Downloads",
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    ToggleRow(
                        label = "Wi-Fi Only",
                        checked = downloads.wifiOnly,
                        onCheckedChange = { v ->
                            viewModel.saveDownloadSettings(
                                downloads.copy(wifiOnly = v)
                            )
                        }
                    )
                    DropdownRow(
                        label = "Default Quality",
                        options = DOWNLOAD_QUALITY_OPTIONS,
                        currentValue = downloads.defaultQuality,
                        onSelect = { v ->
                            viewModel.saveDownloadSettings(
                                downloads.copy(defaultQuality = v)
                            )
                        }
                    )
                    SliderRow(
                        label = "Simultaneous Downloads",
                        value = downloads.simultaneousDownloads.toFloat(),
                        valueRange = 1f..4f,
                        steps = 2,
                        valueFormatter = { it.toInt().toString() },
                        onValueChangeFinished = { v ->
                            viewModel.saveDownloadSettings(
                                downloads.copy(simultaneousDownloads = v.toInt())
                            )
                        }
                    )
                    ToggleRow(
                        label = "Auto Retry",
                        checked = downloads.autoRetry,
                        onCheckedChange = { v ->
                            viewModel.saveDownloadSettings(
                                downloads.copy(autoRetry = v)
                            )
                        },
                        isLast = true
                    )
                }
            }

            // ── Notifications ───────────────────────────────────────────
            item {
                SettingsSection(
                    title = "Notifications",
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    ToggleRow(
                        label = "New Episodes",
                        checked = notifications.newEpisodesEnabled,
                        onCheckedChange = { v ->
                            viewModel.saveNotificationSettings(
                                notifications.copy(newEpisodesEnabled = v)
                            )
                        }
                    )
                    ToggleRow(
                        label = "Sound",
                        checked = notifications.sound,
                        onCheckedChange = { v ->
                            viewModel.saveNotificationSettings(
                                notifications.copy(sound = v)
                            )
                        }
                    )
                    ToggleRow(
                        label = "Vibration",
                        checked = notifications.vibration,
                        onCheckedChange = { v ->
                            viewModel.saveNotificationSettings(
                                notifications.copy(vibration = v)
                            )
                        }
                    )
                    ToggleRow(
                        label = "Grouping",
                        checked = notifications.grouping,
                        onCheckedChange = { v ->
                            viewModel.saveNotificationSettings(
                                notifications.copy(grouping = v)
                            )
                        },
                        isLast = true
                    )
                }
            }

            // ── About ───────────────────────────────────────────────────
            item {
                SettingsSection(
                    title = "About",
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    ReadOnlyRow(label = "App Version", value = "1.0.0")
                    ReadOnlyRow(label = "Tagline", value = "PLUTO — Cinema Beyond Earth")
                    ReadOnlyRow(
                        label = "Built With",
                        value = "Kotlin, Jetpack Compose, Media3",
                        isLast = true
                    )
                }
            }
        }
    }
}

// ── Section container ────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = PLUTOTypography.labelMono,
            color = PLUTOColors.IceBlue,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glassLevel = GlassLevel.Glass2,
            shape = RoundedCornerShape(PLUTOShapes.large)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                content()
            }
        }
    }
}

// ── Row primitives ───────────────────────────────────────────────────────

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = PLUTOTypography.bodyLarge,
            color = PLUTOColors.FrostWhite,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PLUTOColors.FrostWhite,
                checkedTrackColor = PLUTOColors.ElectricBlue,
                checkedBorderColor = PLUTOColors.GlowBlue,
                uncheckedThumbColor = PLUTOColors.IceBlue,
                uncheckedTrackColor = PLUTOColors.NavyDrift,
                uncheckedBorderColor = PLUTOColors.GlassBorder
            )
        )
    }
    if (!isLast) SettingsDivider()
}

@Composable
private fun <T> DropdownRow(
    label: String,
    options: List<Pair<T, String>>,
    currentValue: T,
    onSelect: (T) -> Unit,
    isLast: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.firstOrNull { it.first == currentValue }?.second
        ?: options.firstOrNull()?.second
        ?: "—"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = PLUTOTypography.bodyLarge,
            color = PLUTOColors.FrostWhite,
            modifier = Modifier.weight(1f)
        )
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(PLUTOShapes.pill))
                    .background(PLUTOColors.Glass3)
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = currentLabel,
                    style = PLUTOTypography.bodyMedium,
                    color = PLUTOColors.FrostWhite
                )
                Icon(
                    imageVector = PlutoIcons.ChevronDown,
                    contentDescription = null,
                    tint = PLUTOColors.IceBlue,
                    modifier = Modifier.size(14.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                style = PLUTOTypography.bodyMedium,
                                color = if (value == currentValue) {
                                    PLUTOColors.GlowBlue
                                } else {
                                    PLUTOColors.FrostWhite
                                }
                            )
                        },
                        onClick = {
                            onSelect(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
    if (!isLast) SettingsDivider()
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueFormatter: (Float) -> String,
    onValueChangeFinished: (Float) -> Unit,
    isLast: Boolean = false
) {
    // Re-seed local state when the persisted value changes (after save).
    var localValue by remember(value) { mutableStateOf(value) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = PLUTOTypography.bodyLarge,
                color = PLUTOColors.FrostWhite,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueFormatter(localValue),
                style = PLUTOTypography.metadataMono,
                color = PLUTOColors.IceBlue
            )
        }
        Slider(
            value = localValue,
            onValueChange = { localValue = it },
            onValueChangeFinished = { onValueChangeFinished(localValue) },
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = PLUTOColors.GlowBlue,
                activeTrackColor = PLUTOColors.ElectricBlue,
                inactiveTrackColor = PLUTOColors.NavyDrift,
                activeTickColor = PLUTOColors.IceBlue,
                inactiveTickColor = PLUTOColors.MutedStar
            )
        )
    }
    if (!isLast) SettingsDivider()
}

@Composable
private fun ColorChipRow(
    label: String,
    colorLong: Long,
    isLast: Boolean = false
) {
    // Long is stored as packed 0xAARRGGBB. Color(Long) preserves the bits
    // verbatim, so alpha=0x80 stays at 50% — Color(Long) does not auto-set
    // alpha for values that happen to fit in 32 bits.
    val color = Color(colorLong)
    val hex = colorLong.toString(16).padStart(8, '0').uppercase()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = PLUTOTypography.bodyLarge,
            color = PLUTOColors.FrostWhite,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "#$hex",
                style = PLUTOTypography.metadataMono,
                color = PLUTOColors.IceBlue
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
                    .border(
                        width = 1.dp,
                        color = PLUTOColors.GlassBorderActive,
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
    if (!isLast) SettingsDivider()
}

@Composable
private fun ReadOnlyRow(
    label: String,
    value: String,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = PLUTOTypography.bodyLarge,
            color = PLUTOColors.FrostWhite,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = PLUTOTypography.bodyMedium,
            color = PLUTOColors.IceBlue,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
    if (!isLast) SettingsDivider()
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(1.dp)
            .background(PLUTOColors.GlassBorder.copy(alpha = 0.4f))
    )
}
