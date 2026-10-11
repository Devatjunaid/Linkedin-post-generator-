package com.shelfguard.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.shelfguard.app.ads.AdsManager
import com.shelfguard.app.notifications.NotificationHelper
import com.shelfguard.app.ui.navigation.AppRoot
import com.shelfguard.app.ui.splash.AnimatedSplash
import com.shelfguard.app.ui.theme.ShelfGuardTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    /** Read by AppOpenAdManager: never show an app-open ad on top of the splash. */
    var isShowingSplash by mutableStateOf(true)
        private set

    private var consentResolved by mutableStateOf(false)
    private var openAlertsRequest by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val systemSplash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The system splash and our animated splash share a background, so hand over instantly.
        systemSplash.setOnExitAnimationListener { it.remove() }

        if (savedInstanceState != null) isShowingSplash = false // e.g. rotation: don't replay
        openAlertsRequest = intent.getBooleanExtra(NotificationHelper.EXTRA_OPEN_ALERTS, false)

        AdsManager.gatherConsentAndInitialize(this) { consentResolved = true }

        setContent {
            ShelfGuardTheme {
                var animationDone by remember { mutableStateOf(!isShowingSplash) }

                // Leave the splash once the animation has played and the consent step is done
                // (consent can show a form on first launch in the EEA/UK). Never wait forever.
                LaunchedEffect(animationDone, consentResolved) {
                    if (animationDone && consentResolved) isShowingSplash = false
                }
                LaunchedEffect(animationDone) {
                    if (animationDone) {
                        delay(MAX_EXTRA_SPLASH_MS)
                        isShowingSplash = false
                    }
                }

                Crossfade(targetState = isShowingSplash, animationSpec = tween(450), label = "splash") { splash ->
                    if (splash) {
                        AnimatedSplash(onFinished = { animationDone = true })
                    } else {
                        AppRoot(
                            openAlertsRequest = openAlertsRequest,
                            onAlertsRequestHandled = { openAlertsRequest = false },
                            onExit = { finish() },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(NotificationHelper.EXTRA_OPEN_ALERTS, false)) openAlertsRequest = true
    }

    private companion object {
        const val MAX_EXTRA_SPLASH_MS = 4_000L
    }
}
