package com.inkformlabs.pdfsigner.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inkformlabs.pdfsigner.AppConfig
import com.inkformlabs.pdfsigner.BuildConfig
import com.inkformlabs.pdfsigner.ads.NativeAdCard
import com.inkformlabs.pdfsigner.data.ThemeChoice
import com.inkformlabs.pdfsigner.ui.LocalAppContainer
import com.inkformlabs.pdfsigner.ui.components.InkCard
import com.inkformlabs.pdfsigner.ui.components.SectionLabel
import com.inkformlabs.pdfsigner.ui.theme.InkIcons
import com.inkformlabs.pdfsigner.ui.theme.LocalInk
import com.inkformlabs.pdfsigner.ui.theme.paletteFor
import com.inkformlabs.pdfsigner.util.Intents
import kotlinx.coroutines.launch

@Composable
fun SettingsTab(onMessage: (String) -> Unit) {
    val p = LocalInk.current
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val current by container.settings.theme.collectAsStateWithLifecycle(initialValue = ThemeChoice.CITRUS)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, color = p.ink)

        SectionLabel("Theme")
        ThemeChoice.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { choice ->
                    ThemeCard(
                        choice = choice,
                        selected = choice == current,
                        onClick = { scope.launch { container.settings.setTheme(choice) } },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        SectionLabel("Privacy", Modifier.padding(top = 6.dp))
        InkCard(padding = 4.dp) {
            Column {
                SettingsRow(InkIcons.Shield, "Your documents", "Stored only on this phone. Inkform has no account and no cloud.") {}
                if (container.consent.isPrivacyOptionsRequired) {
                    HorizontalDivider(color = p.line)
                    SettingsRow(InkIcons.Settings, "Ad privacy choices", "Review or change your consent for ads") {
                        activity?.let { act ->
                            container.consent.showPrivacyOptions(act) { error ->
                                if (error != null) onMessage("Couldn't open privacy options: ${error.message}")
                            }
                        }
                    }
                }
                HorizontalDivider(color = p.line)
                SettingsRow(InkIcons.Info, "Privacy policy", "How Inkform handles data") {
                    Intents.openUrl(context, AppConfig.PRIVACY_POLICY_URL)
                }
            }
        }

        SectionLabel("Support", Modifier.padding(top = 6.dp))
        InkCard(padding = 4.dp) {
            Column {
                SettingsRow(InkIcons.Star, "Rate Inkform", "Tell others what you think on Google Play") { Intents.rateApp(context) }
                HorizontalDivider(color = p.line)
                SettingsRow(InkIcons.Share, "Share the app", "Send Inkform to a friend") { Intents.shareApp(context) }
                HorizontalDivider(color = p.line)
                SettingsRow(InkIcons.Info, "Version", BuildConfig.VERSION_NAME) {}
            }
        }

        NativeAdCard()
        Spacer(Modifier.width(1.dp))
    }
}

@Composable
private fun ThemeCard(choice: ThemeChoice, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val p = LocalInk.current
    val preview = paletteFor(choice)
    val border by animateDpAsState(if (selected) 2.5.dp else 1.dp, label = "themeBorder")
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(preview.bg)
            .border(border, if (selected) p.hi else p.line, RoundedCornerShape(20.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(preview.hero),
        ) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(preview.accent2),
            )
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .size(width = 44.dp, height = 12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(preview.accent),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(choice.title, style = MaterialTheme.typography.labelLarge, color = preview.ink, modifier = Modifier.weight(1f))
            if (selected) Icon(InkIcons.Check, contentDescription = "Selected", tint = preview.hi, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val p = LocalInk.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(p.surface2),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = p.ink, modifier = Modifier.size(19.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = p.ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = p.muted)
        }
    }
}
