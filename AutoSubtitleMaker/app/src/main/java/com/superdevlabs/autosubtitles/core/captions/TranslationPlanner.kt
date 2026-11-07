package com.superdevlabs.autosubtitles.core.captions

import com.superdevlabs.autosubtitles.core.model.Caption
import com.superdevlabs.autosubtitles.core.model.Word
import com.superdevlabs.autosubtitles.speech.WhisperText

/**
 * Translation works on whole sentences (a 3-word caption alone translates badly), then puts
 * the result back on the timeline. Pure functions: the translator itself is passed in.
 */
object TranslationPlanner {

    /** Consecutive captions that read as one sentence. */
    data class Sentence(val captions: List<Caption>) {
        val text: String get() = captions.joinToString(" ") { it.text }
        val startMs: Long get() = captions.first().startMs
        val endMs: Long get() = captions.last().endMs
    }

    private const val SENTENCE_END = ".!?。！？؟"

    /**
     * Groups captions into sentences: ends at sentence punctuation, a pause over [maxGapMs],
     * or after [maxCaptions] captions (Fast-engine captions have no punctuation).
     */
    fun sentences(captions: List<Caption>, maxGapMs: Long = 1_200, maxCaptions: Int = 4): List<Sentence> {
        val out = mutableListOf<Sentence>()
        var current = mutableListOf<Caption>()
        for (caption in captions.sortedBy { it.startMs }) {
            val last = current.lastOrNull()
            if (last != null && (caption.startMs - last.endMs > maxGapMs || current.size >= maxCaptions)) {
                out += Sentence(current)
                current = mutableListOf()
            }
            current += caption
            if (caption.text.trimEnd().lastOrNull()?.let { it in SENTENCE_END } == true) {
                out += Sentence(current)
                current = mutableListOf()
            }
        }
        if (current.isNotEmpty()) out += Sentence(current)
        return out
    }

    /**
     * Replace mode: the translated sentence's words are spread over the time each original
     * caption covered (in proportion to its length), then regrouped into captions.
     */
    fun replace(sentences: List<Sentence>, translations: List<String>, wordsPerCaption: Int): List<Caption> {
        val words = mutableListOf<Word>()
        sentences.forEachIndexed { index, sentence ->
            val tokens = WhisperText.tokens(translations.getOrNull(index).orEmpty())
            if (tokens.isEmpty()) return@forEachIndexed
            val shares = split(tokens, sentence.captions.map { it.words.size.coerceAtLeast(1) })
            sentence.captions.forEachIndexed { i, caption ->
                val part = shares[i]
                if (part.isNotEmpty()) {
                    val end = caption.words.lastOrNull()?.endMs ?: caption.endMs
                    words += CaptionEditing.distribute(part, caption.startMs, end)
                }
            }
        }
        return CaptionSegmenter.build(words, wordsPerCaption)
    }

    /** Bilingual mode: each caption keeps its words and gets its slice of the translated sentence. */
    fun bilingual(sentences: List<Sentence>, translations: List<String>): List<Caption> =
        sentences.flatMapIndexed { index, sentence ->
            val tokens = translations.getOrNull(index).orEmpty().split(' ').filter { it.isNotBlank() }
            val unspaced = tokens.size <= 1 && sentence.captions.size > 1
            val shares = if (unspaced) {
                splitChars(translations.getOrNull(index).orEmpty().trim(), sentence.captions.map { it.text.length.coerceAtLeast(1) })
            } else {
                split(tokens, sentence.captions.map { it.words.size.coerceAtLeast(1) }).map { it.joinToString(" ") }
            }
            sentence.captions.mapIndexed { i, caption -> caption.copy(translation = shares[i].ifBlank { null }) }
        }

    /** Splits [tokens] into consecutive parts sized like [weights]; every part may be empty. */
    fun split(tokens: List<String>, weights: List<Int>): List<List<String>> {
        if (weights.isEmpty()) return emptyList()
        val total = weights.sum().toDouble()
        val out = mutableListOf<List<String>>()
        var from = 0
        var acc = 0.0
        weights.forEachIndexed { i, weight ->
            acc += weight
            val to = if (i == weights.lastIndex) tokens.size else Math.round(tokens.size * acc / total).toInt().coerceIn(from, tokens.size)
            out += tokens.subList(from, to)
            from = to
        }
        return out
    }

    private fun splitChars(text: String, weights: List<Int>): List<String> {
        val chars = text.map { it.toString() }
        return split(chars, weights).map { it.joinToString("") }
    }
}
