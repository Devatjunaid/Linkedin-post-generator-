package com.devatjunaid.dripdropdraw.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.devatjunaid.dripdropdraw.engine.Levels

/** Saved progress. Everything stays on the device; nothing is sent anywhere. */
class Progress(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    /** Highest level the player may open. */
    var unlocked by mutableIntStateOf(prefs.getInt(KEY_UNLOCKED, 1))
        private set
    var coins by mutableIntStateOf(prefs.getInt(KEY_COINS, START_COINS))
        private set
    var soundOn by mutableStateOf(prefs.getBoolean(KEY_SOUND, true))
        private set
    var musicOn by mutableStateOf(prefs.getBoolean(KEY_MUSIC, true))
        private set
    var totalStars by mutableIntStateOf(0)
        private set

    /** Levels finished in total (replays count too). Drives the bonus chest every 5 wins. */
    var wins by mutableIntStateOf(prefs.getInt(KEY_WINS, 0))
        private set

    private val stars = IntArray(Levels.COUNT + 1) { prefs.getInt("s_$it", 0) }

    init {
        totalStars = stars.sum()
    }

    fun starsFor(level: Int): Int = stars.getOrElse(level) { 0 }

    /** Saves a finished level. Returns the coins earned. */
    fun completeLevel(level: Int, newStars: Int): Int {
        val old = starsFor(level)
        // Coins for each new star, plus a small bonus every time.
        val earned = (newStars - old).coerceAtLeast(0) * 10 + 5
        if (newStars > old) {
            stars[level] = newStars
            totalStars = stars.sum()
        }
        unlocked = maxOf(unlocked, (level + 1).coerceAtMost(Levels.COUNT))
        coins += earned
        wins += 1
        prefs.edit {
            putInt("s_$level", stars[level])
            putInt(KEY_UNLOCKED, unlocked)
            putInt(KEY_COINS, coins)
            putInt(KEY_WINS, wins)
        }
        return earned
    }

    fun addCoins(amount: Int) {
        coins += amount
        prefs.edit { putInt(KEY_COINS, coins) }
    }

    fun spendCoins(amount: Int): Boolean {
        if (coins < amount) return false
        coins -= amount
        prefs.edit { putInt(KEY_COINS, coins) }
        return true
    }

    fun setSound(on: Boolean) {
        soundOn = on
        prefs.edit { putBoolean(KEY_SOUND, on) }
    }

    fun setMusic(on: Boolean) {
        musicOn = on
        prefs.edit { putBoolean(KEY_MUSIC, on) }
    }

    companion object {
        const val START_COINS = 100
        private const val KEY_UNLOCKED = "unlocked"
        private const val KEY_COINS = "coins"
        private const val KEY_SOUND = "sound"
        private const val KEY_MUSIC = "music"
        private const val KEY_WINS = "wins"
    }
}
