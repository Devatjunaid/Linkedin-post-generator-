package com.superdevlabs.autosubtitles.core.captions

import com.superdevlabs.autosubtitles.core.model.Caption

/**
 * Offline rules for the "viral" caption look: which words get the keyword colour and which
 * emoji pops above a caption. No AI service involved — just word lists (English plus the most
 * common Spanish / Portuguese / French / German / Hindi-in-Latin words).
 */
object Emphasis {

    private val strip = Regex("""^[\p{P}\p{S}&&[^$€£₹¥%#]]+|[\p{P}\p{S}&&[^$€£₹¥%]]+$""")
    private val numberish = Regex("""^[$€£₹¥#]?\d[\d.,:]*[%kKmMbBxX+]?$|^\d+(st|nd|rd|th)$""")

    /** Lower-cased word without surrounding punctuation. */
    fun normalize(word: String): String = word.replace(strip, "").lowercase()

    private val emojiByWord: Map<String, String> = buildMap {
        fun add(emoji: String, vararg words: String) = words.forEach { put(it, emoji) }
        add("💰", "money", "cash", "dollars", "dollar", "rich", "paid", "salary", "profit", "income", "dinero", "dinheiro", "argent", "geld", "paisa", "paise")
        add("🔥", "fire", "lit", "insane", "crazy", "viral", "fuego", "fogo", "feu")
        add("❤️", "love", "loved", "heart", "amor", "amour", "liebe", "pyaar", "pyar")
        add("😂", "funny", "lol", "laugh", "laughing", "joke", "hilarious")
        add("😱", "shocked", "shocking", "scary", "omg", "wow", "unbelievable")
        add("🤯", "mindblowing", "genius", "blown")
        add("🚀", "growth", "grow", "launch", "rocket", "faster", "boost", "scale")
        add("💪", "strong", "strength", "gym", "workout", "power", "powerful", "muscle", "fuerza", "força")
        add("🧠", "brain", "smart", "learning", "knowledge", "idea", "ideas")
        add("💡", "tip", "tips", "hack", "hacks", "trick", "tricks", "secret", "secrets", "lightbulb")
        add("⏰", "minutes", "hours", "tiempo", "tempo", "temps", "zeit", "waqt")
        add("✅", "success", "win", "won")
        add("❌", "wrong", "mistake", "mistakes", "fail", "failed", "nunca", "jamais")
        add("🎯", "goal", "goals", "target", "focus", "aim", "meta")
        add("📈", "increase", "higher", "sales", "followers")
        add("📉", "decrease", "drop", "lost")
        add("🍕", "pizza", "food", "eating", "hungry", "lunch", "dinner", "comida")
        add("☕", "coffee", "café")
        add("😴", "sleep", "tired", "sueño")
        add("🎉", "party", "celebrate", "congrats", "congratulations", "birthday", "fiesta", "festa")
        add("📱", "iphone", "android", "mobile")
        add("💻", "computer", "laptop", "coding", "software", "website")
        add("🎵", "music", "song", "sing", "dance", "música", "musique")
        add("📸", "photo", "photos", "camera")
        add("🌍", "global", "travel", "mundo", "monde", "welt", "duniya")
        add("🏠", "maison")
        add("🙏", "thanks", "gracias", "obrigado", "obrigada", "merci", "danke", "shukriya", "dhanyavaad")
        add("⚠️", "warning", "careful", "danger", "dangerous", "attention")
        add("🏆", "best", "champion", "winner", "mejor", "melhor", "meilleur")
        add("😍", "beautiful", "amazing", "gorgeous", "perfect", "hermoso", "lindo", "belle")
        add("💼", "business", "job", "career", "company", "boss", "trabajo", "trabalho", "travail")
        add("📚", "book", "books", "reading", "study", "school")
        add("🛑", "pause")
        add("👋", "hello", "hey", "bye", "hola", "olá", "bonjour", "hallo", "namaste")
    }

    private val powerWords: Set<String> = setOf(
        "never", "always", "secret", "free", "best", "worst", "every", "everyone", "nobody", "only",
        "must", "stop", "now", "today", "instantly", "guaranteed", "proven", "easy", "simple", "huge",
        "massive", "biggest", "million", "billion", "thousand", "hundred", "double", "triple", "zero",
        "insane", "crazy", "amazing", "important", "mistake", "mistakes", "truth", "real", "fast",
        "money", "rich", "love", "hate", "first", "last", "new", "why", "how", "nunca", "siempre",
        "gratis", "mejor", "jamais", "toujours", "gratuit", "immer", "nie", "kostenlos",
    )

    fun isKeyword(word: String): Boolean {
        val raw = word.trim()
        val n = normalize(raw)
        if (n.isEmpty()) return false
        return numberish.matches(raw.trim('.', ',', '!', '?', ';', ':')) || n in powerWords || n in emojiByWord
    }

    /** The emoji for the first word in [caption] that has one, or null. */
    fun emojiFor(caption: Caption): String? {
        for (word in caption.words) {
            emojiByWord[normalize(word.text)]?.let { return it }
        }
        return null
    }
}
