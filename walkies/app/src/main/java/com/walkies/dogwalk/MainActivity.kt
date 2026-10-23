package com.walkies.dogwalk

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.walkies.dogwalk.ui.WalkiesRoot
import com.walkies.dogwalk.ui.theme.WalkiesTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // The system launch screen hands straight over to the animated splash in Compose.
        installSplashScreen()
        // Light, transparent bars on every device: the app draws behind them and pads with insets.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        val container = (application as WalkiesApp).container
        container.ads.gatherConsent(this)

        setContent {
            WalkiesTheme {
                WalkiesRoot(container = container, onExit = { finish() })
            }
        }
    }
}
