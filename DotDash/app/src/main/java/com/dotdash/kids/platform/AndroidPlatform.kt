package com.dotdash.kids.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import com.dotdash.kids.game.KeyValueStore
import com.dotdash.kids.game.Mixer
import com.dotdash.kids.game.SoundOut
import com.dotdash.kids.game.Speaker
import java.util.Locale

/** Streams the mixed game audio through one AudioTrack. */
class AndroidSound : SoundOut {
    companion object {
        const val RATE = 22050
    }

    private val mixer = Mixer()
    @Volatile private var paused = false
    private val track: AudioTrack

    init {
        val min = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(min, 4096))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track.play()
        Thread({
            val buf = ShortArray(512)
            while (true) {
                if (paused) {
                    Thread.sleep(50)
                    continue
                }
                mixer.mix(buf)
                track.write(buf, 0, buf.size) // blocks, which paces this loop
            }
        }, "game-audio").apply {
            isDaemon = true
            priority = Thread.MAX_PRIORITY
        }.start()
    }

    override fun play(pcm: ShortArray) = mixer.play(pcm)
    override fun setMusic(pcm: ShortArray?) = mixer.setMusic(pcm)

    fun pause() {
        paused = true
        track.pause()
    }

    fun resume() {
        track.play()
        paused = false
    }
}

/** Reads numbers and names aloud with the phone's text-to-speech voice. */
class AndroidSpeaker(context: Context) : Speaker, TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    @Volatile private var ready = false

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        tts.language = Locale.US
        tts.setPitch(1.25f)
        tts.setSpeechRate(0.95f)
        ready = true
    }

    override fun say(text: String) {
        if (ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "dotdash")
    }

    fun stop() {
        if (ready) tts.stop()
    }
}

/** Saved progress lives in SharedPreferences. */
class PrefsStore(context: Context) : KeyValueStore {
    private val prefs = context.getSharedPreferences("dotdash", Context.MODE_PRIVATE)
    override fun get(key: String): String? = prefs.getString(key, null)
    override fun put(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}
