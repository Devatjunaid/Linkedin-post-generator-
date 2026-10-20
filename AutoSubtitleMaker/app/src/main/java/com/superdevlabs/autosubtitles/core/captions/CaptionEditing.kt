package com.superdevlabs.autosubtitles.core.captions

import com.superdevlabs.autosubtitles.core.model.Caption
import com.superdevlabs.autosubtitles.core.model.Word

/** Pure edit operations on a caption list. Every function returns a new, time-sorted list. */
object CaptionEditing {

    private val whitespace = Regex("\\s+")

    /**
     * Replaces the text of one caption. If the word count is unchanged each word keeps its
     * timing; otherwise the caption's time span is shared between the new words in
     * proportion to their length. Blank text deletes the caption.
     */
    fun editText(captions: List<Caption>, id: Long, newText: String): List<Caption> {
        val tokens = newText.trim().split(whitespace).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return delete(captions, id)
        return captions.map { caption ->
            if (caption.id != id) return@map caption
            val words = if (tokens.size == caption.words.size) {
                caption.words.zip(tokens) { word, token -> word.copy(text = token) }
            } else {
                distribute(tokens, caption.startMs, caption.words.lastOrNull()?.endMs ?: caption.endMs)
            }
            caption.copy(words = words)
        }
    }

    fun delete(captions: List<Caption>, id: Long): List<Caption> = captions.filterNot { it.id == id }

    /**
     * Moves the start and/or end of a caption, keeping it inside its neighbours and at least
     * [minDurationMs] long. Word timings are scaled to the new span.
     */
    fun retime(
        captions: List<Caption>,
        id: Long,
        deltaStartMs: Long,
        deltaEndMs: Long,
        minDurationMs: Long = 200,
    ): List<Caption> {
        val index = captions.indexOfFirst { it.id == id }
        if (index < 0) return captions
        val caption = captions[index]
        val lowerBound = captions.getOrNull(index - 1)?.endMs ?: 0L
        val upperBound = captions.getOrNull(index + 1)?.startMs ?: Long.MAX_VALUE
        val newStart = clamp(caption.startMs + deltaStartMs, lowerBound, caption.endMs - minDurationMs)
        val newEnd = clamp(caption.endMs + deltaEndMs, newStart + minDurationMs, upperBound)
        if (newEnd - newStart < minDurationMs || newStart < lowerBound || newEnd > upperBound) return captions
        val updated = caption.copy(
            startMs = newStart,
            endMs = newEnd,
            words = rescale(caption.words, caption.startMs, caption.endMs, newStart, newEnd),
        )
        return captions.toMutableList().also { it[index] = updated }
    }

    /** Shares [startMs]..[endMs] between [tokens] in proportion to their character count. */
    fun distribute(tokens: List<String>, startMs: Long, endMs: Long): List<Word> {
        val span = (endMs - startMs).coerceAtLeast(tokens.size.toLong())
        val weights = tokens.map { it.length.coerceAtLeast(1) }
        val total = weights.sum().toDouble()
        var cursor = startMs.toDouble()
        return tokens.mapIndexed { i, token ->
            val length = span * (weights[i] / total)
            val word = Word(token, cursor.toLong(), (cursor + length).toLong())
            cursor += length
            word
        }
    }

    /** Like coerceIn, but never throws when the bounds cross (the lower bound wins). */
    private fun clamp(value: Long, min: Long, max: Long): Long = maxOf(min, minOf(value, max))

    private fun rescale(words: List<Word>, oldStart: Long, oldEnd: Long, newStart: Long, newEnd: Long): List<Word> {
        val oldSpan = (oldEnd - oldStart).coerceAtLeast(1).toDouble()
        val newSpan = (newEnd - newStart).toDouble()
        fun map(t: Long): Long = (newStart + (t - oldStart) / oldSpan * newSpan).toLong().coerceIn(newStart, newEnd)
        return words.map { it.copy(startMs = map(it.startMs), endMs = map(it.endMs)) }
    }
}
