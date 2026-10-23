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
        super.onCreate(savedInstanceState)
        // Transparent system bars: the app draws behind them and pads content with window insets.
        // WalkiesRoot switches the icon colour between the dark splash and the light app.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        val container = (application as WalkiesApp).container
        container.ads.gatherConsent(this)

        setContent {
            WalkiesTheme {
                WalkiesRoot(container = container, onExit = { finish() })
            }
        }
    }
}
