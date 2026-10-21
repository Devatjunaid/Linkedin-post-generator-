package com.feedthewrongthing.game.game

import android.content.Context
import android.content.SharedPreferences

/** Local-only progress storage. Nothing ever leaves the device. */
class SaveData(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ftwt_save", Context.MODE_PRIVATE)
    private val stars = IntArray(Levels.all.size)
    private val journal = HashSet<String>()
    private val hats = HashSet<String>()
    private val equipped = HashMap<CreatureType, Hat>()
    private val giftsClaimed = HashSet<Int>()

    var unlocked = 1
        private set
    var endlessBest = 0
        private set
    var musicOn = true
    var sfxOn = true
    var hapticsOn = true
    var lastGiftDay = -1L
    var launches = 0
    var levelsPlayed = 0

    init {
        for (i in stars.indices) stars[i] = prefs.getInt("s$i", 0)
        unlocked = prefs.getInt("unlocked", 1).coerceIn(1, Levels.all.size + 1)
        endlessBest = prefs.getInt("endless", 0)
        journal.addAll(prefs.getStringSet("journal", emptySet()) ?: emptySet())
        hats.addAll(prefs.getStringSet("hats", setOf(Hat.PARTY.name)) ?: setOf(Hat.PARTY.name))
        for (c in CreatureType.entries) {
            val h = prefs.getString("hat_${c.name}", null)
            equipped[c] = Hat.entries.firstOrNull { it.name == h } ?: Hat.NONE
        }
        (prefs.getStringSet("gifts", emptySet()) ?: emptySet()).mapNotNullTo(giftsClaimed) { it.toIntOrNull() }
        musicOn = prefs.getBoolean("music", true)
        sfxOn = prefs.getBoolean("sfx", true)
        hapticsOn = prefs.getBoolean("haptics", true)
        lastGiftDay = prefs.getLong("giftDay", -1L)
        launches = prefs.getInt("launches", 0)
        levelsPlayed = prefs.getInt("played", 0)
    }

    fun stars(level: Int): Int = stars.getOrElse(level) { 0 }

    fun totalStars(): Int = stars.sum()

    fun islandStars(island: Int): Int {
        var s = 0
        for (k in 0 until Levels.PER_ISLAND) s += stars(island * Levels.PER_ISLAND + k)
        return s
    }

    fun isUnlocked(level: Int) = level < unlocked

    fun isIslandUnlocked(island: Int): Boolean =
        if (island >= Levels.islandCount) unlocked > Levels.all.size else isUnlocked(island * Levels.PER_ISLAND)

    /** Stores the result of a handcrafted level. Returns true if this unlocked a new island. */
    fun completeLevel(level: Int, s: Int): Boolean {
        if (level in stars.indices && s > stars[level]) stars[level] = s
        var newIsland = false
        if (level + 1 >= unlocked) {
            unlocked = level + 2
            newIsland = (level + 1) % Levels.PER_ISLAND == 0
        }
        levelsPlayed++
        save()
        return newIsland
    }

    fun completeEndless(k: Int) {
        if (k + 1 > endlessBest) endlessBest = k + 1
        levelsPlayed++
        save()
    }

    fun creatureMet(c: CreatureType): Boolean {
        if (c == CreatureType.MUNCHY) return true
        if (c == CreatureType.BIG_MAMA) return unlocked > Levels.all.size - 1
        val firstLevel = Levels.all.indexOfFirst { def -> def.creatures.any { it.type == c } }
        return firstLevel in 0 until unlocked
    }

    fun discover(c: CreatureType, item: ItemType): Boolean {
        val added = journal.add("${c.ordinal}:${item.ordinal}")
        if (added) saveJournal()
        return added
    }

    fun discovered(c: CreatureType, item: ItemType) = journal.contains("${c.ordinal}:${item.ordinal}")

    fun discoveredCount(c: CreatureType): Int = ItemType.entries.count { discovered(c, it) }

    fun totalDiscoveries(): Int = journal.size

    fun ownsHat(h: Hat) = h == Hat.NONE || hats.contains(h.name)

    fun ownedHats(): List<Hat> = Hat.entries.filter { ownsHat(it) }

    /** Gives the next hat the player doesn't own yet; null when the closet is complete. */
    fun grantNextHat(): Hat? {
        val next = Hat.entries.firstOrNull { it != Hat.NONE && !ownsHat(it) } ?: return null
        hats.add(next.name)
        prefs.edit().putStringSet("hats", HashSet(hats)).apply()
        return next
    }

    fun hatOf(c: CreatureType): Hat = equipped[c] ?: Hat.NONE

    fun equip(c: CreatureType, h: Hat) {
        equipped[c] = h
        prefs.edit().putString("hat_${c.name}", h.name).apply()
    }

    fun giftClaimed(level: Int) = giftsClaimed.contains(level)

    fun claimGift(level: Int) {
        giftsClaimed.add(level)
        prefs.edit().putStringSet("gifts", giftsClaimed.map { it.toString() }.toSet()).apply()
    }

    fun saveSettings() {
        prefs.edit()
            .putBoolean("music", musicOn)
            .putBoolean("sfx", sfxOn)
            .putBoolean("haptics", hapticsOn)
            .putLong("giftDay", lastGiftDay)
            .putInt("launches", launches)
            .apply()
    }

    private fun saveJournal() {
        prefs.edit().putStringSet("journal", HashSet(journal)).apply()
    }

    fun save() {
        val e = prefs.edit()
        for (i in stars.indices) e.putInt("s$i", stars[i])
        e.putInt("unlocked", unlocked)
        e.putInt("endless", endlessBest)
        e.putInt("played", levelsPlayed)
        e.apply()
        saveJournal()
        saveSettings()
    }

    fun resetAll() {
        prefs.edit().clear().apply()
        for (i in stars.indices) stars[i] = 0
        unlocked = 1
        endlessBest = 0
        journal.clear()
        hats.clear(); hats.add(Hat.PARTY.name)
        for (c in CreatureType.entries) equipped[c] = Hat.NONE
        giftsClaimed.clear()
        lastGiftDay = -1L
        levelsPlayed = 0
        save()
    }
}
