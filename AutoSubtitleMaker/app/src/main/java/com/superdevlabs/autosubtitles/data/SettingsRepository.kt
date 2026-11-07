package com.superdevlabs.autosubtitles.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.superdevlabs.autosubtitles.core.captions.CaptionSegmenter
import com.superdevlabs.autosubtitles.core.captions.StylePresets
import com.superdevlabs.autosubtitles.speech.ModelCatalog
import com.superdevlabs.autosubtitles.speech.SpeechEngine
import com.superdevlabs.autosubtitles.speech.WhisperCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val engine: SpeechEngine = SpeechEngine.ACCURATE,
    /** Vosk model id used by the Fast engine. */
    val languageId: String = ModelCatalog.DEFAULT_ID,
    val whisperModelId: String = WhisperCatalog.DEFAULT_ID,
    /** Whisper language code or "auto". */
    val whisperLanguage: String = WhisperCatalog.AUTO,
    val wordsPerCaption: Int = 3,
    val presetId: String = StylePresets.all.first().id,
    val shareDiagnostics: Boolean = true,
    val exportCount: Int = 0,
    val lastReviewPromptAt: Long = 0L,
    val askedNotifications: Boolean = false,
) {
    /** Engine id stored on new projects. */
    val engineId: String get() = if (engine == SpeechEngine.ACCURATE) whisperModelId else SpeechEngine.FAST.id

    /** Language id stored on new projects (meaning depends on the engine). */
    val projectLanguageId: String get() = if (engine == SpeechEngine.ACCURATE) whisperLanguage else languageId

    val languageLabel: String
        get() = if (engine == SpeechEngine.ACCURATE) {
            WhisperCatalog.language(whisperLanguage).let { if (it.code == WhisperCatalog.AUTO) "Auto" else it.name }
        } else {
            ModelCatalog.byId(languageId).language
        }
}

/**
 * @param hasFastModel true when a Vosk model is already installed. People upgrading from v1.0
 * keep the Fast engine they already downloaded until they switch; new installs start on Accurate.
 */
class SettingsRepository(private val context: Context, private val hasFastModel: () -> Boolean) {

    private object Keys {
        val engine = stringPreferencesKey("engine")
        val language = stringPreferencesKey("language")
        val whisperModel = stringPreferencesKey("whisper_model")
        val whisperLanguage = stringPreferencesKey("whisper_language")
        val wordsPerCaption = intPreferencesKey("words_per_caption")
        val preset = stringPreferencesKey("preset")
        val shareDiagnostics = booleanPreferencesKey("share_diagnostics")
        val exportCount = intPreferencesKey("export_count")
        val lastReviewPrompt = longPreferencesKey("last_review_prompt")
        val askedNotifications = booleanPreferencesKey("asked_notifications")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map { prefs ->
        AppSettings(
            engine = prefs[Keys.engine]?.let(SpeechEngine::fromId)
                ?: if (hasFastModel()) SpeechEngine.FAST else SpeechEngine.ACCURATE,
            languageId = prefs[Keys.language] ?: ModelCatalog.DEFAULT_ID,
            whisperModelId = prefs[Keys.whisperModel]?.takeIf(WhisperCatalog::isWhisper) ?: WhisperCatalog.DEFAULT_ID,
            whisperLanguage = prefs[Keys.whisperLanguage] ?: WhisperCatalog.AUTO,
            wordsPerCaption = (prefs[Keys.wordsPerCaption] ?: 3)
                .coerceIn(CaptionSegmenter.MIN_WORDS, CaptionSegmenter.MAX_WORDS),
            presetId = prefs[Keys.preset] ?: StylePresets.all.first().id,
            shareDiagnostics = prefs[Keys.shareDiagnostics] ?: true,
            exportCount = prefs[Keys.exportCount] ?: 0,
            lastReviewPromptAt = prefs[Keys.lastReviewPrompt] ?: 0L,
            askedNotifications = prefs[Keys.askedNotifications] ?: false,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setEngine(engine: SpeechEngine) {
        context.settingsStore.edit { it[Keys.engine] = engine.id }
    }

    suspend fun setLanguage(id: String) {
        context.settingsStore.edit { it[Keys.language] = id }
    }

    suspend fun setWhisperModel(id: String) {
        context.settingsStore.edit { it[Keys.whisperModel] = id }
    }

    suspend fun setWhisperLanguage(code: String) {
        context.settingsStore.edit { it[Keys.whisperLanguage] = code }
    }

    suspend fun setWordsPerCaption(count: Int) {
        context.settingsStore.edit {
            it[Keys.wordsPerCaption] = count.coerceIn(CaptionSegmenter.MIN_WORDS, CaptionSegmenter.MAX_WORDS)
        }
    }

    suspend fun setPreset(id: String) {
        context.settingsStore.edit { it[Keys.preset] = id }
    }

    suspend fun setShareDiagnostics(allowed: Boolean) {
        context.settingsStore.edit { it[Keys.shareDiagnostics] = allowed }
    }

    /** Counts a successful export and returns the new total. */
    suspend fun incrementExports(): Int {
        var total = 0
        context.settingsStore.edit {
            total = (it[Keys.exportCount] ?: 0) + 1
            it[Keys.exportCount] = total
        }
        return total
    }

    suspend fun markReviewPrompted(now: Long) {
        context.settingsStore.edit { it[Keys.lastReviewPrompt] = now }
    }

    suspend fun markAskedNotifications() {
        context.settingsStore.edit { it[Keys.askedNotifications] = true }
    }
}
