package com.harrycodess.drawingkids

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.harrycodess.drawingkids.ads.Ads
import com.harrycodess.drawingkids.ads.Interstitials
import com.harrycodess.drawingkids.art.Animals
import com.harrycodess.drawingkids.drawing.CanvasDocument
import com.harrycodess.drawingkids.ui.coloring.ColoringScreen
import com.harrycodess.drawingkids.ui.coloring.FreeDrawScreen
import com.harrycodess.drawingkids.ui.coloring.PickerScreen
import com.harrycodess.drawingkids.ui.gallery.GalleryScreen
import com.harrycodess.drawingkids.ui.home.HomeScreen
import com.harrycodess.drawingkids.ui.splash.SplashScreen
import com.harrycodess.drawingkids.ui.theme.Kid
import com.harrycodess.drawingkids.ui.theme.TwilioKidsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val bars = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        setContent {
            TwilioKidsTheme { KidsApp(this) }
        }
    }
}

private sealed interface Screen {
    data object Splash : Screen
    data object Home : Screen
    data object FreeDraw : Screen
    data object Picker : Screen
    data class Coloring(val animalId: String) : Screen
    data object Gallery : Screen
}

@Composable
private fun KidsApp(activity: Activity) {
    var screen by remember { mutableStateOf<Screen>(Screen.Splash) }
    val freeDrawDoc = remember { CanvasDocument() }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = screen != Screen.Splash && screen != Screen.Home) {
        screen = if (screen is Screen.Coloring) Screen.Picker else Screen.Home
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Kid.Cream)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                (fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.96f)) togetherWith fadeOut(tween(180))
            },
            label = "screens",
        ) { s ->
            when (s) {
                Screen.Splash -> SplashScreen(
                    onFinished = {
                        scope.launch {
                            // Give the cold-start app open ad a moment to arrive, then show it.
                            var waited = 0
                            while (!Ads.appOpen.isAvailable && waited < 2500) {
                                delay(100)
                                waited += 100
                            }
                            Ads.appOpen.showIfAvailable(activity) {
                                screen = Screen.Home
                                Ads.appOpen.resumeAdsEnabled = true
                            }
                        }
                    },
                )

                Screen.Home -> HomeScreen(
                    onFreeDraw = { Interstitials.show(activity) { screen = Screen.FreeDraw } },
                    onColor = { screen = Screen.Picker },
                    onGallery = { screen = Screen.Gallery },
                )

                Screen.FreeDraw -> FreeDrawScreen(freeDrawDoc, onHome = { screen = Screen.Home })

                Screen.Picker -> PickerScreen(
                    onBack = { screen = Screen.Home },
                    onPick = { art -> Interstitials.show(activity) { screen = Screen.Coloring(art.id) } },
                )

                is Screen.Coloring -> {
                    val art = remember(s.animalId) { Animals.byId(s.animalId) }
                    if (art != null) ColoringScreen(art, onBack = { screen = Screen.Picker })
                }

                Screen.Gallery -> GalleryScreen(onHome = { screen = Screen.Home })
            }
        }
    }
}
