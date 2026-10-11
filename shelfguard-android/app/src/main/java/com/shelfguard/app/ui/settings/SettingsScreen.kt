package com.shelfguard.app.ui.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.shelfguard.app.AppContainer
import com.shelfguard.app.BuildConfig
import com.shelfguard.app.ads.AdsManager
import com.shelfguard.app.data.prefs.AppSettings
import com.shelfguard.app.domain.AlertSummary
import com.shelfguard.app.notifications.AlertScheduler
import com.shelfguard.app.notifications.NotificationHelper
import com.shelfguard.app.ui.components.AppCard
import com.shelfguard.app.ui.components.ConfirmDialog
import com.shelfguard.app.ui.components.ShelfGuardLogo
import com.shelfguard.app.ui.components.supportingText
import com.shelfguard.app.ui.navigation.AppNavigator
import com.shelfguard.app.util.containerViewModel
import com.shelfguard.app.util.findActivity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val prefs = container.settings
    val settings = prefs.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setShopName(v: String) = viewModelScope.launch { prefs.setShopName(v.ifBlank { "My Shop" }) }
    fun setCurrency(v: String) = viewModelScope.launch { prefs.setCurrency(v) }
    fun setDefaultAlertDays(v: Int) = viewModelScope.launch { prefs.setDefaultAlertDays(v) }
    fun setDefaultMinStock(v: Int) = viewModelScope.launch { prefs.setDefaultMinStock(v) }

    fun setReminders(context: Context, enabled: Boolean) = viewModelScope.launch {
        prefs.setRemindersEnabled(enabled)
        val s = prefs.current()
        AlertScheduler.schedule(context.applicationContext, enabled, s.reminderHour, s.reminderMinute)
    }

    fun setReminderTime(context: Context, hour: Int, minute: Int) = viewModelScope.launch {
        prefs.setReminderTime(hour, minute)
        val s = prefs.current()
        AlertScheduler.schedule(context.applicationContext, s.remindersEnabled, hour, minute)
    }

    /** Posts today's summary right away; returns how many products need attention. */
    suspend fun checkNow(context: Context): Int {
        val summary = AlertSummary.from(container.inventory.stockSnapshot())
        NotificationHelper.postDailySummary(context.applicationContext, summary)
        return summary.total
    }

    fun deleteAll() = viewModelScope.launch { container.inventory.deleteAllData() }
}

private sealed interface EditDialog {
    data object ShopName : EditDialog
    data object Currency : EditDialog
    data object AlertDays : EditDialog
    data object MinStock : EditDialog
    data object Time : EditDialog
    data object DeleteAll : EditDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navigator: AppNavigator) {
    val vm = containerViewModel { SettingsViewModel(it) }
    val s by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var dialog by remember { mutableStateOf<EditDialog?>(null) }
    val time = LocalTime.of(s.reminderHour, s.reminderMinute).format(DateTimeFormatter.ofPattern("h:mm a"))

    when (dialog) {
        EditDialog.ShopName -> TextDialog("Shop name", s.shopName, onSave = vm::setShopName) { dialog = null }
        EditDialog.Currency -> TextDialog("Currency symbol", s.currency, hint = "e.g. Rs, $, €, ₹, AED", onSave = vm::setCurrency) { dialog = null }
        EditDialog.AlertDays -> TextDialog(
            "Default expiry warning (days)", s.defaultAlertDays.toString(), numeric = true,
            onSave = { it.toIntOrNull()?.coerceIn(0, 365)?.let(vm::setDefaultAlertDays) },
        ) { dialog = null }
        EditDialog.MinStock -> TextDialog(
            "Default low-stock level", s.defaultMinStock.toString(), numeric = true,
            onSave = { it.toIntOrNull()?.coerceAtLeast(0)?.let(vm::setDefaultMinStock) },
        ) { dialog = null }
        EditDialog.Time -> {
            val state = rememberTimePickerState(s.reminderHour, s.reminderMinute, is24Hour = false)
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text("Daily summary time") },
                text = { TimePicker(state = state) },
                confirmButton = {
                    TextButton(onClick = {
                        vm.setReminderTime(context, state.hour, state.minute)
                        dialog = null
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } },
            )
        }
        EditDialog.DeleteAll -> ConfirmDialog(
            title = "Delete all data?",
            message = "Every product, batch and sales record on this device will be permanently deleted. This can't be undone.",
            confirmLabel = "Delete everything",
            destructive = true,
            onConfirm = {
                vm.deleteAll()
                scope.launch { snackbar.showSnackbar("All data deleted") }
            },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Group("Shop") {
                SettingRow(Icons.Outlined.Storefront, "Shop name", s.shopName) { dialog = EditDialog.ShopName }
                Divider()
                SettingRow(Icons.Outlined.Payments, "Currency symbol", s.currency.ifBlank { "None" }) { dialog = EditDialog.Currency }
            }
            Group("Alerts") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                        Text("Daily summary notification", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Expired, expiring and low-stock products",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = s.remindersEnabled, onCheckedChange = { vm.setReminders(context, it) })
                }
                Divider()
                SettingRow(Icons.Outlined.AccessTime, "Time", time, enabled = s.remindersEnabled) { dialog = EditDialog.Time }
                Divider()
                SettingRow(Icons.Outlined.Send, "Check alerts now", "Show today's summary as a notification") {
                    scope.launch {
                        val n = vm.checkNow(context)
                        snackbar.showSnackbar(
                            when {
                                !NotificationHelper.canPost(context) -> "Notifications are turned off for ShelfGuard"
                                n == 0 -> "Nothing needs attention right now"
                                else -> "Notification sent: $n products need attention"
                            },
                        )
                    }
                }
            }
            Group("Defaults for new products") {
                SettingRow(Icons.Outlined.Schedule, "Expiry warning", "${s.defaultAlertDays} days before") { dialog = EditDialog.AlertDays }
                Divider()
                SettingRow(
                    Icons.Outlined.Inventory2, "Low-stock level",
                    if (s.defaultMinStock > 0) "At ${s.defaultMinStock} or less" else "Off",
                ) { dialog = EditDialog.MinStock }
            }
            Group("Privacy & data") {
                if (AdsManager.isPrivacyOptionsRequired) {
                    SettingRow(Icons.Outlined.PrivacyTip, "Ad privacy choices", "Review your consent for personalised ads") {
                        context.findActivity()?.let { AdsManager.showPrivacyOptionsForm(it) }
                    }
                    Divider()
                }
                SettingRow(
                    Icons.Outlined.DeleteForever, "Delete all data", "Products, batches and sales history",
                    tint = MaterialTheme.colorScheme.error,
                ) { dialog = EditDialog.DeleteAll }
            }
            Column(
                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ShelfGuardLogo(Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text("ShelfGuard ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelMedium)
                Text(
                    "Your data stays on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        AppCard(Modifier.fillMaxWidth()) { Column { content() } }
    }
}

@Composable
private fun Divider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    value: String,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (tint == MaterialTheme.colorScheme.error) tint else MaterialTheme.colorScheme.onSurface,
            )
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TextDialog(
    title: String,
    initial: String,
    hint: String? = null,
    numeric: Boolean = false,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = if (numeric) it.filter(Char::isDigit).take(4) else it.take(40) },
                supportingText = supportingText(hint),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
                shape = MaterialTheme.shapes.small,
            )
        },
        confirmButton = { Button(onClick = { onSave(value); onDismiss() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
