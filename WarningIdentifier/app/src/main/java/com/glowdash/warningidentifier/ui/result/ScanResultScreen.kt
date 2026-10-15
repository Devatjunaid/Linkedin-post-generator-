package com.glowdash.warningidentifier.ui.result

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.glowdash.warningidentifier.ads.NativeAdCard
import com.glowdash.warningidentifier.ads.NativeStyle
import com.glowdash.warningidentifier.ai.LightClassifier
import com.glowdash.warningidentifier.ai.ScanSession
import com.glowdash.warningidentifier.data.LightsCatalog
import com.glowdash.warningidentifier.ui.components.RoundIconButton
import com.glowdash.warningidentifier.ui.components.ScreenBackground
import com.glowdash.warningidentifier.ui.components.ShineButton
import com.glowdash.warningidentifier.ui.components.UiIcons
import com.glowdash.warningidentifier.ui.library.LightCard
import com.glowdash.warningidentifier.ui.theme.WiColors
import com.glowdash.warningidentifier.ui.theme.WiType

/**
 * Shows the scan outcome: the identified light when the model is confident,
 * otherwise the symbols matching the detected colours so the user can pick.
 */
@Composable
fun ScanResultScreen(
    onBack: () -> Unit,
    onPick: (String) -> Unit,
    onScanAgain: () -> Unit,
) {
    val analysis = remember { ScanSession.latest }
    if (analysis == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val best = analysis.best
    if (analysis.isConfident && best != null) {
        LightDetailScreen(
            lightId = best.light.id,
            confidence = (best.confidence * 100).toInt(),
            fromScan = true,
            alternatives = analysis.predictions.drop(1).filter { it.confidence >= 0.08f }.map { it.light },
            onBack = onBack,
            onOpenLight = onPick,
        )
    } else {
        CandidatePicker(analysis, onBack, onPick, onScanAgain)
    }
}

@Composable
private fun CandidatePicker(
    analysis: LightClassifier.Analysis,
    onBack: () -> Unit,
    onPick: (String) -> Unit,
    onScanAgain: () -> Unit,
) {
    val candidates = remember(analysis) {
        val fromModel = analysis.predictions.map { it.light }
        val fromColor = LightsCatalog.byColors(analysis.colors)
        (fromModel + fromColor).distinct().ifEmpty { LightsCatalog.all }
    }
    val title = if (analysis.colors.isEmpty()) "No lit symbol spotted" else "Which one do you see?"
    val subtitle = if (analysis.colors.isEmpty()) {
        "Try again with the ignition on and less glare — or pick the symbol below."
    } else {
        "We spotted ${analysis.colors.joinToString(" & ") { it.label.lowercase() }} lights. Tap the symbol that matches your dashboard."
    }

    ScreenBackground {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RoundIconButton(UiIcons.Back, "Back", onBack)
                        Spacer(Modifier.width(12.dp))
                        Text("SCAN RESULT", style = WiType.Label.copy(color = WiColors.TextSecondary))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(title, style = WiType.Display)
                    Spacer(Modifier.height(6.dp))
                    Text(subtitle, style = WiType.Body)
                    if (analysis.colors.isNotEmpty()) {
                        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            analysis.colors.forEach { c ->
                                Row(
                                    Modifier
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(c.color.copy(alpha = 0.14f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(c.color))
                                    Spacer(Modifier.width(6.dp))
                                    Text(c.label, style = WiType.Caption.copy(color = WiColors.TextPrimary))
                                }
                            }
                        }
                    }
                    NativeAdCard("candidates", Modifier.padding(top = 16.dp), style = NativeStyle.SMALL)
                }
            }
            items(candidates, key = { it.id }) { light ->
                LightCard(light) { onPick(light.id) }
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(WiColors.Bg.copy(alpha = 0.92f))
                .padding(horizontal = 22.dp, vertical = 14.dp),
        ) {
            ShineButton("Scan again", UiIcons.Scan, onScanAgain, Modifier.fillMaxWidth())
        }
    }
}
