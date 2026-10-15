package com.voltora.evcalculator

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.voltora.evcalculator.ads.AdsManager
import com.voltora.evcalculator.ui.EvCalculatorApp
import com.voltora.evcalculator.ui.theme.EvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.parseColor("#0B0B0C")),
        )
        // Consent form (EEA/UK/US states) is shown while our animated splash plays.
        AdsManager.gatherConsentAndInit(this)

        setContent {
            EvTheme {
                EvCalculatorApp()
            }
        }
    }
}
