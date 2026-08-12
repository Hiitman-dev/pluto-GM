package com.pluto.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * PLUTOTheme — Compose theme wrapper.
 *
 * Per Section 112 ("DARK THEME") of the cosmic visual spec:
 *   "Dark theme is the PRIMARY experience."
 *
 * PLUTO is always dark — there is no light theme by default. Section 113
 * ("OPTIONAL LIGHT THEME") allows a "Cosmic Dawn" variant in the future,
 * but it's not implemented yet (would require designing an entire second
 * palette per the spec: "do not simply invert dark theme").
 */
private val PlutoDarkColorScheme = darkColorScheme(
    primary = PLUTOColors.ElectricBlue,
    onPrimary = PLUTOColors.FrostWhite,
    secondary = PLUTOColors.GlowBlue,
    onSecondary = PLUTOColors.FrostWhite,
    tertiary = PLUTOColors.IceBlue,
    onTertiary = PLUTOColors.Void,
    background = PLUTOColors.Void,
    onBackground = PLUTOColors.FrostWhite,
    surface = PLUTOColors.DeepSpace,
    onSurface = PLUTOColors.FrostWhite,
    surfaceVariant = PLUTOColors.NavyDrift,
    onSurfaceVariant = PLUTOColors.IceBlue,
    error = PLUTOColors.Danger,
    onError = PLUTOColors.FrostWhite,
    outline = PLUTOColors.GlassBorder,
    outlineVariant = PLUTOColors.MutedStar,
    scrim = PLUTOColors.Void
)

@Composable
fun PLUTOTheme(
    content: @Composable () -> Unit
) {
    // Per spec: dark theme is primary. isSystemInDarkTheme is ignored —
    // PLUTO is always cosmic dark.
    MaterialTheme(
        colorScheme = PlutoDarkColorScheme,
        typography = androidx.compose.material3.Typography(
            displayLarge = PLUTOTypography.displayLarge,
            displayMedium = PLUTOTypography.displayMedium,
            displaySmall = PLUTOTypography.displaySmall,
            headlineLarge = PLUTOTypography.headlineLarge,
            headlineMedium = PLUTOTypography.headlineMedium,
            bodyLarge = PLUTOTypography.bodyLarge,
            bodyMedium = PLUTOTypography.bodyMedium,
            bodySmall = PLUTOTypography.bodySmall,
            labelLarge = PLUTOTypography.labelMono,
            labelMedium = PLUTOTypography.metadataMono,
            labelSmall = PLUTOTypography.labelMono
        ),
        content = content
    )
}
