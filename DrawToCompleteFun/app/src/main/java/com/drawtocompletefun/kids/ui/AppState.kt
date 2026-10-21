package com.drawtocompletefun.kids.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.drawtocompletefun.kids.game.Levels
import com.drawtocompletefun.kids.game.Progress

sealed interface Screen {
    data object Splash : Screen
    data object Home : Screen
    data class Map(val world: Int) : Screen
    data class Game(val level: Int, val run: Int = 0) : Screen
    data object Stickers : Screen
}

enum class GateAction { Rate, Privacy }

sealed interface Overlay {
    data object Exit : Overlay
    data object Settings : Overlay
    data class Treasure(val level: Int) : Overlay
    data class ParentGate(val action: GateAction) : Overlay
}

/** Navigation and app wide actions. Ads are only shown at the few "big" moments below. */
class AppState(val progress: Progress, val platform: GamePlatform) {

    var screen by mutableStateOf<Screen>(Screen.Splash)
        private set

    var overlay by mutableStateOf<Overlay?>(null)

    /** Becomes true once consent and ads start-up have finished (or timed out). */
    var startupDone by mutableStateOf(false)

    private var runCounter = 0

    fun finishSplash() {
        screen = Screen.Home
    }

    fun goHome() {
        screen = Screen.Home
    }

    fun openMap(world: Int = Levels.worldOf(progress.currentLevel).index) {
        screen = Screen.Map(world)
    }

    fun openStickers(withAd: Boolean = true) {
        if (withAd) platform.showInterstitial { screen = Screen.Stickers } else screen = Screen.Stickers
    }

    /** Opens a level, asking for the treasure video first on every 5th level. */
    fun openLevel(level: Int) {
        if (!progress.isUnlocked(level)) return
        if (progress.needsTreasureVideo(level)) {
            overlay = Overlay.Treasure(level)
        } else {
            screen = Screen.Game(level, ++runCounter)
        }
    }

    fun treasureOpened(level: Int) {
        progress.openTreasure(level)
        overlay = null
        screen = Screen.Game(level, ++runCounter)
    }

    /** Player quits a level half way: back to the map, an interstitial may show. */
    fun quitLevel(level: Int) {
        platform.showInterstitial { screen = Screen.Map(Levels.worldOf(level).index) }
    }

    /** Restart button inside a level: no ad, kids just want to try again. */
    fun restart(level: Int) {
        screen = Screen.Game(level, ++runCounter)
    }

    fun replay(level: Int) {
        platform.showInterstitial { screen = Screen.Game(level, ++runCounter) }
    }

    fun next(level: Int) {
        if (level >= Levels.COUNT) {
            platform.showInterstitial { screen = Screen.Map(Levels.worldOf(level).index) }
            return
        }
        val n = level + 1
        if (progress.needsTreasureVideo(n)) {
            // Never stack an interstitial right before a reward video.
            screen = Screen.Map(Levels.worldOf(n).index)
            overlay = Overlay.Treasure(n)
        } else {
            platform.showInterstitial { screen = Screen.Game(n, ++runCounter) }
        }
    }

    fun backToMapFromResult(level: Int) {
        platform.showInterstitial { screen = Screen.Map(Levels.worldOf(level).index) }
    }

    fun gatePassed(action: GateAction) {
        overlay = null
        when (action) {
            GateAction.Rate -> platform.requestReview()
            GateAction.Privacy -> platform.openPrivacyPolicy()
        }
    }

    /** System back. Returns true when handled. */
    fun onBack(): Boolean {
        if (overlay != null) {
            overlay = null
            return true
        }
        when (val s = screen) {
            Screen.Splash -> Unit
            Screen.Home -> overlay = Overlay.Exit
            is Screen.Map, Screen.Stickers -> screen = Screen.Home
            is Screen.Game -> quitLevel(s.level)
        }
        return true
    }
}
