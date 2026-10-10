package com.dotdash.kids

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.dotdash.kids.platform.AdBanner
import com.dotdash.kids.ui.C
import com.dotdash.kids.ui.DotDashApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // system launch screen; the animated splash follows in Compose
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as GameApplication
        setContent {
            // Back button: closes pop-ups, steps back a screen, and on Home opens the exit screen.
            BackHandler { app.state.back() }
            Box(Modifier.fillMaxSize().background(C.Night)) {
                Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                    DotDashApp(app.state) { AdBanner(app.ads) }
                }
            }
        }
    }
}
