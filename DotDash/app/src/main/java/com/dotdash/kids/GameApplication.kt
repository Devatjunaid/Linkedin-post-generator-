package com.dotdash.kids

import android.app.Application
import com.dotdash.kids.game.Audio
import com.dotdash.kids.game.Game
import com.dotdash.kids.game.Progress
import com.dotdash.kids.platform.AdsManager
import com.dotdash.kids.platform.AndroidSound
import com.dotdash.kids.platform.AndroidSpeaker
import com.dotdash.kids.platform.PrefsStore
import com.dotdash.kids.ui.AppState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Holds the game for the whole process, so it survives the activity being recreated. */
class GameApplication : Application() {
    lateinit var ads: AdsManager
        private set
    lateinit var state: AppState
        private set

    override fun onCreate() {
        super.onCreate()
        val progress = Progress(PrefsStore(this)) { SimpleDateFormat("yyyy-M-d", Locale.US).format(Date()) }
        val sound = AndroidSound()
        val speaker = AndroidSpeaker(this)
        val audio = Audio(progress, sound, AndroidSound.RATE)
        ads = AdsManager(
            this,
            onPause = {
                sound.pause()
                speaker.stop()
            },
            onResume = { sound.resume() },
        )
        val game = Game(progress, audio, speaker, ads) {
            // "Exit" on the exit screen: close the app; the next launch starts at the splash.
            ads.resetSession()
            ads.currentActivity?.finishAndRemoveTask()
            state.reset()
        }
        state = AppState(game)
        ads.start()
    }
}
