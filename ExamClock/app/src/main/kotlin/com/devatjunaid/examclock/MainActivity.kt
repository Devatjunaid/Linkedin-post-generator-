package com.devatjunaid.examclock

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devatjunaid.examclock.data.prefs.ThemeMode
import com.devatjunaid.examclock.data.prefs.UserPreferences
import com.devatjunaid.examclock.ui.AppNavigation
import com.devatjunaid.examclock.ui.theme.ExamClockTheme

class MainActivity : ComponentActivity() {

    @Volatile
    private var preferencesLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // System splash (animated icon on Android 12+) stays only until settings are read,
        // then hands over to the in-app animated splash.
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { !preferencesLoaded }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as ExamClockApp).container
        container.ads.gatherConsent(this)

        setContent {
            val prefs by container.preferences.preferences.collectAsStateWithLifecycle<UserPreferences?>(initialValue = null)
            val current = prefs ?: return@setContent
            preferencesLoaded = true

            val dark = when (current.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Keep system-bar icon colours in sync with the in-app theme (edge-to-edge, transparent bars).
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                )
                onDispose {}
            }

            CompositionLocalProvider(LocalAppContainer provides container) {
                ExamClockTheme(current.themeMode) {
                    AppNavigation(onboardingDone = current.onboardingDone)
                }
            }
        }
    }
}
