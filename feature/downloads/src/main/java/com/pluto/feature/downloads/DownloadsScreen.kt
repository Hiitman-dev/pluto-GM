package com.pluto.feature.downloads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pluto.core.designsystem.PLUTOColors
import com.pluto.core.designsystem.PLUTOTypography

/**
 * DownloadsScreen — feature module entry point.
 *
 * The full implementation of this screen lives in this module. The app's
 * NavHost wires it into the navigation graph via [com.pluto.core.navigation.PlutoRoute].
 *
 * Per the master spec, this screen implements:
 *   - PLUTO design system (cosmic background, glass cards, custom icons)
 *   - Real API data via the corresponding ViewModel
 *   - Loading / Empty / Error states with PLUTO shimmer + SIGNAL LOST
 *   - Accessibility (contentDescription, 48dp touch targets, semantic roles)
 *
 * NOTE: This is a stub placeholder so the module compiles. The full
 * Compose UI is implemented in the follow-up commits per the Phase 7
 * plan in the README. The app module's placeholders currently render
 * the design system for preview.
 */
@Composable
fun DownloadsScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "PLUTO · Downloads",
            style = PLUTOTypography.displayMedium,
            color = PLUTOColors.FrostWhite
        )
    }
}
