package com.superdevlabs.autosubtitles

import com.superdevlabs.autosubtitles.core.captions.CaptionEditing
import com.superdevlabs.autosubtitles.core.captions.CaptionSegmenter
import com.superdevlabs.autosubtitles.core.captions.StylePresets
import com.superdevlabs.autosubtitles.core.captions.SubtitleFormat
import com.superdevlabs.autosubtitles.core.captions.SubtitleFormatter
import com.superdevlabs.autosubtitles.core.model.CaptionAnimation
import com.superdevlabs.autosubtitles.core.model.CaptionStyle
import com.superdevlabs.autosubtitles.core.model.TextCase
import com.superdevlabs.autosubtitles.core.model.Word
import com.superdevlabs.autosubtitles.speech.PcmResampler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptionLogicTest {

    private fun words(vararg spec: Pair<String, LongRange>) = spec.map { (t, r) -> Word(t, r.first, r.last) }

    private val sample = words(
        "okay" to 0L..300L, "so" to 320L..450L, "this" to 470L..600L, "morning" to 620L..1_000L,
        "I" to 1_050L..1_120L, "tried" to 1_150L..1_400L,
        // long pause → must start a new caption
        "something" to 2_600L..3_000L, "new" to 3_050L..3_300L,
    )

    @Test
    fun segmenter_respects_word_limit_and_pauses() {
        val captions = CaptionSegmenter.build(sample, maxWords = 3)
        assertEquals(listOf("okay so this", "morning I tried", "something new"), captions.map { it.text })
        // Captions never overlap and are ordered.
        captions.zipWithNext().forEach { (a, b) -> assertTrue(a.endMs <= b.startMs) }
        // Linger: the 2nd caption stays a bit after its last word but ends before the next one.
        assertEquals(1_400L + 450L, captions[1].endMs)
    }

    @Test
    fun segmenter_regroups_from_flattened_words() {
        val three = CaptionSegmenter.build(sample, 3)
        val one = CaptionSegmenter.build(CaptionSegmenter.flatten(three), 1)
        assertEquals(sample.size, one.size)
        assertEquals(sample.map { it.text }, one.map { it.text })
    }

    @Test
    fun segmenter_clamps_word_limit() {
        assertEquals(sample.size, CaptionSegmenter.build(sample, 0).size)
        assertTrue(CaptionSegmenter.build(emptyList(), 3).isEmpty())
    }

    @Test
    fun edit_keeps_timings_when_word_count_matches() {
        val captions = CaptionSegmenter.build(sample, 3)
        val edited = CaptionEditing.editText(captions, captions[0].id, "Okay so THIS")
        assertEquals(captions[0].words.map { it.startMs }, edited[0].words.map { it.startMs })
        assertEquals("Okay so THIS", edited[0].text)
    }

    @Test
    fun edit_redistributes_when_word_count_changes_and_blank_deletes() {
        val captions = CaptionSegmenter.build(sample, 3)
        val edited = CaptionEditing.editText(captions, captions[0].id, "hello there my friend")
        val ws = edited[0].words
        assertEquals(4, ws.size)
        assertEquals(captions[0].startMs, ws.first().startMs)
        ws.zipWithNext().forEach { (a, b) -> assertTrue(a.endMs <= b.startMs) }
        assertEquals(captions.size - 1, CaptionEditing.editText(captions, captions[0].id, "   ").size)
    }

    @Test
    fun retime_stays_within_neighbours_and_never_throws() {
        val captions = CaptionSegmenter.build(sample, 3)
        val middle = captions[1]
        val moved = CaptionEditing.retime(captions, middle.id, -10_000, 10_000)
        val m = moved[1]
        assertTrue(m.startMs >= captions[0].endMs)
        assertTrue(m.endMs <= captions[2].startMs)
        m.words.forEach { assertTrue(it.startMs >= m.startMs && it.endMs <= m.endMs) }
        // Shrinking far past zero length must not crash.
        CaptionEditing.retime(captions, middle.id, 10_000, -10_000)
    }

    @Test
    fun srt_and_vtt_timestamps() {
        assertEquals("01:02:03,045", SubtitleFormatter.srtTime(3_723_045))
        assertEquals("00:00:01.500", SubtitleFormatter.vttTime(1_500))
        assertEquals("0:00:01.50", SubtitleFormatter.assTime(1_500))
        val captions = CaptionSegmenter.build(sample, 3)
        val srt = SubtitleFormatter.srt(captions, CaptionStyle(textCase = TextCase.UPPER))
        assertTrue(srt.startsWith("1\n00:00:00,000 --> "))
        assertTrue(srt.contains("OKAY SO THIS"))
        val vtt = SubtitleFormatter.vtt(captions, CaptionStyle(textCase = TextCase.ORIGINAL))
        assertTrue(vtt.startsWith("WEBVTT\n\n"))
    }

    @Test
    fun ass_contains_style_and_karaoke_tags() {
        val captions = CaptionSegmenter.build(sample, 3)
        val style = StylePresets.byId("karaoke").style
        assertEquals(CaptionAnimation.KARAOKE, style.animation)
        val ass = SubtitleFormatter.format(SubtitleFormat.ASS, captions, style, "Poppins Black", 1080, 1920)
        assertTrue(ass.contains("PlayResY: 1920"))
        assertTrue(ass.contains("Style: Default,Poppins Black,"))
        assertTrue(ass.contains("{\\kf"))
        // White text colour in ASS BGR order with opaque alpha.
        assertTrue(ass.contains("&H00FFFFFF"))
    }

    @Test
    fun presets_keep_user_layout() {
        val current = CaptionStyle(positionY = 0.3f, wordsPerCaption = 5)
        val applied = StylePresets.apply(StylePresets.byId("neon"), current)
        assertEquals(0.3f, applied.positionY)
        assertEquals(5, applied.wordsPerCaption)
        assertEquals("bebas_neue", applied.fontId)
    }

    @Test
    fun resampler_converts_48k_to_16k() {
        val r = PcmResampler(48_000, 16_000)
        var out = 0
        // 1 second of a 440 Hz tone fed in uneven chunks.
        val input = FloatArray(48_000) { kotlin.math.sin(2 * Math.PI * 440 * it / 48_000).toFloat() * 0.5f }
        var offset = 0
        val sizes = intArrayOf(1_000, 4_096, 333, 12_000)
        var k = 0
        while (offset < input.size) {
            val n = minOf(sizes[k++ % sizes.size], input.size - offset)
            val chunk = input.copyOfRange(offset, offset + n)
            r.process(chunk, n) { s ->
                assertTrue(s in -1f..1f)
                out++
            }
            offset += n
        }
        assertTrue("got $out samples", out in 15_990..16_010)
    }
}
