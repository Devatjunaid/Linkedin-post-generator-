package com.devatjunaid.dripdropdraw.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.devatjunaid.dripdropdraw.audio.Sfx
import com.devatjunaid.dripdropdraw.data.Progress
import com.devatjunaid.dripdropdraw.engine.Levels

val LocalSfx = staticCompositionLocalOf<Sfx?> { null }

private sealed interface Screen {
    data object Splash : Screen
    data object Menu : Screen
    data object LevelMap : Screen
    data class Play(val level: Int) : Screen
}

@Composable
fun DripDropDrawApp(progress: Progress, sfx: Sfx, onExit: () -> Unit, onRate: () -> Unit) {
    var screen by remember { mutableStateOf<Screen>(Screen.Splash) }
    CompositionLocalProvider(LocalSfx provides sfx) {
        Box(
            Modifier
                .fillMaxSize()
                .paper()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            when (val s = screen) {
                Screen.Splash -> SplashScreen(onDone = { screen = Screen.Menu })
                Screen.Menu -> MenuScreen(
                    progress = progress,
                    onPlay = { screen = Screen.Play(progress.unlocked) },
                    onLevels = { screen = Screen.LevelMap },
                    onExit = onExit,
                    onRate = onRate,
                )
                Screen.LevelMap -> LevelsScreen(
                    progress = progress,
                    onBack = { screen = Screen.Menu },
                    onPick = { screen = Screen.Play(it) },
                )
                is Screen.Play -> key(s.level) {
                    GameScreen(
                        levelNumber = s.level,
                        progress = progress,
                        onLevels = { screen = Screen.LevelMap },
                        onNext = {
                            screen = if (s.level < Levels.COUNT) Screen.Play(s.level + 1) else Screen.LevelMap
                        },
                    )
                }
            }
        }
    }
}

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
