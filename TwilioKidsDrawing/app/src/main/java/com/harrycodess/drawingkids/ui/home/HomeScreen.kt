package com.harrycodess.drawingkids.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.harrycodess.drawingkids.ads.BannerAd
import com.harrycodess.drawingkids.art.Animals
import com.harrycodess.drawingkids.art.inkStroke
import com.harrycodess.drawingkids.audio.Sfx
import com.harrycodess.drawingkids.drawing.Tool
import com.harrycodess.drawingkids.ui.components.ArtThumb
import com.harrycodess.drawingkids.ui.components.Chunky
import com.harrycodess.drawingkids.ui.components.IconButton32
import com.harrycodess.drawingkids.ui.components.KidIcons.soundIcon
import com.harrycodess.drawingkids.ui.components.Showcase
import com.harrycodess.drawingkids.ui.components.ToolIcon
import com.harrycodess.drawingkids.ui.components.TwilioLogo
import com.harrycodess.drawingkids.ui.theme.Kid
import com.harrycodess.drawingkids.ui.theme.kidText
import kotlin.math.sin

@Composable
fun HomeScreen(onFreeDraw: () -> Unit, onColor: () -> Unit, onGallery: () -> Unit) {
    var soundOn by remember { mutableStateOf(Sfx.enabled) }
    val wobble = rememberInfiniteTransition(label = "wobble")
    val phase by wobble.animateFloat(
        0f, (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )

    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val landscape = maxWidth > maxHeight
            val deco = (minOf(maxWidth.value, maxHeight.value) * 0.2f).coerceIn(70f, 160f).dp

            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val p1 = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.18f, h * 0.26f)
                    cubicTo(w * 0.35f, h * 0.16f, w * 0.5f, h * 0.32f, w * 0.72f, h * 0.22f)
                }
                val p2 = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.22f, h * 0.9f)
                    cubicTo(w * 0.4f, h * 0.96f, w * 0.6f, h * 0.84f, w * 0.82f, h * 0.9f)
                }
                drawPath(p1, Kid.Pink.copy(alpha = 0.3f), style = inkStroke(10.dp.toPx()))
                drawPath(p2, Kid.Blue.copy(alpha = 0.22f), style = inkStroke(14.dp.toPx()))
            }

            Floating("cat", deco, Alignment.TopStart, -10f + 4f * sin(phase))
            Floating("fish", deco, Alignment.CenterEnd, 8f + 5f * sin(phase + 1f), if (landscape) 0.dp else 40.dp)
            Floating("turtle", deco, Alignment.BottomStart, -6f + 3f * sin(phase + 2f))
            Floating("butterfly", deco, Alignment.BottomEnd, 10f + 6f * sin(phase * 2f))

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (landscape) 24.dp else 20.dp, Alignment.CenterVertically),
            ) {
                TwilioLogo(letterSize = if (landscape) 76 else 58, emblemSize = if (landscape) 84.dp else 64.dp)
                Text(
                    "Draw, doodle & color your favorite animals!",
                    style = kidText(if (landscape) 21 else 17, color = Kid.Muted),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                val cards = listOf<@Composable (Modifier, Boolean) -> Unit>(
                    { m, wide -> HomeCard("Free Draw", "Pencils, markers & crayons", Kid.CardYellow, wide, m, onFreeDraw) { FreeDrawArt() } },
                    { m, wide -> HomeCard("Color Animals", "${Animals.all.size} animals to color", Kid.CardMint, wide, m, onColor) { ColorArt() } },
                    { m, wide -> HomeCard("My Gallery", "See your artworks", Kid.CardLilac, wide, m, onGallery) { GalleryArt() } },
                )
                if (landscape) {
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        cards.forEach { it(Modifier.width(250.dp).height(290.dp), false) }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        cards.forEach { it(Modifier.width(320.dp).height(150.dp), true) }
                    }
                }
            }

            Box(Modifier.align(Alignment.TopEnd).padding(14.dp)) {
                IconButton32({
                    soundOn = !soundOn
                    Sfx.enabled = soundOn
                }) { soundIcon(soundOn) }
            }
        }
        BannerAd()
    }
}

@Composable
private fun Floating(id: String, size: Dp, align: Alignment, angle: Float, extraTop: Dp = 0.dp) {
    Box(Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, top = 10.dp + extraTop, bottom = 10.dp), contentAlignment = align) {
        ArtThumb(Showcase.art(id), Showcase.fills(id), Modifier.size(size).rotate(angle))
    }
}

@Composable
private fun HomeCard(
    title: String,
    subtitle: String,
    color: Color,
    wide: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    art: @Composable () -> Unit,
) {
    Chunky(modifier, color = color, shadow = Kid.Ink, corner = 36.dp, depth = 8.dp, border = Kid.Ink, borderWidth = 4.dp, onClick = onClick) {
        if (wide) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(120.dp, 100.dp), contentAlignment = Alignment.Center) { art() }
                Column {
                    Text(title, style = kidText(26))
                    Text(subtitle, style = kidText(15, color = Kid.Ink.copy(alpha = 0.65f)))
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.size(200.dp, 130.dp), contentAlignment = Alignment.Center) { art() }
                Text(title, style = kidText(30))
                Text(subtitle, style = kidText(16, color = Kid.Ink.copy(alpha = 0.65f)))
            }
        }
    }
}

@Composable
private fun FreeDrawArt() {
    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(Tool.PENCIL to Kid.Red, Tool.MARKER to Kid.Blue, Tool.CRAYON to Kid.Green).forEachIndexed { i, (t, c) ->
            ToolIcon(t, c, Modifier.size(64.dp).graphicsLayer { rotationZ = (i - 1) * 12f })
        }
    }
}

@Composable
private fun ColorArt() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ArtThumb(Showcase.art("lion"), Showcase.fills("lion"), Modifier.size(100.dp))
        ArtThumb(Showcase.art("elephant"), emptyMap(), Modifier.size(84.dp))
    }
}

@Composable
private fun GalleryArt() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("pig" to -6f, "whale" to 6f).forEach { (id, angle) ->
            Box(
                Modifier
                    .size(80.dp)
                    .rotate(angle)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(3.dp, Kid.Ink, RoundedCornerShape(12.dp))
                    .padding(6.dp),
            ) {
                ArtThumb(Showcase.art(id), Showcase.fills(id), Modifier.fillMaxSize())
            }
        }
    }
}
