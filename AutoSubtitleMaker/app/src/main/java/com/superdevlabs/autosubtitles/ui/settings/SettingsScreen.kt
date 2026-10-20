package com.superdevlabs.autosubtitles.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.superdevlabs.autosubtitles.AppContainer
import com.superdevlabs.autosubtitles.BuildConfig
import com.superdevlabs.autosubtitles.R
import com.superdevlabs.autosubtitles.core.captions.CaptionSegmenter
import com.superdevlabs.autosubtitles.core.captions.StylePresets
import com.superdevlabs.autosubtitles.data.AppSettings
import com.superdevlabs.autosubtitles.speech.ModelCatalog
import com.superdevlabs.autosubtitles.ui.components.Segmented
import com.superdevlabs.autosubtitles.ui.components.SectionLabel
import com.superdevlabs.autosubtitles.ui.components.pressable
import com.superdevlabs.autosubtitles.ui.theme.AppColors
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(container: AppContainer, contentPadding: PaddingValues, onOpenLanguages: () -> Unit) {
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    var showLicenses by remember { mutableStateOf(false) }
    var privacyRequired by remember { mutableStateOf(container.consent.isPrivacyOptionsRequired) }

    fun open(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (e: ActivityNotFoundException) {
            // No browser / Play Store installed: nothing to open.
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = contentPadding.calculateBottomPadding() + 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium) }

        item { SectionLabel("Defaults for new projects") }
        item {
            SettingRow(Icons.Rounded.Translate, "Spoken language", ModelCatalog.byId(settings.languageId).language, onOpenLanguages)
        }
        item {
            Column(Modifier.fillMaxWidth().tile().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Words per caption", style = MaterialTheme.typography.titleSmall)
                Segmented(
                    options = (CaptionSegmenter.MIN_WORDS..CaptionSegmenter.MAX_WORDS).toList(),
                    selected = settings.wordsPerCaption,
                    label = { it.toString() },
                    onSelect = { n -> scope.launch { container.settings.setWordsPerCaption(n) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            Column(Modifier.fillMaxWidth().tile().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Caption style", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StylePresets.all.forEach { preset ->
                        val selected = preset.id == settings.presetId
                        Text(
                            preset.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (selected) AppColors.Ink else AppColors.Text,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) AppColors.Lime else AppColors.SurfaceHigh)
                                .pressable(onClick = { scope.launch { container.settings.setPreset(preset.id) } })
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }

        item { SectionLabel("Privacy") }
        if (privacyRequired) {
            item {
                SettingRow(Icons.Rounded.PrivacyTip, "Ad privacy choices", "Change your consent") {
                    if (activity != null) {
                        container.consent.showPrivacyOptions(activity) {
                            privacyRequired = container.consent.isPrivacyOptionsRequired
                        }
                    }
                }
            }
        }
        item { SettingRow(Icons.Rounded.Description, "Privacy policy", "How the app handles data") { open(privacyUrl) } }
        item {
            InfoTile(
                "Your videos never leave your phone. Speech recognition, styling and export all run on-device. " +
                    "The internet is only used to download language models and to show ads.",
            )
        }

        item { SectionLabel("About") }
        item {
            SettingRow(Icons.Rounded.Star, "Rate the app", "Takes 5 seconds, helps a lot") {
                open("https://play.google.com/store/apps/details?id=${context.packageName}")
            }
        }
        item { SettingRow(Icons.Rounded.Info, "Open-source licenses", "Fonts, speech engine, libraries") { showLicenses = true } }
        item { SectionLabel("Version ${BuildConfig.VERSION_NAME}") }
    }

    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            confirmButton = { TextButton(onClick = { showLicenses = false }) { Text("Close") } },
            title = { Text("Open-source licenses") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    Text(LICENSES, style = MaterialTheme.typography.bodySmall, color = AppColors.Soft)
                }
            },
            containerColor = AppColors.Surface,
        )
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .tile()
            .pressable(onClick = onClick, pressedScale = 0.97f)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = AppColors.Lime)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = AppColors.Muted)
    }
}

@Composable
private fun InfoTile(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.Soft,
        modifier = Modifier.fillMaxWidth().tile().padding(16.dp),
    )
}

private fun Modifier.tile(): Modifier = this
    .clip(RoundedCornerShape(20.dp))
    .background(AppColors.Surface)
    .border(1.dp, AppColors.Line, RoundedCornerShape(20.dp))

private const val LICENSES = """
Speech recognition — Vosk (alphacephei.com/vosk), Apache License 2.0. Language models: Apache License 2.0.
JNA — Apache License 2.0 / LGPL 2.1.
AndroidX, Jetpack Compose, Media3 — Apache License 2.0.
Kotlin, kotlinx.coroutines, kotlinx.serialization — Apache License 2.0.
Google Mobile Ads SDK & User Messaging Platform — Google terms of service.

Caption fonts (all free for commercial use, embedded in exported videos):
Anton, Bebas Neue, Poppins, Archivo Black, Bangers, DM Serif Display, Pacifico, Caveat, Space Mono, Bricolage Grotesque, JetBrains Mono — SIL Open Font License 1.1.
Permanent Marker — Apache License 2.0.
Full texts: assets/licenses in the app package.
"""
