package com.verba.subtitles.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verba.subtitles.core.captions.CaptionSegmenter
import com.verba.subtitles.core.captions.StylePresets
import com.verba.subtitles.core.captions.SubtitleFormatter
import com.verba.subtitles.core.model.BackgroundMode
import com.verba.subtitles.core.model.Caption
import com.verba.subtitles.core.model.CaptionAnimation
import com.verba.subtitles.core.model.CaptionStyle
import com.verba.subtitles.core.model.HighlightMode
import com.verba.subtitles.core.model.ShadowMode
import com.verba.subtitles.core.model.TextCase
import com.verba.subtitles.core.render.FontRegistry
import com.verba.subtitles.ui.components.ChoiceCard
import com.verba.subtitles.ui.components.ColorSwatch
import com.verba.subtitles.ui.components.SectionLabel
import com.verba.subtitles.ui.components.Segmented
import com.verba.subtitles.ui.components.pressable
import com.verba.subtitles.ui.theme.Verba

typealias StyleEdit = ((CaptionStyle) -> CaptionStyle) -> Unit

@Composable
private fun PanelColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
private fun VerbaSlider(value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Slider(
        value = value.coerceIn(range.start, range.endInclusive),
        onValueChange = onChange,
        valueRange = range,
        colors = SliderDefaults.colors(
            thumbColor = Verba.Lime,
            activeTrackColor = Verba.Lime,
            inactiveTrackColor = Verba.SurfaceTop,
        ),
    )
}

// ---- Captions -------------------------------------------------------------------------------

