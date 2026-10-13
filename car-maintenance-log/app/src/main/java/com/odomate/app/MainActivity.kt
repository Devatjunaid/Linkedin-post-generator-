package com.odomate.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.odomate.app.reminders.Notifications
import com.odomate.app.ui.OdoMateRoot
import com.odomate.app.ui.navigation.LaunchTarget

class MainActivity : ComponentActivity() {
    private var consentDone by mutableStateOf(false)
    private var launchTarget by mutableStateOf<LaunchTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // The system splash only shows the brand background; our animated splash takes over immediately.
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val container = (application as OdoMateApp).container

        if (savedInstanceState == null) launchTarget = targetFrom(intent)

        container.ads.gatherConsent(this) { consentDone = true }

        setContent {
            OdoMateRoot(
                container = container,
                consentDone = consentDone,
                launchTarget = launchTarget,
                onLaunchTargetHandled = { launchTarget = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        targetFrom(intent)?.let { launchTarget = it }
    }

    private fun targetFrom(intent: Intent?): LaunchTarget? {
        if (intent == null) return null
        if (intent.getBooleanExtra(Notifications.EXTRA_OPEN_SCAN, false)) return LaunchTarget.Scan
        val serviceId = intent.getLongExtra(Notifications.EXTRA_SERVICE_ID, 0L)
        val vehicleId = intent.getLongExtra(Notifications.EXTRA_VEHICLE_ID, 0L)
        return if (serviceId != 0L || vehicleId != 0L) LaunchTarget.Service(serviceId, vehicleId) else null
    }
}
