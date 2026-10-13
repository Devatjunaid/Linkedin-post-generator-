package com.odomate.app.ui.services

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.odomate.app.AppContainer
import com.odomate.app.ads.NativeAdCard
import com.odomate.app.domain.Level
import com.odomate.app.domain.ServiceStatus
import com.odomate.app.ui.LocalSettings
import com.odomate.app.ui.appViewModel
import com.odomate.app.ui.components.AppCard
import com.odomate.app.ui.components.EmptyState
import com.odomate.app.ui.components.ScreenHeader
import com.odomate.app.ui.components.SectionLabel
import com.odomate.app.ui.components.ServiceListRow
import com.odomate.app.ui.theme.AppTheme
import com.odomate.app.util.Formats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class ServicesViewModel(container: AppContainer) : ViewModel() {
    val snapshot = container.repository.activeSnapshot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(onOpenService: (Long) -> Unit, onAddCustom: () -> Unit) {
    val vm = appViewModel { c, _ -> ServicesViewModel(c) }
    val snap by vm.snapshot.collectAsStateWithLifecycle()
    val settings = LocalSettings.current
    val c = AppTheme.colors
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val statuses = snap?.statuses ?: emptyList()
    val unit = settings.distanceUnit

    val attention = statuses.filter { it.level == Level.OVERDUE || it.level == Level.SOON }
    val onTrack = statuses.filter { it.level == Level.OK }
    val notSet = statuses.filter { it.level == Level.UNKNOWN }
    val off = statuses.filter { it.level == Level.DISABLED }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Maintenance",
            subtitle = snap?.let { s ->
                "${statuses.count { it.type.enabled }} services tracked" +
                    (s.currentOdometer?.let { " · ${Formats.distance(it, unit)}" } ?: "")
            },
        ) {
            IconButton(
                onClick = onAddCustom,
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(c.accent),
            ) { Icon(Icons.Outlined.Add, contentDescription = "Add custom service", tint = c.onAccent) }
        }

        val labels = listOf("All", "Due · ${attention.size}", "On track")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            labels.forEachIndexed { i, label ->
                SegmentedButton(
                    selected = filter == i,
                    onClick = { filter = i },
                    shape = SegmentedButtonDefaults.itemShape(i, labels.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = c.surface2, activeContentColor = c.text,
                        inactiveContainerColor = c.surface, inactiveContentColor = c.muted,
                        activeBorderColor = c.outline, inactiveBorderColor = c.outline,
                    ),
                    icon = {},
                ) { Text(label) }
            }
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            fun group(title: String, list: List<ServiceStatus>) {
                if (list.isEmpty()) return
                item(key = "h_$title") { SectionLabel(title) }
                item(key = "g_$title") {
                    AppCard(padding = PaddingValues(0.dp)) {
                        list.forEachIndexed { index, status ->
                            ServiceListRow(status, unit, { onOpenService(status.type.id) }, showDivider = index < list.lastIndex)
                        }
                    }
                }
            }

            when (filter) {
                1 -> {
                    if (attention.isEmpty()) {
                        item { EmptyState(Icons.Outlined.CheckCircle, "All caught up", "Nothing is due soon. We'll remind you before anything is.") }
                    }
                    group("Needs attention", attention)
                }
                2 -> group("On track", onTrack)
                else -> {
                    group("Needs attention", attention)
                    if (attention.isNotEmpty() || onTrack.isNotEmpty()) item(key = "ad") { Column(Modifier.padding(top = 8.dp)) { NativeAdCard() } }
                    group("On track", onTrack)
                    group("Not set up yet", notSet)
                    group("Turned off", off)
                }
            }
        }
    }
}
