package com.superdevlabs.autosubtitles.core.captions

import com.superdevlabs.autosubtitles.core.model.BackgroundMode
import com.superdevlabs.autosubtitles.core.model.CaptionAnimation
import com.superdevlabs.autosubtitles.core.model.CaptionStyle
import com.superdevlabs.autosubtitles.core.model.HighlightMode
import com.superdevlabs.autosubtitles.core.model.ShadowMode
import com.superdevlabs.autosubtitles.core.model.TextCase

data class StylePreset(val id: String, val name: String, val style: CaptionStyle, val isNew: Boolean = false)

object StylePresets {

    const val LIME = 0xFFD4FF3A.toInt()
    const val CORAL = 0xFFFF7A59.toInt()
    const val ICE = 0xFF8DF0FF.toInt()
    const val SUN = 0xFFFFE14D.toInt()
    const val LILAC = 0xFFC9A8FF.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
    const val CREAM = 0xFFFFF4D6.toInt()
    const val INK = 0xFF0E0E12.toInt()
    const val BLACK = 0xFF000000.toInt()
    const val GREEN = 0xFF4DFF88.toInt()
    const val YELLOW = 0xFFFFD60A.toInt()
    const val RED = 0xFFFF3B3B.toInt()

    val textColors = listOf(WHITE, CREAM, SUN, ICE, LILAC, CORAL, LIME, INK)
    val accentColors = listOf(LIME, CORAL, ICE, SUN, YELLOW, GREEN, RED, LILAC, WHITE, INK)
    val keywordColors = listOf(GREEN, YELLOW, RED, LIME, CORAL, ICE, LILAC)

