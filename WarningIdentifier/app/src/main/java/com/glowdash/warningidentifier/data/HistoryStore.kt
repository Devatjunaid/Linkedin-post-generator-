package com.glowdash.warningidentifier.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class HistoryEntry(val light: WarningLight, val timestamp: Long, val confidence: Int)

/** Recent scans, stored locally in SharedPreferences (no account, no cloud). */
object HistoryStore {
    private const val PREFS = "scan_history"
    private const val KEY = "items"
    private const val MAX_ITEMS = 30

    private var prefs: SharedPreferences? = null
    private val _items = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val items: StateFlow<List<HistoryEntry>> = _items

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _items.value = load()
    }

    fun add(lightId: String, confidence: Int) {
        val light = LightsCatalog.find(lightId) ?: return
        val updated = (listOf(HistoryEntry(light, System.currentTimeMillis(), confidence)) + _items.value)
            .take(MAX_ITEMS)
        _items.value = updated
        save(updated)
    }

    fun clear() {
        _items.value = emptyList()
        save(emptyList())
    }

    private fun load(): List<HistoryEntry> {
        val raw = prefs?.getString(KEY, null).orEmpty()
        return raw.lines().mapNotNull { line ->
            val parts = line.split('|')
            if (parts.size != 3) return@mapNotNull null
            val light = LightsCatalog.find(parts[0]) ?: return@mapNotNull null
            HistoryEntry(light, parts[1].toLongOrNull() ?: 0L, parts[2].toIntOrNull() ?: -1)
        }
    }

    private fun save(items: List<HistoryEntry>) {
        val raw = items.joinToString("\n") { "${it.light.id}|${it.timestamp}|${it.confidence}" }
        prefs?.edit()?.putString(KEY, raw)?.apply()
    }
}
