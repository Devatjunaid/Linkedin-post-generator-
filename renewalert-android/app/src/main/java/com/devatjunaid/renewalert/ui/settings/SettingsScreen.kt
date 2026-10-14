package com.devatjunaid.renewalert.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.StarRate
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.devatjunaid.renewalert.BuildConfig
import com.devatjunaid.renewalert.R
import com.devatjunaid.renewalert.data.ThemeMode
import com.devatjunaid.renewalert.domain.Catalog
import com.devatjunaid.renewalert.domain.Formats
import com.devatjunaid.renewalert.security.AppLock
import com.devatjunaid.renewalert.ui.AppViewModel
import com.devatjunaid.renewalert.ui.components.SectionCard
import com.devatjunaid.renewalert.ui.components.SectionLabel
import com.devatjunaid.renewalert.ui.components.SettingRow
import com.devatjunaid.renewalert.ui.components.SwitchRow
import com.devatjunaid.renewalert.ui.theme.Brand
import com.devatjunaid.renewalert.ui.theme.RenewAlertTheme

private enum class SettingsDialog { CURRENCY, BUDGET, RATES, THEME, REMINDER_DAYS, DELETE, LICENSES }

@Composable
fun SettingsScreen(vm: AppViewModel, onOpenBackup: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val p by vm.portfolio.collectAsState()
    val s = settings ?: return
    val c = RenewAlertTheme.colors
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    var notifGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifGranted = it }
    val privacyUrl = stringResource(R.string.privacy_policy_url)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineLarge, color = c.onSurface) }
        item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Brand.Navy, Brand.NavyLight))).padding(16.dp),
            ) {
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF9FE0B9), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Private by design", style = MaterialTheme.typography.titleMedium, color = c.onHero)
                    Text(
                        "No bank link, no account. Your subscriptions are stored only on this phone and never sent anywhere. " +
                            "Ads are provided by Google AdMob, which doesn't see your subscriptions.",
                        style = MaterialTheme.typography.bodySmall, color = c.onHeroMuted,
                    )
                }
            }
        }

        item { SectionLabel("Security") }
        item {
            SectionCard(padding = PaddingValues(0.dp)) {
                SwitchRow(
                    "App lock", "Fingerprint, face or screen lock", s.appLock, Icons.Rounded.Fingerprint,
                ) { on ->
                    if (activity == null) return@SwitchRow
                    if (!AppLock.isAvailable(context)) {
                        Toast.makeText(context, "Set up a screen lock on your phone first", Toast.LENGTH_LONG).show()
                        return@SwitchRow
                    }
                    AppLock.prompt(activity, if (on) "Turn on app lock" else "Turn off app lock", "Confirm it's you", onSuccess = { vm.setAppLock(on) })
                }
                SwitchRow("Hide amounts", "Show •••• instead of prices", s.hideAmounts, Icons.Rounded.Visibility, showDivider = true) { vm.setHideAmounts(it) }
                SwitchRow("Block screenshots", "Also hides Renew Alert in recent apps", s.blockScreenshots, Icons.Rounded.Block, showDivider = true) { vm.setBlockScreenshots(it) }
            }
        }

        item { SectionLabel("Reminders") }
        item {
            SectionCard(padding = PaddingValues(0.dp)) {
                if (!notifGranted) {
                    SettingRow(
                        "Allow notifications", "Needed for reminders and trial alerts", Icons.Rounded.NotificationsActive,
                        titleColor = c.warnText,
                    ) {
                        if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                SwitchRow("Renewal reminders", "Before each charge", s.renewalReminders, Icons.Rounded.Notifications, showDivider = !notifGranted) { vm.setRenewalReminders(it) }
                SettingRow("Default reminder", "For new subscriptions", Icons.Rounded.CalendarMonth, value = reminderLabel(s.defaultReminderDays), showDivider = true) {
                    dialog = SettingsDialog.REMINDER_DAYS
                }
                SwitchRow("Trial alerts", "3 days, 1 day and the morning it ends", s.trialAlerts, Icons.Rounded.Timer, showDivider = true) { vm.setTrialAlerts(it) }
                SwitchRow("Weekly summary", "Sundays: what's due in the week ahead", s.weeklyDigest, Icons.Rounded.CalendarMonth, showDivider = true) { vm.setWeeklyDigest(it) }
                SettingRow("System notification settings", icon = Icons.Rounded.Notifications, showDivider = true) {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                }
            }
        }

        item { SectionLabel("Money") }
        item {
            SectionCard(padding = PaddingValues(0.dp)) {
                SettingRow("Main currency", "Totals and budget use it", Icons.Rounded.AccountBalanceWallet, value = "${s.currency} ${Formats.symbol(s.currency)}") {
                    dialog = SettingsDialog.CURRENCY
                }
                SettingRow(
                    "Monthly budget", "Warns on Home when you go over", Icons.Rounded.AccountBalanceWallet,
                    value = if (s.budgetMinor > 0) Formats.money(s.budgetMinor, s.currency) else "Off", showDivider = true,
                ) { dialog = SettingsDialog.BUDGET }
                val others = p.subs.map { it.currency }.distinct().filter { it != s.currency }
                SettingRow(
                    "Exchange rates",
                    if (others.isEmpty()) "Only needed for other currencies" else "${others.size} other currenc${if (others.size == 1) "y" else "ies"} in use",
                    Icons.Rounded.CurrencyExchange, showDivider = true,
                ) { dialog = SettingsDialog.RATES }
                SwitchRow("Show yearly totals", "Home shows the yearly figure first", s.showYearly, Icons.Rounded.CalendarMonth, showDivider = true) { vm.setShowYearly(it) }
            }
        }

        item { SectionLabel("Appearance") }
        item {
            SectionCard(padding = PaddingValues(0.dp)) {
                SettingRow("Theme", icon = Icons.Rounded.Palette, value = s.theme.name.lowercase().replaceFirstChar { it.uppercase() }) { dialog = SettingsDialog.THEME }
                SettingRow("Home-screen widget", "Long-press your home screen › Widgets › Renew Alert", Icons.Rounded.Widgets, showDivider = true)
            }
        }

        item { SectionLabel("Your data") }
        item {
            SectionCard(padding = PaddingValues(0.dp)) {
                SettingRow(
                    "Backup & export",
                    if (s.lastBackupMillis > 0) "Last backup ${Formats.fullDate(java.time.Instant.ofEpochMilli(s.lastBackupMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate())}" else "Encrypted backup, CSV, PDF report",
                    Icons.Rounded.Backup,
                    onClick = onOpenBackup,
                )
                SettingRow("Delete all data", "Removes every subscription and setting", Icons.Rounded.DeleteForever, titleColor = c.danger, showDivider = true) {
                    dialog = SettingsDialog.DELETE
                }
            }
        }

        item { SectionLabel("Ads & privacy") }
        item {
            SectionCard(padding = PaddingValues(0.dp)) {
                if (vm.ads.privacyOptionsRequired) {
                    SettingRow("Ad privacy choices", "Change your consent for personalised ads", Icons.Rounded.PrivacyTip) {
                        (context as? Activity)?.let { vm.ads.showPrivacyOptions(it) {} }
                    }
                }
                SettingRow("Privacy policy", icon = Icons.Rounded.Policy, showDivider = vm.ads.privacyOptionsRequired) {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))) }
                }
            }
        }

        item { SectionLabel("About") }
        item {
            SectionCard(padding = PaddingValues(0.dp)) {
                SettingRow("Rate Renew Alert", icon = Icons.Rounded.StarRate) { openStore(context) }
                SettingRow("Share with a friend", icon = Icons.Rounded.Share, showDivider = true) {
                    val text = "I track my subscriptions privately with Renew Alert: https://play.google.com/store/apps/details?id=${context.packageName.removeSuffix(".debug")}"
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Share Renew Alert"))
                }
                SettingRow("Open-source licences", icon = Icons.Rounded.Info, showDivider = true) { dialog = SettingsDialog.LICENSES }
                SettingRow("Version", icon = Icons.Rounded.Info, value = BuildConfig.VERSION_NAME, showDivider = true)
            }
        }
    }

    when (dialog) {
        SettingsDialog.CURRENCY -> ChoiceDialog(
            "Main currency",
            (listOf(s.currency) + Catalog.currencies.filter { it != s.currency }).map { it to "$it  ${Formats.symbol(it)}" },
            s.currency, onDismiss = { dialog = null },
        ) { vm.setCurrency(it); dialog = null }
        SettingsDialog.THEME -> ChoiceDialog(
            "Theme", listOf(ThemeMode.SYSTEM to "System default", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark"),
            s.theme, onDismiss = { dialog = null },
        ) { vm.setTheme(it); dialog = null }
        SettingsDialog.REMINDER_DAYS -> ChoiceDialog(
            "Default reminder", listOf(-1, 0, 1, 2, 3, 7).map { it to reminderLabel(it) },
            s.defaultReminderDays, onDismiss = { dialog = null },
        ) { vm.setDefaultReminderDays(it); dialog = null }
        SettingsDialog.BUDGET -> AmountDialog(
            "Monthly budget", s.currency, if (s.budgetMinor > 0) Formats.minorToInput(s.budgetMinor) else "",
            "Leave empty to turn the budget off.", onDismiss = { dialog = null },
        ) { vm.setBudget(it ?: 0L); dialog = null }
        SettingsDialog.RATES -> RatesDialog(
            main = s.currency,
            currencies = p.subs.map { it.currency }.distinct().filter { it != s.currency },
            rates = s.rates,
            onDismiss = { dialog = null },
        ) { vm.setRates(it); dialog = null }
        SettingsDialog.DELETE -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Delete all data?") },
            text = { Text("Every subscription, price history and setting on this phone is removed. This can't be undone. Make a backup first if you might need it.") },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    vm.deleteAllData { }
                    Toast.makeText(context, "All data deleted", Toast.LENGTH_SHORT).show()
                }) { Text("Delete everything", color = c.danger) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } },
        )
        SettingsDialog.LICENSES -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Open-source licences") },
            text = {
                Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Android Jetpack (Compose, Room, WorkManager, DataStore, Biometric, Glance) — Apache License 2.0")
                    Text("Kotlin and kotlinx.serialization — Apache License 2.0")
                    Text("Google Mobile Ads SDK and User Messaging Platform — Google terms of service")
                    Text("DM Sans and Space Grotesk fonts — SIL Open Font License 1.1")
                }
            },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("Close") } },
        )
        null -> Unit
    }
}

