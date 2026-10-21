package com.feedthewrongthing.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.io.File

/**
 * Plays generated sound effects through a SoundPool and loops generated music with
 * static AudioTracks (gapless). All generation happens on a background thread.
 */
class AudioEngine(context: Context) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val ids = IntArray(Sfx.entries.size)
    private val lastPlay = LongArray(Sfx.entries.size)
    private val tracks = HashMap<Music, AudioTrack>()
    private var current: Music? = null
    private var wanted: Music? = null
    private var paused = false
    private var ducked = false

    var sfxOn = true
    var musicOn = true
        set(value) {
            field = value
            applyMusic()
        }

    fun start() {
        Thread({
            val dir = File(appContext.cacheDir, "sfx")
            for (s in Sfx.entries) {
                try {
                    val f = File(dir, "${s.name.lowercase()}_v2.wav")
                    if (!f.exists()) Synth.writeWav(f, Synth.toPcm(s.render()))
                    val id = pool.load(f.absolutePath, 1)
                    ids[s.ordinal] = id
                } catch (_: Exception) {
                }
            }
            for (m in Music.entries) {
                try {
                    val pcm = m.render()
                    val track = buildTrack(pcm)
                    main.post {
                        tracks[m] = track
                        applyMusic()
                    }
                } catch (_: Exception) {
                }
            }
        }, "audio-gen").start()
    }

    private fun buildTrack(pcm: ShortArray): AudioTrack {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val fmt = AudioFormat.Builder()
            .setSampleRate(Synth.SR)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val t = AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(fmt)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        t.write(pcm, 0, pcm.size)
        t.setLoopPoints(0, pcm.size, -1)
        t.setVolume(0.42f)
        return t
    }

    fun play(s: Sfx, volume: Float = 1f, rate: Float = 1f) {
        if (!sfxOn || paused) return
        val id = ids[s.ordinal]
        if (id == 0) return
        val now = SystemClock.uptimeMillis()
        if (now - lastPlay[s.ordinal] < 35) return
        lastPlay[s.ordinal] = now
        pool.play(id, volume, volume, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    fun music(m: Music) {
        wanted = m
        applyMusic()
    }

    private fun applyMusic() {
        val want = if (musicOn && !paused && !ducked) wanted else null
        if (current != want) {
            current?.let { tracks[it]?.let { t -> safe { t.pause() } } }
            current = null
        }
        if (want != null && current == null) {
            val t = tracks[want] ?: return
            safe { t.play() }
            current = want
        }
    }

    /** Silences everything while a full-screen ad is showing. */
    fun duck(on: Boolean) {
        ducked = on
        if (on) pool.autoPause() else pool.autoResume()
        applyMusic()
    }

    fun onPause() {
        paused = true
        pool.autoPause()
        applyMusic()
    }

    fun onResume() {
        paused = false
        if (!ducked) pool.autoResume()
        applyMusic()
    }

    fun release() {
        for (t in tracks.values) safe { t.release() }
        tracks.clear()
        pool.release()
    }

    private inline fun safe(block: () -> Unit) {
        try { block() } catch (_: IllegalStateException) { }
    }
}
