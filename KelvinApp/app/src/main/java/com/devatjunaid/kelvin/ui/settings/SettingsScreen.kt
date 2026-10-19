package com.devatjunaid.kelvin.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.devatjunaid.kelvin.BuildConfig
import com.devatjunaid.kelvin.data.Settings
import com.devatjunaid.kelvin.ui.components.Chip
import com.devatjunaid.kelvin.ui.components.GlassCard
import com.devatjunaid.kelvin.ui.components.IconBadge
import com.devatjunaid.kelvin.ui.components.KIcons
import com.devatjunaid.kelvin.ui.components.NoteLine
import com.devatjunaid.kelvin.ui.components.SectionLabel
import com.devatjunaid.kelvin.ui.components.Stepper
import com.devatjunaid.kelvin.ui.components.ToggleRow
import com.devatjunaid.kelvin.ui.components.pressable
import com.devatjunaid.kelvin.ui.theme.K
import com.devatjunaid.kelvin.ui.theme.KType
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Replace with your hosted privacy policy before publishing (required by Google Play for apps with ads). */
const val PRIVACY_POLICY_URL = "https://example.com/kelvin/privacy"

@Composable
fun SettingsScreen(
    settings: Settings,
    privacyOptionsRequired: Boolean,
    onFahrenheit: (Boolean) -> Unit,
    onSound: (Boolean) -> Unit,
    onVibration: (Boolean) -> Unit,
    onBreathing: (Boolean) -> Unit,
    onColdGoal: (Int) -> Unit,
    onHeatGoal: (Int) -> Unit,
    onReminder: (enabled: Boolean, hour: Int, minute: Int) -> Unit,
    onPrivacyOptions: () -> Unit,
    onSafetyGuide: () -> Unit,
) {
    val context = LocalContext.current
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var permissionDenied by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted) onReminder(true, settings.reminderHour, settings.reminderMinute)
    }
    fun enableReminder(enable: Boolean) {
        if (!enable) {
            onReminder(false, settings.reminderHour, settings.reminderMinute)
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && !NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onReminder(true, settings.reminderHour, settings.reminderMinute)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = top + 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("Settings", style = KType.Title) }

        item {
            GlassCard {
                SectionLabel("Units")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Celsius °C", !settings.fahrenheit, { onFahrenheit(false) }, accent = K.Ice)
                    Chip("Fahrenheit °F", settings.fahrenheit, { onFahrenheit(true) }, accent = K.Ice)
                }
            }
        }

        item {
            GlassCard(padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                SectionLabel("During a session")
                ToggleRow("Sound cues", "3-2-1 countdown and phase changes", settings.sound, onSound, icon = KIcons.Volume)
                ToggleRow("Vibration", "Phase changes and a tap every cold minute", settings.vibration, onVibration, icon = KIcons.Vibrate)
                ToggleRow("Breathing guide", "Inhale 4 s · exhale 6 s in the cold", settings.breathingGuide, onBreathing, icon = KIcons.Breath)
            }
        }

        item {
            GlassCard {
                SectionLabel("Weekly goals")
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Cold", style = KType.BodyStrong)
                        Text("minutes per week", style = KType.Small)
                    }
                    Stepper(
                        "${settings.weeklyColdGoalMin}",
                        { onColdGoal(settings.weeklyColdGoalMin - 1) },
                        { onColdGoal(settings.weeklyColdGoalMin + 1) },
                        "Lower cold goal",
                        "Raise cold goal",
                    )
                }
                HorizontalDivider(color = K.Line)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Heat", style = KType.BodyStrong)
                        Text("minutes per week", style = KType.Small)
                    }
                    Stepper(
                        "${settings.weeklyHeatGoalMin}",
                        { onHeatGoal(settings.weeklyHeatGoalMin - 5) },
                        { onHeatGoal(settings.weeklyHeatGoalMin + 5) },
                        "Lower heat goal",
                        "Raise heat goal",
                    )
                }
                NoteLine("The default 11 min cold / 57 min heat per week is a popular target from contrast-therapy research. Adjust it to what suits you.", tint = K.Ice)
            }
        }

        item {
            GlassCard(padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                SectionLabel("Daily reminder")
                ToggleRow(
                    "Remind me",
                    "Only if I haven't logged a session that day",
                    settings.reminderEnabled,
                    { enableReminder(it) },
                    icon = KIcons.Bell,
                )
                val time = LocalTime.of(settings.reminderHour, settings.reminderMinute)
                val is24h = DateFormat.is24HourFormat(context)
                LinkRow(
                    icon = KIcons.Clock,
                    title = "Time",
                    value = time.format(DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm a", Locale.getDefault())),
                ) {
                    TimePickerDialog(context, { _, h, m -> onReminder(settings.reminderEnabled, h, m) }, settings.reminderHour, settings.reminderMinute, is24h).show()
                }
                if (permissionDenied) NoteLine("Notifications are blocked for Kelvin. Enable them in system settings to get reminders.")
            }
        }

        item {
            GlassCard(padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                SectionLabel("Safety & privacy")
                LinkRow(KIcons.Shield, "Safety guide", null, onSafetyGuide)
                if (privacyOptionsRequired) LinkRow(KIcons.Privacy, "Ad privacy options", null, onPrivacyOptions)
                LinkRow(KIcons.Info, "Privacy policy", null) {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))) }
                }
            }
        }

        item {
            Text(
                "Kelvin ${BuildConfig.VERSION_NAME} · Your sessions are stored only on this device.",
                style = KType.Small,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, value: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick, label = title)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(icon, K.Ice)
        Text(title, style = KType.BodyStrong, modifier = Modifier.weight(1f))
        if (value != null) Text(value, style = KType.Mono.copy(fontSize = KType.Small.fontSize * 1.2f))
        Icon(KIcons.ChevronRight, contentDescription = null, tint = K.Muted)
    }
}
