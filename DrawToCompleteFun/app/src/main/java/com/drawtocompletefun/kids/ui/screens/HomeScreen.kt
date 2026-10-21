package com.drawtocompletefun.kids.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtocompletefun.kids.game.Pictures
import com.drawtocompletefun.kids.ui.AppState
import com.drawtocompletefun.kids.ui.Overlay
import com.drawtocompletefun.kids.ui.Sfx
import com.drawtocompletefun.kids.ui.components.Card
import com.drawtocompletefun.kids.ui.components.ChunkyButton
import com.drawtocompletefun.kids.ui.components.Clouds
import com.drawtocompletefun.kids.ui.components.GameIcon
import com.drawtocompletefun.kids.ui.components.Ico
import com.drawtocompletefun.kids.ui.components.IconTile
import com.drawtocompletefun.kids.ui.components.KText
import com.drawtocompletefun.kids.ui.components.Pip
import com.drawtocompletefun.kids.ui.components.StarPill
import com.drawtocompletefun.kids.ui.components.drawDashedShape
import com.drawtocompletefun.kids.ui.components.drawShape
import com.drawtocompletefun.kids.ui.theme.Palette

@Composable
fun HomeScreen(state: AppState) {
    val progress = state.progress
    val sfx = state.platform
    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Sky),
    ) {
        Clouds()
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StarPill(progress.totalStars)
                Spacer(Modifier.weight(1f))
                IconTile(
                    if (progress.soundOn) Ico.SoundOn else Ico.SoundOff,
                    label = if (progress.soundOn) "Sound on" else "Sound off",
                    onClick = {
                        progress.updateSound(!progress.soundOn)
                        sfx.play(Sfx.Tap)
                    },
                )
                Spacer(Modifier.size(10.dp))
                IconTile(Ico.Settings, "Settings", onClick = {
                    sfx.play(Sfx.Tap)
                    state.overlay = Overlay.Settings
                })
            }

            Spacer(Modifier.height(18.dp))
            KText("Draw to", size = 44.sp, weight = FontWeight.Black, align = TextAlign.Center)
            Row(verticalAlignment = Alignment.CenterVertically) {
                KText("Complete", size = 48.sp, weight = FontWeight.Black, color = Palette.Purple)
                Box(
                    Modifier
                        .padding(start = 8.dp)
                        .graphicsLayer { rotationZ = -8f }
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.Yellow)
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                ) { KText("FUN!", size = 22.sp, weight = FontWeight.Black) }
            }
            KText(
                "Find the missing line. Draw it. Win stars!",
                size = 15.sp,
                color = Palette.InkSoft,
                align = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) { HeroDrawing() }

            ChunkyButton(
                onClick = {
                    sfx.play(Sfx.Whoosh)
                    state.openLevel(progress.currentLevel)
                },
                color = Palette.Yellow,
                shadow = Palette.YellowShadow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .pulse(),
                shape = RoundedCornerShape(28.dp),
                depth = 7.dp,
                label = "Play level ${progress.currentLevel}",
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    GameIcon(Ico.Play, Palette.Ink, 34.dp)
                    Column {
                        KText("PLAY", size = 32.sp, weight = FontWeight.Black)
                        KText("Level ${progress.currentLevel}", size = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HomeTile(Ico.Map, "Levels", Modifier.weight(1f)) {
                    sfx.play(Sfx.Tap)
                    state.openMap()
                }
                HomeTile(Ico.Sticker, "Stickers", Modifier.weight(1f)) {
                    sfx.play(Sfx.Tap)
                    state.openStickers()
                }
                Box(Modifier.weight(1f)) {
                    HomeTile(Ico.Gift, "Daily gift", Modifier.fillMaxWidth()) {
                        sfx.play(Sfx.Tap)
                        state.openStickers(withAd = false)
                    }
                    if (progress.canClaimGift()) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-6).dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Palette.Coral),
                            contentAlignment = Alignment.Center,
                        ) { KText("1", size = 13.sp, weight = FontWeight.Black) }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            sfx.Banner(Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun HomeTile(ico: Ico, text: String, modifier: Modifier, onClick: () -> Unit) {
    ChunkyButton(
        onClick = onClick,
        color = Palette.Purple,
        shadow = Palette.PurpleShadow,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(20.dp),
        depth = 5.dp,
        label = text,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GameIcon(ico, Palette.Paper, 28.dp)
            KText(text, size = 14.sp, color = Palette.Paper, weight = FontWeight.Black)
        }
    }
}

@Composable
private fun Modifier.pulse(): Modifier {
    val t = rememberInfiniteTransition(label = "pulse")
    val s by t.animateFloat(1f, 1.04f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s")
    return this.graphicsLayer {
        scaleX = s
        scaleY = s
    }
}

/** The house whose roof keeps drawing itself: shows kids the game in one glance. */
@Composable
private fun HeroDrawing() {
    val house = Pictures.house
    val roof = house.shape("roofR")!!
    val t = rememberInfiniteTransition(label = "hero")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "p")
    Box(Modifier.size(260.dp, 230.dp)) {
        Card(Modifier.size(230.dp, 210.dp).align(Alignment.TopStart), radius = 32.dp) {
            Canvas(
                Modifier
                    .size(230.dp, 210.dp)
                    .padding(18.dp),
            ) {
                val w = size.minDimension * 0.026f
                house.shapes.filter { it.tag != "roofR" }.forEach { drawShape(it, Palette.Ink, w) }
                drawDashedShape(roof, Palette.Ink.copy(alpha = 0.25f), w * 0.8f)
                drawShape(roof, Palette.Coral, w * 1.3f, (p / 0.55f).coerceAtMost(1f))
            }
        }
        Pip(
            Modifier
                .size(56.dp, 130.dp)
                .align(Alignment.BottomEnd),
        )
    }
}
