package com.unpin.photoscrub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unpin.photoscrub.AppViewModel
import com.unpin.photoscrub.BatchItem
import com.unpin.photoscrub.ads.NativeAdCard
import com.unpin.photoscrub.data.MetaCategory
import com.unpin.photoscrub.data.Preset
import com.unpin.photoscrub.data.formatBytes
import com.unpin.photoscrub.ui.UnpinActions
import com.unpin.photoscrub.ui.components.CardBox
import com.unpin.photoscrub.ui.components.EmptyState
import com.unpin.photoscrub.ui.components.PhotoThumb
import com.unpin.photoscrub.ui.components.PrimaryButton
import com.unpin.photoscrub.ui.components.RoundIconButton
import com.unpin.photoscrub.ui.components.SectionLabel
import com.unpin.photoscrub.ui.components.ToggleRow
import com.unpin.photoscrub.ui.components.UnpinIcons
import com.unpin.photoscrub.ui.theme.Card
import com.unpin.photoscrub.ui.theme.Ink
import com.unpin.photoscrub.ui.theme.Line
import com.unpin.photoscrub.ui.theme.Muted
import com.unpin.photoscrub.ui.theme.MutedOnInk
import com.unpin.photoscrub.ui.theme.Paper
import com.unpin.photoscrub.ui.theme.Signal
import com.unpin.photoscrub.ui.theme.SignalDeep
import com.unpin.photoscrub.ui.theme.UnpinText

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BatchScreen(vm: AppViewModel, actions: UnpinActions) {
    val items = vm.batch
    val selected = vm.selectedItems
    Column(Modifier.fillMaxSize().background(Paper)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(UnpinIcons.Close, "Close", { vm.back() }, bordered = false)
            Text(
                if (items.isEmpty()) "Batch clean" else "${selected.size} selected",
                style = UnpinText.title, color = Ink, modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            if (items.isNotEmpty()) {
                Text(
                    if (items.filter { it.error == null }.all { it.selected }) "Select none" else "Select all",
                    style = UnpinText.bodyStrong.copy(fontSize = 13.sp), color = Ink,
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Card)
                        .border(1.dp, Line, RoundedCornerShape(22.dp))
                        .clickable(role = Role.Button, onClick = vm::selectAll)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
        }

        if (items.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(
                    "Clean many at once",
                    "Pick up to 50 photos. Unpin saves clean copies to Pictures/Unpin and never touches the originals.",
                ) { PrimaryButton("Pick photos", actions.pickForBatch, icon = UnpinIcons.Photos) }
            }
            return@Column
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            val withGps = selected.count { it.info?.hasLocation == true }
            val bytes = selected.sumOf { it.info?.sizeBytes ?: 0L }
            val formats = selected.mapNotNull { it.info?.mime?.substringAfter('/')?.uppercase() }.distinct().joinToString(", ")
            Row(
                Modifier.padding(horizontal = 20.dp).padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Signal))
                Text(
                    buildString {
                        append("$withGps with GPS · ${formatBytes(bytes)}")
                        if (formats.isNotEmpty()) append(" · $formats")
                        if (vm.batchLoading) append(" · reading…")
                    },
                    style = UnpinText.mono.copy(fontSize = 11.sp), color = Muted,
                )
            }

            // photo grid, 4 per row
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (items.toList() + listOf<BatchItem?>(null)).chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { item ->
                            Box(Modifier.weight(1f).aspectRatio(1f)) {
                                if (item == null) AddTile(actions.pickForBatch) else BatchTile(item) { vm.toggleItem(item.uri) }
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            val failed = items.filter { it.error != null }
            if (failed.isNotEmpty()) {
                Text(
                    "${failed.size} photo${if (failed.size == 1) "" else "s"} couldn't be read and will be skipped.",
                    style = UnpinText.small, color = SignalDeep, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
            NativeAdCard(Modifier.padding(horizontal = 16.dp))

            SectionLabel("Scrub preset", Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp))
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Preset.entries.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { p ->
                            PresetCard(p, vm.preset == p, Modifier.weight(1f)) { vm.preset = p }
                        }
                    }
                }
            }
            if (vm.preset == Preset.CUSTOM) {
                Text("Remove these:", style = UnpinText.small, color = Muted, modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 6.dp))
                FlowRow(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MetaCategory.optionalOnes.forEach { c ->
                        val on = c in vm.customRemove
                        Text(
                            c.title,
                            style = UnpinText.bodyStrong.copy(fontSize = 13.sp),
                            color = if (on) Paper else Ink,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (on) Ink else Card)
                                .border(1.dp, if (on) Ink else Line, RoundedCornerShape(14.dp))
                                .toggleable(on, role = Role.Checkbox) { vm.toggleCustom(c) }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            CardBox(Modifier.padding(horizontal = 16.dp), padding = 14.dp) {
                Column {
                    ToggleRow("Anonymise file names", "IMG_4021.jpg → unpin_3f9a1c2e.jpg", vm.anonymize, { vm.anonymize = it }, showDivider = false)
                    ToggleRow("Keep rotation", "Photos stay upright after the wipe", vm.keepOrientation, { vm.keepOrientation = it }, showDivider = true)
                    ToggleRow("Keep colour profile", "Colours look the same; it reveals nothing about you", vm.keepColorProfile, { vm.keepColorProfile = it }, showDivider = true)
                }
            }
            Text(
                "Clean copies go to Pictures/${com.unpin.photoscrub.data.GallerySaver.FOLDER}. Originals stay untouched.",
                style = UnpinText.small, color = Muted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        Box(Modifier.fillMaxWidth().background(Paper).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected.isEmpty() || vm.batchLoading) Signal.copy(alpha = 0.4f) else Signal)
                    .clickable(enabled = selected.isNotEmpty() && !vm.batchLoading, role = Role.Button, onClick = actions.runBatch)
                    .padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (vm.batchLoading) "Reading photos…" else "Scrub ${selected.size} photo${if (selected.size == 1) "" else "s"}",
                    style = UnpinText.button, color = Ink, modifier = Modifier.weight(1f),
                )
                Row(
                    Modifier.height(42.dp).clip(RoundedCornerShape(13.dp)).background(Ink).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(vm.preset.title, style = UnpinText.mono, color = Paper)
                    Spacer(Modifier.width(8.dp))
                    Icon(UnpinIcons.Arrow, null, tint = Signal, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun BatchTile(item: BatchItem, onToggle: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .toggleable(item.selected, enabled = item.error == null, role = Role.Checkbox) { onToggle() }
            .alpha(if (item.selected) 1f else 0.5f)
    ) {
        PhotoThumb(item.uri, Modifier.fillMaxSize(), corner = 14.dp)
        Box(
            Modifier
                .padding(6.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (item.selected) Ink else Ink.copy(alpha = 0.25f))
                .border(2.dp, Card, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (item.selected) Icon(UnpinIcons.Check, null, tint = Card, modifier = Modifier.size(12.dp))
        }
        if (item.info?.hasLocation == true) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(6.dp).size(20.dp).clip(RoundedCornerShape(6.dp)).background(Signal),
                contentAlignment = Alignment.Center,
            ) {
                Icon(UnpinIcons.Pin, "Has GPS", tint = Ink, modifier = Modifier.size(12.dp))
            }
        }
        if (item.error != null) {
            Box(Modifier.fillMaxSize().background(Ink.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                Icon(UnpinIcons.Warning, item.error, tint = Paper, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun AddTile(onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, Line, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClickLabel = "Add photos", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(UnpinIcons.Plus, "Add photos", tint = Muted, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun PresetCard(preset: Preset, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Ink else Card)
            .border(1.dp, if (selected) Ink else Line, RoundedCornerShape(16.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(preset.title, style = UnpinText.bodyStrong.copy(fontSize = 14.5.sp), color = if (selected) Paper else Ink)
        Text(preset.description, style = UnpinText.small.copy(fontSize = 12.sp), color = if (selected) MutedOnInk else Muted)
    }
}
