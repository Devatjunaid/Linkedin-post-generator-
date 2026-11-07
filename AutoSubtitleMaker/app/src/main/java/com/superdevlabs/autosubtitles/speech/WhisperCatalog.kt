package com.superdevlabs.autosubtitles.speech

/** Which speech recognizer a project uses. */
enum class SpeechEngine(val id: String, val label: String) {
    /** Vosk: small per-language models, very fast, exact word timing, no punctuation. */
    FAST("vosk", "Fast"),

    /** Whisper (via sherpa-onnx): punctuation, capitals, accents, 50+ languages. Slower. */
    ACCURATE("whisper", "Accurate");

    companion object {
        fun fromId(id: String?): SpeechEngine = entries.firstOrNull { it.id == id } ?: FAST
    }
}

/**
 * Offline OpenAI Whisper models converted for sherpa-onnx (MIT licence), int8-quantised.
 * Each model is three files, downloaded once from Hugging Face at a pinned revision.
 */
data class WhisperModel(
    val id: String,
    val name: String,
    val detail: String,
    val sizeMb: Int,
    /** Minimum device RAM we recommend, in GB. */
    val minRamGb: Int,
    private val repo: String,
    private val revision: String,
    private val prefix: String,
) {
    val encoder: String get() = "$prefix-encoder.int8.onnx"
    val decoder: String get() = "$prefix-decoder.int8.onnx"
    val tokens: String get() = "$prefix-tokens.txt"
    val files: List<String> get() = listOf(encoder, decoder, tokens)

    fun url(file: String): String = "https://huggingface.co/$repo/resolve/$revision/$file"
}

data class SpokenLanguage(val code: String, val name: String, val english: String)

object WhisperCatalog {

    val models: List<WhisperModel> = listOf(
        WhisperModel(
            "whisper-tiny", "Lite", "Fastest · good for clear speech", 104, 2,
            "csukuangfj/sherpa-onnx-whisper-tiny", "65176e2deb88badc814a94058666cadccc29b61c", "tiny",
        ),
        WhisperModel(
            "whisper-base", "Balanced", "Recommended · best speed/accuracy", 160, 3,
            "csukuangfj/sherpa-onnx-whisper-base", "bb53ee204431c90d314c1cc08d28d23e5b7927cc", "base",
        ),
        WhisperModel(
            "whisper-small", "Pro", "Most accurate · slower, needs 6 GB+ RAM", 375, 6,
            "csukuangfj/sherpa-onnx-whisper-small", "8f3c18b358db4d1f2fc1eae49d75cd20989e4309", "small",
        ),
    )

    const val DEFAULT_ID = "whisper-base"
    const val AUTO = "auto"

    fun byId(id: String): WhisperModel = models.firstOrNull { it.id == id } ?: models.first { it.id == DEFAULT_ID }

    fun isWhisper(id: String): Boolean = models.any { it.id == id }

    /** Languages offered in the picker (Whisper understands ~99; these are the strongest). */
    val languages: List<SpokenLanguage> = listOf(
        SpokenLanguage(AUTO, "Auto-detect", "Detects the language for you"),
        SpokenLanguage("en", "English", "English"),
        SpokenLanguage("es", "Español", "Spanish"),
        SpokenLanguage("pt", "Português", "Portuguese"),
        SpokenLanguage("fr", "Français", "French"),
        SpokenLanguage("de", "Deutsch", "German"),
        SpokenLanguage("it", "Italiano", "Italian"),
        SpokenLanguage("hi", "हिन्दी", "Hindi"),
        SpokenLanguage("ur", "اردو", "Urdu"),
        SpokenLanguage("ar", "العربية", "Arabic"),
        SpokenLanguage("bn", "বাংলা", "Bengali"),
        SpokenLanguage("id", "Bahasa Indonesia", "Indonesian"),
        SpokenLanguage("ms", "Bahasa Melayu", "Malay"),
        SpokenLanguage("tl", "Filipino", "Filipino / Tagalog"),
        SpokenLanguage("vi", "Tiếng Việt", "Vietnamese"),
        SpokenLanguage("th", "ไทย", "Thai"),
        SpokenLanguage("tr", "Türkçe", "Turkish"),
        SpokenLanguage("ru", "Русский", "Russian"),
        SpokenLanguage("uk", "Українська", "Ukrainian"),
        SpokenLanguage("pl", "Polski", "Polish"),
        SpokenLanguage("nl", "Nederlands", "Dutch"),
        SpokenLanguage("fa", "فارسی", "Persian"),
        SpokenLanguage("ja", "日本語", "Japanese"),
        SpokenLanguage("ko", "한국어", "Korean"),
        SpokenLanguage("zh", "中文", "Chinese"),
        SpokenLanguage("ta", "தமிழ்", "Tamil"),
        SpokenLanguage("te", "తెలుగు", "Telugu"),
        SpokenLanguage("mr", "मराठी", "Marathi"),
        SpokenLanguage("gu", "ગુજરાતી", "Gujarati"),
        SpokenLanguage("kn", "ಕನ್ನಡ", "Kannada"),
        SpokenLanguage("ml", "മലയാളം", "Malayalam"),
        SpokenLanguage("pa", "ਪੰਜਾਬੀ", "Punjabi"),
        SpokenLanguage("ne", "नेपाली", "Nepali"),
        SpokenLanguage("si", "සිංහල", "Sinhala"),
        SpokenLanguage("he", "עברית", "Hebrew"),
        SpokenLanguage("el", "Ελληνικά", "Greek"),
        SpokenLanguage("cs", "Čeština", "Czech"),
        SpokenLanguage("sk", "Slovenčina", "Slovak"),
        SpokenLanguage("ro", "Română", "Romanian"),
        SpokenLanguage("hu", "Magyar", "Hungarian"),
        SpokenLanguage("bg", "Български", "Bulgarian"),
        SpokenLanguage("hr", "Hrvatski", "Croatian"),
        SpokenLanguage("sr", "Српски", "Serbian"),
        SpokenLanguage("sv", "Svenska", "Swedish"),
        SpokenLanguage("da", "Dansk", "Danish"),
        SpokenLanguage("no", "Norsk", "Norwegian"),
        SpokenLanguage("fi", "Suomi", "Finnish"),
        SpokenLanguage("sw", "Kiswahili", "Swahili"),
        SpokenLanguage("az", "Azərbaycan", "Azerbaijani"),
        SpokenLanguage("kk", "Қазақ", "Kazakh"),
        SpokenLanguage("uz", "Oʻzbek", "Uzbek"),
        SpokenLanguage("af", "Afrikaans", "Afrikaans"),
        SpokenLanguage("ca", "Català", "Catalan"),
    )

    fun language(code: String): SpokenLanguage =
        languages.firstOrNull { it.code == code } ?: SpokenLanguage(code, code.uppercase(), code)
}
