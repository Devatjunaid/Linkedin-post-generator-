package com.devatjunaid.dripdropdraw.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devatjunaid.dripdropdraw.ads.AdsManager
import com.devatjunaid.dripdropdraw.ads.BannerAd
import com.devatjunaid.dripdropdraw.audio.Sound
import com.devatjunaid.dripdropdraw.data.Progress
import com.devatjunaid.dripdropdraw.engine.EventType
import com.devatjunaid.dripdropdraw.engine.GameSession
import com.devatjunaid.dripdropdraw.engine.LevelDef
import com.devatjunaid.dripdropdraw.engine.Levels
import com.devatjunaid.dripdropdraw.engine.World
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val HINT_COST = 30
private const val FREE_HINT_LEVELS = 3
private const val CHEST_COINS = 50
private const val CHEST_EVERY = 5

private class WinInfo(val stars: Int, val coins: Int)

@Composable
fun GameScreen(levelNumber: Int, progress: Progress, onLevels: () -> Unit, onNext: () -> Unit) {
    val level = remember(levelNumber) { Levels.get(levelNumber) }
    val theme = Worlds[level.world.coerceIn(0, Worlds.size - 1)]
    val sfx = LocalSfx.current
    val activity = LocalContext.current.findActivity()
    val textMeasurer = rememberTextMeasurer()
    val renderer = remember(textMeasurer) { GameRenderer(textMeasurer) }

    var session by remember { mutableStateOf(GameSession(level)) }
    var state by remember { mutableStateOf(GameSession.State.READY) }
    var frame by remember { mutableIntStateOf(0) }
    var clock by remember { mutableFloatStateOf(0f) }
    var inkLeft by remember { mutableFloatStateOf(1f) }
    var showHint by remember { mutableStateOf(levelNumber == 1) }
    var hintDialog by remember { mutableStateOf(false) }
    var win by remember { mutableStateOf<WinInfo?>(null) }
    var failed by remember { mutableStateOf(false) }
    var chest by remember { mutableStateOf(false) }
    var allDone by remember { mutableStateOf(false) }

    /** A break between levels: maybe an interstitial (not in the first few levels). */
    fun adBreak(then: () -> Unit) {
        if (levelNumber <= FREE_HINT_LEVELS) then() else AdsManager.maybeShowInterstitial(activity, then)
    }

    fun restart() {
        win = null
        failed = false
        state = GameSession.State.READY
        session = GameSession(level)
    }

    // Sounds for things happening in the world.
    DisposableEffect(session) {
        session.listener = { e ->
            when (e.type) {
                EventType.SPLASH_IN_CUP -> sfx?.play(Sound.DROP, 0.45f, 0.85f + Random.nextFloat() * 0.5f, minGapMs = 70)
                EventType.SIZZLE -> sfx?.play(Sound.SIZZLE, 0.7f, minGapMs = 650)
                EventType.BALL_BONK -> sfx?.play(Sound.BONK, 0.8f, minGapMs = 160)
            }
        }
        onDispose { session.listener = null }
    }

    // The game loop: fixed physics steps, drawn every frame.
    LaunchedEffect(session) {
        val s = session
        var last = 0L
        var acc = 0f
        var lastState = s.state
        var lastInk = s.inkUsed
        while (true) {
            withFrameNanos { now ->
                if (last == 0L) last = now
                val dt = ((now - last) / 1e9f).coerceIn(0f, 0.1f)
                last = now
                acc += dt
                var steps = 0
                while (acc >= World.STEP && steps < 4) {
                    s.step()
                    acc -= World.STEP
                    steps++
                }
                if (steps == 4) acc = 0f
                clock += dt
                frame++
            }
            if (s.inkUsed > lastInk) {
                sfx?.play(Sound.DRAW, 0.6f, 0.9f + Random.nextFloat() * 0.3f, minGapMs = 130)
                lastInk = s.inkUsed
            }
            inkLeft = s.inkLeftFraction
            if (s.state != lastState) {
                lastState = s.state
                state = s.state
            }
        }
    }

    // Reactions to the level ending. Runs beside the game loop so the cup keeps laughing.
    LaunchedEffect(session, state) {
        val s = session
        when (state) {
            GameSession.State.RUNNING -> sfx?.play(Sound.WHOOSH, 0.5f)
            GameSession.State.WON -> {
                sfx?.play(Sound.WIN)
                val earned = progress.completeLevel(levelNumber, s.stars)
                delay(500)
                sfx?.play(Sound.GIGGLE)
                delay(900)
                win = WinInfo(s.stars, earned)
                chest = progress.wins % CHEST_EVERY == 0 && AdsManager.rewardedReady()
            }
            GameSession.State.FAILED -> {
                sfx?.play(Sound.FAIL)
                delay(1300)
                failed = true
            }
            else -> Unit
        }
    }

    BackHandler { adBreak(onLevels) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                levelNumber = levelNumber,
                coins = progress.coins,
                onHome = { adBreak(onLevels) },
                onRestart = { restart() },
                onHint = {
                    when {
                        showHint -> Unit
                        levelNumber <= FREE_HINT_LEVELS -> showHint = true
                        else -> hintDialog = true
                    }
                },
            )
            InkBar(level, inkLeft)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clipToBounds(),
            ) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(session) {
                            val s = session
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val fit = ViewFit.of(size.width.toFloat(), size.height.toFloat())
                                s.touchDown(fit.toWorldX(down.position.x), fit.toWorldY(down.position.y))
                                down.consume()
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    s.touchMove(fit.toWorldX(change.position.x), fit.toWorldY(change.position.y))
                                    change.consume()
                                }
                                s.touchUp()
                            }
                        },
                ) {
                    if (frame >= 0) renderer.draw(this, session, clock, showHint, theme)
                }
                if (session.state == GameSession.State.READY && session.strokes.isEmpty() && levelNumber <= 2) {
                    TutorialHint(Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
                }
            }
            BannerAd()
        }

        val w = win
        when {
            allDone -> AllDoneDialog(onClose = { allDone = false; onLevels() })
            w != null -> {
                WinPanel(
                    level = levelNumber,
                    stars = w.stars,
                    coins = w.coins,
                    onReplay = { adBreak { restart() } },
                    onNext = {
                        if (levelNumber >= Levels.COUNT) allDone = true else adBreak(onNext)
                    },
                    onHome = { adBreak(onLevels) },
                )
                if (chest) {
                    BonusChestDialog(
                        coins = CHEST_COINS,
                        onWatch = {
                            AdsManager.showRewarded(
                                activity,
                                onReward = {
                                    progress.addCoins(CHEST_COINS)
                                    sfx?.play(Sound.COIN)
                                },
                                onClosed = { chest = false },
                            )
                        },
                        onSkip = { chest = false },
                    )
                }
            }
            failed -> FailPanel(
                onRetry = { adBreak { restart() } },
                onHint = {
                    if (levelNumber <= FREE_HINT_LEVELS) {
                        showHint = true
                        restart()
                    } else {
                        hintDialog = true
                    }
                },
                onHome = { adBreak(onLevels) },
                hintShown = showHint,
            )
        }
        if (hintDialog) {
            HintDialog(
                cost = HINT_COST,
                coins = progress.coins,
                onBuy = {
                    if (progress.spendCoins(HINT_COST)) {
                        sfx?.play(Sound.COIN)
                        showHint = true
                        if (failed) restart()
                    }
                    hintDialog = false
                },
                onClose = { hintDialog = false },
            )
        }
    }
}

