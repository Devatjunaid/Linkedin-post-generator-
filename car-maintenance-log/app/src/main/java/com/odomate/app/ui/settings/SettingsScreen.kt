package com.odomate.app.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Garage
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.odomate.app.AppContainer
import com.odomate.app.BuildConfig
import com.odomate.app.ads.NativeAdCard
import com.odomate.app.ads.NativeAdSize
import com.odomate.app.data.ThemeMode
import com.odomate.app.reminders.Notifications
import com.odomate.app.reminders.ReminderScheduler
import com.odomate.app.ui.LocalAppContainer
import com.odomate.app.ui.LocalMessenger
import com.odomate.app.ui.LocalSettings
import com.odomate.app.ui.appViewModel
import com.odomate.app.ui.components.AppCard
import com.odomate.app.ui.components.AppTextField
import com.odomate.app.ui.components.ConfirmDialog
import com.odomate.app.ui.components.IconBadge
import com.odomate.app.ui.components.LogoMark
import com.odomate.app.ui.components.ScreenHeader
import com.odomate.app.ui.components.SectionLabel
import com.odomate.app.ui.theme.AppTheme
import com.odomate.app.util.findActivity
import com.odomate.app.util.openPlayStore
import com.odomate.app.util.shareApp
import com.odomate.app.util.shareFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val settings get() = container.settings

    fun setNotifications(context: android.content.Context, on: Boolean) {
        viewModelScope.launch {
            container.settings.setNotifications(on)
            if (on) ReminderScheduler.schedule(context, container.settings.current().reminderHour)
            else ReminderScheduler.cancel(context)
        }
    }

    fun setReminderHour(context: android.content.Context, hour: Int) {
        viewModelScope.launch {
            container.settings.setReminderHour(hour)
            if (container.settings.current().notificationsEnabled) ReminderScheduler.schedule(context, hour)
        }
    }

    fun update(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    suspend fun exportCsv() = container.repository.activeVehicle.first()?.id?.let { container.backup.exportCsv(it) }
    suspend fun backup(uri: android.net.Uri) = container.backup.exportJson(uri)
    suspend fun restore(uri: android.net.Uri) = container.backup.importJson(uri)
}

