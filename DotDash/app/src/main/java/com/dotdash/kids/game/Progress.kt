package com.dotdash.kids.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

const val GOLDEN_ZIP_STARS = 150
const val DAILY_GIFT_STARS = 5
const val EGG_STARS = 5
const val COLOR_IT_STARS = 1

/** Simple persistent key/value storage (SharedPreferences on Android). */
interface KeyValueStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
}

data class FinishResult(val gained: Int, val firstTime: Boolean, val egg: Boolean)

/** Saved progress. Every field is Compose state, so screens update by themselves. */
class Progress(private val kv: KeyValueStore, private val today: () -> String) {
    private val best = mutableStateMapOf<Int, Int>() // level index -> best stars (1..3)
    private val colors = mutableStateMapOf<String, String>() // picture id -> colour from "Color it!"
    private val eggs = mutableStateMapOf<Int, Boolean>()
    private val colored = mutableStateMapOf<String, Boolean>()
    private var bonus by mutableIntStateOf(0)
    private var lastGift by mutableStateOf("")
    var sound by mutableStateOf(true)
        private set
    var music by mutableStateOf(true)
        private set

    init {
        kv.get("best")?.split(',')?.filter { it.contains(':') }?.forEach {
            val (i, s) = it.split(':')
            best[i.toInt()] = s.toInt()
        }
        kv.get("colors")?.split(';')?.filter { it.contains('=') }?.forEach {
            val (id, c) = it.split('=')
            colors[id] = c
        }
        kv.get("eggs")?.split(',')?.filter { it.isNotBlank() }?.forEach { eggs[it.toInt()] = true }
        kv.get("colored")?.split(',')?.filter { it.isNotBlank() }?.forEach { colored[it] = true }
        bonus = kv.get("bonus")?.toIntOrNull() ?: 0
        lastGift = kv.get("lastGift") ?: ""
        sound = kv.get("sound") != "0"
        music = kv.get("music") != "0"
    }

    private fun save() {
        kv.put("best", best.entries.joinToString(",") { "${it.key}:${it.value}" })
        kv.put("colors", colors.entries.joinToString(";") { "${it.key}=${it.value}" })
        kv.put("eggs", eggs.keys.joinToString(","))
        kv.put("colored", colored.keys.joinToString(","))
        kv.put("bonus", bonus.toString())
        kv.put("lastGift", lastGift)
        kv.put("sound", if (sound) "1" else "0")
        kv.put("music", if (music) "1" else "0")
    }

    fun bestStars(index: Int) = best[index] ?: 0
    fun colorOf(pic: Picture): String = colors[pic.id] ?: pic.fill
    fun eggHatched(index: Int) = eggs[index] == true

    fun totalStars() = best.values.sum() + bonus
    fun worldStars(w: Int) = (0 until LEVELS_PER_WORLD).sumOf { best[w * LEVELS_PER_WORLD + it] ?: 0 }
    fun completedCount() = best.size
    fun isDone(index: Int) = best.containsKey(index)

    fun worldUnlocked(w: Int): Boolean =
        w == 0 || totalStars() >= Pictures.worlds[w].unlock || isDone(w * LEVELS_PER_WORLD)

    fun levelUnlocked(index: Int): Boolean {
        val w = index / LEVELS_PER_WORLD
        if (!worldUnlocked(w)) return false
        return index == w * LEVELS_PER_WORLD || isDone(index) || isDone(index - 1)
    }

    /** The level the PLAY button opens: the first unfinished level in an open world. */
    fun nextLevel(): Int = Levels.all.firstOrNull { !isDone(it.index) && levelUnlocked(it.index) }?.index ?: 0
    fun currentWorld() = Levels.all[nextLevel()].world
    fun goldenZip() = totalStars() >= GOLDEN_ZIP_STARS

    fun finishLevel(index: Int, stars: Int): FinishResult {
        val before = best[index] ?: 0
        val firstTime = before == 0
        if (stars > before) best[index] = stars
        var egg = false
        if (firstTime && Levels.all[index].egg && eggs[index] != true) {
            eggs[index] = true
            bonus += EGG_STARS
            egg = true
        }
        save()
        return FinishResult(maxOf(0, stars - before), firstTime, egg)
    }

    fun giftReady() = lastGift != today()
    fun claimGift() {
        lastGift = today()
        bonus += DAILY_GIFT_STARS
        save()
    }

    /** Returns true the first time a picture is coloured (worth a bonus star). */
    fun setColor(pic: Picture, color: String): Boolean {
        colors[pic.id] = color
        val first = colored[pic.id] != true
        if (first) {
            colored[pic.id] = true
            bonus += COLOR_IT_STARS
        }
        save()
        return first
    }

    fun toggleSound(): Boolean {
        sound = !sound
        save()
        return sound
    }

    fun toggleMusic(): Boolean {
        music = !music
        save()
        return music
    }
}
