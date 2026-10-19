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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unpin.photoscrub.AppViewModel
import com.unpin.photoscrub.BuildConfig
import com.unpin.photoscrub.R
import com.unpin.photoscrub.Screen
import com.unpin.photoscrub.UnpinApp
import com.unpin.photoscrub.ads.NativeAdCard
import com.unpin.photoscrub.data.GallerySaver
import com.unpin.photoscrub.data.Permissions
import com.unpin.photoscrub.data.ScrubSession
import com.unpin.photoscrub.data.formatBytes
import com.unpin.photoscrub.ui.UnpinActions
import com.unpin.photoscrub.ui.components.CardBox
import com.unpin.photoscrub.ui.components.EmptyState
import com.unpin.photoscrub.ui.components.OutlineButton
import com.unpin.photoscrub.ui.components.PrimaryButton
import com.unpin.photoscrub.ui.components.RoundIconButton
import com.unpin.photoscrub.ui.components.SectionLabel
import com.unpin.photoscrub.ui.components.ShredderHero
import com.unpin.photoscrub.ui.components.StatTile
import com.unpin.photoscrub.ui.components.UnpinIcons
import com.unpin.photoscrub.ui.theme.Card
import com.unpin.photoscrub.ui.theme.Clean
import com.unpin.photoscrub.ui.theme.CleanDeep
import com.unpin.photoscrub.ui.theme.CleanTint
import com.unpin.photoscrub.ui.theme.Ink
import com.unpin.photoscrub.ui.theme.Line
import com.unpin.photoscrub.ui.theme.LineSoft
import com.unpin.photoscrub.ui.theme.Muted
import com.unpin.photoscrub.ui.theme.Paper
import com.unpin.photoscrub.ui.theme.Signal
import com.unpin.photoscrub.ui.theme.SignalDeep
import com.unpin.photoscrub.ui.theme.SignalTint
import com.unpin.photoscrub.ui.theme.UnpinText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun TopBar(title: String, onBack: () -> Unit, close: Boolean = false, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(if (close) UnpinIcons.Close else UnpinIcons.Back, if (close) "Close" else "Back", onBack, bordered = false)
        Text(title, style = UnpinText.label, color = Muted, modifier = Modifier.weight(1f).padding(start = 4.dp))
        trailing()
    }
}

private fun dateLabel(ts: Long, pattern: String = "d MMM yyyy") =
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(ts)).uppercase()

// ------------------------------------------------------------------ Report

