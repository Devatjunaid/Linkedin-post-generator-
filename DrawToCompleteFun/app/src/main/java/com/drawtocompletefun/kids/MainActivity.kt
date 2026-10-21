package com.drawtocompletefun.kids

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.drawtocompletefun.kids.ads.AdsManager
import com.drawtocompletefun.kids.audio.SoundPlayer
import com.drawtocompletefun.kids.data.PrefsStore
import com.drawtocompletefun.kids.game.Progress
import com.drawtocompletefun.kids.ui.AppRoot
import com.drawtocompletefun.kids.ui.AppState

class MainActivity : ComponentActivity() {

    private lateinit var sounds: SoundPlayer
    private lateinit var ads: AdsManager
    private lateinit var appState: AppState

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val progress = Progress(PrefsStore(this))
        sounds = SoundPlayer(this) { progress.soundOn }
        ads = AdsManager(this)
        appState = AppState(progress, AndroidPlatform(this, sounds, ads))
        ads.start { appState.startupDone = true }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    appState.onBack()
                }
            },
        )

        setContent { AppRoot(appState) }
    }

    override fun onResume() {
        super.onResume()
        sounds.onResume()
    }

    override fun onPause() {
        sounds.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        ads.destroy()
        sounds.release()
        super.onDestroy()
    }
}
