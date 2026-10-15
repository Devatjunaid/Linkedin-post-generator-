package com.glowdash.warningidentifier.ui.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.glowdash.warningidentifier.ads.AdsManager
import com.glowdash.warningidentifier.ads.NativeAdCard
import com.glowdash.warningidentifier.ads.NativeStyle
import com.glowdash.warningidentifier.data.LightsCatalog
import com.glowdash.warningidentifier.data.Severity
import com.glowdash.warningidentifier.data.WarningLight
import com.glowdash.warningidentifier.ui.components.FloatingNavBar
import com.glowdash.warningidentifier.ui.components.LightIcon
import com.glowdash.warningidentifier.ui.components.NavTab
import com.glowdash.warningidentifier.ui.components.PathIcon
import com.glowdash.warningidentifier.ui.components.Pressable
import com.glowdash.warningidentifier.ui.components.ScreenBackground
import com.glowdash.warningidentifier.ui.components.UiIcons
import com.glowdash.warningidentifier.ui.components.findActivity
import com.glowdash.warningidentifier.ui.theme.WiColors
import com.glowdash.warningidentifier.ui.theme.WiType
import kotlinx.coroutines.delay

private sealed interface GridEntry {
    val key: String
    data class Light(val light: WarningLight, val index: Int) : GridEntry { override val key = light.id }
    data class Ad(val placement: String) : GridEntry { override val key = "ad_$placement" }
}

@Composable
fun LibraryScreen(
    onOpenLight: (String) -> Unit,
    onScan: () -> Unit,
    onTab: (NavTab) -> Unit,
) {
    val activity = LocalContext.current.findActivity()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val severity = filter?.let { Severity.valueOf(it) }

    val entries = remember(query, severity) {
        val lights = LightsCatalog.search(query, severity)
        buildList<GridEntry> {
            lights.forEachIndexed { i, light ->
                add(GridEntry.Light(light, i))
                // Native ads sit in the free full-width rows between cards.
                if (i == 3) add(GridEntry.Ad("library_1"))
                if (i == 15) add(GridEntry.Ad("library_2"))
            }
        }
    }

    ScreenBackground(topGlow = WiColors.Pink, bottomGlow = WiColors.Cyan) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
                Column {
                    Text("${LightsCatalog.all.size} SYMBOLS · WORKS OFFLINE", style = WiType.Label.copy(color = WiColors.TextSecondary))
                    Text("Light Library", style = WiType.Display)
                    Spacer(Modifier.height(14.dp))
                    SearchField(query) { query = it }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip("All", WiColors.TextPrimary, filter == null) { filter = null }
                        Severity.entries.forEach { s ->
                            FilterChip(s.label.lowercase().replaceFirstChar { it.uppercase() }, s.color, filter == s.name) {
                                filter = s.name
                            }
                        }
                    }
                }
            }
            if (entries.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                    Text("No symbols match \"$query\".", style = WiType.Body, modifier = Modifier.padding(top = 24.dp))
                }
            }
            items(
                entries,
                key = { it.key },
                span = { if (it is GridEntry.Ad) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
            ) { entry ->
                when (entry) {
                    is GridEntry.Light -> LightCard(
                        entry.light,
                        appearDelay = (entry.index * 45L).coerceAtMost(450L),
                        modifier = Modifier.animateItem(),
                    ) {
                        AdsManager.showInterstitialThen(activity) { onOpenLight(entry.light.id) }
                    }
                    is GridEntry.Ad -> NativeAdCard(entry.placement, style = NativeStyle.SMALL)
                }
            }
        }

        FloatingNavBar(
            current = NavTab.LIBRARY,
            onTab = onTab,
            onScan = onScan,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(WiColors.Surface)
            .border(1.dp, WiColors.Stroke, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PathIcon(UiIcons.Search, WiColors.TextMuted, size = 18.dp, strokeWidth = 2.2f)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text("Search: battery, oil, tire…", style = WiType.Body.copy(color = WiColors.TextMuted))
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = WiType.Body.copy(color = WiColors.TextPrimary),
                cursorBrush = SolidColor(WiColors.Cyan),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FilterChip(label: String, dot: androidx.compose.ui.graphics.Color, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) WiColors.TextPrimary else WiColors.Surface, label = "chipBg")
    val fg by animateColorAsState(if (selected) WiColors.Bg else WiColors.TextSecondary, label = "chipFg")
    Pressable(
        onClick = onClick,
        modifier = Modifier.height(40.dp),
        shape = RoundedCornerShape(99.dp),
        background = bg,
        border = if (selected) null else WiColors.Stroke,
    ) {
        Row(
            Modifier.align(Alignment.Center).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (selected && dot == WiColors.TextPrimary) WiColors.Bg else dot))
            Spacer(Modifier.width(6.dp))
            Text(label, style = WiType.Caption.copy(color = fg))
        }
    }
}

/** Glowing symbol card used by the library and the scan candidate picker. */
@Composable
fun LightCard(
    light: WarningLight,
    modifier: Modifier = Modifier,
    appearDelay: Long = 0L,
    onClick: () -> Unit,
) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(appearDelay)
        appear.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    val t = rememberInfiniteTransition(label = "cardGlow")
    val glow by t.animateFloat(0.10f, 0.22f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "g")
    val c = light.color.color

    Pressable(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 18.dp.toPx()
            },
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.copy(alpha = glow)),
                    contentAlignment = Alignment.Center,
                ) { LightIcon(light, size = 30.dp) }
                Spacer(Modifier.weight(1f))
                Text(
                    light.severity.label,
                    style = WiType.Label.copy(color = light.severity.onColor, fontSize = WiType.Label.fontSize * 0.9f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(light.severity.color)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(light.name, style = WiType.CardTitle.copy(fontSize = WiType.Caption.fontSize * 1.17f), maxLines = 1)
                Text(light.summary, style = WiType.Caption, maxLines = 2)
            }
        }
    }
}
