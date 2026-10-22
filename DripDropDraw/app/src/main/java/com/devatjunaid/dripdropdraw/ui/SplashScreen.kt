package com.devatjunaid.dripdropdraw.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devatjunaid.dripdropdraw.audio.Sound
import com.devatjunaid.dripdropdraw.engine.CupDef
import com.devatjunaid.dripdropdraw.engine.FaucetDef
import com.devatjunaid.dripdropdraw.engine.GameSession
import com.devatjunaid.dripdropdraw.engine.LevelDef
import com.devatjunaid.dripdropdraw.engine.Pt
import com.devatjunaid.dripdropdraw.engine.WallDef
import com.devatjunaid.dripdropdraw.engine.World
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** A tiny real level that plays itself behind the title. */
private fun splashLevel(): LevelDef {
    val cup = CupDef(x = 700f, bottom = 1390f, topHalf = 95f, bottomHalf = 68f, height = 185f, target = 26)
    return LevelDef(
        number = 1,
        world = 0,
        gravityScale = 1f,
        faucets = listOf(FaucetDef(260f, 640f, 70)),
        cups = listOf(cup),
        walls = listOf(WallDef(560f, 1414f, 840f, 1414f)),
        balls = emptyList(),
        seesaws = emptyList(),
        movers = emptyList(),
        spinners = emptyList(),
        fans = emptyList(),
        fires = emptyList(),
        hint = listOf(listOf(Pt(185f, 780f), Pt(700f - 66f, 1390f - 185f - 45f))),
    )
}

/**
 * Animated splash: the title bounces in, a pencil draws a line and water slides into the cup,
 * which giggles. Tap to skip.
 */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val sfx = LocalSfx.current
    val windowInfo = LocalWindowInfo.current
    val session = remember { GameSession(splashLevel()) }
    val textMeasurer = rememberTextMeasurer()
    val renderer = remember(textMeasurer) { GameRenderer(textMeasurer) }
    var frame by remember { mutableIntStateOf(0) }
    var clock by remember { mutableFloatStateOf(0f) }
    val titleTop = remember { Animatable(0f) }
    val titleBottom = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    var finished by remember { mutableIntStateOf(0) }

    fun finish() {
        if (finished == 0) {
            finished = 1
            onDone()
        }
    }

    LaunchedEffect(Unit) {
        var last = 0L
        var acc = 0f
        while (true) {
            withFrameNanos { now ->
                if (last == 0L) last = now
                val dt = ((now - last) / 1e9f).coerceIn(0f, 0.1f)
                last = now
                acc += dt
                while (acc >= World.STEP) {
                    session.step()
                    acc -= World.STEP
                }
                clock += dt
                frame++
            }
        }
    }

    LaunchedEffect(Unit) {
        // Wait until the player can see the screen (not behind the lock screen).
        snapshotFlow { windowInfo.isWindowFocused }.first { it }
        sfx?.play(Sound.WHOOSH, 0.6f)
        launch { titleTop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f)) }
        delay(250)
        launch { titleBottom.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 300f)) }
        delay(350)

        // The pencil draws the line.
        val line = session.level.hint.first()
        val a = line[0]
        val b = line[1]
        val steps = 28
        session.touchDown(a.x, a.y)
        for (i in 1..steps) {
            val t = i / steps.toFloat()
            session.touchMove(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
            if (i % 5 == 0) sfx?.play(Sound.DRAW, 0.5f)
            delay(28)
        }
        session.touchUp()

        // Wait for the cup to fill (but never too long).
        var waited = 0
        while (waited < 3200 && session.world.cups[0].fill < session.level.cups[0].target) {
            delay(50)
            waited += 50
        }
        sfx?.play(Sound.GIGGLE)
        delay(900)
        fade.animateTo(0f, tween(250))
        finish()
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .pointerInput(Unit) { detectTapGestures { if (clock > 0.8f) finish() } },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (frame >= 0) renderer.draw(this, session, clock, false, Worlds[0])
        }
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedTitle(
                "Drip Drop",
                Palette.Water,
                54.sp,
                Modifier.graphicsLayer {
                    scaleX = titleTop.value
                    scaleY = titleTop.value
                    rotationZ = (1f - titleTop.value) * -20f
                },
            )
            OutlinedTitle(
                "Draw",
                Palette.Orange,
                66.sp,
                Modifier.graphicsLayer {
                    scaleX = titleBottom.value
                    scaleY = titleBottom.value
                    rotationZ = (1f - titleBottom.value) * 20f
                },
            )
        }
    }
}
