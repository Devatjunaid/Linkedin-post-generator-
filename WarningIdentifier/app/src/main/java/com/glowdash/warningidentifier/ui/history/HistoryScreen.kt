package com.glowdash.warningidentifier.ui.history

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glowdash.warningidentifier.ads.NativeAdCard
import com.glowdash.warningidentifier.ads.NativeStyle
import com.glowdash.warningidentifier.data.HistoryStore
import com.glowdash.warningidentifier.ui.components.FloatingNavBar
import com.glowdash.warningidentifier.ui.components.NavTab
import com.glowdash.warningidentifier.ui.components.PathIcon
import com.glowdash.warningidentifier.ui.components.RoundIconButton
import com.glowdash.warningidentifier.ui.components.ScreenBackground
import com.glowdash.warningidentifier.ui.components.ShineButton
import com.glowdash.warningidentifier.ui.components.UiIcons
import com.glowdash.warningidentifier.ui.home.RecentRow
import com.glowdash.warningidentifier.ui.theme.WiColors
import com.glowdash.warningidentifier.ui.theme.WiType

@Composable
fun HistoryScreen(
    onOpenLight: (String) -> Unit,
    onScan: () -> Unit,
    onTab: (NavTab) -> Unit,
) {
    val items by HistoryStore.items.collectAsState()

    ScreenBackground(topGlow = WiColors.Blue, bottomGlow = WiColors.Pink) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("SAVED ON THIS PHONE", style = WiType.Label.copy(color = WiColors.TextSecondary))
                        Text("Scan History", style = WiType.Display)
                    }
                    if (items.isNotEmpty()) {
                        RoundIconButton(UiIcons.Trash, "Clear history", { HistoryStore.clear() }, tint = WiColors.Red)
                    }
                }
            }
            if (items.isEmpty()) {
                item(key = "empty") {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PathIcon(UiIcons.History, WiColors.TextMuted, size = 56.dp, strokeWidth = 1.6f)
                        Spacer(Modifier.height(14.dp))
                        Text("No scans yet", style = WiType.Title)
                        Text(
                            "Scan a dashboard light and it will be saved here.",
                            style = WiType.BodySmall.copy(textAlign = TextAlign.Center),
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
                        )
                        ShineButton("Scan Dashboard", UiIcons.Scan, onScan, Modifier.fillMaxWidth())
                    }
                }
            }
            itemsIndexed(items, key = { _, e -> "${e.light.id}_${e.timestamp}" }) { index, entry ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RecentRow(entry) { onOpenLight(entry.light.id) }
                    if (index == 2) NativeAdCard("history", style = NativeStyle.SMALL)
                }
            }
        }

        Box(Modifier.align(Alignment.BottomCenter)) {
            FloatingNavBar(current = NavTab.HISTORY, onTab = onTab, onScan = onScan)
        }
    }
}
