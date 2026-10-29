package com.drawtocompletefun.kids.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.drawtocompletefun.kids.ui.screens.AdBreakNotice
import com.drawtocompletefun.kids.ui.screens.ExitDialog
import com.drawtocompletefun.kids.ui.screens.GameScreen
import com.drawtocompletefun.kids.ui.screens.HomeScreen
import com.drawtocompletefun.kids.ui.screens.LevelMapScreen
import com.drawtocompletefun.kids.ui.screens.ParentGateDialog
import com.drawtocompletefun.kids.ui.screens.SettingsDialog
import com.drawtocompletefun.kids.ui.screens.SplashScreen
import com.drawtocompletefun.kids.ui.screens.StickersScreen
import com.drawtocompletefun.kids.ui.screens.TreasureDialog

@Composable
fun AppRoot(state: AppState) {
    val progress = state.progress

    LaunchedEffect(progress.musicOn, state.screen is Screen.Splash) {
        state.platform.setMusicEnabled(progress.musicOn && state.screen !is Screen.Splash)
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = state.screen,
            transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(200)) },
            label = "screens",
        ) { screen ->
            when (screen) {
                Screen.Splash -> SplashScreen(ready = state.startupDone, onFinished = state::finishSplash)
                Screen.Home -> HomeScreen(state)
                is Screen.Map -> LevelMapScreen(state, screen.world)
                is Screen.Game -> GameScreen(state, screen.level, screen.run)
                Screen.Stickers -> StickersScreen(state)
            }
        }

        val overlay = state.overlay
        AnimatedVisibility(
            visible = overlay != null,
            enter = fadeIn(tween(160)) + scaleIn(initialScale = 0.9f),
            exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.95f),
        ) {
            // Keep showing the last overlay while it animates out.
            val shown = overlay ?: return@AnimatedVisibility
            when (shown) {
                Overlay.Exit -> ExitDialog(state)
                Overlay.Settings -> SettingsDialog(state)
                is Overlay.Treasure -> TreasureDialog(state, shown.level)
                is Overlay.ParentGate -> ParentGateDialog(state, shown.action)
                is Overlay.AdBreak -> AdBreakNotice(state, shown)
            }
        }
    }
}
