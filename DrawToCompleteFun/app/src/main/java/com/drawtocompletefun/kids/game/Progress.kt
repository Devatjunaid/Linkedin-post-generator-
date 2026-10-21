package com.drawtocompletefun.kids.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate

/** Tiny persistence abstraction so game code does not depend on Android. */
interface KeyValueStore {
    fun getString(key: String, default: String): String
    fun putString(key: String, value: String)
    fun getInt(key: String, default: Int): Int
    fun putInt(key: String, value: Int)
    fun getLong(key: String, default: Long): Long
    fun putLong(key: String, value: Long)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}

/** All saved progress, exposed as Compose state so screens update by themselves. */
class Progress(private val store: KeyValueStore) {

    val stars = mutableStateListOf<Int>().apply {
        val saved = store.getString(KEY_STARS, "")
        repeat(Levels.COUNT) { i -> add(saved.getOrNull(i)?.digitToIntOrNull() ?: 0) }
    }

    private val openedTreasures = mutableStateListOf<Int>().apply {
        store.getString(KEY_TREASURE, "").split(',').mapNotNull { it.toIntOrNull() }.forEach { add(it) }
    }

    var bonusStars by mutableIntStateOf(store.getInt(KEY_BONUS, 0))
        private set

    var soundOn by mutableStateOf(store.getBoolean(KEY_SOUND, true))
        private set

    var musicOn by mutableStateOf(store.getBoolean(KEY_MUSIC, true))
        private set

    var streak by mutableIntStateOf(store.getInt(KEY_STREAK, 0))
        private set

    private var lastGiftDay by mutableStateOf(store.getLong(KEY_GIFT_DAY, Long.MIN_VALUE))

    val levelStars: Int get() = stars.sum()
    val totalStars: Int get() = levelStars + bonusStars

    fun starsFor(level: Int): Int = stars[level - 1]

    fun isCompleted(level: Int): Boolean = stars[level - 1] > 0

    fun isUnlocked(level: Int): Boolean = level == 1 || isCompleted(level - 1)

    fun isWorldUnlocked(world: World): Boolean = isUnlocked(world.firstLevel)

    fun needsTreasureVideo(level: Int): Boolean =
        Levels.get(level).isTreasureLevel && !isCompleted(level) && level !in openedTreasures

    /** First level that is unlocked but not finished yet (or the last level). */
    val currentLevel: Int
        get() = (1..Levels.COUNT).firstOrNull { isUnlocked(it) && !isCompleted(it) } ?: Levels.COUNT

    fun completedIn(world: World): Int = (world.firstLevel..world.lastLevel).count { isCompleted(it) }

    fun starsIn(world: World): Int = (world.firstLevel..world.lastLevel).sumOf { stars[it - 1] }

    /** Saves a result. Returns how many new stars were earned. */
    fun record(level: Int, earned: Int): Int {
        val old = stars[level - 1]
        if (earned <= old) return 0
        stars[level - 1] = earned
        store.putString(KEY_STARS, stars.joinToString(""))
        return earned - old
    }

    fun openTreasure(level: Int) {
        if (level in openedTreasures) return
        openedTreasures += level
        store.putString(KEY_TREASURE, openedTreasures.joinToString(","))
    }

    fun updateSound(on: Boolean) {
        soundOn = on
        store.putBoolean(KEY_SOUND, on)
    }

    fun updateMusic(on: Boolean) {
        musicOn = on
        store.putBoolean(KEY_MUSIC, on)
    }

    // ------------------------------------------------------------ Daily gift
    private fun today(): Long = LocalDate.now().toEpochDay()

    fun canClaimGift(): Boolean = lastGiftDay != today()

    /** Streak shown in the week strip: days already claimed in this run (0..7). */
    val streakForDisplay: Int
        get() {
            val t = today()
            return when {
                lastGiftDay == t || lastGiftDay == t - 1 -> streak
                else -> 0
            }
        }

    /** Claims today's gift and returns the number of bonus stars given. */
    fun claimGift(): Int {
        val t = today()
        if (lastGiftDay == t) return 0
        val newStreak = if (lastGiftDay == t - 1) streak % 7 + 1 else 1
        val reward = if (newStreak == 7) 3 else 1
        streak = newStreak
        lastGiftDay = t
        bonusStars += reward
        store.putInt(KEY_STREAK, streak)
        store.putLong(KEY_GIFT_DAY, t)
        store.putInt(KEY_BONUS, bonusStars)
        return reward
    }

    // ------------------------------------------------------------ Stickers
    fun stickerCost(index: Int): Int = (index + 1) * STARS_PER_STICKER

    fun isStickerUnlocked(index: Int): Boolean = totalStars >= stickerCost(index)

    val unlockedStickers: Int get() = Pictures.stickerPictures.indices.count { isStickerUnlocked(it) }

    /** Stars still needed for the next sticker, or null if all are unlocked. */
    val starsToNextSticker: Int?
        get() {
            val next = Pictures.stickerPictures.indices.firstOrNull { !isStickerUnlocked(it) } ?: return null
            return stickerCost(next) - totalStars
        }

    companion object {
        const val STARS_PER_STICKER = 14
        private const val KEY_STARS = "stars"
        private const val KEY_TREASURE = "treasures"
        private const val KEY_BONUS = "bonus"
        private const val KEY_SOUND = "sound"
        private const val KEY_MUSIC = "music"
        private const val KEY_STREAK = "streak"
        private const val KEY_GIFT_DAY = "gift_day"
    }
}
