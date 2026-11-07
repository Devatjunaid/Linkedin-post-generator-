package com.superdevlabs.autosubtitles.speech

import com.superdevlabs.autosubtitles.core.model.Word

/**
 * Pure helpers that turn one Whisper result (text for one speech segment) into timed words.
 *
 * The published Whisper models give no per-word timestamps, so each segment's time is
 * shared between its words by spoken length: longer words get more time, and punctuation
 * leaves a short pause. Segments are short (voice-activity detection cuts at pauses, max
 * ~10 s), which keeps the estimate close to the real timing.
 */
object WhisperText {

    private val whitespace = Regex("\\s+")
    private val bracketed = Regex("""[\[(（【][^\])）】]*[\])）】]""")
    private val noLetters = Regex("""^[\p{P}\p{S}\s]*$""")

    /** Phrases Whisper is known to invent on silence, music or noise. */
    private val hallucinations = setOf(
        "thank you", "thank you.", "thanks for watching", "thanks for watching!", "thank you for watching",
        "thank you for watching.", "please subscribe", "subscribe to my channel", "you", "bye", "bye.",
        "subtitles by the amara.org community", "♪", "music",
    )

    /** Removes [Music], (applause), ♪ notes and other non-speech markers. Empty = drop the segment. */
    fun clean(raw: String, segmentMs: Long): String {
        var text = raw.replace(bracketed, " ").replace("♪", " ").replace(whitespace, " ").trim()
        if (noLetters.matches(text)) return ""
        if (segmentMs < 2_500 && text.lowercase().trim() in hallucinations) return ""
        text = collapseRepeats(text)
        return text
    }

    /** "no no no no no no no no" → "no no no" (Whisper sometimes loops on one token). */
    private fun collapseRepeats(text: String): String {
        val tokens = text.split(' ')
        if (tokens.size < 6) return text
        val out = ArrayList<String>(tokens.size)
        var run = 0
        for (token in tokens) {
            run = if (out.isNotEmpty() && out.last().equals(token, ignoreCase = true)) run + 1 else 1
            if (run <= 3) out += token
        }
        return out.joinToString(" ")
    }

    /**
     * Splits text into display words. Languages written without spaces (Chinese, Japanese,
     * Thai…) are cut into short chunks so captions still advance word by word.
     */
    fun tokens(text: String): List<String> {
        val parts = text.split(whitespace).filter { it.isNotEmpty() }
        return parts.flatMap { part -> if (part.length > 8 && isUnspaced(part)) chunk(part) else listOf(part) }
    }

    private fun isUnspaced(part: String): Boolean = part.any { c ->
        val block = Character.UnicodeBlock.of(c)
        block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
            block == Character.UnicodeBlock.HIRAGANA ||
            block == Character.UnicodeBlock.KATAKANA ||
            block == Character.UnicodeBlock.THAI ||
            block == Character.UnicodeBlock.LAO ||
            block == Character.UnicodeBlock.KHMER ||
            block == Character.UnicodeBlock.MYANMAR
    }

    /** Cuts after punctuation, otherwise every 4 characters. */
    private fun chunk(part: String): List<String> {
        val out = mutableListOf<String>()
        val current = StringBuilder()
        for (c in part) {
            current.append(c)
            val punct = Character.getType(c).let {
                it == Character.OTHER_PUNCTUATION.toInt() || it == Character.END_PUNCTUATION.toInt()
            }
            if (punct || current.length >= 4) {
                out += current.toString()
                current.clear()
            }
        }
        if (current.isNotEmpty()) {
            // Keep a trailing punctuation-only piece with the previous chunk.
            if (out.isNotEmpty() && current.length == 1) out[out.lastIndex] = out.last() + current else out += current.toString()
        }
        return out
    }

    /** Shares [startMs]..[endMs] between the words of [text]. */
    fun timedWords(text: String, startMs: Long, endMs: Long): List<Word> {
        val words = tokens(text)
        if (words.isEmpty()) return emptyList()
        val span = (endMs - startMs).coerceAtLeast(words.size * 80L).toDouble()
        // Weight ≈ syllable-ish length, plus a pause after punctuation.
        val speech = words.map { w -> w.count { it.isLetterOrDigit() }.coerceAtLeast(1) + 1.5 }
        val pause = words.mapIndexed { i, w ->
            if (i == words.lastIndex) 0.0 else when (w.last()) {
                '.', '!', '?', '。', '！', '？' -> 3.5
                ',', ';', ':', '，', '、', '—' -> 1.8
                else -> 0.0
            }
        }
        val total = speech.sum() + pause.sum()
        var cursor = startMs.toDouble()
        return words.mapIndexed { i, w ->
            val length = span * speech[i] / total
            val word = Word(w, cursor.toLong(), (cursor + length).toLong())
            cursor += length + span * pause[i] / total
            word
        }
    }

    /**
     * Splits a long speech segment at its quietest moments (likely a breath between words)
     * until every piece is at most [maxSamples]. Returns (offset, length) pairs.
     */
    fun splitPoints(samples: FloatArray, maxSamples: Int, frame: Int = 320): List<Pair<Int, Int>> {
        val out = mutableListOf<Pair<Int, Int>>()
        fun split(offset: Int, length: Int) {
            if (length <= maxSamples || length < frame * 4) {
                out += offset to length
                return
            }
            // Look for the quietest 20 ms frame in the middle 30–70 % of the piece.
            val from = offset + (length * 0.3f).toInt()
            val to = offset + (length * 0.7f).toInt() - frame
            var best = from
            var bestEnergy = Double.MAX_VALUE
            var pos = from
            while (pos <= to) {
                var energy = 0.0
                for (i in pos until pos + frame) energy += samples[i] * samples[i]
                if (energy < bestEnergy) {
                    bestEnergy = energy
                    best = pos
                }
                pos += frame / 2
            }
            val cut = best + frame / 2
            split(offset, cut - offset)
            split(cut, offset + length - cut)
        }
        split(0, samples.size)
        return out
    }

    /** Most common language among detections; ties go to the earliest. */
    fun majority(detected: List<String>): String? =
        detected.filter { it.isNotBlank() }
            .groupingBy { it }.eachCount()
            .maxByOrNull { it.value }?.key
}
