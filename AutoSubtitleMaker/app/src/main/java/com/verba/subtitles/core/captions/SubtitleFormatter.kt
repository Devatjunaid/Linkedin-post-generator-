package com.verba.subtitles.core.captions

import com.verba.subtitles.core.model.BackgroundMode
import com.verba.subtitles.core.model.Caption
import com.verba.subtitles.core.model.CaptionAnimation
import com.verba.subtitles.core.model.CaptionStyle
import com.verba.subtitles.core.model.ShadowMode
import com.verba.subtitles.core.model.TextCase
import java.util.Locale
import kotlin.math.roundToInt

enum class SubtitleFormat(val extension: String, val mimeType: String, val label: String) {
    SRT("srt", "application/x-subrip", "SRT"),
    VTT("vtt", "text/vtt", "VTT"),
    ASS("ass", "text/x-ssa", "ASS"),
    TXT("txt", "text/plain", "TXT"),
}

/** Writes captions as subtitle files. ASS keeps font, colours, outline, box and karaoke timing. */
object SubtitleFormatter {

    fun format(
        format: SubtitleFormat,
        captions: List<Caption>,
        style: CaptionStyle,
        fontName: String,
        videoWidth: Int,
        videoHeight: Int,
    ): String = when (format) {
        SubtitleFormat.SRT -> srt(captions, style)
        SubtitleFormat.VTT -> vtt(captions, style)
        SubtitleFormat.ASS -> ass(captions, style, fontName, videoWidth, videoHeight)
        SubtitleFormat.TXT -> captions.joinToString("\n") { applyCase(it.text, style.textCase) } + "\n"
    }

    fun srt(captions: List<Caption>, style: CaptionStyle): String = buildString {
        captions.forEachIndexed { index, caption ->
            append(index + 1).append('\n')
            append(srtTime(caption.startMs)).append(" --> ").append(srtTime(caption.endMs)).append('\n')
            append(applyCase(caption.text, style.textCase)).append("\n\n")
        }
    }

    fun vtt(captions: List<Caption>, style: CaptionStyle): String = buildString {
        append("WEBVTT\n\n")
        captions.forEach { caption ->
            append(vttTime(caption.startMs)).append(" --> ").append(vttTime(caption.endMs)).append('\n')
            append(applyCase(caption.text, style.textCase)).append("\n\n")
        }
    }

    fun ass(captions: List<Caption>, style: CaptionStyle, fontName: String, videoWidth: Int, videoHeight: Int): String {
        val width = videoWidth.coerceAtLeast(2)
        val height = videoHeight.coerceAtLeast(2)
        val shortSide = minOf(width, height)
        val fontSize = (style.textSize * shortSide).roundToInt().coerceAtLeast(8)
        val outline = (style.outlineWidth * fontSize * 0.5f).let { if (it < 0.1f) 0f else it }
        val boxed = style.backgroundMode != BackgroundMode.NONE
        val borderStyle = if (boxed) 3 else 1
        val backAlpha = if (boxed) style.backgroundOpacity else 0.6f
        val shadow = when (style.shadowMode) {
            ShadowMode.NONE -> 0
            ShadowMode.DROP -> 2
            ShadowMode.GLOW -> 3
        }
        val marginV = ((1f - style.positionY) * height - fontSize).roundToInt().coerceAtLeast(10)
        val karaoke = style.animation == CaptionAnimation.KARAOKE

        return buildString {
            append("[Script Info]\n")
            append("ScriptType: v4.00+\n")
            append("PlayResX: ").append(width).append('\n')
            append("PlayResY: ").append(height).append('\n')
            append("WrapStyle: 0\n")
            append("ScaledBorderAndShadow: yes\n\n")
            append("[V4+ Styles]\n")
            append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, ")
            append("Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, ")
            append("Alignment, MarginL, MarginR, MarginV, Encoding\n")
            append("Style: Default,").append(fontName.replace(",", " ")).append(',').append(fontSize).append(',')
            // In ASS karaoke, SecondaryColour is the not-yet-sung colour and Primary the sung one.
            if (karaoke) {
                append(assColor(style.highlightColor)).append(',').append(assColor(style.textColor)).append(',')
            } else {
                append(assColor(style.textColor)).append(',').append(assColor(style.highlightColor)).append(',')
            }
            append(assColor(if (boxed) style.backgroundColor else style.outlineColor)).append(',')
            append(assColor(style.backgroundColor, backAlpha)).append(',')
            append("-1,0,0,0,100,100,0,0,").append(borderStyle).append(',')
            append(String.format(Locale.US, "%.1f", if (boxed) fontSize * 0.2f else outline)).append(',')
            append(shadow).append(",2,40,40,").append(marginV).append(",1\n\n")
            append("[Events]\n")
            append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n")
            captions.forEach { caption ->
                append("Dialogue: 0,").append(assTime(caption.startMs)).append(',').append(assTime(caption.endMs))
                append(",Default,,0,0,0,,")
                if (karaoke) {
                    var cursor = caption.startMs
                    caption.words.forEachIndexed { i, word ->
                        val lead = ((word.startMs - cursor).coerceAtLeast(0) / 10)
                        if (lead > 0) append("{\\k").append(lead).append('}')
                        val dur = ((word.endMs - word.startMs).coerceAtLeast(10) / 10)
                        append("{\\kf").append(dur).append('}').append(escapeAss(applyCase(word.text, style.textCase)))
                        if (i < caption.words.lastIndex) append(' ')
                        cursor = word.endMs
                    }
                } else {
                    append(escapeAss(applyCase(caption.text, style.textCase)))
                }
                append('\n')
            }
        }
    }

    fun applyCase(text: String, case: TextCase): String = when (case) {
        TextCase.ORIGINAL -> text
        TextCase.UPPER -> text.uppercase(Locale.getDefault())
        TextCase.LOWER -> text.lowercase(Locale.getDefault())
    }

    fun srtTime(ms: Long): String = clock(ms, ',')

    fun vttTime(ms: Long): String = clock(ms, '.')

    fun assTime(ms: Long): String {
        val t = ms.coerceAtLeast(0)
        val h = t / 3_600_000
        val m = (t / 60_000) % 60
        val s = (t / 1_000) % 60
        val cs = (t % 1_000) / 10
        return String.format(Locale.US, "%d:%02d:%02d.%02d", h, m, s, cs)
    }

    private fun clock(ms: Long, separator: Char): String {
        val t = ms.coerceAtLeast(0)
        val h = t / 3_600_000
        val m = (t / 60_000) % 60
        val s = (t / 1_000) % 60
        val milli = t % 1_000
        return String.format(Locale.US, "%02d:%02d:%02d%c%03d", h, m, s, separator, milli)
    }

    /** ASS colours are &HAABBGGRR with alpha inverted (00 = opaque). */
    private fun assColor(argb: Int, opacity: Float = 1f): String {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        val alpha = 255 - (opacity.coerceIn(0f, 1f) * 255).roundToInt()
        return String.format(Locale.US, "&H%02X%02X%02X%02X", alpha, b, g, r)
    }

    private fun escapeAss(text: String): String = text.replace("{", "(").replace("}", ")").replace("\n", "\\N")
}
