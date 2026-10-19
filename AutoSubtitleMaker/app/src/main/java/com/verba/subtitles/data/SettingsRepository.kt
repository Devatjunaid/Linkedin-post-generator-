package com.verba.subtitles.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.verba.subtitles.core.captions.CaptionSegmenter
import com.verba.subtitles.core.captions.StylePresets
import com.verba.subtitles.speech.ModelCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val languageId: String = ModelCatalog.DEFAULT_ID,
    val wordsPerCaption: Int = 3,
    val presetId: String = StylePresets.all.first().id,
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val language = stringPreferencesKey("language")
        val wordsPerCaption = intPreferencesKey("words_per_caption")
        val preset = stringPreferencesKey("preset")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map { prefs ->
        AppSettings(
            languageId = prefs[Keys.language] ?: ModelCatalog.DEFAULT_ID,
            wordsPerCaption = (prefs[Keys.wordsPerCaption] ?: 3)
                .coerceIn(CaptionSegmenter.MIN_WORDS, CaptionSegmenter.MAX_WORDS),
            presetId = prefs[Keys.preset] ?: StylePresets.all.first().id,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setLanguage(id: String) {
        context.settingsStore.edit { it[Keys.language] = id }
    }

    suspend fun setWordsPerCaption(count: Int) {
        context.settingsStore.edit {
            it[Keys.wordsPerCaption] = count.coerceIn(CaptionSegmenter.MIN_WORDS, CaptionSegmenter.MAX_WORDS)
        }
    }

    suspend fun setPreset(id: String) {
        context.settingsStore.edit { it[Keys.preset] = id }
    }
}
