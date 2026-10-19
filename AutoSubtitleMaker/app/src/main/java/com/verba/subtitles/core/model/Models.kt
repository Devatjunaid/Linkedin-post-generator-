package com.verba.subtitles.core.model

import kotlinx.serialization.Serializable

/** One recognised word with its timing inside the video, in milliseconds. */
@Serializable
data class Word(
    val text: String,
    val startMs: Long,
    val endMs: Long,
)

/** One on-screen subtitle: a short group of words shown together. */
@Serializable
data class Caption(
    val id: Long,
    val startMs: Long,
    val endMs: Long,
    val words: List<Word>,
) {
    val text: String get() = words.joinToString(" ") { it.text }
}

@Serializable
enum class TextCase { ORIGINAL, UPPER, LOWER }

/** How the word currently being spoken is emphasised. */
@Serializable
enum class HighlightMode { NONE, COLOR, BOX }

@Serializable
enum class BackgroundMode { NONE, LINE_BOX, BLOCK }

@Serializable
enum class ShadowMode { NONE, DROP, GLOW }

@Serializable
enum class CaptionAnimation { NONE, POP, BOUNCE, SLIDE_UP, FADE, TYPEWRITER, KARAOKE }

/**
 * Complete look of the subtitles. Sizes are fractions of the video's short side so the
 * preview and the exported video render identically at any resolution.
 */
@Serializable
data class CaptionStyle(
    val presetId: String = "pop",
    val fontId: String = "anton",
    val textSize: Float = 0.085f,
    val letterSpacing: Float = 0.02f,
    val textCase: TextCase = TextCase.UPPER,
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val highlightMode: HighlightMode = HighlightMode.COLOR,
    val highlightColor: Int = 0xFFD4FF3A.toInt(),
    val outlineWidth: Float = 0.12f,
    val outlineColor: Int = 0xFF000000.toInt(),
    val backgroundMode: BackgroundMode = BackgroundMode.NONE,
    val backgroundColor: Int = 0xFF0E0E12.toInt(),
    val backgroundOpacity: Float = 0.85f,
    val shadowMode: ShadowMode = ShadowMode.DROP,
    val animation: CaptionAnimation = CaptionAnimation.POP,
    /** Vertical centre of the caption block: 0 = top of the frame, 1 = bottom. */
    val positionY: Float = 0.78f,
    val wordsPerCaption: Int = 3,
)
