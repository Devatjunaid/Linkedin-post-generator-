package com.verba.subtitles.ui.editor

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.verba.subtitles.core.model.Caption
import com.verba.subtitles.core.model.CaptionStyle
import com.verba.subtitles.core.model.Word
import com.verba.subtitles.core.render.CaptionRenderer
import com.verba.subtitles.core.render.FontRegistry

/** Draws the real caption renderer at [timeMs] — the same code that burns captions into the export. */
@Composable
fun CaptionLayer(
    renderer: CaptionRenderer,
    captions: List<Caption>,
    style: CaptionStyle,
    timeMs: () -> Long,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val now = timeMs()
        drawIntoCanvas { canvas ->
            renderer.draw(canvas.nativeCanvas, size.width.toInt(), size.height.toInt(), now, captions, style)
        }
    }
}

private val sampleCaption = Caption(
    id = 1,
    startMs = 0,
    endMs = 1_800,
    words = listOf(Word("Wake", 0, 380), Word("up", 380, 900), Word("now", 900, 1_800)),
)

/**
 * Small looping sample used on preset and motion cards. The text is scaled up because the
 * renderer sizes text from the short side of the canvas, which is tiny on a card.
 */
@Composable
fun MiniCaptionPreview(fonts: FontRegistry, style: CaptionStyle, modifier: Modifier = Modifier, animate: Boolean = true) {
    val renderer = remember(fonts) { CaptionRenderer(fonts) }
    val transition = rememberInfiniteTransition(label = "mini")
    val loop by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2_100f,
        animationSpec = infiniteRepeatable(tween(2_100, easing = LinearEasing)),
        label = "loop",
    )
    val previewStyle = remember(style) {
        style.copy(textSize = (style.textSize * 2.4f).coerceAtMost(0.26f), positionY = 0.5f)
    }
    Canvas(modifier) {
        val t = if (animate) loop.toLong().coerceAtMost(1_790) else 700L
        drawIntoCanvas { canvas ->
            renderer.drawCaption(canvas.nativeCanvas, size.width.toInt(), size.height.toInt(), t, sampleCaption, previewStyle)
        }
    }
}
