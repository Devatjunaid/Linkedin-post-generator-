package com.unpin.photoscrub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unpin.photoscrub.AppViewModel
import com.unpin.photoscrub.data.MetaCategory
import com.unpin.photoscrub.data.MetadataReader
import com.unpin.photoscrub.data.Permissions
import com.unpin.photoscrub.data.PhotoInfo
import com.unpin.photoscrub.data.formatBytes
import com.unpin.photoscrub.ui.UnpinActions
import com.unpin.photoscrub.ui.components.CardBox
import com.unpin.photoscrub.ui.components.EmptyState
import com.unpin.photoscrub.ui.components.MapPreview
import com.unpin.photoscrub.ui.components.PhotoThumb
import com.unpin.photoscrub.ui.components.PrimaryButton
import com.unpin.photoscrub.ui.components.RoundIconButton
import com.unpin.photoscrub.ui.components.SectionLabel
import com.unpin.photoscrub.ui.components.Tag
import com.unpin.photoscrub.ui.components.UnpinCheck
import com.unpin.photoscrub.ui.components.UnpinIcons
import com.unpin.photoscrub.ui.theme.Card
import com.unpin.photoscrub.ui.theme.Clean
import com.unpin.photoscrub.ui.theme.CleanDeep
import com.unpin.photoscrub.ui.theme.CleanTint
import com.unpin.photoscrub.ui.theme.Ink
import com.unpin.photoscrub.ui.theme.Line
import com.unpin.photoscrub.ui.theme.LineSoft
import com.unpin.photoscrub.ui.theme.Mono
import com.unpin.photoscrub.ui.theme.Muted
import com.unpin.photoscrub.ui.theme.Paper
import com.unpin.photoscrub.ui.theme.Signal
import com.unpin.photoscrub.ui.theme.SignalDeep
import com.unpin.photoscrub.ui.theme.SignalTint
import com.unpin.photoscrub.ui.theme.UnpinText

@Composable
fun InspectScreen(vm: AppViewModel, actions: UnpinActions) {
    val info = vm.inspected
    Column(Modifier.fillMaxSize().background(Paper)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(UnpinIcons.Back, "Back", { vm.back() }, bordered = false)
            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(
                    info?.displayName ?: "Inspector", style = UnpinText.mono.copy(fontSize = 14.sp), color = Ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (info != null) {
                    val dims = if (info.width > 0 && info.height > 0) " · ${info.width} × ${info.height}" else ""
                    Text(
                        "${formatBytes(info.sizeBytes)}$dims · ${info.totalFields} fields found",
                        style = UnpinText.small, color = Muted, maxLines = 1,
                    )
                }
            }
            RoundIconButton(UnpinIcons.Photos, "Pick another photo", actions.pickForInspect, bordered = false)
        }

        when {
            vm.inspectLoading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Signal)
            }
            info == null -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = if (vm.inspectError != null) "Couldn't read that one" else "Inspect a photo",
                    body = vm.inspectError ?: "See exactly what a photo gives away before you send it.",
                ) { PrimaryButton("Choose a photo", actions.pickForInspect, icon = UnpinIcons.Photos) }
            }
            else -> {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
                    InspectBody(vm, info, actions)
                }
                InspectBar(vm, info, actions)
            }
        }
    }
}

