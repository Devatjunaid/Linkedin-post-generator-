package com.dotdash.kids.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotdash.kids.game.LEVELS_PER_WORLD
import com.dotdash.kids.game.Levels
import com.dotdash.kids.game.Pictures

private val TIPS = listOf(
    "Tap the dots in order!",
    "Who is hiding in the dots?",
    "Every picture plays a song!",
    "Collect stars to open new worlds!",
    "Fill your sticker book!",
)

@Composable
fun SoundToggles(state: AppState, musicWorld: Int?) {
    val p = state.game.progress
    RoundButton({ p.toggleSound(); state.game.audio.tap() }, if (p.sound) "Sound on" else "Sound off") {
        Icon24(if (p.sound) Ic.Sound else Ic.Mute, 24.dp, C.White)
    }
    RoundButton({
        p.toggleMusic()
        state.game.audio.music(musicWorld)
        state.game.audio.tap()
    }, if (p.music) "Music on" else "Music off") {
        Icon24(if (p.music) Ic.Music else Ic.NoMusic, 24.dp, C.White)
    }
}

@Composable
fun HomeScreen(state: AppState) {
    val game = state.game
    val p = game.progress
    var tip by remember { mutableStateOf(TIPS.random()) }
    val shakes = remember { List(3) { mutableIntStateOf(0) } }
    LaunchedEffect(Unit) { game.audio.music(0) }

    val next = Levels.all[p.nextLevel()]
    val allDone = p.completedCount() == Levels.all.size

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val short = maxHeight < 780.dp
        Sky(26, seed = 5)
        // Scrolls on very short phones; otherwise the bottom buttons sit at the bottom.
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StarCount(p.totalStars())
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { SoundToggles(state, 0) }
            }
            Logo(if (short) 56.dp else 84.dp, LogoMotion.Bob, Modifier.padding(top = 6.dp))
            Row(
                Modifier.fillMaxWidth().heightIn(min = if (short) 80.dp else 100.dp).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
            ) {
                Bubble(zipLine(tip), Modifier.widthIn(max = 210.dp).padding(bottom = 40.dp))
                ZipView(if (short) 74.dp else 92.dp, Modifier.bob(), gold = p.goldenZip())
            }
            ChunkyButton(
                onClick = { game.audio.pop(); state.openLevel(p.nextLevel()) },
                color = C.Sun, base = C.SunDark,
                modifier = Modifier.padding(top = 6.dp).fillMaxWidth(0.8f).widthIn(max = 300.dp).breathe(),
                height = if (short) 70.dp else 82.dp, depth = 9.dp,
                label = "Play",
            ) {
                Icon24(Ic.Play, 34.dp, C.Ink, stroke = 2f)
                BasicText("PLAY", style = display(if (short) 34.sp else 40.sp, C.Ink))
            }
            BasicText(
                if (allDone) "Every picture found! Play them again!" else "Level ${next.number} · ${Pictures.worlds[next.world].name}",
                Modifier.padding(top = 12.dp),
                style = body(15.sp, C.Muted),
            )
            Row(
                Modifier.padding(top = if (short) 12.dp else 20.dp).widthIn(max = 440.dp).fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Pictures.worlds.forEachIndexed { w, world ->
                    val open = p.worldUnlocked(w)
                    val done = (0 until LEVELS_PER_WORLD).count { p.isDone(w * LEVELS_PER_WORLD + it) }
                    val hero = world.pictures.first { it.id == world.heroPicture }
                    ChunkyButton(
                        onClick = {
                            if (open) {
                                game.audio.pop()
                                state.go(Screen.WorldMap(w))
                            } else {
                                game.audio.wrong()
                                shakes[w].intValue++
                                tip = "Collect **${world.unlock}** stars to open ${world.name}!"
                            }
                        },
                        color = hex(world.color), base = hex(world.dark),
                        modifier = Modifier.weight(1f).shake(shakes[w].intValue).graphicsLayer { alpha = if (open) 1f else 0.6f },
                        height = 128.dp, depth = 6.dp, shape = RoundedCornerShape(24.dp),
                        label = world.name,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box {
                                PictureIcon(hero, 64.dp)
                                if (!open) {
                                    Box(
                                        Modifier.align(Alignment.BottomEnd).offset(6.dp, 2.dp).size(30.dp).clip(CircleShape).background(C.Ink),
                                        contentAlignment = Alignment.Center,
                                    ) { Icon24(Ic.Lock, 18.dp, C.White) }
                                }
                            }
                            BasicText(world.short, style = display(16.sp, hex(world.ink)))
                            if (open) BasicText("$done/$LEVELS_PER_WORLD", style = body(13.sp, hex(world.ink), FontWeight.Black))
                            else Row(verticalAlignment = Alignment.CenterVertically) {
                                StarIcon(14.dp)
                                BasicText(" ${world.unlock}", style = body(13.sp, hex(world.ink), FontWeight.Black))
                            }
                        }
                    }
                }
            }
          }
            Row(
                Modifier.padding(top = 18.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
            Row(
                Modifier.widthIn(max = 440.dp).fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SoftButton(
                    { game.audio.pop(); state.go(Screen.Stickers(p.currentWorld())) },
                    Modifier.weight(1f), "Stickers",
                ) {
                    Icon24(Ic.Book, 26.dp, C.Sky)
                    BasicText("Stickers", style = body(15.sp, C.Cream, FontWeight.Black))
                    BasicText("${p.completedCount()}/60", style = body(12.sp, C.Muted))
                }
                Box(Modifier.weight(1f)) {
                    SoftButton({ game.audio.pop(); state.open(Overlay.Gift) }, Modifier.fillMaxWidth(), "Daily gift") {
                        Icon24(Ic.Gift, 26.dp, C.Coral)
                        BasicText("Daily gift", style = body(15.sp, C.Cream, FontWeight.Black))
                        if (!p.giftReady()) BasicText("Tomorrow", style = body(12.sp, C.Muted))
                    }
                    if (p.giftReady()) {
                        Box(
                            Modifier.align(Alignment.TopEnd).offset((-8).dp, 6.dp).size(22.dp).breathe().clip(CircleShape).background(C.Coral),
                            contentAlignment = Alignment.Center,
                        ) { BasicText("1", style = body(12.sp, C.White, FontWeight.Black)) }
                    }
                }
            }
            }
        }
    }
}

@Composable
fun SoftButton(onClick: () -> Unit, modifier: Modifier, label: String, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    ChunkyButton(onClick, C.Night2, C.Deep, modifier, height = 64.dp, depth = 5.dp, shape = RoundedCornerShape(22.dp), label = label, content = content)
}

