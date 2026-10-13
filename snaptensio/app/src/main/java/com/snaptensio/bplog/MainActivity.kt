package com.snaptensio.bplog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snaptensio.bplog.data.ThemeMode
import com.snaptensio.bplog.ui.AppRoot
import com.snaptensio.bplog.ui.MainViewModel
import com.snaptensio.bplog.ui.theme.SnapTensioTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep the system splash (animated icon) only until settings are read; the branded
        // Compose splash then takes over seamlessly on the same blue background.
        splash.setKeepOnScreenCondition { vm.settings.value == null }
        enableEdgeToEdge()

        // Consent (GDPR / US privacy) must be gathered before any ad request.
        appContainer.ads.gatherConsent(this) {}

        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            SnapTensioTheme(settings?.theme ?: ThemeMode.SYSTEM) {
                AppRoot(vm)
            }
        }
    }
}
