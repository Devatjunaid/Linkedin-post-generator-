package com.glowdash.warningidentifier

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.glowdash.warningidentifier.ads.AdsManager
import com.glowdash.warningidentifier.data.HistoryStore
import com.glowdash.warningidentifier.ui.WarningApp
import com.glowdash.warningidentifier.ui.theme.WarningTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        HistoryStore.init(this)
        AdsManager.gatherConsentAndInit(this)

        setContent {
            WarningTheme {
                WarningApp()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) AdsManager.releaseNatives()
    }
}
