package com.driveready.testprep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.driveready.testprep.ui.navigation.DriveReadyNavHost
import com.driveready.testprep.ui.theme.DriveReadyTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // System splash hands over to the animated Compose splash as soon as settings load.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { viewModel.settings.value == null }

        // Edge-to-edge using the modern API only; insets are applied per screen in Compose.
        enableEdgeToEdge()

        setContent {
            DriveReadyTheme {
                DriveReadyNavHost(viewModel = viewModel)
            }
        }
    }
}
