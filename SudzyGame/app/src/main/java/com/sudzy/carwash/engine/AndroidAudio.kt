package com.sudzy.carwash.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

/**
 * Sound effects from assets/sfx/<name>.wav via SoundPool, looping music from assets/music/<track>.wav.
 * All sounds are synthesised by tools/make_sounds.py — nothing licensed from third parties.
 */
class AndroidAudio(private val context: Context) : Audio {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .build()
    private val ids = HashMap<String, Int>()
    private val loops = HashMap<String, Int>()
    private var player: MediaPlayer? = null
    private var track: String? = null
    private var musicOn = true
    private var sfxOn = true
    private var paused = false

    init {
        context.assets.list("sfx")?.forEach { file ->
            if (file.endsWith(".wav")) context.assets.openFd("sfx/$file").use { fd -> ids[file.removeSuffix(".wav")] = pool.load(fd, 1) }
        }
    }

    override fun sfx(name: String, volume: Float, rate: Float) {
        if (!sfxOn || paused) return
        val id = ids[name] ?: return
        val v = volume.coerceIn(0f, 1f)
        pool.play(id, v, v, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    override fun startLoop(name: String) {
        if (!sfxOn || paused || loops.containsKey(name)) return
        val id = ids[name] ?: return
        val stream = pool.play(id, 0.55f, 0.55f, 2, -1, 1f)
        if (stream != 0) loops[name] = stream
    }

    override fun stopLoop(name: String) { loops.remove(name)?.let { pool.stop(it) } }

    private fun stopAllLoops() { loops.values.forEach { pool.stop(it) }; loops.clear() }

    override fun music(track: String?) {
        if (track == this.track && player != null) return
        this.track = track
        releasePlayer()
        if (track == null || !musicOn) return
        try {
            context.assets.openFd("music/$track.wav").use { fd ->
                player = MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    isLooping = true
                    setVolume(0.45f, 0.45f)
                    prepare()
                    if (!paused) start()
                }
            }
        } catch (e: Exception) {
            releasePlayer()
        }
    }

    override fun setMusicEnabled(on: Boolean) {
        musicOn = on
        if (!on) releasePlayer() else { val t = track; track = null; music(t) }
    }

    override fun setSfxEnabled(on: Boolean) { sfxOn = on; if (!on) stopAllLoops() }

    fun pauseAll() {
        paused = true
        stopAllLoops()
        pool.autoPause()
        player?.let { if (it.isPlaying) it.pause() }
    }

    fun resumeAll() {
        paused = false
        pool.autoResume()
        if (musicOn) player?.start() ?: run { val t = track; track = null; music(t) }
    }

    private fun releasePlayer() { player?.release(); player = null }

    fun release() { releasePlayer(); pool.release() }
}
