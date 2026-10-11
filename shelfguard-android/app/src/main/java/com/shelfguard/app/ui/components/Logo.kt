package com.shelfguard.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.shelfguard.app.ui.theme.Brand

/** Progress (0..1) of each part of the logo; the animated splash drives these. */
@Immutable
data class LogoParts(
    val tile: Float = 1f,
    val top: Float = 1f,
    val left: Float = 1f,
    val right: Float = 1f,
    val tape: Float = 1f,
    val badge: Float = 1f,
    /** Rotation of the clock's minute hand in degrees. */
    val handAngle: Float = 0f,
)

/**
 * The ShelfGuard mark, drawn in code so it stays crisp at any size and can be animated.
 * An isometric stock box sealed with amber tape, guarded by an amber clock badge (expiry).
 * Geometry matches res/drawable/ic_launcher_foreground.xml (108-unit grid).
 */
@Composable
fun ShelfGuardLogo(
    modifier: Modifier = Modifier,
    parts: LogoParts = LogoParts(),
    withTile: Boolean = true,
) {
    Canvas(modifier) {
        val u = size.minDimension / 108f
        val origin = Offset((size.width - 108f * u) / 2f, (size.height - 108f * u) / 2f)
        translate(origin.x, origin.y) {
            if (withTile && parts.tile > 0f) drawTile(u, parts.tile)
            drawBox(u, parts)
            if (parts.badge > 0f) drawBadge(u, parts.badge, parts.handAngle)
        }
    }
}

private fun poly(u: Float, vararg pts: Float): Path = Path().apply {
    moveTo(pts[0] * u, pts[1] * u)
    var i = 2
    while (i < pts.size) {
        lineTo(pts[i] * u, pts[i + 1] * u)
        i += 2
    }
    close()
}

private fun DrawScope.drawTile(u: Float, p: Float) {
    val side = 108f * u
    scale(0.6f + 0.4f * p, pivot = Offset(side / 2f, side / 2f)) {
        drawRoundRect(
            brush = Brush.linearGradient(
                0f to Color(0xFF0A3D2B),
                0.55f to Color(0xFF11603F),
                1f to Color(0xFF1A8A5A),
                start = Offset.Zero,
                end = Offset(side, side),
            ),
            size = Size(side, side),
            cornerRadius = CornerRadius(side * 0.27f),
            alpha = p,
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0x3329D88F), Color(0x0029D88F)),
                center = Offset(54f * u, 50f * u),
                radius = 34f * u,
            ),
            radius = 34f * u,
            center = Offset(54f * u, 50f * u),
            alpha = p,
        )
    }
}

private fun DrawScope.drawBox(u: Float, parts: LogoParts) {
    val shadowAlpha = minOf(parts.left, parts.right)
    if (shadowAlpha > 0f) {
        drawPath(poly(u, 30f, 44f, 50f, 55f, 50f, 77f, 30f, 66f), Color.Black.copy(alpha = 0.2f * shadowAlpha))
        drawPath(poly(u, 50f, 55f, 70f, 44f, 70f, 66f, 50f, 77f), Color.Black.copy(alpha = 0.2f * shadowAlpha))
    }
    // Each face slides in from its own side.
    face(u, parts.left, dx = -14f, dy = 6f) {
        drawPath(poly(u, 30f, 41f, 50f, 52f, 50f, 74f, 30f, 63f), Brand.MintPale, alpha = it)
    }
    face(u, parts.right, dx = 14f, dy = 6f) {
        drawPath(poly(u, 50f, 52f, 70f, 41f, 70f, 63f, 50f, 74f), Brand.Mint, alpha = it)
    }
    face(u, parts.top, dx = 0f, dy = -16f) {
        drawPath(poly(u, 50f, 30f, 70f, 41f, 50f, 52f, 30f, 41f), Color.White, alpha = it)
    }
    if (parts.tape > 0f) {
        // Tape "unrolls" from the front edge towards the back.
        val t = parts.tape
        val sx = 37.72f; val sy = 45.25f
        val ex = sx + 20f * t; val ey = sy - 11f * t
        drawPath(poly(u, sx, sy, ex, ey, ex + 4.56f, ey + 2.5f, sx + 4.56f, sy + 2.5f), Brand.AmberTape)
        drawPath(poly(u, 37.72f, 45.25f, 42.28f, 47.75f, 42.28f, 47.75f + 8f * t, 37.72f, 45.25f + 8f * t), Brand.AmberDark)
    }
}

private inline fun DrawScope.face(u: Float, p: Float, dx: Float, dy: Float, draw: DrawScope.(Float) -> Unit) {
    if (p <= 0f) return
    translate((1f - p) * dx * u, (1f - p) * dy * u) { draw(p.coerceIn(0f, 1f)) }
}

private fun DrawScope.drawBadge(u: Float, p: Float, handAngle: Float) {
    val c = Offset(67f * u, 65f * u)
    scale(p, pivot = c) {
        drawCircle(Color.White, radius = 13f * u, center = c)
        drawCircle(Brand.Amber, radius = 10.5f * u, center = c)
        rotate(handAngle, pivot = c) {
            drawLine(Color.White, c, Offset(67f * u, 58.5f * u), strokeWidth = 2.6f * u, cap = StrokeCap.Round)
        }
        rotate(handAngle / 12f, pivot = c) {
            drawLine(Color.White, c, Offset(71.5f * u, 67.6f * u), strokeWidth = 2.6f * u, cap = StrokeCap.Round)
        }
        drawCircle(Color.White, radius = 1.8f * u, center = c)
    }
}
