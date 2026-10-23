package com.superdevlabs.petfood.portioncalc

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.superdevlabs.petfood.portioncalc.ui.AppNavHost
import com.superdevlabs.petfood.portioncalc.ui.LocalAppContainer
import com.superdevlabs.petfood.portioncalc.ui.PetFoodTheme
import com.superdevlabs.petfood.portioncalc.ui.splash.AnimatedSplash

class MainActivity : FragmentActivity() {
    private var consentDone by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        // The system splash is a plain brand colour; the animated logo follows in Compose.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Draw behind the status and navigation bars; every screen applies WindowInsets itself.
        enableEdgeToEdge()

        val container = (application as PetFoodApp).container
        container.ads.gatherConsent(this) { consentDone = true }

        setContent {
            PetFoodTheme {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    var showSplash by rememberSaveable { mutableStateOf(savedInstanceState == null) }
                    AnimatedContent(
                        targetState = showSplash,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "splash",
                    ) { splash ->
                        if (splash) {
                            AnimatedSplash(consentDone = consentDone, onFinished = { showSplash = false })
                        } else {
                            AppNavHost()
                        }
                    }
                }
            }
        }
    }
}
