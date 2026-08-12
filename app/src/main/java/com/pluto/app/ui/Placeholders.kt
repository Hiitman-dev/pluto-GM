package com.pluto.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pluto.core.designsystem.PLUTOColors
import com.pluto.core.designsystem.PLUTOTypography

// Feature screen placeholders — the actual implementations live in
// feature/* modules and are wired in by the app's NavHost. These stubs
// exist so the app compiles standalone for design system preview.

@Composable
internal fun HomePlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("PLUTO · Home", style = PLUTOTypography.displayMedium, color = PLUTOColors.FrostWhite)
    }
}

@Composable internal fun SearchPlaceholder() =
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("PLUTO · Discover", style = PLUTOTypography.displayMedium, color = PLUTOColors.FrostWhite)
    }

@Composable internal fun DownloadsPlaceholder() =
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("PLUTO · Downloads", style = PLUTOTypography.displayMedium, color = PLUTOColors.FrostWhite)
    }

@Composable internal fun FavoritesPlaceholder() =
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("PLUTO · Library", style = PLUTOTypography.displayMedium, color = PLUTOColors.FrostWhite)
    }

@Composable internal fun HistoryPlaceholder() =
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("PLUTO · History", style = PLUTOTypography.displayMedium, color = PLUTOColors.FrostWhite)
    }

@Composable internal fun NotificationsPlaceholder() =
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("PLUTO · Notifications", style = PLUTOTypography.displayMedium, color = PLUTOColors.FrostWhite)
    }

@Composable internal fun SettingsPlaceholder() =
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("PLUTO · Settings", style = PLUTOTypography.displayMedium, color = PLUTOColors.FrostWhite)
    }

@Composable internal fun MovieDetailsPlaceholder(movieId: Int) =
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("PLUTO · Movie $movieId", style = PLUTOTypography.displayMedium, color = PLUTOColors.FrostWhite)
    }

@Composable internal fun SeriesDetailsPlaceholder(seriesId: Int) =
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("PLUTO · Series $seriesId", style = PLUTOTypography.displayMedium, color = PLUTOColors.FrostWhite)
    }
