package com.aquadoselab.poolchemistry.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.aquadoselab.poolchemistry.BuildConfig
import com.aquadoselab.poolchemistry.ads.AdsManager
import com.aquadoselab.poolchemistry.chem.Units
import com.aquadoselab.poolchemistry.chem.Volume
import com.aquadoselab.poolchemistry.data.Pool
import com.aquadoselab.poolchemistry.data.TempUnit
import com.aquadoselab.poolchemistry.data.UnitSystem
import com.aquadoselab.poolchemistry.ui.AppViewModel
import com.aquadoselab.poolchemistry.ui.Routes
import com.aquadoselab.poolchemistry.ui.components.AppCard
import com.aquadoselab.poolchemistry.ui.components.AppLogo
import com.aquadoselab.poolchemistry.ui.components.ConfirmDialog
import com.aquadoselab.poolchemistry.ui.components.Segmented
import com.aquadoselab.poolchemistry.ui.components.ScreenColumn
import com.aquadoselab.poolchemistry.ui.components.SecondaryButton
import com.aquadoselab.poolchemistry.ui.components.SectionTitle
import com.aquadoselab.poolchemistry.ui.components.findActivity
import com.aquadoselab.poolchemistry.ui.theme.AppColors

/** Replace with your hosted privacy policy before publishing (required by Google Play and AdMob). */
const val PRIVACY_POLICY_URL = "https://example.com/aquadose/privacy"

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavHostController) {
    val data by vm.data.collectAsState()
    val context = LocalContext.current
    var deleting by remember { mutableStateOf<Pool?>(null) }
    val settings = data.settings

    ScreenColumn {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        SectionTitle("Units")
        AppCard {
            Text("Measurements", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
            Segmented(
                options = UnitSystem.entries.toList(),
                selected = settings.units,
                label = { if (it == UnitSystem.US) "US (gal, fl oz, lb)" else "Metric (L, mL, kg)" },
                onSelect = { u -> vm.updateSettings { it.copy(units = u) } }
            )
            Text("Temperature", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
            Segmented(
                options = TempUnit.entries.toList(),
                selected = settings.tempUnit,
                label = { if (it == TempUnit.F) "°F" else "°C" },
                onSelect = { u -> vm.updateSettings { it.copy(tempUnit = u) } }
            )
        }

        SectionTitle("My pools")
        AppCard(padding = 8.dp) {
            if (data.pools.isEmpty()) {
                Text("No pools yet.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
            }
            val activeId = data.activePool?.id
            data.pools.forEachIndexed { i, p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.setActivePool(p.id) }) {
                        Icon(
                            if (p.id == activeId) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = if (p.id == activeId) "Active pool" else "Make active",
                            tint = AppColors.Teal
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${Units.gallonsToDisplay(Volume.gallons(p), settings.units)} · ${p.sanitizer.label} · ${p.surface.label}",
                            style = MaterialTheme.typography.bodySmall, color = AppColors.Muted
                        )
                    }
                    IconButton(onClick = { nav.navigate(Routes.pool(p.id)) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit ${p.name}", tint = AppColors.Muted)
                    }
                    IconButton(onClick = { deleting = p }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete ${p.name}", tint = AppColors.Muted)
                    }
                }
                if (i < data.pools.lastIndex) HorizontalDivider(color = AppColors.Border)
            }
        }
        SecondaryButton("Add a pool", icon = Icons.Filled.Add) { nav.navigate(Routes.pool()) }

        SectionTitle("App")
        AppCard(padding = 4.dp) {
            SettingRow("Rate AquaDose", Icons.Filled.Star) { openPlayStore(context) }
            HorizontalDivider(color = AppColors.Border)
            SettingRow("Share with a friend", Icons.Filled.Share) {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "I use AquaDose to work out exactly how much chlorine and acid my pool needs: https://play.google.com/store/apps/details?id=${context.packageName}"
                    )
                }
                context.startActivity(Intent.createChooser(send, "Share AquaDose"))
            }
            HorizontalDivider(color = AppColors.Border)
            SettingRow("Privacy policy", Icons.Filled.Policy) {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))) }
            }
            if (AdsManager.privacyOptionsRequired()) {
                HorizontalDivider(color = AppColors.Border)
                SettingRow("Ad privacy choices", Icons.Filled.PrivacyTip) {
                    context.findActivity()?.let { AdsManager.showPrivacyOptions(it) }
                }
            }
        }

        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppLogo(size = 48.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("AquaDose", style = MaterialTheme.typography.titleMedium)
                    Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
                }
            }
            Text(
                "AquaDose gives dosing estimates for residential pools. Results depend on accurate tests, pool volume and product strength. Always follow product labels and local regulations. Commercial pools must follow their health authority's rules.",
                style = MaterialTheme.typography.bodySmall, color = AppColors.Muted
            )
        }
        Spacer(Modifier.height(4.dp))
    }

    deleting?.let { p ->
        ConfirmDialog(
            title = "Delete ${p.name}?",
            message = "This removes the pool and all of its test history. This can't be undone.",
            confirm = "Delete",
            onConfirm = { vm.deletePool(p.id); deleting = null },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun SettingRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = AppColors.Teal)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = AppColors.Ink, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AppColors.Muted)
    }
}
