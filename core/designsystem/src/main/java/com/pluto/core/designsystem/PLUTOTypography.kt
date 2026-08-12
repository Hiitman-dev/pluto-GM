package com.pluto.core.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * PLUTOTypography — type system.
 *
 * Per Section 8 (typography) of the cosmic visual spec:
 *   Display:    Space Grotesk
 *   Body:       Inter or Manrope
 *   Metadata:   Space Mono
 *
 * PLUTO ships with default system fonts as fallback — the app module's
 * res/font/ folder contains the actual font files, loaded via
 * FontFamily.SansSerif default when files aren't bundled.
 *
 * Per Section 43 (Premium Number Display): metadata such as years,
 * quality tiers (1080p), timestamps, and episode codes (S02E05) uses
 * Space Mono with controlled letter spacing, making them feel like
 * astronomical coordinates.
 *
 * Per Section 44 (Cosmic Data Labels): small labels use uppercase,
 * wide tracking, monospace. E.g. "S E A S O N   0 2".
 */
object PLUTOTypography {
    // Defaults — overridable by app module with bundled fonts.
    val Display = FontFamily.Default
    val Body = FontFamily.Default
    val Mono = FontFamily.Monospace

    val displayXLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 54.sp,
        letterSpacing = (-0.5).sp
    )

    val displayLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.3).sp
    )

    val displayMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.2).sp
    )

    val displaySmall = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    )

    val headlineLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    )

    val headlineMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp
    )

    val bodyLarge = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    )

    val bodyMedium = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp
    )

    val bodySmall = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    /** Monospace label with wide tracking — "S E A S O N   0 2" feel. */
    val labelMono = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.8.sp
    )

    /** Metadata numbers — years, timestamps, episode codes. */
    val metadataMono = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )

    /** PLUTO brand wordmark — 0.28em letter spacing per the spec. */
    val brandWordmark = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 18.sp,
        letterSpacing = 5.0.sp // ~0.28em at 18sp
    )
}