@Composable
fun ReportScreen(vm: AppViewModel, actions: UnpinActions) {
    val session = vm.report
    Column(Modifier.fillMaxSize().background(Paper)) {
        TopBar("SCRUB REPORT · " + (session?.let { dateLabel(it.timestamp) } ?: ""), { vm.open(Screen.HOME) }, close = true)
        if (session == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState("No report yet", "Clean a photo to see what was removed.")
            }
            return@Column
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            ShredderHero(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(220.dp).clip(RoundedCornerShape(28.dp))
            )
            val n = session.photoCount
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text(
                    when {
                        n == 0 -> "Nothing was cleaned."
                        n == 1 -> "1 photo, zero clues."
                        else -> "$n photos, zero clues."
                    },
                    style = UnpinText.hero, color = Ink,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (n > 0) "Clean copies saved to Pictures/${GallerySaver.FOLDER}. Your originals weren't touched — delete them in your gallery if you no longer need them."
                    else "None of the photos could be cleaned. Details below.",
                    style = UnpinText.body, color = Muted,
                )
            }
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("${session.fieldsRemoved}", "metadata fields removed", Modifier.weight(1f))
                    StatTile("${session.locationsErased}", "exact locations erased", Modifier.weight(1f), highlight = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val saved = session.bytesSaved
                    StatTile(
                        formatBytes(kotlin.math.abs(saved)),
                        if (saved >= 0) "lighter in total" else "larger (re-encoded HEIC)",
                        Modifier.weight(1f),
                    )
                    val pct = if (n == 0) 0 else session.losslessCount * 100 / n
                    StatTile("$pct%", "kept bit-for-bit image quality", Modifier.weight(1f))
                }
            }

            val allVerified = session.photoCount > 0 && session.verifiedCount == session.photoCount
            Row(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (allVerified) CleanTint else SignalTint)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(if (allVerified) Clean else Signal),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (allVerified) UnpinIcons.Check else UnpinIcons.Warning, null, tint = if (allVerified) Card else Ink, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    if (allVerified) "Verified. Unpin re-scanned every copy — none of the removed tags remain."
                    else "${session.verifiedCount} of ${session.photoCount} copies passed the re-scan.",
                    style = UnpinText.small.copy(fontSize = 13.sp), color = if (allVerified) CleanDeep else SignalDeep,
                )
            }

            if (session.failures.isNotEmpty()) {
                CardBox(Modifier.padding(horizontal = 16.dp), padding = 14.dp) {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        SectionLabel("Skipped (${session.failedCount})", color = SignalDeep)
                        session.failures.take(6).forEach {
                            Text(it, style = UnpinText.small, color = Muted, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            NativeAdCard(Modifier.padding(horizontal = 16.dp))
        }

        if (session.outputs.isNotEmpty()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
            Row(
                Modifier.fillMaxWidth().background(Paper).padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlineButton("Open", { actions.view(session.outputs.first()) }, Modifier.width(120.dp), icon = UnpinIcons.Folder)
                PrimaryButton(
                    "Share clean copies", { actions.share(session.outputs) }, Modifier.weight(1f),
                    icon = UnpinIcons.Share, container = Ink, content = Paper, iconTint = Signal,
                )
            }
        }
    }
}

// ------------------------------------------------------------------ History

@Composable
fun HistoryScreen(vm: AppViewModel, actions: UnpinActions) {
    var confirmClear by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Paper)) {
        TopBar("HISTORY", { vm.back() }) {
            if (vm.history.isNotEmpty()) {
                Text(
                    "Clear", style = UnpinText.bodyStrong.copy(fontSize = 13.sp), color = SignalDeep,
                    modifier = Modifier.clickable(role = Role.Button) { confirmClear = true }.padding(12.dp),
                )
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
            Text("Every clean-up", style = UnpinText.hero, color = Ink, modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp))
            Text(
                "${vm.totalCleaned} photos cleaned · ${vm.totalLocations} locations erased · stored only on this phone",
                style = UnpinText.small, color = Muted, modifier = Modifier.padding(start = 4.dp, bottom = 16.dp),
            )
            if (vm.history.isEmpty()) {
                EmptyState("Nothing here yet", "Your clean-ups will be listed here.") {
                    PrimaryButton("Pick photos to clean", actions.pickForBatch, icon = UnpinIcons.Photos)
                }
                Spacer(Modifier.height(12.dp))
                NativeAdCard()
            } else {
                vm.history.forEachIndexed { i, s ->
                    HistoryRow(s) { vm.openReport(s) }
                    Spacer(Modifier.height(8.dp))
                    if (i == 2 || (i == vm.history.lastIndex && vm.history.size < 3)) {
                        NativeAdCard()
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?", style = UnpinText.section) },
            text = { Text("This only clears Unpin's list. Your clean copies stay in Pictures/${GallerySaver.FOLDER}.", style = UnpinText.body) },
            confirmButton = { TextButton({ vm.clearHistory(); confirmClear = false }) { Text("Clear", color = SignalDeep) } },
            dismissButton = { TextButton({ confirmClear = false }) { Text("Cancel", color = Ink) } },
            containerColor = Card,
        )
    }
}

@Composable
private fun HistoryRow(s: ScrubSession, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Card)
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(if (s.locationsErased > 0) SignalTint else CleanTint),
            contentAlignment = Alignment.Center,
        ) {
            Text("${s.photoCount}", style = UnpinText.section, color = if (s.locationsErased > 0) SignalDeep else CleanDeep)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${s.photoCount} photo${if (s.photoCount == 1) "" else "s"} · ${s.fieldsRemoved} fields",
                style = UnpinText.bodyStrong, color = Ink,
            )
            Text(
                dateLabel(s.timestamp, "EEE d MMM · HH:mm").lowercase().replaceFirstChar { it.titlecase() } +
                    if (s.locationsErased > 0) " · ${s.locationsErased} locations erased" else "",
                style = UnpinText.small, color = Muted,
            )
        }
        Icon(UnpinIcons.Arrow, null, tint = Muted, modifier = Modifier.size(18.dp))
    }
}

