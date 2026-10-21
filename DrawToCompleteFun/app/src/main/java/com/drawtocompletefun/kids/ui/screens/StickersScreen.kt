package com.drawtocompletefun.kids.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtocompletefun.kids.game.Pictures
import com.drawtocompletefun.kids.ui.AppState
import com.drawtocompletefun.kids.ui.Sfx
import com.drawtocompletefun.kids.ui.components.Card
import com.drawtocompletefun.kids.ui.components.ChunkyButton
import com.drawtocompletefun.kids.ui.components.GameIcon
import com.drawtocompletefun.kids.ui.components.Ico
import com.drawtocompletefun.kids.ui.components.IconTile
import com.drawtocompletefun.kids.ui.components.KText
import com.drawtocompletefun.kids.ui.components.PictureView
import com.drawtocompletefun.kids.ui.components.StarPill
import com.drawtocompletefun.kids.ui.theme.Palette

private val STICKER_INKS = listOf(
    Color(0xFFE0A100), Color(0xFFD94A6A), Color(0xFFE07A00), Color(0xFF6C5CE7), Color(0xFF1E9BE8),
    Color(0xFFB57E00), Color(0xFF2FBF71), Color(0xFFD9467A), Color(0xFF9B59B6), Color(0xFF8A5A3B),
)
private val STICKER_GROUNDS = listOf(
    Color(0xFFFFF4D6), Color(0xFFFFE3EC), Color(0xFFFFF0E0), Color(0xFFEEF0FF), Color(0xFFE2F3FF),
    Color(0xFFFFF4D6), Color(0xFFE4F8EC), Color(0xFFFFE3EC), Color(0xFFF3E8FF), Color(0xFFF5EBE0),
)

@Composable
fun StickersScreen(state: AppState) {
    val progress = state.progress
    val platform = state.platform
    var lastGift by remember { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Blush),
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Ico.Back, "Back to home", onClick = {
                    platform.play(Sfx.Tap)
                    state.goHome()
                }, shadow = Palette.BlushShadow)
                KText(
                    "My Rewards",
                    size = 28.sp,
                    weight = FontWeight.Black,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                )
                StarPill(progress.totalStars)
            }

            // Daily gift
            Card(Modifier.fillMaxWidth(), shadow = Palette.BlushShadow) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            KText("Daily gift", size = 21.sp, weight = FontWeight.Black)
                            val streak = progress.streakForDisplay
                            KText(
                                if (streak > 0) "$streak-day streak. Keep it going!" else "Come back every day for stars!",
                                size = 13.sp,
                                color = Palette.InkSoft,
                            )
                        }
                        ShakingGift()
                    }
                    WeekStrip(progress.streakForDisplay, progress.canClaimGift())
                    val can = progress.canClaimGift()
                    ChunkyButton(
                        onClick = {
                            if (can) {
                                lastGift = progress.claimGift()
                                platform.play(Sfx.Unlock)
                            }
                        },
                        color = if (can) Palette.Purple else Color(0xFFDDF5E7),
                        shadow = if (can) Palette.PurpleShadow else Color(0xFFBFE6CF),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        depth = if (can) 5.dp else 2.dp,
                        label = if (can) "Open today's gift" else "Gift claimed",
                    ) {
                        KText(
                            when {
                                can -> "Open today's gift"
                                lastGift > 0 -> "Yay! +$lastGift star${if (lastGift > 1) "s" else ""}"
                                else -> "Come back tomorrow!"
                            },
                            size = 19.sp,
                            weight = FontWeight.Black,
                            color = if (can) Palette.Paper else Color(0xFF145C36),
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.Bottom) {
                KText("Sticker book", size = 22.sp, weight = FontWeight.Black, modifier = Modifier.weight(1f))
                KText("${progress.unlockedStickers} of ${Pictures.stickerPictures.size}", size = 14.sp, color = Palette.InkSoft)
            }

            Pictures.stickerPictures.chunked(4).forEachIndexed { row, pics ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pics.forEachIndexed { col, pic ->
                        val index = row * 4 + col
                        Sticker(
                            index = index,
                            name = pic.name,
                            unlocked = progress.isStickerUnlocked(index),
                            cost = progress.stickerCost(index),
                            modifier = Modifier.weight(1f),
                        ) {
                            PictureView(
                                pic,
                                Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                ink = STICKER_INKS[index % STICKER_INKS.size],
                                widthFactor = 1.9f,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .background(Palette.Paper)
                .navigationBarsPadding()
                .padding(vertical = 4.dp),
        ) { platform.Banner(Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun Sticker(
    index: Int,
    name: String,
    unlocked: Boolean,
    cost: Int,
    modifier: Modifier,
    art: @Composable () -> Unit,
) {
    val tilt = listOf(-4f, 3f, -2f, 4f, -3f)[index % 5]
    Box(
        modifier
            .aspectRatio(1f)
            .graphicsLayer { rotationZ = if (unlocked) tilt else 0f }
            .clip(RoundedCornerShape(20.dp))
            .background(if (unlocked) STICKER_GROUNDS[index % STICKER_GROUNDS.size] else Color(0xFFF7D2DE))
            .then(if (unlocked) Modifier.border(4.dp, Color.White, RoundedCornerShape(20.dp)) else Modifier)
            .semantics { contentDescription = if (unlocked) "$name sticker" else "Locked sticker, needs $cost stars" },
        contentAlignment = Alignment.Center,
    ) {
        if (unlocked) {
            art()
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                KText("?", size = 26.sp, weight = FontWeight.Black, color = Color(0xFF8E4A62))
                KText("$cost", size = 11.sp, weight = FontWeight.Black, color = Color(0xFF6E3049), align = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun WeekStrip(rawStreak: Int, canClaim: Boolean) {
    // A finished week starts over on the next claim.
    val streak = if (canClaim && rawStreak >= 7) 0 else rawStreak
    val labels = listOf("1", "2", "3", "4", "5", "6", "7")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { i, l ->
            val done = i < streak
            val today = canClaim && i == streak % 7
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                done -> Palette.Green
                                today -> Palette.Cream
                                else -> Color(0xFFF1F3F7)
                            },
                        )
                        .then(if (today) Modifier.border(3.dp, Palette.Ink, RoundedCornerShape(12.dp)) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) {
                        GameIcon(Ico.Check, Color.White, 18.dp)
                    } else if (i == 6) {
                        GameIcon(Ico.Gift, Palette.Coral, 18.dp)
                    }
                }
                KText("Day $l", size = 10.sp, color = Palette.InkSoft)
            }
        }
    }
}

@Composable
private fun ShakingGift() {
    val t = rememberInfiniteTransition(label = "gift")
    val r by t.animateFloat(-8f, 8f, infiniteRepeatable(tween(260, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "r")
    Box(
        Modifier
            .size(56.dp)
            .graphicsLayer { rotationZ = r }
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Coral),
        contentAlignment = Alignment.Center,
    ) { GameIcon(Ico.Gift, Palette.Ink, 32.dp) }
}