    val all: List<StylePreset> = listOf(
        // ---- Trending short-video looks (v1.1) ----
        StylePreset(
            "viral", "Viral",
            CaptionStyle(
                presetId = "viral", fontId = "anton", textSize = 0.095f, letterSpacing = 0.01f,
                textCase = TextCase.UPPER, highlightMode = HighlightMode.COLOR, highlightColor = YELLOW,
                outlineWidth = 0.16f, outlineColor = BLACK, shadowMode = ShadowMode.DROP,
                animation = CaptionAnimation.POP, autoEmoji = true, keywordHighlight = true, keywordColor = GREEN,
                positionY = 0.7f, wordsPerCaption = 2,
            ),
            isNew = true,
        ),
        StylePreset(
            "spotlight", "Spotlight",
            CaptionStyle(
                presetId = "spotlight", fontId = "poppins_black", textSize = 0.075f, letterSpacing = 0f,
                textCase = TextCase.UPPER, highlightMode = HighlightMode.BOX, highlightColor = RED,
                outlineWidth = 0.12f, outlineColor = BLACK, shadowMode = ShadowMode.DROP,
                animation = CaptionAnimation.POP, keywordHighlight = false,
            ),
            isNew = true,
        ),
        StylePreset(
            "hype", "Hype",
            CaptionStyle(
                presetId = "hype", fontId = "archivo_black", textSize = 0.088f, letterSpacing = 0f,
                textCase = TextCase.UPPER, textColor = WHITE, highlightMode = HighlightMode.COLOR,
                highlightColor = GREEN, outlineWidth = 0.18f, outlineColor = BLACK, shadowMode = ShadowMode.GLOW,
                animation = CaptionAnimation.BOUNCE, autoEmoji = true, keywordHighlight = true, keywordColor = YELLOW,
            ),
            isNew = true,
        ),
        StylePreset(
            "story", "Story",
            CaptionStyle(
                presetId = "story", fontId = "poppins_bold", textSize = 0.058f, letterSpacing = 0f,
                textCase = TextCase.ORIGINAL, textColor = INK, highlightMode = HighlightMode.COLOR,
                highlightColor = CORAL, outlineWidth = 0f, backgroundMode = BackgroundMode.LINE_BOX,
                backgroundColor = WHITE, backgroundOpacity = 1f, shadowMode = ShadowMode.NONE,
                animation = CaptionAnimation.SLIDE_UP, autoEmoji = true,
            ),
            isNew = true,
        ),
        StylePreset(
            "podcast", "Podcast",
            CaptionStyle(
                presetId = "podcast", fontId = "bricolage", textSize = 0.06f, letterSpacing = 0f,
                textCase = TextCase.ORIGINAL, highlightMode = HighlightMode.COLOR, highlightColor = ICE,
                outlineWidth = 0f, backgroundMode = BackgroundMode.BLOCK, backgroundColor = INK,
                backgroundOpacity = 0.7f, shadowMode = ShadowMode.NONE, animation = CaptionAnimation.KARAOKE,
                keywordHighlight = true, keywordColor = YELLOW, wordsPerCaption = 5,
            ),
            isNew = true,
        ),
        // ---- Classics ----
        StylePreset("pop", "Pop", CaptionStyle()),
        StylePreset(
            "karaoke", "Karaoke",
            CaptionStyle(
                presetId = "karaoke", fontId = "poppins_black", textSize = 0.07f, letterSpacing = 0f,
                textCase = TextCase.ORIGINAL, highlightMode = HighlightMode.BOX, highlightColor = LIME,
                outlineWidth = 0f, shadowMode = ShadowMode.DROP, animation = CaptionAnimation.KARAOKE,
            ),
        ),
        StylePreset(
            "boxed", "Boxed",
            CaptionStyle(
                presetId = "boxed", fontId = "poppins_bold", textSize = 0.06f, letterSpacing = 0f,
                textCase = TextCase.ORIGINAL, highlightMode = HighlightMode.COLOR, highlightColor = CORAL,
                outlineWidth = 0f, backgroundMode = BackgroundMode.LINE_BOX, backgroundColor = INK,
                backgroundOpacity = 0.85f, shadowMode = ShadowMode.NONE, animation = CaptionAnimation.SLIDE_UP,
            ),
        ),
        StylePreset(
            "neon", "Neon",
            CaptionStyle(
                presetId = "neon", fontId = "bebas_neue", textSize = 0.1f, letterSpacing = 0.06f,
                textCase = TextCase.UPPER, textColor = 0xFFFFE9E2.toInt(), highlightMode = HighlightMode.COLOR,
                highlightColor = CORAL, outlineWidth = 0f, shadowMode = ShadowMode.GLOW,
                animation = CaptionAnimation.FADE,
            ),
        ),
        StylePreset(
            "minimal", "Minimal",
            CaptionStyle(
                presetId = "minimal", fontId = "dm_serif_display", textSize = 0.062f, letterSpacing = 0f,
                textCase = TextCase.ORIGINAL, highlightMode = HighlightMode.NONE, outlineWidth = 0f,
                shadowMode = ShadowMode.DROP, animation = CaptionAnimation.FADE,
            ),
        ),
        StylePreset(
            "comic", "Comic",
            CaptionStyle(
                presetId = "comic", fontId = "bangers", textSize = 0.092f, letterSpacing = 0.04f,
                textCase = TextCase.UPPER, textColor = SUN, highlightMode = HighlightMode.COLOR,
                highlightColor = WHITE, outlineWidth = 0.16f, outlineColor = BLACK,
                shadowMode = ShadowMode.DROP, animation = CaptionAnimation.BOUNCE,
            ),
        ),
        StylePreset(
            "typewriter", "Typewriter",
            CaptionStyle(
                presetId = "typewriter", fontId = "space_mono_bold", textSize = 0.055f, letterSpacing = 0f,
                textCase = TextCase.ORIGINAL, highlightMode = HighlightMode.COLOR, highlightColor = LIME,
                outlineWidth = 0f, backgroundMode = BackgroundMode.BLOCK, backgroundOpacity = 0.75f,
                shadowMode = ShadowMode.NONE, animation = CaptionAnimation.TYPEWRITER,
            ),
        ),
        StylePreset(
            "marker", "Marker",
            CaptionStyle(
                presetId = "marker", fontId = "permanent_marker", textSize = 0.078f, letterSpacing = 0f,
                textCase = TextCase.ORIGINAL, highlightMode = HighlightMode.COLOR, highlightColor = LIME,
                outlineWidth = 0.1f, shadowMode = ShadowMode.DROP, animation = CaptionAnimation.BOUNCE,
            ),
        ),
    )

    fun byId(id: String): StylePreset = all.firstOrNull { it.id == id } ?: all.first()

    /** Applies a preset but keeps the user's layout choices (position, words per caption). */
    fun apply(preset: StylePreset, current: CaptionStyle): CaptionStyle =
        preset.style.copy(positionY = current.positionY, wordsPerCaption = current.wordsPerCaption)
}
