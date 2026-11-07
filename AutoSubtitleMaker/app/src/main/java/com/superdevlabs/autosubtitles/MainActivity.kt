package com.superdevlabs.autosubtitles

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.superdevlabs.autosubtitles.ui.navigation.AppNavHost
import com.superdevlabs.autosubtitles.ui.theme.AppTheme
import com.superdevlabs.autosubtitles.work.Notifications

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        val container = appContainer
        handleOpenIntent(intent)
        // Skip the splash when launched from a "subtitles ready" notification.
        val showSplash = savedInstanceState == null && container.pendingOpen.value == null
        // Ask for ad consent (GDPR/US states) first; ads are only initialised once allowed.
        container.consent.gather(this) { container.ads.initialize() }

        setContent {
            AppTheme {
                AppNavHost(container = container, showSplash = showSplash)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOpenIntent(intent)
    }

    /** A tapped notification asks to open a project (editor or export screen). */
    private fun handleOpenIntent(intent: Intent?) {
        val projectId = intent?.getStringExtra(Notifications.EXTRA_OPEN_PROJECT) ?: return
        val target = intent.getStringExtra(Notifications.EXTRA_OPEN_TARGET) ?: Notifications.TARGET_EDITOR
        intent.removeExtra(Notifications.EXTRA_OPEN_PROJECT)
        appContainer.pendingOpen.value = projectId to target
    }
}
