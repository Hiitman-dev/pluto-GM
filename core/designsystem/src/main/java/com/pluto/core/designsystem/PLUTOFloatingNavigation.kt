package com.pluto.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size as sizeModifier

/**
 * PLUTOFloatingNavigation — glassmorphic pill nav.
 *
 * Implements Section 81 ("NAVIGATION") + Section 50 ("NAVIGATION ANIMATION")
 * of the cosmic visual spec.
 *
 *   Glassmorphic. Thin icons. Active icon glows.
 *   Do not imitate Telegram.
 *
 * Active state: bright ice white + electric blue glow + subtle orbital pulse.
 * Inactive: muted ice blue, calm.
 *
 * Switching: active icon emits a tiny blue pulse. Indicator moves smoothly.
 * Do not bounce the entire nav bar.
 */
@Composable
fun PLUTOFloatingNavigation(
    items: List<PLUTONavItem>,
    current: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        PLUTOColors.DeepSpace.copy(alpha = 0.78f),
                        PLUTOColors.NavyDrift.copy(alpha = 0.72f)
                    )
                )
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                PLUTONavButton(
                    item = item,
                    isActive = item.id == current,
                    onClick = { onChange(item.id) }
                )
            }
        }
    }
}

data class PLUTONavItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
)

@Composable
private fun PLUTONavButton(
    item: PLUTONavItem,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "nav-scale"
    )
    val tint = if (isActive) PLUTOColors.FrostWhite else PLUTOColors.MutedStar
    val bg = if (isActive) PLUTOColors.ElectricBlue.copy(alpha = 0.85f) else Color.Transparent
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .sizeModifier(48.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            item.icon,
            contentDescription = item.contentDescription,
            tint = tint,
            modifier = Modifier
                .size(22.dp)
                .then(if (isActive) Modifier.shadow(8.dp, CircleShape) else Modifier)
        )
        // Active orbital pulse — small dot at top
        AnimatedVisibility(
            visible = isActive,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(PLUTOColors.GlowBlue)
            )
        }
    }
}
