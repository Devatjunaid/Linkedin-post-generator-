package com.brickbeam.buildmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brickbeam.buildmate.ui.AppViewModel
import com.brickbeam.buildmate.ui.BuildMateRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The system splash is dismissed immediately; the animated Compose splash takes over
        // on the same background colour.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = appContainer
        // Consent (UMP) must be gathered before any ad request; ads initialise afterwards.
        container.ads.gatherConsent(this)

        setContent {
            val vm: AppViewModel = viewModel(factory = AppViewModel.factory(container))
            BuildMateRoot(vm, container.ads, this)
        }
    }
}