// ------------------------------------------------------------------ Settings

@Composable
fun SettingsScreen(vm: AppViewModel, actions: UnpinActions) {
    val context = LocalContext.current
    val ads = (context.applicationContext as UnpinApp).ads
    val privacyRequired by ads.privacyOptionsRequired.collectAsStateWithLifecycle()
    val policyUrl = stringResource(R.string.privacy_policy_url)
    Column(Modifier.fillMaxSize().background(Paper)) {
        TopBar("SETTINGS", { vm.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text("Settings", style = UnpinText.hero, color = Ink, modifier = Modifier.padding(start = 4.dp, bottom = 16.dp))

            SectionLabel("How Unpin works", Modifier.padding(start = 4.dp, bottom = 8.dp))
            CardBox(padding = 14.dp) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    InfoLine(UnpinIcons.Lock, "Photos are processed on this phone and never uploaded.")
                    InfoLine(UnpinIcons.Photos, "JPEG, PNG and WebP are cleaned losslessly. HEIC and others are re-saved as high-quality JPEG.")
                    InfoLine(UnpinIcons.Shield, "Each clean copy is re-scanned to confirm the removed tags are gone.")
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel("Permissions & privacy", Modifier.padding(start = 4.dp, bottom = 8.dp))
            CardBox(padding = 14.dp) {
                Column {
                    if (Permissions.mediaLocation != null) {
                        val granted = Permissions.hasMediaLocation(context)
                        SettingsRow(
                            "Media location",
                            if (granted) "Allowed — Unpin can show where photos were taken" else "Off — GPS is hidden from Unpin but still removed",
                            if (granted) "Manage" else "Allow",
                        ) { if (granted || vm.askedMediaLocation) actions.openAppSettings() else actions.requestMediaLocation() }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(LineSoft))
                    }
                    if (privacyRequired) {
                        SettingsRow("Ad privacy choices", "Change how ads may use your data", "Open", actions.showPrivacyOptions)
                        Box(Modifier.fillMaxWidth().height(1.dp).background(LineSoft))
                    }
                    SettingsRow("Privacy policy", "What Unpin and its ad partner collect", "Read") { actions.openUrl(policyUrl) }
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel("Data", Modifier.padding(start = 4.dp, bottom = 8.dp))
            CardBox(padding = 14.dp) {
                SettingsRow("Clear history", "Removes Unpin's list of clean-ups (not your photos)", "Clear") { vm.clearHistory() }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "Unpin ${BuildConfig.VERSION_NAME} · Fonts: Bricolage Grotesque, Instrument Sans and JetBrains Mono, licensed under the SIL Open Font License 1.1.",
                style = UnpinText.small, color = Muted, modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun InfoLine(icon: ImageVector, text: String) {
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = Ink, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = UnpinText.small.copy(fontSize = 13.sp), color = Ink)
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, action: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = UnpinText.bodyStrong, color = Ink)
            Text(subtitle, style = UnpinText.small, color = Muted)
        }
        Spacer(Modifier.width(12.dp))
        Text(action, style = UnpinText.bodyStrong.copy(fontSize = 13.sp), color = SignalDeep)
    }
}
