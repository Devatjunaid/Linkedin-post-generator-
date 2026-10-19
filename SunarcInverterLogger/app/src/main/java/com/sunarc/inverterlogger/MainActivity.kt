package com.sunarc.inverterlogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sunarc.inverterlogger.ui.AppViewModel
import com.sunarc.inverterlogger.ui.SunarcRoot
import com.sunarc.inverterlogger.ui.theme.SunarcTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // The system splash shows the static mark for a moment; the animated splash follows in Compose.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        (application as SunarcApp).container.ads.gatherConsent(this)
        setContent {
            SunarcTheme {
                SunarcRoot(vm)
            }
        }
    }
}
