package com.pluto.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * PlutoIcons — PLUTO's custom icon language.
 *
 * Implements Section 13 ("ICONOGRAPHY") + Section 14 ("CUSTOM PLUTO ICON
 * SYSTEM") of the cosmic visual spec.
 *
 * Design language (Section 15):
 *   - thin geometric strokes (1.5 - 2 dp)
 *   - rounded geometry
 *   - orbital / planetary inspiration
 *   - consistent stroke width
 *   - 24dp grid with consistent optical center
 *
 * Per Section 67 ("NO RANDOM ICONS"): all icons live in this single
 * object so they can be reused across the app. Do NOT create duplicate
 * versions.
 *
 * Each icon uses the cosmic visual language — for example, Play is a
 * triangle surrounded by orbital energy, Search has a tiny stellar point,
 * Download is an arrow entering an orbital container.
 */
object PlutoIcons {

    // ── Navigation ──────────────────────────────────────────────────────

    /** PlutoHome — a dwelling arc with a stellar point inside. */
    val Home: ImageVector by lazy {
        buildVector("Home") {
            // Outer arc (dome)
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
                moveTo(4f, 11f)
                lineTo(12f, 4f)
                lineTo(20f, 11f)
                lineTo(20f, 20f)
                arcTo(2f, 2f, 0f, false, true, 18f, 22f)
                lineTo(6f, 22f)
                arcTo(2f, 2f, 0f, false, true, 4f, 20f)
                close()
            }
            // Stellar point (center)
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 14f)
                arcTo(1.2f, 1.2f, 0f, false, true, 12f, 16.4f)
                arcTo(1.2f, 1.2f, 0f, false, true, 12f, 14f)
                close()
            }
        }
    }

    /** PlutoSearch — magnifier with a tiny stellar point. */
    val Search: ImageVector by lazy {
        buildVector("Search") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
                // Lens
                moveTo(11f, 11f)
                arcTo(7f, 7f, 0f, true, false, 4f, 4f)
                arcTo(7f, 7f, 0f, false, false, 11f, 11f)
                close()
            }
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
                // Handle
                moveTo(16f, 16f)
                lineTo(21f, 21f)
            }
            path(fill = SolidColor(Color.Black)) {
                // Stellar point at lens center
                moveTo(11f, 10.5f)
                arcTo(0.8f, 0.8f, 0f, false, true, 11f, 12.1f)
                arcTo(0.8f, 0.8f, 0f, false, true, 11f, 10.5f)
                close()
            }
        }
    }

    /** PlutoDownload — arrow entering an orbital container. */
    val Download: ImageVector by lazy {
        buildVector("Download") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
                // Arrow shaft
                moveTo(12f, 4f)
                lineTo(12f, 15f)
                // Arrow head
                moveTo(7f, 10f)
                lineTo(12f, 15f)
                lineTo(17f, 10f)
            }
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f) {
                // Orbital container (open-bottomed arc)
                moveTo(4f, 17f)
                lineTo(4f, 19f)
                arcTo(2f, 2f, 0f, false, false, 6f, 21f)
                lineTo(18f, 21f)
                arcTo(2f, 2f, 0f, false, false, 20f, 19f)
                lineTo(20f, 17f)
            }
        }
    }

    val Play: ImageVector by lazy {
        buildVector("Play") {
            // Play triangle (filled)
            path(fill = SolidColor(Color.Black)) {
                moveTo(9f, 6f)
                lineTo(19f, 12f)
                lineTo(9f, 18f)
                close()
            }
            // Orbital arc around triangle
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.2f) {
                moveTo(5f, 12f)
                arcTo(7f, 7f, 0f, false, true, 12f, 5f)
                arcTo(7f, 7f, 0f, false, true, 19f, 12f)
            }
        }
    }

    val Pause: ImageVector by lazy {
        buildVector("Pause") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(7f, 5f)
                lineTo(10f, 5f)
                lineTo(10f, 19f)
                lineTo(7f, 19f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(14f, 5f)
                lineTo(17f, 5f)
                lineTo(17f, 19f)
                lineTo(14f, 19f)
                close()
            }
        }
    }

    /** PlutoFavorite — heart with subtle orbital arc. */
    val Favorite: ImageVector by lazy {
        buildVector("Favorite") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
                moveTo(12f, 21f)
                curveTo(12f, 21f, 4f, 16f, 4f, 10f)
                arcTo(4f, 4f, 0f, false, true, 12f, 8f)
                arcTo(4f, 4f, 0f, false, true, 20f, 10f)
                curveTo(20f, 16f, 12f, 21f, 12f, 21f)
                close()
            }
            // Tiny orbital pulse
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.0f) {
                moveTo(12f, 3f)
                arcTo(1.5f, 1.5f, 0f, false, true, 12f, 6f)
            }
        }
    }

    val FavoriteFilled: ImageVector by lazy {
        buildVector("FavoriteFilled") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 21f)
                curveTo(12f, 21f, 4f, 16f, 4f, 10f)
                arcTo(4f, 4f, 0f, false, true, 12f, 8f)
                arcTo(4f, 4f, 0f, false, true, 20f, 10f)
                curveTo(20f, 16f, 12f, 21f, 12f, 21f)
                close()
            }
        }
    }

    val Notification: ImageVector by lazy {
        buildVector("Notification") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
                // Bell body
                moveTo(6f, 16f)
                lineTo(6f, 11f)
                arcTo(6f, 6f, 0f, false, true, 18f, 11f)
                lineTo(18f, 16f)
                lineTo(20f, 18f)
                lineTo(4f, 18f)
                lineTo(6f, 16f)
                close()
            }
            // Clapper
            path(fill = SolidColor(Color.Black)) {
                moveTo(10.5f, 20f)
                arcTo(1.5f, 1.5f, 0f, false, false, 13.5f, 20f)
                close()
            }
            // Orbital arc around top
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.2f) {
                moveTo(9f, 5f)
                arcTo(4f, 4f, 0f, false, true, 15f, 5f)
            }
        }
    }

    val Settings: ImageVector by lazy {
        buildVector("Settings") {
            // Observatory dial — outer ring with notches
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f) {
                moveTo(12f, 4f)
                arcTo(8f, 8f, 0f, true, true, 4f, 12f)
                arcTo(8f, 8f, 0f, false, true, 12f, 4f)
                close()
            }
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f) {
                moveTo(12f, 8f)
                arcTo(4f, 4f, 0f, true, true, 8f, 12f)
                arcTo(4f, 4f, 0f, false, true, 12f, 8f)
                close()
            }
            // Center stellar point
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 11f)
                arcTo(1f, 1f, 0f, false, true, 12f, 13f)
                arcTo(1f, 1f, 0f, false, true, 12f, 11f)
                close()
            }
            // Tick marks
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.4f, strokeLineCap = StrokeCap.Round) {
                moveTo(12f, 2f); lineTo(12f, 3.5f)
                moveTo(12f, 20.5f); lineTo(12f, 22f)
                moveTo(2f, 12f); lineTo(3.5f, 12f)
                moveTo(20.5f, 12f); lineTo(22f, 12f)
            }
        }
    }

    val History: ImageVector by lazy {
        buildVector("History") {
            // Trajectory arc
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f, strokeLineCap = StrokeCap.Round) {
                moveTo(4f, 12f)
                arcTo(8f, 8f, 0f, true, true, 7f, 18f)
            }
            // Clock hands (trajectory point)
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f, strokeLineCap = StrokeCap.Round) {
                moveTo(12f, 8f)
                lineTo(12f, 12f)
                lineTo(15f, 14f)
            }
            // Origin star
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 12f)
                arcTo(1f, 1f, 0f, false, true, 4f, 14f)
                arcTo(1f, 1f, 0f, false, true, 4f, 12f)
                close()
            }
        }
    }

    val Back: ImageVector by lazy {
        buildVector("Back") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(15f, 5f)
                lineTo(8f, 12f)
                lineTo(15f, 19f)
            }
        }
    }

    val Close: ImageVector by lazy {
        buildVector("Close") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
                moveTo(6f, 6f); lineTo(18f, 18f)
                moveTo(18f, 6f); lineTo(6f, 18f)
            }
        }
    }

    val Refresh: ImageVector by lazy {
        buildVector("Refresh") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(20f, 11f)
                arcTo(8f, 8f, 0f, false, false, 12f, 4f)
                arcTo(8f, 8f, 0f, false, false, 5f, 9f)
                moveTo(4f, 4f)
                lineTo(5f, 9f)
                lineTo(10f, 8f)
                moveTo(4f, 13f)
                arcTo(8f, 8f, 0f, false, false, 12f, 20f)
                arcTo(8f, 8f, 0f, false, false, 19f, 15f)
                moveTo(20f, 20f)
                lineTo(19f, 15f)
                lineTo(14f, 16f)
            }
        }
    }

    val Copy: ImageVector by lazy {
        buildVector("Copy") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(9f, 9f)
                lineTo(9f, 5f)
                arcTo(1f, 1f, 0f, false, true, 10f, 4f)
                lineTo(18f, 4f)
                arcTo(1f, 1f, 0f, false, true, 19f, 5f)
                lineTo(19f, 16f)
                arcTo(1f, 1f, 0f, false, true, 18f, 17f)
                lineTo(15f, 17f)
            }
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(15f, 9f)
                lineTo(6f, 9f)
                arcTo(1f, 1f, 0f, false, false, 5f, 10f)
                lineTo(5f, 19f)
                arcTo(1f, 1f, 0f, false, false, 6f, 20f)
                lineTo(14f, 20f)
                arcTo(1f, 1f, 0f, false, false, 15f, 19f)
                lineTo(15f, 10f)
                arcTo(1f, 1f, 0f, false, false, 14f, 9f)
                close()
            }
        }
    }

    val Share: ImageVector by lazy {
        buildVector("Share") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round) {
                // Three orbital nodes
                moveTo(7f, 12f)
                arcTo(2f, 2f, 0f, true, false, 7f, 12.01f)
                close()
                moveTo(17f, 6f)
                arcTo(2f, 2f, 0f, true, false, 17f, 6.01f)
                close()
                moveTo(17f, 18f)
                arcTo(2f, 2f, 0f, true, false, 17f, 18.01f)
                close()
            }
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round) {
                // Connecting trajectories
                moveTo(8.5f, 11f); lineTo(15.5f, 7f)
                moveTo(8.5f, 13f); lineTo(15.5f, 17f)
            }
        }
    }

    val Lock: ImageVector by lazy {
        buildVector("Lock") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round) {
                // Shackle
                moveTo(8f, 11f)
                lineTo(8f, 7f)
                arcTo(4f, 4f, 0f, false, true, 16f, 7f)
                lineTo(16f, 11f)
            }
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f) {
                // Body
                moveTo(5f, 11f)
                lineTo(19f, 11f)
                arcTo(1f, 1f, 0f, false, true, 20f, 12f)
                lineTo(20f, 20f)
                arcTo(1f, 1f, 0f, false, true, 19f, 21f)
                lineTo(5f, 21f)
                arcTo(1f, 1f, 0f, false, true, 4f, 20f)
                lineTo(4f, 12f)
                arcTo(1f, 1f, 0f, false, true, 5f, 11f)
                close()
            }
        }
    }

    val Unlock: ImageVector by lazy {
        buildVector("Unlock") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round) {
                // Open shackle
                moveTo(8f, 11f)
                lineTo(8f, 7f)
                arcTo(4f, 4f, 0f, false, true, 16f, 7f)
            }
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f) {
                moveTo(5f, 11f)
                lineTo(19f, 11f)
                arcTo(1f, 1f, 0f, false, true, 20f, 12f)
                lineTo(20f, 20f)
                arcTo(1f, 1f, 0f, false, true, 19f, 21f)
                lineTo(5f, 21f)
                arcTo(1f, 1f, 0f, false, true, 4f, 20f)
                lineTo(4f, 12f)
                arcTo(1f, 1f, 0f, false, true, 5f, 11f)
                close()
            }
        }
    }

    val Volume: ImageVector by lazy {
        buildVector("Volume") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 10f)
                lineTo(8f, 10f)
                lineTo(13f, 5f)
                lineTo(13f, 19f)
                lineTo(8f, 14f)
                lineTo(4f, 14f)
                close()
            }
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f, strokeLineCap = StrokeCap.Round) {
                moveTo(16f, 9f)
                arcTo(4f, 4f, 0f, false, true, 16f, 15f)
                moveTo(18.5f, 7f)
                arcTo(7f, 7f, 0f, false, true, 18.5f, 17f)
            }
        }
    }

    val Mute: ImageVector by lazy {
        buildVector("Mute") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 10f)
                lineTo(8f, 10f)
                lineTo(13f, 5f)
                lineTo(13f, 19f)
                lineTo(8f, 14f)
                lineTo(4f, 14f)
                close()
            }
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f, strokeLineCap = StrokeCap.Round) {
                moveTo(16f, 9f); lineTo(21f, 14f)
                moveTo(21f, 9f); lineTo(16f, 14f)
            }
        }
    }

    val Brightness: ImageVector by lazy {
        buildVector("Brightness") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 8f)
                arcTo(4f, 4f, 0f, true, false, 12f, 16f)
                arcTo(4f, 4f, 0f, false, false, 12f, 8f)
                close()
            }
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f, strokeLineCap = StrokeCap.Round) {
                moveTo(12f, 2f); lineTo(12f, 4f)
                moveTo(12f, 20f); lineTo(12f, 22f)
                moveTo(2f, 12f); lineTo(4f, 12f)
                moveTo(20f, 12f); lineTo(22f, 12f)
                moveTo(4.93f, 4.93f); lineTo(6.34f, 6.34f)
                moveTo(17.66f, 17.66f); lineTo(19.07f, 19.07f)
                moveTo(19.07f, 4.93f); lineTo(17.66f, 6.34f)
                moveTo(6.34f, 17.66f); lineTo(4.93f, 19.07f)
            }
        }
    }

    val Fullscreen: ImageVector by lazy {
        buildVector("Fullscreen") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(4f, 9f); lineTo(4f, 4f); lineTo(9f, 4f)
                moveTo(15f, 4f); lineTo(20f, 4f); lineTo(20f, 9f)
                moveTo(20f, 15f); lineTo(20f, 20f); lineTo(15f, 20f)
                moveTo(9f, 20f); lineTo(4f, 20f); lineTo(4f, 15f)
            }
        }
    }

    val Forward: ImageVector by lazy {
        buildVector("Forward") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 5f); lineTo(19f, 12f); lineTo(12f, 19f)
                moveTo(5f, 5f); lineTo(12f, 12f); lineTo(5f, 19f)
            }
        }
    }

    val Rewind: ImageVector by lazy {
        buildVector("Rewind") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 5f); lineTo(5f, 12f); lineTo(12f, 19f)
                moveTo(19f, 5f); lineTo(12f, 12f); lineTo(19f, 19f)
            }
        }
    }

    val Speed: ImageVector by lazy {
        buildVector("Speed") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f) {
                // Outer orbital arc
                moveTo(4f, 14f)
                arcTo(8f, 8f, 0f, false, true, 20f, 14f)
            }
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f, strokeLineCap = StrokeCap.Round) {
                // Trajectory pointer
                moveTo(12f, 14f)
                lineTo(16f, 9f)
            }
            path(fill = SolidColor(Color.Black)) {
                // Center
                moveTo(12f, 13f)
                arcTo(1f, 1f, 0f, false, true, 12f, 15f)
                arcTo(1f, 1f, 0f, false, true, 12f, 13f)
                close()
            }
        }
    }

    val Subtitle: ImageVector by lazy {
        buildVector("Subtitle") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f) {
                moveTo(3f, 5f)
                lineTo(21f, 5f)
                arcTo(1f, 1f, 0f, false, true, 22f, 6f)
                lineTo(22f, 18f)
                arcTo(1f, 1f, 0f, false, true, 21f, 19f)
                lineTo(3f, 19f)
                arcTo(1f, 1f, 0f, false, true, 2f, 18f)
                lineTo(2f, 6f)
                arcTo(1f, 1f, 0f, false, true, 3f, 5f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(5f, 11f); lineTo(10f, 11f); lineTo(10f, 13f); lineTo(5f, 13f); close()
                moveTo(12f, 11f); lineTo(19f, 11f); lineTo(19f, 13f); lineTo(12f, 13f); close()
                moveTo(5f, 14f); lineTo(7f, 14f); lineTo(7f, 16f); lineTo(5f, 16f); close()
                moveTo(9f, 14f); lineTo(19f, 14f); lineTo(19f, 16f); lineTo(9f, 16f); close()
            }
        }
    }

    val Audio: ImageVector by lazy {
        buildVector("Audio") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 10f)
                lineTo(8f, 10f)
                lineTo(13f, 5f)
                lineTo(13f, 19f)
                lineTo(8f, 14f)
                lineTo(4f, 14f)
                close()
            }
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f, strokeLineCap = StrokeCap.Round) {
                moveTo(16f, 9f); arcTo(4f, 4f, 0f, false, true, 16f, 15f)
            }
        }
    }

    val Quality: ImageVector by lazy {
        buildVector("Quality") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f) {
                // Diamond / spacecraft label
                moveTo(12f, 3f)
                lineTo(21f, 12f)
                lineTo(12f, 21f)
                lineTo(3f, 12f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 10f)
                arcTo(2f, 2f, 0f, false, true, 12f, 14f)
                arcTo(2f, 2f, 0f, false, true, 12f, 10f)
                close()
            }
        }
    }

    val Movie: ImageVector by lazy {
        buildVector("Movie") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f) {
                moveTo(3f, 5f); lineTo(21f, 5f); lineTo(21f, 19f); lineTo(3f, 19f); close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 8f); lineTo(7f, 8f); lineTo(5f, 11f); lineTo(7f, 14f); lineTo(3f, 14f); close()
                moveTo(21f, 8f); lineTo(17f, 8f); lineTo(19f, 11f); lineTo(17f, 14f); lineTo(21f, 14f); close()
            }
        }
    }

    val Series: ImageVector by lazy {
        buildVector("Series") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f) {
                // Outer TV frame
                moveTo(3f, 6f); lineTo(21f, 6f); lineTo(21f, 18f); lineTo(3f, 18f); close()
            }
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round) {
                // Antennae
                moveTo(8f, 6f); lineTo(12f, 10f)
                moveTo(16f, 6f); lineTo(12f, 10f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 13f)
                arcTo(1.2f, 1.2f, 0f, false, true, 12f, 15.4f)
                arcTo(1.2f, 1.2f, 0f, false, true, 12f, 13f)
                close()
            }
        }
    }

    val External: ImageVector by lazy {
        buildVector("External") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(14f, 4f)
                lineTo(20f, 4f)
                lineTo(20f, 10f)
                moveTo(20f, 4f)
                lineTo(13f, 11f)
                moveTo(19f, 14f)
                lineTo(19f, 20f)
                lineTo(5f, 20f)
                lineTo(5f, 6f)
                lineTo(11f, 6f)
            }
        }
    }

    val Signal: ImageVector by lazy {
        buildVector("Signal") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f, strokeLineCap = StrokeCap.Round) {
                // Concentric signal arcs
                moveTo(8f, 16f); arcTo(4f, 4f, 0f, false, true, 16f, 16f)
                moveTo(5f, 16f); arcTo(7f, 7f, 0f, false, true, 19f, 16f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 15f)
                arcTo(1.5f, 1.5f, 0f, false, true, 12f, 18f)
                arcTo(1.5f, 1.5f, 0f, false, true, 12f, 15f)
                close()
            }
        }
    }

    val Check: ImageVector by lazy {
        buildVector("Check") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2.0f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(5f, 13f)
                lineTo(10f, 18f)
                lineTo(20f, 6f)
            }
        }
    }

    val ChevronDown: ImageVector by lazy {
        buildVector("ChevronDown") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f)
            }
        }
    }

    val ChevronRight: ImageVector by lazy {
        buildVector("ChevronRight") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(9f, 6f); lineTo(15f, 12f); lineTo(9f, 18f)
            }
        }
    }

    val Star: ImageVector by lazy {
        buildVector("Star") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                lineTo(14.5f, 9f)
                lineTo(22f, 9f)
                lineTo(16f, 14f)
                lineTo(18.5f, 22f)
                lineTo(12f, 17f)
                lineTo(5.5f, 22f)
                lineTo(8f, 14f)
                lineTo(2f, 9f)
                lineTo(9.5f, 9f)
                close()
            }
        }
    }

    val Galaxy: ImageVector by lazy {
        buildVector("Galaxy") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f) {
                // Spiral arms
                moveTo(2f, 12f)
                curveTo(2f, 8f, 6f, 4f, 12f, 4f)
                moveTo(22f, 12f)
                curveTo(22f, 16f, 18f, 20f, 12f, 20f)
            }
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.2f) {
                moveTo(6f, 12f); arcTo(6f, 6f, 0f, false, true, 18f, 12f); arcTo(6f, 6f, 0f, false, true, 6f, 12f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 11f); arcTo(1.2f, 1.2f, 0f, false, true, 12f, 13.4f); arcTo(1.2f, 1.2f, 0f, false, true, 12f, 11f); close()
            }
        }
    }

    // Helper: build a 24dp ImageVector with consistent viewport + naming.
    private fun buildVector(name: String, builder: androidx.compose.ui.graphics.vector.ImageVector.Builder.() -> Unit): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply(builder).build()
    }
}