@Composable
private fun TopBar(levelNumber: Int, coins: Int, onHome: () -> Unit, onRestart: () -> Unit, onHint: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconCircleButton(IconKind.HOME, "Levels", onHome, size = 46.dp)
        IconCircleButton(IconKind.RESTART, "Restart", onRestart, size = 46.dp)
        Text(
            "Level $levelNumber",
            style = textStyle(22.sp, FontWeight.SemiBold),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Chip {
            GameIcon(IconKind.COIN, Modifier.size(24.dp))
            Text("$coins", style = textStyle(17.sp))
        }
        IconCircleButton(IconKind.BULB, "Hint", onHint, color = Palette.Green, size = 46.dp)
    }
}

@Composable
private fun InkBar(level: LevelDef, inkLeft: Float) {
    val three = 1f - level.threeStarInk / level.inkLimit
    val two = 1f - level.twoStarInk / level.inkLimit
    val stars = when {
        inkLeft >= three -> 3
        inkLeft >= two -> 2
        else -> 1
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameIcon(IconKind.PENCIL, Modifier.size(22.dp))
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .width(200.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Palette.Locked)
                .border(2.5.dp, Palette.Ink, RoundedCornerShape(9.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(inkLeft.coerceIn(0f, 1f))
                    .background(if (stars == 3) Palette.InkGreen else if (stars == 2) Palette.Star else Palette.Orange),
            )
            for (mark in listOf(three, two)) {
                Box(
                    Modifier
                        .offset(x = 200.dp * mark)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Palette.Ink),
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        for (i in 0 until 3) {
            GameIcon(IconKind.STAR, Modifier.size(22.dp), fill = if (i < stars) Palette.Star else Palette.StarOff)
        }
    }
}

@Composable
private fun TutorialHint(modifier: Modifier) {
    Row(
        modifier
            .background(Palette.White, RoundedCornerShape(20.dp))
            .border(2.5.dp, Palette.Ink, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GameIcon(IconKind.PENCIL, Modifier.size(22.dp))
        Text(
            "Draw a slide from the tap to the cup!",
            style = androidx.compose.ui.text.TextStyle(fontFamily = HandFont, fontSize = 20.sp, color = Palette.Ink),
        )
    }
}
