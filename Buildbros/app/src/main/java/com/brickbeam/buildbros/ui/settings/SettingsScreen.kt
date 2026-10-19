package com.brickbeam.buildbros.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brickbeam.buildbros.BuildConfig
import com.brickbeam.buildbros.R
import com.brickbeam.buildbros.data.AppSettings
import com.brickbeam.buildbros.data.UnitPreference
import com.brickbeam.buildbros.ui.LocalAppEnv
import com.brickbeam.buildbros.ui.Routes
import com.brickbeam.buildbros.ui.components.BmCard
import com.brickbeam.buildbros.ui.components.ScreenHeader
import com.brickbeam.buildbros.ui.components.SectionTitle
import com.brickbeam.buildbros.ui.components.Segmented
import com.brickbeam.buildbros.ui.components.ToggleRow
import com.brickbeam.buildbros.ui.graphics.AnimatedLogo
import com.brickbeam.buildbros.ui.theme.BmIcons
import com.brickbeam.buildbros.ui.theme.BmType
import com.brickbeam.buildbros.ui.theme.LocalPalette
import com.brickbeam.buildbros.ui.theme.Palette
import com.brickbeam.buildbros.ui.theme.Palettes

@Composable
fun SettingsScreen() {
    val env = LocalAppEnv.current
    val p = LocalPalette.current
    val context = LocalContext.current
    val settings by env.vm.settings.collectAsStateWithLifecycle()
    val privacyRequired by env.ads.privacyOptionsRequired.collectAsStateWithLifecycle()
    val s = settings ?: AppSettings()
    val policyUrl = stringResource(R.string.privacy_policy_url)

    fun open(url: String) = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader("Settings", "Look, units and privacy")

        SectionTitle("Theme")
        Palettes.ALL.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { pal -> ThemeCard(pal, pal.id == s.theme, Modifier.weight(1f)) { env.vm.setTheme(pal.id) } }
            }
        }
        BmCard(Modifier.fillMaxWidth()) {
            ToggleRow("Animations", s.motion, { env.vm.setMotion(it) }, sub = "Material animations, transitions and the animated splash")
        }

        SectionTitle("Units & region")
        Segmented(UnitPreference.entries, s.unitPreference, { it.label }, { env.vm.setUnits(it) }, Modifier.fillMaxWidth())
        BmCard(Modifier.fillMaxWidth()) {
            NavRow(BmIcons.Place, "Region", "${s.region.name} · ${s.region.currency}") { env.go(Routes.REGION) }
            NavRow(BmIcons.Tag, "My prices", "Edit the prices used for costs") { env.go(Routes.PRICES) }
            NavRow(BmIcons.Ruler, "Unit converter", "Length, area, volume, weight, strength") { env.go(Routes.CONVERTER) }
        }

        SectionTitle("Privacy & support")
        BmCard(Modifier.fillMaxWidth()) {
            if (privacyRequired) {
                NavRow(BmIcons.Shield, "Privacy & ad choices", "Review or change your consent") {
                    env.activity()?.let { env.ads.showPrivacyOptions(it) }
                }
            }
            NavRow(BmIcons.Document, "Privacy policy", "How data and ads are handled") { open(policyUrl) }
            NavRow(BmIcons.Star, "Rate Buildbros", "Tell us what to build next") {
                open("market://details?id=${context.packageName}").onFailure {
                    open("https://play.google.com/store/apps/details?id=${context.packageName}")
                }
            }
            NavRow(BmIcons.Share, "Share the app", null) {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Buildbros: material calculator for bricks, concrete, steel, tiles and paint. https://play.google.com/store/apps/details?id=${context.packageName}")
                }
                runCatching { context.startActivity(Intent.createChooser(send, "Share Buildbros")) }
            }
        }

        BmCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnimatedLogo(Modifier.size(52.dp), badge = p.surface2, roof = p.accent, loop = s.motion, animate = s.motion)
                Column(Modifier.weight(1f)) {
                    Text("Buildbros ${BuildConfig.VERSION_NAME}", style = BmType.section, color = p.ink)
                    Text(
                        "Estimates use standard trade formulas and the prices you enter. They are not professional engineering advice. " +
                            "Check quantities with your supplier and follow local building codes.",
                        style = BmType.tiny, color = p.mute,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(pal: Palette, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(pal.bg)
            .border(if (selected) 3.dp else 1.dp, if (selected) p.accentText else pal.line, RoundedCornerShape(20.dp))
            .clickable(role = Role.RadioButton, onClickLabel = "Use ${pal.name} theme", onClick = onClick)
            .semantics { this.selected = selected }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(pal.name, style = BmType.title.copy(fontSize = BmType.section.fontSize * 1.2f), color = pal.ink)
        Text(pal.tagline, style = BmType.tiny, color = pal.mute)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(pal.surface, pal.surface2, pal.line).forEach {
                Box(Modifier.weight(1f).height(22.dp).clip(RoundedCornerShape(6.dp)).background(it))
            }
            Box(Modifier.weight(2f).height(22.dp).clip(RoundedCornerShape(6.dp)).background(pal.accent))
        }
        Box(
            Modifier.fillMaxWidth().heightIn(min = 30.dp).clip(RoundedCornerShape(10.dp)).background(pal.accent),
            contentAlignment = Alignment.Center,
        ) { Text(if (selected) "In use" else "Use theme", style = BmType.tiny.copy(fontWeight = FontWeight.Bold), color = pal.onAccent) }
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, sub: String?, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(p.surface2), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = p.ink, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = BmType.body.copy(fontWeight = FontWeight.Bold), color = p.ink)
            if (sub != null) Text(sub, style = BmType.tiny, color = p.mute)
        }
        Icon(BmIcons.Chevron, null, tint = p.mute)
    }
}
