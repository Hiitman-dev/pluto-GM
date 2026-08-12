package com.pluto.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * PLUTODimensions — centralized spacing & sizing.
 *
 * Per Section 7 of the master spec ("DESIGN SYSTEM"): do NOT scatter
 * dimensions throughout the project. All values live here.
 *
 * Mobile horizontal margins per the cosmic spec: 32-48px (we use 24dp
 * for compact, 32dp for normal, 48dp for spacious).
 */
object PLUTODimensions {
    // Spacing
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 48.dp

    // Horizontal content padding (Section: SPACING)
    val contentPaddingCompact = 24.dp
    val contentPaddingNormal = 32.dp
    val contentPaddingExpanded = 48.dp

    // Card sizing
    val cardRadius = 20.dp
    val cardRadiusLarge = 24.dp
    val cardRadiusSmall = 14.dp
    val cardGlowRadius = 24.dp

    // Card widths (poster aspect ratio 2:3)
    val posterCardWidthSm = 110.dp
    val posterCardWidthMd = 140.dp
    val posterCardWidthLg = 170.dp
    val posterCardHeightRatio = 1.5f // 2:3

    // Hero
    val heroMinHeight = 320.dp
    val heroDefaultHeight = 480.dp
    val heroMaxHeight = 640.dp

    // Navigation
    val navPillHeight = 64.dp
    val navPillRadius = 32.dp
    val navItemSize = 44.dp

    // Touch targets (Section 68 — Accessibility: minimum 48dp where possible)
    val minTouchTarget = 48.dp

    // Player
    val playerControlSize = 48.dp
    val playerSeekBarHeight = 4.dp
}

/**
 * PLUTOShapes — corner radius definitions.
 */
object PLUTOShapes {
    // Re-exported as numbers for use in Compose Modifier.clip()
    val small = 8.dp
    val medium = 14.dp
    val large = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val pill = 100.dp // fully rounded
}

/**
 * PLUTOGlow — elevation shadow values.
 *
 * Per Section 11 of the cosmic spec ("Card Interaction"): "Do NOT use
 * heavy shadows. Use soft atmospheric glow, subtle edge lighting, depth."
 */
object PLUTOGlow {
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val xl = 40.dp

    // Soft blue glow color (RGBA hex for use with Brush)
    const val blueGlowAlpha = 0.45f
    const val iceGlowAlpha = 0.30f
}

/**
 * PLUTOAnimations / CosmicMotion — centralized timing tokens.
 *
 * Per Section 11 of the master spec ("Animation System"):
 *   Fast interactions:    ~120-180ms
 *   Normal transitions:   ~200-350ms
 *   Cinematic transitions: ~400-700ms
 *
 * Per Section 68 of the cosmic visual spec ("Design Tokens"):
 *   CosmicMotion.Fast / Normal / Cinematic
 *   CosmicGlow.Small / Medium / Large
 *   CosmicSpacing.Small / Medium / Large
 */
object CosmicMotion {
    const val Fast = 160
    const val Normal = 280
    const val Cinematic = 550

    // Easings — based on Material 3 emphasized easing, tuned for cosmic feel
    val emphasizedEasing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
    val standardEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    val deceleratedEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    fun <T> fast(): DurationBasedAnimationSpec<T> = tween(Fast, easing = emphasizedEasing)
    fun <T> normal(): DurationBasedAnimationSpec<T> = tween(Normal, easing = emphasizedEasing)
    fun <T> cinematic(): DurationBasedAnimationSpec<T> = tween(Cinematic, easing = emphasizedEasing)

    // Stagger (Section 9 of the cosmic visual spec)
    const val StaggerFast = 50
    const val StaggerNormal = 80
}

object CosmicSpacing {
    val small = PLUTODimensions.sm
    val medium = PLUTODimensions.lg
    val large = PLUTODimensions.xxl
}
