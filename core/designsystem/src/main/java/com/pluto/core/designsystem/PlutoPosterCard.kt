package com.pluto.core.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

/**
 * PlutoPosterCard — content card floating in space.
 *
 * Implements Section 11 ("CARD INTERACTION") + Section 17 ("MOVIE CARD")
 * of the cosmic visual spec.
 *
 *   On press: scale 1.0 -> 0.97 -> 1.0. Subtle blue glow.
 *   Do NOT use heavy shadows. Use soft atmospheric glow + edge lighting.
 *
 * Cards display: poster, title, year, rating, type badge, quality if
 * available. Optional progress bar for Continue Watching.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlutoPosterCard(
    title: String,
    imageUrl: String,
    year: Int = 0,
    rating: Double = 0.0,
    isSeries: Boolean = false,
    qualityLabel: String? = null,
    progress: Float? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    cardWidth: Int = 140
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card-scale"
    )

    Column(
        modifier = modifier
            .width(cardWidth.dp)
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onLongClick = onLongClick,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(PLUTOShapes.large))
                .background(PLUTOColors.NavyDrift)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Top gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Type badge (top-left)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(PLUTOShapes.pill))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isSeries) PlutoIcons.Series else PlutoIcons.Movie,
                        contentDescription = null,
                        tint = PLUTOColors.IceBlue,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = if (isSeries) "Series" else "Movie",
                        style = PLUTOTypography.labelMono.copy(fontSize = 9.sp),
                        color = PLUTOColors.IceBlue,
                        modifier = Modifier.padding(start = 3.dp)
                    )
                }
            }

            // Rating (top-right)
            if (rating > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(PLUTOShapes.pill))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            PlutoIcons.Star,
                            contentDescription = null,
                            tint = PLUTOColors.Warning,
                            modifier = Modifier.size(9.dp)
                        )
                        Text(
                            text = "%.1f".format(rating),
                            style = PLUTOTypography.metadataMono.copy(fontSize = 9.sp),
                            color = PLUTOColors.FrostWhite,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }

            // Quality chip (bottom-right, optional)
            if (qualityLabel != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(PLUTOShapes.pill))
                        .background(PLUTOColors.ElectricBlue.copy(alpha = 0.6f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        qualityLabel,
                        style = PLUTOTypography.labelMono.copy(fontSize = 8.sp),
                        color = PLUTOColors.FrostWhite
                    )
                }
            }

            // Bottom gradient + title
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    title,
                    style = PLUTOTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = PLUTOColors.FrostWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (year > 0) {
                    Text(
                        year.toString(),
                        style = PLUTOTypography.metadataMono.copy(fontSize = 9.sp),
                        color = PLUTOColors.IceBlue,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Progress bar (Continue Watching)
            if (progress != null && progress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(PLUTOColors.NavyDrift)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(3.dp)
                            .background(PLUTOColors.GlowBlue)
                    )
                }
            }
        }
    }
}