@Composable
private fun InspectBody(vm: AppViewModel, info: PhotoInfo, actions: UnpinActions) {
    val context = LocalContext.current
    Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(170.dp)) {
        PhotoThumb(info.uri, Modifier.fillMaxSize(), corner = 22.dp)
        if (info.hasLocation) {
            Tag("LOCATION EXPOSED", Signal, Ink, Modifier.align(Alignment.TopStart).padding(10.dp), icon = UnpinIcons.Pin)
        } else {
            Tag("NO GPS FOUND", Clean, Card, Modifier.align(Alignment.TopStart).padding(10.dp), icon = UnpinIcons.Shield)
        }
    }

    Spacer(Modifier.height(12.dp))
    val lat = info.latitude
    val lon = info.longitude
    if (lat != null && lon != null) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, Line, RoundedCornerShape(22.dp))
                .background(Card)
        ) {
            MapPreview(lat, lon, Modifier.fillMaxWidth().height(118.dp))
            Column(Modifier.padding(12.dp)) {
                Text(MetadataReader.formatCoord(lat, lon), style = UnpinText.mono.copy(fontSize = 12.5.sp), color = Ink)
                Spacer(Modifier.height(2.dp))
                val place = vm.placeName
                Text(
                    when (vm.placeLookupState) {
                        AppViewModel.LookupState.DONE -> "Near $place"
                        AppViewModel.LookupState.LOADING -> "Looking up the place…"
                        AppViewModel.LookupState.FAILED -> "No place name available (needs internet)"
                        AppViewModel.LookupState.IDLE -> "Anyone with this file can find this spot, often to within a few metres."
                    },
                    style = UnpinText.small, color = SignalDeep,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallAction("Open in Maps", UnpinIcons.Map) { actions.openMaps(lat, lon) }
                    if (vm.placeLookupState == AppViewModel.LookupState.IDLE || vm.placeLookupState == AppViewModel.LookupState.FAILED) {
                        SmallAction("Show place name", UnpinIcons.Search) { vm.lookUpPlace() }
                    }
                }
            }
        }
    } else if (Permissions.mediaLocation != null && !Permissions.hasMediaLocation(context)) {
        Row(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(SignalTint).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(UnpinIcons.Info, null, tint = SignalDeep, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Android is hiding this photo's GPS from Unpin. Allow \"media location\" to see it — it's removed either way.",
                    style = UnpinText.small, color = SignalDeep,
                )
                Text(
                    "Allow access", style = UnpinText.bodyStrong.copy(fontSize = 13.sp), color = Ink,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clickable(role = Role.Button) {
                            if (vm.askedMediaLocation) actions.openAppSettings() else actions.requestMediaLocation()
                        }
                        .padding(vertical = 6.dp),
                )
            }
        }
    }

    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("What a stranger learns", style = UnpinText.section, color = Ink, modifier = Modifier.weight(1f))
        SectionLabel("Remove")
    }

    if (info.categories.isEmpty()) {
        Row(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CleanTint).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(UnpinIcons.Shield, null, tint = Clean, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text("Nothing found. This photo is already clean.", style = UnpinText.body, color = CleanDeep)
        }
    } else {
        CardBox(Modifier.padding(horizontal = 16.dp), padding = 14.dp) {
            Column {
                info.categories.values.forEachIndexed { i, cat ->
                    val c = cat.category
                    val on = c in vm.inspectRemove
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(LineSoft))
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(if (c.sensitive) SignalTint else LineSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(c.code, style = UnpinText.label.copy(letterSpacing = 0.sp), color = if (c.sensitive) SignalDeep else Ink)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                c.title + if (cat.fieldCount > 1) "  ·  ${cat.fieldCount} tags" else "",
                                style = UnpinText.bodyStrong.copy(fontSize = 14.sp), color = Ink,
                            )
                            Text(
                                cat.summary,
                                style = UnpinText.mono.copy(fontSize = 11.5.sp, fontFamily = Mono),
                                // the redaction bar: values marked for removal are blacked out
                                color = if (on) Ink else Muted,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (on) Ink else Card)
                                    .padding(horizontal = 3.dp),
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (c.optional) {
                            UnpinCheck(on, { vm.toggleInspect(c) })
                        } else {
                            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                Icon(UnpinIcons.Lock, "Always removed", tint = Muted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
        Text(
            "Locked rows are always removed. Clean copies keep full image quality where the format allows.",
            style = UnpinText.small, color = Muted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun InspectBar(vm: AppViewModel, info: PhotoInfo, actions: UnpinActions) {
    val total = info.categories.size
    val count = info.categories.keys.count { it in vm.inspectRemove || !it.optional }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
    Row(Modifier.fillMaxWidth().background(Paper).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(if (total == 0) "Already clean" else "$count of $total groups", style = UnpinText.section.copy(fontSize = 17.sp), color = Ink)
            Text("Saved as a new clean copy", style = UnpinText.small, color = Muted)
        }
        PrimaryButton(
            "Scrub photo", actions.scrubInspected, container = Ink, content = Paper, iconTint = Signal, icon = UnpinIcons.Trash,
        )
    }
}

@Composable
private fun SmallAction(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Ink, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = UnpinText.bodyStrong.copy(fontSize = 13.sp), color = Ink)
    }
}
