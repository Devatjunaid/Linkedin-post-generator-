package com.drawtocompletefun.kids.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.drawtocompletefun.kids.R
import com.drawtocompletefun.kids.ui.Sfx

/** Short effects through SoundPool and one gentle looping tune through MediaPlayer. */
class SoundPlayer(context: Context, private val soundOn: () -> Boolean) {

    private val appContext = context.applicationContext

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val ids: Map<Sfx, Int> = mapOf(
        Sfx.Tap to R.raw.sfx_tap,
        Sfx.Pop to R.raw.sfx_pop,
        Sfx.Oops to R.raw.sfx_oops,
        Sfx.Star1 to R.raw.sfx_star1,
        Sfx.Star2 to R.raw.sfx_star2,
        Sfx.Star3 to R.raw.sfx_star3,
        Sfx.Fanfare to R.raw.sfx_fanfare,
        Sfx.Unlock to R.raw.sfx_unlock,
        Sfx.Whoosh to R.raw.sfx_whoosh,
    ).mapValues { (_, res) -> pool.load(appContext, res, 1) }

    private var music: MediaPlayer? = null
    private var musicWanted = false
    private var inForeground = true

    fun play(sfx: Sfx) {
        if (!soundOn()) return
        val id = ids[sfx] ?: return
        val volume = if (sfx == Sfx.Tap) 0.6f else 0.9f
        pool.play(id, volume, volume, 1, 0, 1f)
    }

    fun setMusic(enabled: Boolean) {
        musicWanted = enabled
        updateMusic()
    }

    fun onResume() {
        inForeground = true
        updateMusic()
    }

    fun onPause() {
        inForeground = false
        updateMusic()
    }

    private fun updateMusic() {
        if (musicWanted && inForeground) {
            val player = music ?: MediaPlayer.create(appContext, R.raw.music_loop)?.apply {
                isLooping = true
                setVolume(MUSIC_VOLUME, MUSIC_VOLUME)
            }.also { music = it }
            if (player != null && !player.isPlaying) player.start()
        } else {
            music?.takeIf { it.isPlaying }?.pause()
        }
    }

    fun release() {
        music?.release()
        music = null
        pool.release()
    }

    private companion object {
        const val MUSIC_VOLUME = 0.32f
    }
}
