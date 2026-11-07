package com.superdevlabs.autosubtitles.translate

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.superdevlabs.autosubtitles.core.captions.TranslationPlanner
import com.superdevlabs.autosubtitles.core.model.Caption
import com.superdevlabs.autosubtitles.util.await
import java.io.IOException
import java.util.Locale

enum class TranslateMode(val label: String, val hint: String) {
    BILINGUAL("Add as 2nd line", "Original + translation together"),
    REPLACE("Replace", "Only the translated subtitles"),
}

data class TargetLanguage(val code: String, val name: String, val english: String)

class UnsupportedLanguageException(language: String) :
    Exception("Translation from $language isn't available yet.")

/**
 * Google ML Kit on-device translation: free, no API key, no quota. Each language pack
 * (~30 MB) downloads once the first time it is used, then works offline.
 */
class CaptionTranslator {

    /** Every language ML Kit can translate to, named in its own language. */
    val targets: List<TargetLanguage> by lazy {
        TranslateLanguage.getAllLanguages()
            .map { code ->
                val locale = Locale.forLanguageTag(code)
                TargetLanguage(
                    code = code,
                    name = locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) },
                    english = locale.getDisplayLanguage(Locale.ENGLISH),
                )
            }
            .sortedBy { it.english }
    }

    fun isSupported(code: String): Boolean = TranslateLanguage.fromLanguageTag(code) != null

    /**
     * Translates [captions] from [sourceCode] to [targetCode].
     * [onProgress] gets 0..1; the first part is the one-time language download.
     */
    suspend fun translate(
        captions: List<Caption>,
        sourceCode: String,
        targetCode: String,
        mode: TranslateMode,
        wordsPerCaption: Int,
        onProgress: (Float, String) -> Unit,
    ): List<Caption> {
        val source = TranslateLanguage.fromLanguageTag(sourceCode) ?: throw UnsupportedLanguageException(sourceCode.uppercase())
        val target = TranslateLanguage.fromLanguageTag(targetCode) ?: throw UnsupportedLanguageException(targetCode.uppercase())
        // Captions that already carry a translation are re-translated from their original words.
        val clean = captions.map { it.copy(translation = null) }
        if (source == target) return clean

        val translator = Translation.getClient(
            TranslatorOptions.Builder().setSourceLanguage(source).setTargetLanguage(target).build(),
        )
        try {
            onProgress(0f, "Getting the language pack (one time, ~30 MB)…")
            try {
                translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            } catch (e: Exception) {
                throw IOException("Couldn't download the language pack. Check your connection and retry.", e)
            }
            val sentences = TranslationPlanner.sentences(clean)
            val translated = ArrayList<String>(sentences.size)
            sentences.forEachIndexed { index, sentence ->
                translated += translator.translate(sentence.text).await()
                onProgress(0.1f + 0.9f * (index + 1) / sentences.size, "Translating ${index + 1} of ${sentences.size}")
            }
            return when (mode) {
                TranslateMode.REPLACE -> TranslationPlanner.replace(sentences, translated, wordsPerCaption)
                TranslateMode.BILINGUAL -> TranslationPlanner.bilingual(sentences, translated)
            }
        } finally {
            translator.close()
        }
    }
}
