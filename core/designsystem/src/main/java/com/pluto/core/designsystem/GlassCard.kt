package com.pluto.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * GlassCard — PLUTO glassmorphic surface.
 *
 * Implements Section 33 ("GLASS SYSTEM") of the cosmic visual spec.
 *
 * Hierarchy:
 *   Glass1: almost transparent (default)
 *   Glass2: slightly blue (used for cards)
 *   Glass3: stronger blue tint (used for active surfaces)
 *   Glass4: active surface with glow (used for selected items / pressed)
 *
 * Per Section 34 ("Glass Reflection"): optional diagonal light sweep
 * (enabled by default — extremely low opacity, "noticeable only
 * subconsciously").
 *
 * Per Section 58 ("Performance Rule"): blur is expensive — caller can
 * opt out via [blurEnabled] = false on heavy lists.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glassLevel: GlassLevel = GlassLevel.Glass2,
    shape: Shape = RoundedCornerShape(PLUTOShapes.large),
    blurEnabled: Boolean = true,
    reflection: Boolean = true,
    border: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val baseColor = when (glassLevel) {
        GlassLevel.Glass1 -> PLUTOColors.Glass1
        GlassLevel.Glass2 -> PLUTOColors.Glass2
        GlassLevel.Glass3 -> PLUTOColors.Glass3
        GlassLevel.Glass4 -> PLUTOColors.Glass4
    }
    val borderColor = when (glassLevel) {
        GlassLevel.Glass1, GlassLevel.Glass2 -> PLUTOColors.GlassBorder
        GlassLevel.Glass3, GlassLevel.Glass4 -> PLUTOColors.GlassBorderActive
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (blurEnabled) Modifier.background(
                    Brush.linearGradient(
                        colors = listOf(
                            baseColor,
                            baseColor.copy(alpha = baseColor.alpha * 0.7f)
                        )
                    )
                ) else Modifier.background(baseColor)
            )
            .then(if (border) Modifier.border(BorderStroke(1.dp, borderColor), shape) else Modifier)
            .then(if (reflection) Modifier.glassReflection(shape) else Modifier)
    ) {
        content()
    }
}

enum class GlassLevel { Glass1, Glass2, Glass3, Glass4 }

/**
 * Glass reflection — diagonal light sweep across the surface.
 * Extremely low opacity (8%), meant to be "noticeable only subconsciously".
 */
private fun Modifier.glassReflection(shape: Shape): Modifier = this.then(
    Modifier.drawWithReflection(shape)
)

private fun Modifier.drawWithReflection(shape: Shape): Modifier = this.then(
    Modifier
        .clip(shape)
        .background(
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.0f),
                    Color.White.copy(alpha = 0.08f),
                    Color.White.copy(alpha = 0.0f)
                ),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
            )
        )
)