@Composable
fun CaptionsPanel(
    captions: List<Caption>,
    positionMs: () -> Long,
    selectedId: Long?,
    style: CaptionStyle,
    onSeek: (Caption) -> Unit,
    onEdit: (Caption) -> Unit,
) {
    if (captions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No captions. Re-run the project with the right language.", color = Verba.Muted)
        }
        return
    }
    val listState = rememberLazyListState()
    val userScrolling by listState.interactionSource.collectIsDraggedAsState()
    // derivedStateOf: recompose only when the active caption changes, not every frame.
    val activeIndex by remember(captions) {
        derivedStateOf {
            val now = positionMs()
            val i = captions.indexOfLast { it.startMs <= now }
            if (i >= 0 && now < captions[i].endMs) i else -1
        }
    }
    LaunchedEffect(activeIndex) {
        if (!userScrolling && activeIndex >= 0) listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(captions, key = { _, c -> c.id }) { index, caption ->
            val active = index == activeIndex
            val selected = caption.id == selectedId
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (active) Verba.Lime.copy(alpha = 0.14f) else Verba.SurfaceHigh)
                    .border(1.5.dp, if (selected) Verba.Coral else if (active) Verba.Lime else Verba.Line, RoundedCornerShape(16.dp))
                    .pressable(onClick = { onSeek(caption) }, pressedScale = 0.98f)
                    .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${clockPrecise(caption.startMs)} → ${clockPrecise(caption.endMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) Verba.Lime else Verba.Muted,
                    )
                    Text(
                        SubtitleFormatter.applyCase(caption.text, style.textCase),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(Modifier.size(44.dp).pressable(onClick = { onEdit(caption) }), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit caption", tint = Verba.Muted, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ---- Style presets --------------------------------------------------------------------------

@Composable
fun StylePanel(fonts: FontRegistry, style: CaptionStyle, onPreset: (String) -> Unit) {
    PanelColumn {
        SectionLabel("Presets")
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 3,
        ) {
            StylePresets.all.forEach { preset ->
                ChoiceCard(
                    selected = preset.id == style.presetId,
                    onClick = { onPreset(preset.id) },
                    modifier = Modifier.weight(1f).height(96.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MiniCaptionPreview(fonts, preset.style, Modifier.fillMaxWidth().height(64.dp))
                        Text(preset.name, style = MaterialTheme.typography.labelMedium, color = Verba.Soft)
                    }
                }
            }
        }
        Text(
            "Presets change font, colours and motion together. Fine-tune anything in the other tabs.",
            style = MaterialTheme.typography.bodySmall,
            color = Verba.Muted,
        )
    }
}

// ---- Font -----------------------------------------------------------------------------------

@Composable
fun FontPanel(fonts: FontRegistry, style: CaptionStyle, onStyle: StyleEdit, onImport: (Uri) -> Unit) {
    val custom by fonts.custom.collectAsStateWithLifecycle()
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImport(uri)
    }
    val options = remember(custom) { fonts.all() }
    PanelColumn {
        SectionLabel("Typeface")
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 3,
        ) {
            options.forEach { option ->
                val family = remember(option.id) { FontFamily(fonts.typeface(option.id)) }
                ChoiceCard(
                    selected = option.id == style.fontId,
                    onClick = { onStyle { it.copy(fontId = option.id) } },
                    modifier = Modifier.weight(1f).height(72.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Aa", fontFamily = family, fontSize = 24.sp, color = Verba.Text, maxLines = 1)
                        Text(option.name, style = MaterialTheme.typography.labelSmall, color = Verba.Soft, maxLines = 1)
                    }
                }
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, Verba.SurfaceTop, RoundedCornerShape(16.dp))
                    .pressable(onClick = {
                        importer.launch(
                            arrayOf(
                                "font/ttf", "font/otf", "font/sfnt", "application/x-font-ttf",
                                "application/x-font-otf", "application/font-sfnt", "application/octet-stream",
                            ),
                        )
                    }),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = Verba.Lime)
                    Text("Import .ttf/.otf", style = MaterialTheme.typography.labelSmall, color = Verba.Soft)
                }
            }
        }

        SectionLabel("Size")
        VerbaSlider(style.textSize, 0.035f..0.14f) { v -> onStyle { it.copy(textSize = v) } }
        SectionLabel("Letter spacing")
        VerbaSlider(style.letterSpacing, -0.02f..0.2f) { v -> onStyle { it.copy(letterSpacing = v) } }
        SectionLabel("Case")
        Segmented(
            options = TextCase.entries,
            selected = style.textCase,
            label = {
                when (it) {
                    TextCase.ORIGINAL -> "Aa"
                    TextCase.UPPER -> "AA"
                    TextCase.LOWER -> "aa"
                }
            },
            onSelect = { c -> onStyle { it.copy(textCase = c) } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---- Color ----------------------------------------------------------------------------------

@Composable
private fun SwatchRow(colors: List<Int>, selected: Int, label: String, onPick: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { c ->
            ColorSwatch(Color(c), selected == c, "$label ${Integer.toHexString(c)}", onClick = { onPick(c) })
        }
    }
}

@Composable
fun ColorPanel(style: CaptionStyle, onStyle: StyleEdit) {
    PanelColumn {
        SectionLabel("Text colour")
        SwatchRow(StylePresets.textColors, style.textColor, "Text") { c -> onStyle { it.copy(textColor = c) } }

        SectionLabel("Spoken word highlight")
        Segmented(
            options = HighlightMode.entries,
            selected = style.highlightMode,
            label = {
                when (it) {
                    HighlightMode.NONE -> "Off"
                    HighlightMode.COLOR -> "Colour"
                    HighlightMode.BOX -> "Box"
                }
            },
            onSelect = { m -> onStyle { it.copy(highlightMode = m) } },
            modifier = Modifier.fillMaxWidth(),
        )
        SwatchRow(StylePresets.accentColors, style.highlightColor, "Highlight") { c -> onStyle { it.copy(highlightColor = c) } }

        SectionLabel("Outline")
        VerbaSlider(style.outlineWidth, 0f..0.25f) { v -> onStyle { it.copy(outlineWidth = v) } }
        SwatchRow(
            listOf(StylePresets.BLACK, StylePresets.INK, StylePresets.WHITE, StylePresets.CORAL, StylePresets.LILAC),
            style.outlineColor,
            "Outline",
        ) { c -> onStyle { it.copy(outlineColor = c) } }

        SectionLabel("Background")
        Segmented(
            options = BackgroundMode.entries,
            selected = style.backgroundMode,
            label = {
                when (it) {
                    BackgroundMode.NONE -> "None"
                    BackgroundMode.LINE_BOX -> "Per line"
                    BackgroundMode.BLOCK -> "Block"
                }
            },
            onSelect = { m -> onStyle { it.copy(backgroundMode = m) } },
            modifier = Modifier.fillMaxWidth(),
        )
        if (style.backgroundMode != BackgroundMode.NONE) {
            SwatchRow(
                listOf(StylePresets.INK, StylePresets.BLACK, StylePresets.WHITE, StylePresets.LIME, StylePresets.CORAL),
                style.backgroundColor,
                "Background",
            ) { c -> onStyle { it.copy(backgroundColor = c) } }
            SectionLabel("Background opacity")
            VerbaSlider(style.backgroundOpacity, 0.2f..1f) { v -> onStyle { it.copy(backgroundOpacity = v) } }
        }

        SectionLabel("Shadow")
        Segmented(
            options = ShadowMode.entries,
            selected = style.shadowMode,
            label = {
                when (it) {
                    ShadowMode.NONE -> "None"
                    ShadowMode.DROP -> "Drop"
                    ShadowMode.GLOW -> "Glow"
                }
            },
            onSelect = { m -> onStyle { it.copy(shadowMode = m) } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---- Motion ---------------------------------------------------------------------------------

private fun CaptionAnimation.label(): String = when (this) {
    CaptionAnimation.NONE -> "Still"
    CaptionAnimation.POP -> "Pop"
    CaptionAnimation.BOUNCE -> "Bounce"
    CaptionAnimation.SLIDE_UP -> "Slide up"
    CaptionAnimation.FADE -> "Fade"
    CaptionAnimation.TYPEWRITER -> "Typewriter"
    CaptionAnimation.KARAOKE -> "Karaoke"
}

@Composable
fun MotionPanel(fonts: FontRegistry, style: CaptionStyle, onStyle: StyleEdit) {
    PanelColumn {
        SectionLabel("Animation")
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 3,
        ) {
            CaptionAnimation.entries.forEach { animation ->
                ChoiceCard(
                    selected = animation == style.animation,
                    onClick = { onStyle { it.copy(animation = animation) } },
                    modifier = Modifier.weight(1f).height(96.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MiniCaptionPreview(fonts, style.copy(animation = animation), Modifier.fillMaxWidth().height(64.dp))
                        Text(animation.label(), style = MaterialTheme.typography.labelMedium, color = Verba.Soft)
                    }
                }
            }
        }
        Text(
            "Animations are timed to each spoken word and are burned into the exported video exactly as previewed.",
            style = MaterialTheme.typography.bodySmall,
            color = Verba.Muted,
        )
    }
}

// ---- Layout ---------------------------------------------------------------------------------

private enum class Placement(val label: String, val y: Float) { TOP("Top", 0.16f), MIDDLE("Middle", 0.5f), BOTTOM("Bottom", 0.78f) }

@Composable
fun LayoutPanel(style: CaptionStyle, onStyle: StyleEdit, onWordsPerCaption: (Int) -> Unit) {
    PanelColumn {
        SectionLabel("Position")
        val nearest = Placement.entries.minBy { kotlin.math.abs(it.y - style.positionY) }
        Segmented(
            options = Placement.entries,
            selected = nearest,
            label = { it.label },
            onSelect = { p -> onStyle { it.copy(positionY = p.y) } },
            modifier = Modifier.fillMaxWidth(),
        )
        VerbaSlider(style.positionY, 0.06f..0.94f) { v -> onStyle { it.copy(positionY = v) } }
        Text("Tip: drag the subtitles up or down directly on the video.", style = MaterialTheme.typography.bodySmall, color = Verba.Muted)

        Spacer(Modifier.height(4.dp))
        SectionLabel("Words per caption")
        Segmented(
            options = (CaptionSegmenter.MIN_WORDS..CaptionSegmenter.MAX_WORDS).toList(),
            selected = style.wordsPerCaption,
            label = { it.toString() },
            onSelect = onWordsPerCaption,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Verba.Coral))
            Spacer(Modifier.width(8.dp))
            Text(
                "Changing this re-groups all captions (your text edits are kept).",
                style = MaterialTheme.typography.bodySmall,
                color = Verba.Muted,
            )
        }
    }
}
