package com.dailycash.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.dailycash.tracker.ads.AdsConsent
import com.dailycash.tracker.ui.DailyCashNav
import com.dailycash.tracker.ui.theme.DailyCashTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Ask for ad consent where the law requires it; ads start only after this.
        AdsConsent.gather(this)
        setContent {
            DailyCashTheme {
                DailyCashNav(onExit = { finish() })
            }
        }
    }
}
