package com.devatjunaid.examclock.ui.settings

import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devatjunaid.examclock.BuildConfig
import com.devatjunaid.examclock.LocalAppContainer
import com.devatjunaid.examclock.R
import com.devatjunaid.examclock.data.catalog.ExamCatalog
import com.devatjunaid.examclock.data.prefs.ThemeMode
import com.devatjunaid.examclock.ui.ExternalLinks
import com.devatjunaid.examclock.ui.appViewModel
import com.devatjunaid.examclock.ui.components.AppCard
import com.devatjunaid.examclock.ui.components.Format
import com.devatjunaid.examclock.ui.components.ScreenScaffold
import com.devatjunaid.examclock.ui.exams.TimePickerDialog
import java.time.LocalTime

@Composable
fun SettingsScreen(onBack: () -> Unit, onPrivacyPolicy: () -> Unit, onReset: () -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { SettingsViewModel(it, context) }
    val prefs by vm.state.collectAsStateWithLifecycle()
    val privacyRequired by vm.privacyOptionsRequired.collectAsStateWithLifecycle()
    val ads = LocalAppContainer.current.ads
    val activity = LocalActivity.current
    var dialog by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(title = stringResource(R.string.settings_title), onBack = onBack) { padding ->
        val p = prefs ?: return@ScreenScaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Group(stringResource(R.string.settings_profile)) {
                    Item(stringResource(R.string.setup_name_label), p.userName.ifBlank { stringResource(R.string.settings_not_set) }) { dialog = "name" }
                    Item(stringResource(R.string.settings_country), ExamCatalog.country(p.countryCode).name) { dialog = "country" }
                    Item(stringResource(R.string.setup_goal_label), Format.minutes(context, p.dailyGoalMinutes)) { dialog = "goal" }
                }
            }
            item {
                Group(stringResource(R.string.settings_reminders)) {
                    ToggleItem(stringResource(R.string.setup_reminders_label), stringResource(R.string.setup_reminders_hint), p.remindersEnabled, vm::setReminders)
                    if (p.remindersEnabled) {
                        Item(stringResource(R.string.settings_reminder_time), Format.time(context, LocalTime.of(p.reminderMinuteOfDay / 60, p.reminderMinuteOfDay % 60))) { dialog = "time" }
                    }
                }
            }
            item {
                Group(stringResource(R.string.settings_focus)) {
                    Stepper(stringResource(R.string.settings_focus_length), p.focusMinutes, 5, 120, 5) { vm.setFocus(it, p.shortBreakMinutes, p.longBreakMinutes) }
                    Stepper(stringResource(R.string.settings_short_break), p.shortBreakMinutes, 1, 30, 1) { vm.setFocus(p.focusMinutes, it, p.longBreakMinutes) }
                    Stepper(stringResource(R.string.settings_long_break), p.longBreakMinutes, 5, 60, 5) { vm.setFocus(p.focusMinutes, p.shortBreakMinutes, it) }
                    ToggleItem(stringResource(R.string.settings_keep_screen_on), stringResource(R.string.settings_keep_screen_on_hint), p.keepScreenOn, vm::setKeepScreenOn)
                }
            }
            item {
                Group(stringResource(R.string.settings_appearance)) {
                    val modes = ThemeMode.entries
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(16.dp)) {
                        modes.forEachIndexed { i, m ->
                            SegmentedButton(
                                selected = p.themeMode == m,
                                onClick = { vm.setTheme(m) },
                                shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                            ) {
                                Text(stringResource(when (m) {
                                    ThemeMode.SYSTEM -> R.string.theme_system
                                    ThemeMode.LIGHT -> R.string.theme_light
                                    ThemeMode.DARK -> R.string.theme_dark
                                }))
                            }
                        }
                    }
                }
            }
            item {
                Group(stringResource(R.string.settings_privacy)) {
                    if (privacyRequired) {
                        Item(stringResource(R.string.settings_ad_privacy), stringResource(R.string.settings_ad_privacy_hint)) {
                            activity?.let { ads.showPrivacyOptions(it) }
                        }
                    }
                    Item(stringResource(R.string.settings_privacy_policy), null, onPrivacyPolicy)
                }
            }
            item {
                Group(stringResource(R.string.settings_about)) {
                    Item(stringResource(R.string.action_rate_us), stringResource(R.string.settings_rate_hint)) { ExternalLinks.openStoreListing(context) }
                    Item(stringResource(R.string.settings_share), null) { ExternalLinks.shareApp(context) }
                    Item(stringResource(R.string.settings_version), BuildConfig.VERSION_NAME, null)
                }
            }
            item {
                Group(stringResource(R.string.settings_data)) {
                    Item(stringResource(R.string.settings_reset), stringResource(R.string.settings_reset_hint)) { dialog = "reset" }
                }
            }
        }

        when (dialog) {
            "name" -> TextDialog(stringResource(R.string.setup_name_label), p.userName, onDismiss = { dialog = null }) {
                vm.setName(it)
                dialog = null
            }
            "country" -> CountryDialog(p.countryCode, onDismiss = { dialog = null }) {
                vm.setCountry(it)
                dialog = null
            }
            "goal" -> GoalDialog(p.dailyGoalMinutes, context, onDismiss = { dialog = null }) {
                vm.setGoal(it)
                dialog = null
            }
            "time" -> TimePickerDialog(LocalTime.of(p.reminderMinuteOfDay / 60, p.reminderMinuteOfDay % 60), onDismiss = { dialog = null }) {
                vm.setReminderMinute(it.hour * 60 + it.minute)
                dialog = null
            }
            "reset" -> AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text(stringResource(R.string.settings_reset)) },
                text = { Text(stringResource(R.string.settings_reset_confirm)) },
                confirmButton = {
                    TextButton(onClick = {
                        dialog = null
                        vm.resetAll(onReset)
                    }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.action_cancel)) } },
            )
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AppCard(contentPadding = 0.dp) { content() }
    }
}

@Composable
private fun Item(title: String, value: String?, onClick: (() -> Unit)?) {
    val content: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (value != null) {
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface) { content() }
    } else {
        content()
    }
}

@Composable
private fun ToggleItem(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun Stepper(title: String, value: Int, min: Int, max: Int, step: Int, onChange: (Int) -> Unit) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        OutlinedIconButton(onClick = { onChange((value - step).coerceAtLeast(min)) }, enabled = value > min) {
            Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.cd_less_time, title))
        }
        Text(Format.minutes(context, value), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(72.dp))
        OutlinedIconButton(onClick = { onChange((value + step).coerceAtMost(max)) }, enabled = value < max) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.cd_more_time, title))
        }
    }
}

@Composable
private fun TextDialog(title: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it.take(40) }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun CountryDialog(current: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_country)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(ExamCatalog.countries + ExamCatalog.other, key = { it.code }) { c ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(value = c.code == current, role = Role.RadioButton, onValueChange = { onPick(c.code) })
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = c.code == current, onClick = null)
                        Text(c.name, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun GoalDialog(initial: Int, context: Context, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var value by remember { mutableIntStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.setup_goal_label)) },
        text = {
            Column {
                Text(Format.minutes(context, value), style = MaterialTheme.typography.headlineSmall)
                Slider(
                    value = value.toFloat(),
                    onValueChange = { value = ((it / 15f).toInt() * 15).coerceIn(30, 600) },
                    valueRange = 30f..600f,
                    steps = (600 - 30) / 15 - 1,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

