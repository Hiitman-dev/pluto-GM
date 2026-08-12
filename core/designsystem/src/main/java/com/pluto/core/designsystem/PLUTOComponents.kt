package com.pluto.core.designsystem

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * PLUTOButton — primary call-to-action.
 *
 * Implements Section 19 ("COSMIC BUTTONS") + Section 20 ("PRIMARY PLAY
 * BUTTON") of the cosmic visual spec.
 *
 * Variants:
 *   Primary:   glass / atmospheric surface + blue glow + subtle gradient
 *   Secondary: transparent + thin border + subtle blue glow
 *   Tertiary:  icon only
 *
 * Per Section 79 ("DESIGN MICRO-INTERACTIONS"): card press scale 1.0 ->
 * 0.97 -> 1.0 with subtle blue glow on press.
 */
@Composable
fun PLUTOButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    variant: PLUTOButtonVariant = PLUTOButtonVariant.Primary
) {
    val containerColor = when (variant) {
        PLUTOButtonVariant.Primary -> PLUTOColors.ElectricBlue
        PLUTOButtonVariant.Secondary -> PLUTOColors.Glass2
        PLUTOButtonVariant.Tertiary -> Color.Transparent
    }
    val contentColor = when (variant) {
        PLUTOButtonVariant.Primary -> PLUTOColors.FrostWhite
        PLUTOButtonVariant.Secondary -> PLUTOColors.IceBlue
        PLUTOButtonVariant.Tertiary -> PLUTOColors.IceBlue
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(PLUTOShapes.pill),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = PLUTOColors.NavyDrift,
            disabledContentColor = PLUTOColors.MutedStar
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Box(modifier = Modifier.width(8.dp))
        }
        Text(text, style = PLUTOTypography.bodyMedium)
    }
}

enum class PLUTOButtonVariant { Primary, Secondary, Tertiary }

/**
 * PLUTOOutlinedButton — secondary CTA with glass border.
 */
@Composable
fun PLUTOOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(PLUTOShapes.pill),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = PLUTOColors.FrostWhite,
            containerColor = PLUTOColors.Glass2
        ),
        border = BorderStroke(1.dp, PLUTOColors.GlassBorderActive),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Box(modifier = Modifier.width(8.dp))
        }
        Text(text, style = PLUTOTypography.bodyMedium)
    }
}

/**
 * PLUTOIconCircle — icon-only circular button with glow.
 */
@Composable
fun PLUTOIconCircle(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Int = 40,
    tint: Color = PLUTOColors.FrostWhite,
    background: Color = PLUTOColors.Glass2,
    glow: Boolean = false
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(50))
            .background(background)
            .then(
                if (glow) Modifier.border(
                    BorderStroke(1.dp, PLUTOColors.GlowBlue.copy(alpha = 0.5f)),
                    RoundedCornerShape(50)
                ) else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size((size * 0.5).dp))
    }
}

// ── Loading / Empty / Error ─────────────────────────────────────────────

/**
 * PLUTOShimmer — cosmic shimmer loader.
 *
 * Per Section 26 ("SHIMMER"): "light traveling through cosmic glass,
 * not generic gray shimmer. Dark navy surface + soft blue highlight +
 * very low opacity + smooth movement."
 */
@Composable
fun PLUTOShimmer(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(PLUTOShapes.medium)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-translate"
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        PLUTOColors.NavyDrift.copy(alpha = 0.4f),
                        PLUTOColors.GlowBlue.copy(alpha = 0.12f),
                        PLUTOColors.NavyDrift.copy(alpha = 0.4f)
                    ),
                    start = androidx.compose.ui.geometry.Offset(translate * 200f, 0f),
                    end = androidx.compose.ui.geometry.Offset((translate + 1f) * 200f, 0f)
                )
            )
    )
}

/**
 * PlutoSpinner — orbit loader (Section 25).
 */
@Composable
fun PlutoSpinner(modifier: Modifier = Modifier, size: Int = 32) {
    CircularProgressIndicator(
        modifier = modifier.size(size.dp),
        color = PLUTOColors.GlowBlue,
        trackColor = PLUTOColors.NavyDrift,
        strokeWidth = 2.dp
    )
}

/**
 * SignalLostState — cosmic error state (Section 61).
 *
 * Per Section 108 ("PREMIUM ERROR LANGUAGE"): avoid technical language.
 * Instead of "HTTP 500" show "Signal interrupted."
 */
@Composable
fun SignalLostState(
    message: String = "Unable to reach the PLUTO network.",
    onRetry: (() -> Unit)? = null,
    onGoBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            PlutoIcons.Signal,
            contentDescription = null,
            tint = PLUTOColors.Danger,
            modifier = Modifier.size(56.dp)
        )
        Text(
            "SIGNAL LOST",
            style = PLUTOTypography.displayMedium,
            color = PLUTOColors.FrostWhite,
            modifier = Modifier.padding(top = 20.dp)
        )
        Text(
            message,
            style = PLUTOTypography.bodyMedium,
            color = PLUTOColors.MutedStar,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (onRetry != null || onGoBack != null) {
            Row(
                modifier = Modifier.padding(top = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (onGoBack != null) {
                    PLUTOOutlinedButton(text = "Go Back", onClick = onGoBack)
                }
                if (onRetry != null) {
                    PLUTOButton(text = "Retry", onClick = onRetry, icon = PlutoIcons.Refresh)
                }
            }
        }
    }
}

/**
 * EmptyState — premium empty states (Section 107).
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    icon: ImageVector = PlutoIcons.Galaxy,
    action: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = PLUTOColors.MutedStar,
            modifier = Modifier.size(48.dp)
        )
        Text(
            title,
            style = PLUTOTypography.displaySmall,
            color = PLUTOColors.FrostWhite,
            modifier = Modifier.padding(top = 20.dp)
        )
        Text(
            message,
            style = PLUTOTypography.bodyMedium,
            color = PLUTOColors.MutedStar,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (action != null) {
            Box(modifier = Modifier.padding(top = 24.dp)) { action() }
        }
    }
}
