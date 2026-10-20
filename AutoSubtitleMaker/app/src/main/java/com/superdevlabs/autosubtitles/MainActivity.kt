package com.superdevlabs.autosubtitles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.superdevlabs.autosubtitles.ui.navigation.AppNavHost
import com.superdevlabs.autosubtitles.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        val container = appContainer
        // Ask for ad consent (GDPR/US states) first; ads are only initialised once allowed.
        container.consent.gather(this) { container.ads.initialize() }

        setContent {
            AppTheme {
                AppNavHost(container = container, showSplash = savedInstanceState == null)
            }
        }
    }
}
