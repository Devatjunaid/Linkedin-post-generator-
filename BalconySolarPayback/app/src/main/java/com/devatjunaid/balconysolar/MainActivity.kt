package com.devatjunaid.balconysolar

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.devatjunaid.balconysolar.ads.AdsManager
import com.devatjunaid.balconysolar.ui.AppRoot
import com.devatjunaid.balconysolar.ui.theme.BalconySolarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        // Consent first; the Mobile Ads SDK starts only once ads may be requested.
        AdsManager.gatherConsent(this)
        setContent {
            BalconySolarTheme { AppRoot() }
        }
    }
}