private fun reminderLabel(d: Int) = when (d) {
    -1 -> "Off"; 0 -> "On the day"; 1 -> "1 day before"; 7 -> "1 week before"; else -> "$d days before"
}

private fun openStore(context: android.content.Context) {
    val pkg = context.packageName.removeSuffix(".debug")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
    } catch (_: Exception) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))) }
    }
}

fun rateApp(context: android.content.Context) = openStore(context)

@Composable
private fun <T> ChoiceDialog(title: String, options: List<Pair<T, String>>, selected: T, onDismiss: () -> Unit, onPick: (T) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                options.forEach { (value, label) ->
                    item {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = value == selected, onClick = { onPick(value) })
                            TextButton(onClick = { onPick(value) }) { Text(label, color = RenewAlertTheme.colors.onSurface) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun AmountDialog(title: String, currency: String, initial: String, hint: String, onDismiss: () -> Unit, onSave: (Long?) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text, onValueChange = { v -> text = v.filter { it.isDigit() || it == '.' || it == ',' }.take(12) },
                    singleLine = true, prefix = { Text(Formats.symbol(currency) + " ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Spacer(Modifier.size(6.dp))
                Text(hint, style = MaterialTheme.typography.bodySmall, color = RenewAlertTheme.colors.muted)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(if (text.isBlank()) null else Formats.parseMinor(text)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RatesDialog(main: String, currencies: List<String>, rates: Map<String, Double>, onDismiss: () -> Unit, onSave: (Map<String, Double>) -> Unit) {
    val values = remember { mutableStateMapOf<String, String>().apply { currencies.forEach { put(it, rates[it]?.toString() ?: "") } } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exchange rates") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (currencies.isEmpty()) {
                    Text("All your subscriptions use $main. Rates are only needed when you add one in another currency.")
                } else {
                    Text("How much is 1 unit of each currency in $main? Renew Alert works offline, so rates aren't fetched automatically.", style = MaterialTheme.typography.bodySmall)
                    currencies.forEach { code ->
                        OutlinedTextField(
                            value = values[code].orEmpty(),
                            onValueChange = { v -> values[code] = v.filter { it.isDigit() || it == '.' }.take(12) },
                            label = { Text("1 $code =") },
                            suffix = { Text(main) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val out = rates.toMutableMap()
                values.forEach { (k, v) ->
                    val rate = v.toDoubleOrNull()?.takeIf { it > 0 }
                    if (rate != null) { out[k] = rate } else { out.remove(k) }
                }
                onSave(out)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
