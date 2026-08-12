package com.pluto.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * PLUTOColors — the cosmic color palette.
 *
 * EXACT palette from Section 8 of the master spec:
 *   Void          #03040A
 *   Deep Space    #070B1A
 *   Navy Drift    #0C1233
 *   Electric Blue #0A4DFF
 *   Glow Blue     #2D7FFF
 *   Ice Blue      #8AB4FF
 *   Frost White   #E8F4FF
 *   Muted Star    #5A6B8C
 *   Danger        #FF3B5C
 *
 * Per Section 112 of the master spec: "Dark theme is the PRIMARY experience.
 * Avoid pure black everywhere. Use layered darkness." PLUTO does NOT use
 * flat Color.Black for surfaces — instead, Void and DeepSpace form the
 * atmospheric base, with NavyDrift providing elevation.
 */
object PLUTOColors {
    val Void = Color(0xFF03040A)
    val DeepSpace = Color(0xFF070B1A)
    val NavyDrift = Color(0xFF0C1233)
    val ElectricBlue = Color(0xFF0A4DFF)
    val GlowBlue = Color(0xFF2D7FFF)
    val IceBlue = Color(0xFF8AB4FF)
    val FrostWhite = Color(0xFFE8F4FF)
    val MutedStar = Color(0xFF5A6B8C)
    val Danger = Color(0xFFFF3B5C)
    val Success = Color(0xFF2D7FFF)
    val Warning = Color(0xFFFFE74C)

    // Atmospheric layers (Section 8 — "Create atmospheric layers")
    // Each is semi-transparent so they composite over the cosmic background.
    val VoidAtmospheric = Color(0xFF03040A)
    val DeepSpaceAtmospheric = Color(0xF0070B1A)
    val NavyAtmospheric = Color(0xE60C1233)
    val BlueAtmospheric = Color(0x330A4DFF)
    val ElectricGlow = Color(0x4D2D7FFF)
    val IceHighlight = Color(0x668AB4FF)

    // Glass hierarchy (Section 33 of the cosmic visual spec)
    val Glass1 = Color(0x12E8F4FF) // almost transparent
    val Glass2 = Color(0x1A8AB4FF) // slightly blue
    val Glass3 = Color(0x262D7FFF) // stronger blue tint
    val Glass4 = Color(0x330A4DFF) // active surface with glow

    // Border colors
    val GlassBorder = Color(0x1F8AB4FF)
    val GlassBorderActive = Color(0x4D2D7FFF)

    // Text
    val TextPrimary = FrostWhite
    val TextSecondary = IceBlue
    val TextTertiary = MutedStar
    val TextOnPrimary = FrostWhite
    val TextDanger = Danger
}

/**
 * Atmospheric gradient stops — used by [CosmicBackground] to layer
 * the deep-space backdrop per Section 2 of the cosmic visual spec.
 */
object PLUTOGradients {
    // Vertical night sky: Void -> Deep Space -> Void
    val nightSky = listOf(
        PLUTOColors.Void to 0.0f,
        PLUTOColors.DeepSpace to 0.5f,
        PLUTOColors.Void to 1.0f
    )

    // Blue nebula bloom
    val nebula = listOf(
        PLUTOColors.ElectricBlue.copy(alpha = 0.18f) to 0.0f,
        PLUTOColors.GlowBlue.copy(alpha = 0.12f) to 0.5f,
        PLUTOColors.NavyDrift.copy(alpha = 0.0f) to 1.0f
    )

    // Hero cinematic gradient (top transparent -> bottom Void)
    val heroBottom = listOf(
        PLUTOColors.Void.copy(alpha = 0.0f) to 0.0f,
        PLUTOColors.Void.copy(alpha = 0.4f) to 0.5f,
        PLUTOColors.Void.copy(alpha = 0.95f) to 1.0f
    )
}
