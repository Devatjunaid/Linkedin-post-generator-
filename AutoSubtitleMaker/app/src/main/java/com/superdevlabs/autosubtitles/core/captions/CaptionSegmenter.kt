package com.superdevlabs.autosubtitles.core.captions

import com.superdevlabs.autosubtitles.core.model.Caption
import com.superdevlabs.autosubtitles.core.model.Word

/**
 * Groups timed words into readable captions.
 *
 * A new caption starts when the current one reaches [maxWords] words or [maxChars]
 * characters, when the speaker pauses for longer than [maxGapMs], or when the caption
 * would stay on screen longer than [maxDurationMs].
 */
object CaptionSegmenter {

    const val MIN_WORDS = 1
    const val MAX_WORDS = 6

    fun build(
        words: List<Word>,
        maxWords: Int,
        maxChars: Int = 32,
        maxGapMs: Long = 700,
        maxDurationMs: Long = 4_000,
        lingerMs: Long = 450,
    ): List<Caption> {
        val limit = maxWords.coerceIn(MIN_WORDS, MAX_WORDS)
        val sorted = words.filter { it.text.isNotBlank() }.sortedBy { it.startMs }
        if (sorted.isEmpty()) return emptyList()

        val groups = mutableListOf<MutableList<Word>>()
        var current = mutableListOf<Word>()
        var chars = 0
        for (word in sorted) {
            val first = current.firstOrNull()
            val last = current.lastOrNull()
            val breakHere = first != null && last != null && (
                current.size >= limit ||
                    chars + 1 + word.text.length > maxChars ||
                    word.startMs - last.endMs > maxGapMs ||
                    word.endMs - first.startMs > maxDurationMs
                )
            if (breakHere) {
                groups += current
                current = mutableListOf()
                chars = 0
            }
            current += word
            chars += if (chars == 0) word.text.length else word.text.length + 1
        }
        if (current.isNotEmpty()) groups += current

        return groups.mapIndexed { index, group ->
            val nextStart = groups.getOrNull(index + 1)?.first()?.startMs
            val naturalEnd = group.last().endMs + lingerMs
            val end = if (nextStart != null) minOf(naturalEnd, nextStart) else naturalEnd
            Caption(
                id = index.toLong() + 1,
                startMs = group.first().startMs,
                endMs = maxOf(end, group.last().endMs),
                words = group.toList(),
            )
        }
    }

    /** All words of the given captions in time order (used when re-grouping). */
    fun flatten(captions: List<Caption>): List<Word> = captions.flatMap { it.words }.sortedBy { it.startMs }
}
