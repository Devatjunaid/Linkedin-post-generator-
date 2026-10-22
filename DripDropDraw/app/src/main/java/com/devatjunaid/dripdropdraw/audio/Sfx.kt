package com.devatjunaid.dripdropdraw.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.SystemClock
import com.devatjunaid.dripdropdraw.R

enum class Sound(val res: Int) {
    TAP(R.raw.sfx_tap),
    POP(R.raw.sfx_pop),
    COIN(R.raw.sfx_coin),
    DROP(R.raw.sfx_drop),
    DRAW(R.raw.sfx_draw),
    SIZZLE(R.raw.sfx_sizzle),
    BONK(R.raw.sfx_bonk),
    WIN(R.raw.sfx_win),
    STAR(R.raw.sfx_star),
    FAIL(R.raw.sfx_fail),
    GIGGLE(R.raw.sfx_giggle),
    WHOOSH(R.raw.sfx_whoosh),
}

/** Sound effects (SoundPool) and the looping background music (MediaPlayer). */
class Sfx(context: Context) {

    private val app = context.applicationContext
    private val pool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val ids = Sound.entries.associateWith { pool.load(app, it.res, 1) }
    private val lastPlayed = LongArray(Sound.entries.size)

    var soundOn = true
    var musicOn = true
        set(value) {
            field = value
            if (value) resumeMusic() else pauseMusic()
        }

    private var music: MediaPlayer? = null
    private var inForeground = false

    /**
     * Plays a sound. [minGapMs] stops the same sound from stacking up (dozens of drops can
     * land in a cup at the same time).
     */
    fun play(sound: Sound, volume: Float = 1f, rate: Float = 1f, minGapMs: Long = 0) {
        if (!soundOn || !inForeground) return
        val now = SystemClock.uptimeMillis()
        if (now - lastPlayed[sound.ordinal] < minGapMs) return
        lastPlayed[sound.ordinal] = now
        val id = ids[sound] ?: return
        pool.play(id, volume, volume, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    fun onForeground() {
        inForeground = true
        resumeMusic()
    }

    fun onBackground() {
        inForeground = false
        pauseMusic()
    }

    private fun resumeMusic() {
        if (!musicOn || !inForeground) return
        val player = music ?: MediaPlayer.create(app, R.raw.music_loop)?.apply {
            isLooping = true
            setVolume(0.35f, 0.35f)
        }?.also { music = it } ?: return
        if (!player.isPlaying) player.start()
    }

    private fun pauseMusic() {
        music?.let { if (it.isPlaying) it.pause() }
    }

    fun release() {
        pool.release()
        music?.release()
        music = null
    }
}
