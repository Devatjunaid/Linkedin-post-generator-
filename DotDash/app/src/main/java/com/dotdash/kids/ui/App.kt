package com.dotdash.kids.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dotdash.kids.game.Game
import com.dotdash.kids.game.Levels
import com.dotdash.kids.game.Picture

sealed interface Screen {
    data object Splash : Screen
    data object Home : Screen
    data class WorldMap(val world: Int) : Screen
    /** [visit] makes "play again" a fresh screen even for the same level. */
    data class Play(val index: Int, val visit: Long = System.nanoTime()) : Screen
    data class Stickers(val world: Int) : Screen
}

sealed interface Overlay {
    data object Gift : Overlay
    data object Exit : Overlay
    class ColorIt(val picture: Picture, val onPick: (String) -> Unit) : Overlay
    class Sticker(val picture: Picture) : Overlay
}

/** Navigation for the whole game: one screen plus a stack of pop-ups. */
class AppState(val game: Game) {
    var screen by mutableStateOf<Screen>(Screen.Splash)
        private set
    val overlays = mutableStateListOf<Overlay>()
    var bannerOn by mutableStateOf(false)
        private set
    /** True while the exit screen is waving goodbye; back is ignored. */
    var leaving by mutableStateOf(false)

    fun go(s: Screen) {
        overlays.clear()
        if (s != Screen.Splash) bannerOn = true
        screen = s
    }

    /** After "Exit": the next launch starts from the splash again. */
    fun reset() {
        overlays.clear()
        leaving = false
        bannerOn = false
        screen = Screen.Splash
    }

    fun open(o: Overlay) {
        overlays.add(o)
    }

    fun close(o: Overlay) {
        overlays.remove(o)
    }

    /** Opening any level goes through the interstitial first. */
    fun openLevel(index: Int) = game.ads.onLevelOpen { go(Screen.Play(index)) }

    /** Android back button. */
    fun back() {
        if (leaving) return
        if (overlays.isNotEmpty()) {
            overlays.removeAt(overlays.lastIndex)
            return
        }
        when (val s = screen) {
            Screen.Splash -> Unit
            Screen.Home -> {
                game.audio.pop()
                open(Overlay.Exit)
            }
            is Screen.WorldMap, is Screen.Stickers -> go(Screen.Home)
            is Screen.Play -> go(Screen.WorldMap(Levels.all[s.index].world))
        }
    }
}

/**
 * The whole game. [banner] is the AdMob banner, shown under the game after the
 * splash so it never covers anything.
 */
@Composable
fun DotDashApp(state: AppState, banner: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(C.Night)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(
                targetState = state.screen,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "screen",
            ) { s ->
                key(s) {
                    when (s) {
                        Screen.Splash -> SplashScreen(state)
                        Screen.Home -> HomeScreen(state)
                        is Screen.WorldMap -> MapScreen(state, s.world)
                        is Screen.Play -> PlayScreen(state, s.index)
                        is Screen.Stickers -> StickersScreen(state, s.world)
                    }
                }
            }
            for (o in state.overlays) {
                key(o) { OverlayHost(state, o) }
            }
        }
        if (state.bannerOn) banner()
    }
}
