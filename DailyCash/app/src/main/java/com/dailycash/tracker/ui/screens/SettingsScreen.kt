package com.dailycash.tracker.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailycash.tracker.BuildConfig
import com.dailycash.tracker.ads.AdsConsent
import com.dailycash.tracker.data.Money
import com.dailycash.tracker.ui.MainViewModel
import com.dailycash.tracker.ui.brand.DailyCashLogo
import com.dailycash.tracker.ui.components.CurrencyPickerDialog
import com.dailycash.tracker.ui.theme.Palette
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Replace with the address of your hosted privacy policy before publishing (Play requires one for apps with ads). */
const val PRIVACY_POLICY_URL = "https://example.com/daily-cash/privacy"

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val home by vm.homeCurrency.collectAsStateWithLifecycle()
    val rates by vm.rates.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val privacyRequired by AdsConsent.privacyOptionsRequired.collectAsStateWithLifecycle()
    var pickHome by remember { mutableStateOf(false) }

    if (pickHome) {
        CurrencyPickerDialog(
            title = "Your home currency",
            onPick = { vm.setHomeCurrency(it); pickHome = false },
            onDismiss = { pickHome = false },
        )
    }

    val spin = rememberInfiniteTransition(label = "refresh")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "a")

    Column(Modifier.fillMaxSize().background(Palette.Ink).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Palette.Text) }
            Text("Settings", color = Palette.Text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Group("Money") {
                SettingRow(Icons.Rounded.Home, "Home currency", "${Money.displayName(home)} ($home) — everything is totalled in this") { pickHome = true }
                SettingRow(Icons.Rounded.Place, "You're in", "${vm.place.countryName} · ${vm.place.currency}, from your mobile network")
            }
            Group("Exchange rates") {
                val updated = if (rates.live && rates.updatedAt > 0) {
                    "Updated " + DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.getDefault())
                        .format(Instant.ofEpochMilli(rates.updatedAt).atZone(ZoneId.systemDefault()))
                } else "Offline estimate — refresh when online"
                SettingRow(Icons.Rounded.SwapHoriz, "Rates", updated)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = !refreshing, role = Role.Button) {
                            vm.refreshRates { ok ->
                                Toast.makeText(context, if (ok) "Rates updated" else "Couldn't reach the rates service", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = Palette.Mint,
                        modifier = Modifier.size(22.dp).rotate(if (refreshing) angle else 0f),
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(if (refreshing) "Refreshing…" else "Refresh now", color = Palette.Mint, fontWeight = FontWeight.Bold)
                }
                Text("Rates by ExchangeRate-API (open.er-api.com). Each entry keeps the rate of the day it was saved.", color = Palette.Faint, fontSize = 11.sp)
            }
            Group("Your data") {
                SettingRow(Icons.Rounded.FileDownload, "Export as CSV", "Share every entry to a spreadsheet, email or drive") {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_SUBJECT, "Daily Cash export")
                        putExtra(Intent.EXTRA_TEXT, vm.exportCsv())
                    }
                    context.startActivity(Intent.createChooser(send, "Export Daily Cash"))
                }
                Text("Your entries are stored only on this phone (and in your own Android backup). Nothing is uploaded.", color = Palette.Faint, fontSize = 11.sp)
            }
            Group("Privacy") {
                if (privacyRequired && activity != null) {
                    SettingRow(Icons.Rounded.Lock, "Ad privacy choices", "Change your consent for personalised ads") {
                        AdsConsent.showPrivacyOptions(activity)
                    }
                }
                SettingRow(Icons.Rounded.Policy, "Privacy policy", "How ads and data are handled") {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, PRIVACY_POLICY_URL.toUri())) }
                }
            }
            Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                DailyCashLogo(size = 96.dp, playEntrance = false)
                Spacer(Modifier.height(8.dp))
                Text("Daily Cash ${BuildConfig.VERSION_NAME}", color = Palette.Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Text(title.uppercase(), color = Palette.Muted, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) { content() }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Palette.Sky, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Palette.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = Palette.Muted, fontSize = 12.sp)
        }
        if (onClick != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Palette.Faint)
    }
}