/** Replace with your hosted privacy policy before publishing (required by Google Play for AdMob apps). */
private const val PRIVACY_POLICY_URL = "https://example.com/odomate/privacy"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onManageVehicles: () -> Unit) {
    val vm = appViewModel { c, _ -> SettingsViewModel(c) }
    val settings = LocalSettings.current
    val context = LocalContext.current
    val c = AppTheme.colors
    val ads = LocalAppContainer.current.ads
    val privacyRequired by ads.privacyOptionsRequired.collectAsStateWithLifecycle()
    val message = LocalMessenger.current
    val scope = rememberCoroutineScope()

    var dialog by remember { mutableStateOf<String?>(null) }
    var pendingUnit by remember { mutableStateOf<String?>(null) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.setNotifications(context, true)
        else message("Allow notifications in system settings to get reminders")
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            vm.backup(uri).onSuccess {
                ads.showInterstitial(context.findActivity()) { message("Backup saved") }
            }.onFailure { message("Backup failed: ${it.message}") }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            vm.restore(uri).onSuccess { n -> message("Restored $n ${if (n == 1) "vehicle" else "vehicles"}") }
                .onFailure { message("Restore failed: ${it.message}") }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("More")
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Group("Garage") {
                    SettingRow(Icons.Outlined.Garage, "Manage vehicles", "Add, edit or switch cars", onClick = onManageVehicles)
                }
            }
            item {
                Group("Reminders") {
                    SettingSwitch(Icons.Outlined.Notifications, "Service reminders", "Alerts when something is due soon or overdue", settings.notificationsEnabled) { on ->
                        if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifications.canPost(context)) {
                            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else vm.setNotifications(context, on)
                    }
                    Divider()
                    SettingRow(Icons.Outlined.Schedule, "Reminder time", "Daily check at ${formatHour(settings.reminderHour)}") { dialog = "hour" }
                    Divider()
                    SettingSwitch(Icons.Outlined.PhotoCamera, "Weekly odometer nudge", "Reminds you to snap the odometer if you haven't in a week", settings.weeklyNudge) { on ->
                        vm.update { vm.settings.setWeeklyNudge(on) }
                    }
                    Divider()
                    SettingRow(Icons.Outlined.NotificationsActive, "Send a test reminder", "Check that notifications reach you") {
                        if (!Notifications.canPost(context)) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else message("Notifications are turned off for OdoMate in system settings")
                        } else {
                            ReminderScheduler.runNow(context, force = true)
                            message("Checking now. A notification will appear in a moment.")
                        }
                    }
                }
            }
            item {
                Group("Units & display") {
                    SettingRow(Icons.Outlined.Speed, "Distance", if (settings.miles) "Miles" else "Kilometres") {
                        pendingUnit = if (settings.miles) "km" else "mi"
                    }
                    Divider()
                    SettingRow(Icons.Outlined.LocalGasStation, "Fuel volume", if (settings.volumeUnit == "gal") "Gallons" else "Litres") {
                        vm.update { vm.settings.setVolumeUnit(if (settings.volumeUnit == "gal") "L" else "gal") }
                    }
                    Divider()
                    SettingRow(Icons.Outlined.Payments, "Currency symbol", settings.currency) { dialog = "currency" }
                    Divider()
                    SettingRow(
                        Icons.Outlined.DarkMode, "Theme",
                        when (settings.themeMode) {
                            ThemeMode.DARK -> "Dark"
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.SYSTEM -> "Match system"
                        },
                    ) { dialog = "theme" }
                }
            }
            item {
                Group("Your data") {
                    SettingRow(Icons.Outlined.FileDownload, "Export history (CSV)", "Open in Sheets or Excel, or send to your mechanic") {
                        scope.launch {
                            val result = vm.exportCsv()
                            result?.onSuccess { uri ->
                                ads.showInterstitial(context.findActivity()) { context.shareFile(uri, "text/csv", "Export history") }
                            }?.onFailure { message("Export failed: ${it.message}") }
                        }
                    }
                    Divider()
                    SettingRow(Icons.Outlined.Backup, "Back up to a file", "Save everything to Drive or your phone") {
                        backupLauncher.launch("OdoMate-backup-${LocalDate.now()}.json")
                    }
                    Divider()
                    SettingRow(Icons.Outlined.Restore, "Restore from backup", "Replaces the data on this phone") { dialog = "restore" }
                }
            }
            item {
                Group("Support") {
                    SettingRow(Icons.Outlined.StarOutline, "Rate OdoMate", "Takes 10 seconds and helps a lot") {
                        vm.update { vm.settings.setRateDone() }
                        context.openPlayStore()
                    }
                    Divider()
                    SettingRow(Icons.Outlined.Share, "Share with a friend", null) { context.shareApp() }
                    Divider()
                    SettingRow(Icons.Outlined.PrivacyTip, "Privacy policy", null) {
                        runCatching {
                            context.startActivity(
                                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(PRIVACY_POLICY_URL))
                            )
                        }
                    }
                    if (privacyRequired) {
                        Divider()
                        SettingRow(Icons.Outlined.Info, "Ad privacy choices", "Change your ad consent") {
                            context.findActivity()?.let { ads.showPrivacyOptions(it) }
                        }
                    }
                }
            }
            item { NativeAdCard(size = NativeAdSize.MEDIUM) }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    LogoMark(28.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("OdoMate ${BuildConfig.VERSION_NAME}", color = c.muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    when (dialog) {
        "hour" -> ChoiceDialog(
            title = "Daily reminder time",
            options = listOf(7, 8, 9, 12, 17, 19, 21).map { it to formatHour(it) },
            selected = settings.reminderHour,
            onSelect = { vm.setReminderHour(context, it) },
            onDismiss = { dialog = null },
        )
        "theme" -> ChoiceDialog(
            title = "Theme",
            options = listOf(ThemeMode.DARK to "Dark", ThemeMode.LIGHT to "Light", ThemeMode.SYSTEM to "Match system"),
            selected = settings.themeMode,
            onSelect = { mode -> vm.update { vm.settings.setTheme(mode) } },
            onDismiss = { dialog = null },
        )
        "currency" -> CurrencyDialog(settings.currency, { symbol -> vm.update { vm.settings.setCurrency(symbol) } }, { dialog = null })
        "restore" -> ConfirmDialog(
            title = "Restore from backup?",
            text = "Everything currently on this phone is replaced by the backup. Receipt photos aren't included in backups.",
            confirmLabel = "Choose file",
            onConfirm = { restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
            onDismiss = { dialog = null },
        )
    }

    pendingUnit?.let { unit ->
        ConfirmDialog(
            title = "Switch to ${if (unit == "mi") "miles" else "kilometres"}?",
            text = "Existing readings and intervals keep their numbers. They aren't converted. Best done before you log much data.",
            confirmLabel = "Switch",
            onConfirm = { vm.update { vm.settings.setDistanceUnit(unit) } },
            onDismiss = { pendingUnit = null },
        )
    }
}

private fun formatHour(hour: Int): String = java.time.LocalTime.of(hour, 0)
    .format(java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT))

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column {
        SectionLabel(title)
        AppCard(padding = PaddingValues(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(color = AppTheme.colors.divider, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    val c = AppTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, c.muted, c.surface2, 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = c.text, style = MaterialTheme.typography.titleSmall)
            subtitle?.let { Text(it, color = c.muted, style = MaterialTheme.typography.bodySmall) }
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = c.faint)
    }
}

@Composable
private fun SettingSwitch(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = AppTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, c.muted, c.surface2, 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = c.text, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = c.muted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.onAccent),
        )
    }
}

@Composable
private fun <T> ChoiceDialog(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, onDismiss: () -> Unit) {
    val c = AppTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(title, color = c.text) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(value); onDismiss() }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, color = if (value == selected) c.accentText else c.text, fontWeight = if (value == selected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                        if (value == selected) Icon(Icons.Outlined.CheckCircle, null, tint = c.accentText)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = c.muted) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CurrencyDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val c = AppTheme.colors
    var text by remember { mutableStateOf(current) }
    val common = listOf("$", "€", "£", "₹", "Rs ", "₨", "AED ", "SAR ", "¥", "R$", "C$", "A$")
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text("Currency symbol", color = c.text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    common.forEach { sym ->
                        FilterChip(
                            selected = text == sym,
                            onClick = { text = sym },
                            label = { Text(sym.trim()) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = c.surface2, labelColor = c.text,
                                selectedContainerColor = c.accent, selectedLabelColor = c.onAccent,
                            ),
                        )
                    }
                }
                AppTextField(text, { text = it.take(5) }, "Or type your own")
            }
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onSave(text); onDismiss() }) {
                Text("Save", color = c.accentText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.muted) } },
    )
}
