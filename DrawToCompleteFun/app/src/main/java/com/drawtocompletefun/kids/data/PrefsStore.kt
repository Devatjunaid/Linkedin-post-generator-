package com.drawtocompletefun.kids.data

import android.content.Context
import androidx.core.content.edit
import com.drawtocompletefun.kids.game.KeyValueStore

/** Progress is stored only on the device. Nothing is sent anywhere. */
class PrefsStore(context: Context) : KeyValueStore {
    private val prefs = context.getSharedPreferences("draw_to_complete_progress", Context.MODE_PRIVATE)

    override fun getString(key: String, default: String): String = prefs.getString(key, default) ?: default
    override fun putString(key: String, value: String) = prefs.edit { putString(key, value) }
    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    override fun putInt(key: String, value: Int) = prefs.edit { putInt(key, value) }
    override fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)
    override fun putLong(key: String, value: Long) = prefs.edit { putLong(key, value) }
    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) = prefs.edit { putBoolean(key, value) }
}
