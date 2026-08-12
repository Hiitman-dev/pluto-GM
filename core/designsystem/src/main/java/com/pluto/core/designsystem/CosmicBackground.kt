package com.pluto.core.designsystem

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * CosmicBackground — the foundational deep-space backdrop.
 *
 * Implements Section 9 ("COSMIC BACKGROUND ENGINE") + Section 2 ("COSMIC
 * LAYERS") of the cosmic visual spec.
 *
 * Layers (back to front):
 *   1. Base void (#03040A) — solid fill
 *   2. Deep space gradient — subtle vertical variation
 *   3. Nebula bloom — soft blue radial bloom at top corners
 *   4. Star field — sparse, deterministic, multi-size stars
 *   5. Orbital arc — very subtle SVG-like rings
 *
 * Per Section 10 ("STAR FIELD"): stars are subtle, varied, sparse.
 * Per Section 58 ("Performance Rule"): no canvas of thousands of
 * particles — we use ~25 carefully placed stars.
 *
 * Per Section 4 ("Parallax Cosmos"): subtle parallax via the
 * [ParallaxOffset] parameter. Caller passes scroll offset; we move
 * the nebula layer at 1.1x, stars at 1.2x.
 */
@Composable
fun CosmicBackground(
    modifier: Modifier = Modifier,
    parallaxOffset: Float = 0f,
    starCount: Int = 28,
    seed: Long = 42L
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cosmic-twinkle")
    val twinkle by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = CosmicMotion.standardEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PLUTOColors.Void)
    ) {
        // Layer 2: vertical deep-space gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            PLUTOColors.DeepSpace,
                            PLUTOColors.Void,
                            PLUTOColors.DeepSpace,
                            PLUTOColors.Void
                        )
                    )
                )
        )

        // Layer 3: nebula bloom (parallax 1.1x)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PLUTOColors.ElectricBlue.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(0.2f * Float.MAX_VALUE, 0f).let {
                            // Anchor to top-left, scaled by parallax
                            Offset(
                                x = Float.MAX_VALUE * 0.2f,
                                y = Float.MAX_VALUE * 0.0f - parallaxOffset * 0.1f
                            )
                        },
                        radius = Float.MAX_VALUE * 0.5f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PLUTOColors.GlowBlue.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(Float.MAX_VALUE * 0.85f, Float.MAX_VALUE * 0.15f - parallaxOffset * 0.11f),
                        radius = Float.MAX_VALUE * 0.45f
                    )
                )
        )

        // Layer 4: star field (parallax 1.2x) — Canvas with deterministic seed
        StarField(
            starCount = starCount,
            seed = seed,
            twinkle = twinkle,
            parallaxOffset = parallaxOffset * 0.12f,
            modifier = Modifier.fillMaxSize()
        )

        // Layer 5: orbital arc — extremely subtle
        OrbitalArc(modifier = Modifier.fillMaxSize())
    }
}

/**
 * StarField — deterministic, sparse, varied stars.
 *
 * Uses a seeded RNG so stars are stable across recompositions.
 * Sizes: 0.5dp - 2dp. Opacities: 0.3 - 0.95.
 */
@Composable
private fun StarField(
    starCount: Int,
    seed: Long,
    twinkle: Float,
    parallaxOffset: Float,
    modifier: Modifier = Modifier
) {
    val stars = remember(seed, starCount) {
        val rng = Random(seed)
        List(starCount) {
            Star(
                xFraction = rng.nextFloat(),
                yFraction = rng.nextFloat(),
                radius = 0.5f + rng.nextFloat() * 1.5f,
                baseAlpha = 0.3f + rng.nextFloat() * 0.65f,
                isIce = rng.nextBoolean()
            )
        }
    }
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        stars.forEach { star ->
            val cx = star.xFraction * size.width
            val cy = star.yFraction * size.height + parallaxOffset
            val r = with(density) { star.radius.dp.toPx() }
            val color = if (star.isIce) PLUTOColors.IceBlue else PLUTOColors.FrostWhite
            drawCircle(
                color = color.copy(alpha = star.baseAlpha * twinkle),
                radius = r,
                center = Offset(cx, cy)
            )
        }
    }
}

private data class Star(
    val xFraction: Float,
    val yFraction: Float,
    val radius: Float,
    val baseAlpha: Float,
    val isIce: Boolean
)

/**
 * OrbitalArc — very subtle SVG-like decorative ring.
 *
 * Per Section 35 ("Orbital System") of the cosmic spec: use orbital
 * elements as decorative anchors, not on every component.
 */
@Composable
private fun OrbitalArc(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val alpha = 0.08f
        // Top-right ring
        drawCircle(
            color = PLUTOColors.GlowBlue.copy(alpha = alpha),
            radius = size.minDimension * 0.55f,
            center = Offset(size.width * 0.85f, size.height * 0.05f),
            style = Stroke(width = 1.dp.toPx())
        )
        // Smaller inner ring
        drawCircle(
            color = PLUTOColors.IceBlue.copy(alpha = alpha * 0.7f),
            radius = size.minDimension * 0.4f,
            center = Offset(size.width * 0.85f, size.height * 0.05f),
            style = Stroke(width = 0.8.dp.toPx())
        )
        // Bottom-left ring
        drawCircle(
            color = PLUTOColors.ElectricBlue.copy(alpha = alpha * 0.5f),
            radius = size.minDimension * 0.35f,
            center = Offset(size.width * 0.1f, size.height * 0.95f),
            style = Stroke(width = 0.6.dp.toPx())
        )
    }
}

/**
 * NebulaLayer — standalone nebula gradient (used as accent on details screen).
 */
@Composable
fun NebulaLayer(
    modifier: Modifier = Modifier,
    intensity: Float = 1.0f
) {
    Box(
        modifier = modifier
            .blur(40.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        PLUTOColors.ElectricBlue.copy(alpha = 0.12f * intensity),
                        PLUTOColors.GlowBlue.copy(alpha = 0.08f * intensity),
                        Color.Transparent
                    )
                )
            )
    )
}

/**
 * PlanetGlow — soft circular glow used behind hero posters.
 */
@Composable
fun PlanetGlow(
    modifier: Modifier = Modifier,
    color: Color = PLUTOColors.ElectricBlue,
    intensity: Float = 0.6f
) {
    Box(
        modifier = modifier
            .blur(60.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = intensity * 0.4f),
                        color.copy(alpha = intensity * 0.15f),
                        Color.Transparent
                    )
                )
            )
    )
}
