package com.dotdash.kids.game

/** Plays PCM produced by [SoundBank] (AudioTrack on Android). */
interface SoundOut {
    fun play(pcm: ShortArray)
    fun setMusic(pcm: ShortArray?)
}

/** Reads numbers and names aloud (TextToSpeech on Android). */
interface Speaker {
    fun say(text: String)
}

/** Shows the interstitial when a level opens, then runs [then]. */
interface LevelAds {
    fun onLevelOpen(then: () -> Unit)
}

/** Game-facing sound API. Respects the sound and music switches. */
class Audio(private val progress: Progress, private val out: SoundOut, sampleRate: Int) {
    private val bank = SoundBank(sampleRate)
    @Volatile private var wantedMusic: Int? = null

    init {
        // Render the most common sounds in the background so the first taps are instant.
        Thread {
            (0 until 11).forEach { bank.get("dot:$it") }
            listOf("wrong", "pop", "tap", "whoosh", "sparkle", "star:0", "star:1", "star:2").forEach { bank.get(it) }
        }.apply { isDaemon = true }.start()
    }

    private fun sfx(key: String) {
        if (progress.sound) out.play(bank.get(key))
    }

    fun dot(i: Int) = sfx("dot:$i")
    fun wrong() = sfx("wrong")
    fun pop() = sfx("pop")
    fun tap() = sfx("tap")
    fun whoosh() = sfx("whoosh")
    fun sparkle() = sfx("sparkle")
    fun star(k: Int) = sfx("star:$k")
    fun fanfare() = sfx("fanfare")
    fun unlock() = sfx("unlock")
    fun gift() = sfx("gift")
    fun bye() = sfx("bye")
    fun voice(spec: String) = sfx("voice:$spec")

    /** Plays the loop for [world], or silence for null. */
    fun music(world: Int?) {
        wantedMusic = world
        if (world == null || !progress.music) {
            out.setMusic(null)
            return
        }
        Thread {
            val pcm = bank.music(world)
            if (wantedMusic == world && progress.music) out.setMusic(pcm)
        }.apply { isDaemon = true }.start()
    }

    fun refreshMusic() = music(wantedMusic)
}

/** Everything the screens need, bundled so the UI stays platform-free. */
class Game(
    val progress: Progress,
    val audio: Audio,
    val speaker: Speaker,
    val ads: LevelAds,
    val exitApp: () -> Unit,
) {
    fun say(text: String) {
        if (progress.sound) speaker.say(text)
    }
}
