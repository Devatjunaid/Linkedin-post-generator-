package com.verba.subtitles.speech

/**
 * Offline Vosk speech models (Apache 2.0). Each is downloaded once, then recognition runs
 * fully on the phone: no API key, no quota, no per-minute cost.
 *
 * To host the files yourself (recommended for production), upload the same zips to your own
 * server / GitHub Releases / Firebase Storage and change [BASE_URL].
 */
data class SpeechModel(
    val id: String,
    val language: String,
    val detail: String,
    val sizeMb: Int,
    val zipName: String,
) {
    val url: String get() = ModelCatalog.BASE_URL + zipName
}

object ModelCatalog {

    const val BASE_URL = "https://alphacephei.com/vosk/models/"

    val models: List<SpeechModel> = listOf(
        SpeechModel("en-us", "English", "US · fast", 40, "vosk-model-small-en-us-0.15.zip"),
        SpeechModel("en-us-lgraph", "English", "US · more accurate", 128, "vosk-model-en-us-0.22-lgraph.zip"),
        SpeechModel("en-in", "English", "India", 36, "vosk-model-small-en-in-0.4.zip"),
        SpeechModel("es", "Español", "Spanish", 39, "vosk-model-small-es-0.42.zip"),
        SpeechModel("pt", "Português", "Portuguese", 31, "vosk-model-small-pt-0.3.zip"),
        SpeechModel("fr", "Français", "French", 41, "vosk-model-small-fr-0.22.zip"),
        SpeechModel("de", "Deutsch", "German", 45, "vosk-model-small-de-0.15.zip"),
        SpeechModel("it", "Italiano", "Italian", 48, "vosk-model-small-it-0.22.zip"),
        SpeechModel("nl", "Nederlands", "Dutch", 39, "vosk-model-small-nl-0.22.zip"),
        SpeechModel("ru", "Русский", "Russian", 45, "vosk-model-small-ru-0.22.zip"),
        SpeechModel("uk", "Українська", "Ukrainian", 133, "vosk-model-small-uk-v3-small.zip"),
        SpeechModel("tr", "Türkçe", "Turkish", 35, "vosk-model-small-tr-0.3.zip"),
        SpeechModel("hi", "हिन्दी", "Hindi", 42, "vosk-model-small-hi-0.22.zip"),
        SpeechModel("fa", "فارسی", "Persian", 47, "vosk-model-small-fa-0.42.zip"),
        SpeechModel("vi", "Tiếng Việt", "Vietnamese", 32, "vosk-model-small-vn-0.4.zip"),
        SpeechModel("ja", "日本語", "Japanese", 48, "vosk-model-small-ja-0.22.zip"),
        SpeechModel("ko", "한국어", "Korean", 82, "vosk-model-small-ko-0.22.zip"),
        SpeechModel("zh", "中文", "Chinese", 42, "vosk-model-small-cn-0.22.zip"),
    )

    const val DEFAULT_ID = "en-us"

    fun byId(id: String): SpeechModel = models.firstOrNull { it.id == id } ?: models.first()
}
