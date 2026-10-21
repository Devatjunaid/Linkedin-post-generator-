package com.devatjunaid.dripdropdraw

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.devatjunaid.dripdropdraw.ui.DripDropDrawApp

class MainActivity : ComponentActivity() {

    private val app get() = application as DripApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        // Hands the system splash over to our own animated splash straight away.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        val progress = app.progress
        val sfx = app.sfx
        sfx.soundOn = progress.soundOn
        sfx.musicOn = progress.musicOn

        setContent {
            DripDropDrawApp(
                progress = progress,
                sfx = sfx,
                onExit = { finish() },
                onRate = { openStorePage() },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        app.sfx.onForeground()
    }

    override fun onStop() {
        app.sfx.onBackground()
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun openStorePage() {
        val market = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
        try {
            startActivity(market)
        } catch (_: ActivityNotFoundException) {
            val web = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri())
            try {
                startActivity(web)
            } catch (_: ActivityNotFoundException) {
                // No store and no browser: nothing to open.
            }
        }
    }
}
