package com.harrycodess.drawingkids.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harrycodess.drawingkids.art.circle
import com.harrycodess.drawingkids.art.inBox
import com.harrycodess.drawingkids.art.inkStroke
import com.harrycodess.drawingkids.art.svg
import com.harrycodess.drawingkids.ui.theme.Kid
import com.harrycodess.drawingkids.ui.theme.KidFont
import androidx.compose.ui.graphics.asComposePath

private val palette = svg(
    "M54 28 C73 28 87 40 87 54 C87 63 81 67 73 65 C67 63 62 67 64 74 C66 82 62 85 54 85 " +
        "C35 85 21 72 21 56 C21 41 35 28 54 28 Z",
)
private val paints = listOf(
    Triple(39f, 48f, Kid.Red), Triple(54f, 40f, Kid.Gold), Triple(69f, 46f, Kid.Green),
    Triple(34f, 63f, Kid.Blue), Triple(46f, 74f, Kid.Purple),
).map { (x, y, c) -> circle(x, y, 6).asComposePath() to c }
private val pencil = svg("M63.1 75.7 L82.1 48.8 L90.3 54.6 L71.3 81.5 Z")
private val pencilCap = svg("M82.1 48.8 L85.6 43.9 L93.8 49.7 L90.3 54.6 Z")
private val pencilWood = svg("M62 86 L63.1 75.7 L71.3 81.5 Z")
private val pencilLead = svg("M62 86 L62.5 81.8 L65.7 84.2 Z")

/** The palette-and-pencil emblem (same drawing as the launcher icon), in a 108 box. */
fun DrawScope.drawEmblem() {
    drawCircle(Kid.Yellow, 50f, androidx.compose.ui.geometry.Offset(54f, 56f))
    drawCircle(Kid.Ink, 50f, androidx.compose.ui.geometry.Offset(54f, 56f), style = Stroke(3f))
    drawPath(palette, Color.White)
    drawPath(palette, Kid.Ink, style = inkStroke(3f))
    paints.forEach { (p, c) ->
        drawPath(p, c)
        drawPath(p, Kid.Ink, style = inkStroke(2f))
    }
    listOf(pencil to Kid.Pink, pencilCap to Kid.Blush, pencilWood to Kid.Sand).forEach { (p, c) ->
        drawPath(p, c)
        drawPath(p, Kid.Ink, style = inkStroke(2.5f))
    }
    drawPath(pencilLead, Kid.Ink)
}

@Composable
fun Emblem(modifier: Modifier) {
    Canvas(modifier) { inBox(108f) { drawEmblem() } }
}

/** Letters with a thick ink outline, like a sticker. */
@Composable
fun OutlinedLetter(ch: String, color: Color, size: Int, modifier: Modifier = Modifier) {
    val base = TextStyle(fontFamily = KidFont, fontWeight = FontWeight.Black, fontSize = size.sp)
    Box(modifier) {
        Text(ch, style = base.copy(color = Kid.Ink, drawStyle = Stroke(width = size * 0.22f)))
        Text(ch, style = base.copy(color = color))
    }
}

/**
 * "Twilio Kids Drawing" logo. [reveal] (0..1) lets the splash screen pop the letters in one by one.
 */
@Composable
fun TwilioLogo(modifier: Modifier = Modifier, letterSize: Int = 64, reveal: Float = 1f, emblemSize: Dp = 0.dp) {
    val word = "Twilio"
    val colors = listOf(Kid.Red, Kid.Orange, Kid.Gold, Kid.Green, Kid.Blue, Kid.Purple)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (emblemSize > 0.dp) Emblem(Modifier.size(emblemSize).padding(end = 6.dp))
            word.forEachIndexed { i, ch ->
                val local = ((reveal * (word.length + 2)) - i).coerceIn(0f, 1f)
                val bounce = if (local < 1f) local * (1.25f - 0.25f * local) else 1f
                OutlinedLetter(
                    ch.toString(), colors[i], letterSize,
                    Modifier
                        .graphicsLayer {
                            scaleX = bounce
                            scaleY = bounce
                            alpha = local
                            rotationZ = if (i % 2 == 0) -6f else 5f
                        }
                        .offset(y = if (i % 2 == 0) 0.dp else 4.dp),
                )
            }
        }
        val tagAlpha = ((reveal * (word.length + 2)) - word.length).coerceIn(0f, 1f)
        Row(
            Modifier
                .graphicsLayer { alpha = tagAlpha; scaleX = 0.6f + 0.4f * tagAlpha; scaleY = 0.6f + 0.4f * tagAlpha }
                .rotate(-3f)
                .background(Kid.Ink, RoundedCornerShape(50))
                .border(3.dp, Color.White, RoundedCornerShape(50))
                .padding(horizontal = 18.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Kids", style = TextStyle(fontFamily = KidFont, fontWeight = FontWeight.Black, fontSize = (letterSize * 0.38f).sp, color = Kid.Yellow))
            Text("Drawing", style = TextStyle(fontFamily = KidFont, fontWeight = FontWeight.Black, fontSize = (letterSize * 0.38f).sp, color = Color.White))
        }
    }
}
