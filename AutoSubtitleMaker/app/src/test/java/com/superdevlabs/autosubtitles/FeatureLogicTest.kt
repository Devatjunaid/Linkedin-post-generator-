package com.superdevlabs.autosubtitles

import com.superdevlabs.autosubtitles.core.captions.CaptionSegmenter
import com.superdevlabs.autosubtitles.core.captions.Emphasis
import com.superdevlabs.autosubtitles.core.captions.SubtitleFormatter
import com.superdevlabs.autosubtitles.core.captions.TranslationPlanner
import com.superdevlabs.autosubtitles.core.model.Caption
import com.superdevlabs.autosubtitles.core.model.CaptionStyle
import com.superdevlabs.autosubtitles.core.model.TextCase
import com.superdevlabs.autosubtitles.core.model.Word
import com.superdevlabs.autosubtitles.speech.WhisperText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureLogicTest {

    // ---- Whisper text → timed words ---------------------------------------------------------

    @Test
    fun whisper_words_fill_the_segment_in_order() {
        val words = WhisperText.timedWords("Everybody, go ahead and have a seat.", 9_410, 12_690)
        assertEquals(listOf("Everybody,", "go", "ahead", "and", "have", "a", "seat."), words.map { it.text })
        assertEquals(9_410, words.first().startMs)
        assertTrue(words.last().endMs <= 12_690)
        assertTrue(words.last().endMs >= 12_600)
        words.zipWithNext().forEach { (a, b) -> assertTrue(a.endMs <= b.startMs) }
        // "Everybody," is longer than "go" so it gets more time.
        assertTrue(words[0].endMs - words[0].startMs > words[1].endMs - words[1].startMs)
        // A comma leaves a short pause after the word.
        assertTrue(words[1].startMs > words[0].endMs)
    }

    @Test
    fun whisper_cleaning_drops_noise_markers_and_hallucinations() {
        assertEquals("", WhisperText.clean("[Music]", 4_000))
        assertEquals("", WhisperText.clean("♪ ♪", 4_000))
        assertEquals("", WhisperText.clean("Thank you.", 1_200))
        assertEquals("Thank you.", WhisperText.clean("Thank you.", 6_000))
        assertEquals("Hello there", WhisperText.clean("Hello (laughs) there", 3_000))
        assertEquals("no no no stop", WhisperText.clean("no no no no no no no no stop", 5_000))
    }

    @Test
    fun whisper_unspaced_languages_are_chunked() {
        val tokens = WhisperText.tokens("感谢各位今天来到这里我们开始吧")
        assertTrue(tokens.size >= 3)
        assertEquals("感谢各位今天来到这里我们开始吧", tokens.joinToString(""))
    }

    @Test
    fun long_segments_split_at_the_quietest_point() {
        val rate = 16_000
        // 6 s of "speech", 0.3 s of silence, 6 s of speech.
        val samples = FloatArray(rate * 12 + rate * 3 / 10) { i ->
            val inGap = i in rate * 6 until rate * 6 + rate * 3 / 10
            if (inGap) 0f else (if (i % 2 == 0) 0.5f else -0.5f)
        }
        val pieces = WhisperText.splitPoints(samples, maxSamples = rate * 7)
        assertEquals(2, pieces.size)
        val cut = pieces[1].first
        assertTrue("cut at $cut", cut in rate * 6 until rate * 6 + rate * 3 / 10)
        assertEquals(samples.size, pieces.sumOf { it.second })
    }

    @Test
    fun language_vote_picks_majority() {
        assertEquals("en", WhisperText.majority(listOf("en", "zh", "en", "en")))
        assertNull(WhisperText.majority(emptyList()))
    }

    // ---- Captions ---------------------------------------------------------------------------

    @Test
    fun segmenter_breaks_after_sentence_end() {
        val words = listOf(
            Word("Hi.", 0, 200), Word("I'm", 220, 400), Word("Sam", 420, 600),
        )
        val captions = CaptionSegmenter.build(words, maxWords = 3)
        assertEquals(listOf("Hi.", "I'm Sam"), captions.map { it.text })
    }

    @Test
    fun keywords_and_emoji_are_detected() {
        assertTrue(Emphasis.isKeyword("$500"))
        assertTrue(Emphasis.isKeyword("10x"))
        assertTrue(Emphasis.isKeyword("MONEY!"))
        assertFalse(Emphasis.isKeyword("the"))
        val caption = Caption(1, 0, 1000, listOf(Word("make", 0, 300), Word("money", 300, 600), Word("fast", 600, 900)))
        assertEquals("💰", Emphasis.emojiFor(caption))
        assertNull(Emphasis.emojiFor(Caption(2, 0, 500, listOf(Word("the", 0, 200), Word("and", 200, 400)))))
    }

    // ---- Translation planning ---------------------------------------------------------------

    private fun cap(id: Long, text: String, start: Long, end: Long): Caption {
        val tokens = text.split(' ')
        val step = (end - start) / tokens.size
        return Caption(id, start, end, tokens.mapIndexed { i, t -> Word(t, start + i * step, start + (i + 1) * step) })
    }

    private val captions = listOf(
        cap(1, "hello my friends", 0, 900),
        cap(2, "how are you.", 950, 1_800),
        // long pause → new sentence
        cap(3, "see you soon", 4_000, 5_000),
    )

    @Test
    fun sentences_group_by_punctuation_and_pauses() {
        val sentences = TranslationPlanner.sentences(captions)
        assertEquals(2, sentences.size)
        assertEquals("hello my friends how are you.", sentences[0].text)
        assertEquals("see you soon", sentences[1].text)
    }

    @Test
    fun bilingual_spreads_translation_over_the_sentence_captions() {
        val sentences = TranslationPlanner.sentences(captions)
        val result = TranslationPlanner.bilingual(sentences, listOf("hola mis amigos cómo estás.", "hasta pronto"))
        assertEquals(3, result.size)
        assertEquals(captions.map { it.text }, result.map { it.text })
        assertEquals("hola mis amigos", result[0].translation)
        assertEquals("cómo estás.", result[1].translation)
        assertEquals("hasta pronto", result[2].translation)
    }

    @Test
    fun replace_rebuilds_captions_inside_the_original_time() {
        val sentences = TranslationPlanner.sentences(captions)
        val result = TranslationPlanner.replace(sentences, listOf("hola mis amigos cómo estás.", "hasta pronto"), wordsPerCaption = 3)
        val words = result.flatMap { it.words }
        assertEquals("hola mis amigos cómo estás. hasta pronto", words.joinToString(" ") { it.text })
        assertTrue(words.first().startMs >= 0)
        assertTrue(words.first { it.text == "hasta" }.startMs >= 4_000)
        assertTrue(result.all { it.translation == null })
    }

    @Test
    fun split_keeps_every_token() {
        val parts = TranslationPlanner.split(listOf("a", "b", "c", "d", "e"), listOf(1, 1, 3))
        assertEquals(listOf("a", "b", "c", "d", "e"), parts.flatten())
        assertEquals(3, parts.size)
    }

    @Test
    fun srt_includes_translation_line() {
        val bilingual = listOf(captions[0].copy(translation = "hola mis amigos"))
        val srt = SubtitleFormatter.srt(bilingual, CaptionStyle(textCase = TextCase.ORIGINAL))
        assertTrue(srt, srt.contains("hello my friends\nhola mis amigos\n"))
    }
}
