package com.walkies.dogwalk.ui.reminders

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.walkies.dogwalk.AppContainer
import com.walkies.dogwalk.R
import com.walkies.dogwalk.ads.NativeAdCache
import com.walkies.dogwalk.ads.NativeAdCard
import com.walkies.dogwalk.data.Reminder
import com.walkies.dogwalk.data.ReminderKind
import com.walkies.dogwalk.reminders.Notifications
import com.walkies.dogwalk.ui.components.SectionCard
import com.walkies.dogwalk.ui.components.SectionHeader
import com.walkies.dogwalk.ui.containerFactory
import com.walkies.dogwalk.ui.theme.WalkiesColors
import com.walkies.dogwalk.util.formatClock
import com.walkies.dogwalk.util.formatDuration

private val NO_PEE_HOURS = listOf(3, 4, 5, 6, 8)

/** Sentinel for "the editor is open for a new reminder". */
private val NEW_REMINDER = Reminder(id = -1, kind = ReminderKind.WALK, label = "", hour = 0, minute = 0)

@Composable
fun RemindersScreen(
    container: AppContainer,
    viewModel: RemindersViewModel = viewModel(factory = containerFactory { RemindersViewModel(it) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    var editing by remember { mutableStateOf<Reminder?>(null) }

    // Re-check the notification permission whenever the screen comes back (e.g. from Settings).
    var canNotify by remember { mutableStateOf(Notifications.canNotify(context)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) canNotify = Notifications.canNotify(context)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    var askedOnce by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        canNotify = granted
        askedOnce = true
    }
    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedOnce) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            context.startActivity(intent)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(
                        R.string.reminders_subtitle,
                        state.reminders.count { it.enabled } + if (state.noPeeEnabled) 1 else 0,
                        state.reminders.size + 1,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(R.string.reminders_title), style = MaterialTheme.typography.headlineMedium)
            }
            FilledIconButton(
                onClick = { editing = NEW_REMINDER },
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = WalkiesColors.Walk, contentColor = WalkiesColors.Ink),
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.reminders_add))
            }
        }
        Spacer(Modifier.height(16.dp))

        if (!canNotify) {
            SectionCard(Modifier.background(Color.Transparent)) {
                Text(stringResource(R.string.reminders_permission_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.reminders_permission_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = ::requestNotifications, shape = RoundedCornerShape(14.dp)) {
                    Text(
                        stringResource(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedOnce) {
                                R.string.reminders_permission_button
                            } else {
                                R.string.reminders_permission_settings
                            },
                        ),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        state.nudge?.let { nudge ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(WalkiesColors.Sky, RoundedCornerShape(26.dp))
                    .padding(18.dp),
            ) {
                Text(
                    stringResource(R.string.nudge_label).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = WalkiesColors.SkyDeep,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(
                        R.string.nudge_body,
                        state.dogName,
                        formatDuration(context, nudge.averageGapMillis),
                        nudge.suggestedHours,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.acceptNudge(nudge) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WalkiesColors.Ink, contentColor = Color.White),
                    ) { Text(stringResource(R.string.nudge_accept)) }
                    TextButton(onClick = viewModel::dismissNudge) {
                        Text(stringResource(R.string.nudge_dismiss), color = WalkiesColors.Ink)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        SectionCard(contentPadding = 0.dp) {
            SectionHeader(
                stringResource(R.string.reminders_schedule),
                Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 4.dp),
            )
            state.reminders.forEach { r ->
                ReminderRow(
                    reminder = r,
                    onClick = { editing = r },
                    onToggle = { enabled ->
                        viewModel.toggle(r, enabled)
                        if (enabled && !canNotify) requestNotifications()
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.background, modifier = Modifier.padding(horizontal = 18.dp))
            }
            NoPeeRow(
                enabled = state.noPeeEnabled,
                hours = state.noPeeHours,
                onChange = { enabled, hours ->
                    viewModel.setNoPee(enabled, hours)
                    if (enabled && !canNotify) requestNotifications()
                },
            )
        }
        Spacer(Modifier.height(18.dp))
        NativeAdCard(container.ads, NativeAdCache.REMINDERS)
        Spacer(Modifier.height(24.dp))
    }

    editing?.let { current ->
        val isNew = current === NEW_REMINDER
        ReminderEditorDialog(
            existing = if (isNew) null else current,
            onDismiss = { editing = null },
            onSave = { reminder ->
                editing = null
                viewModel.save(reminder)
                if (!canNotify) requestNotifications()
                // Adding a reminder is a major action: interstitial afterwards, within the cap.
                if (isNew && activity != null) container.ads.interstitials.showIfAllowed(activity) {}
            },
            onDelete = { reminder ->
                editing = null
                viewModel.delete(reminder)
            },
        )
    }
}

@Composable
private fun ReminderRow(reminder: Reminder, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val context = LocalContext.current
    val tint = when {
        !reminder.enabled -> MaterialTheme.colorScheme.background
        reminder.kind == ReminderKind.WALK -> WalkiesColors.WalkTint
        else -> WalkiesColors.PeeTint
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(76.dp)
                .height(42.dp)
                .background(tint, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(formatClock(context, reminder.hour, reminder.minute), style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(reminder.label, style = MaterialTheme.typography.titleSmall)
            Text(
                daysText(reminder) + (reminder.durationMinutes?.let { " · " + stringResource(R.string.reminder_duration, it) } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = reminder.enabled, onCheckedChange = onToggle, colors = walkiesSwitchColors())
    }
}

@Composable
private fun daysText(reminder: Reminder): String = when (reminder.daysMask) {
    Reminder.ALL_DAYS -> stringResource(R.string.reminder_every_day)
    Reminder.WEEKDAYS -> stringResource(R.string.reminder_weekdays)
    Reminder.WEEKENDS -> stringResource(R.string.reminder_weekends)
    else -> java.time.DayOfWeek.entries
        .filter { reminder.daysMask and (1 shl (it.value - 1)) != 0 }
        .joinToString(" ") { it.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()) }
}

@Composable
private fun NoPeeRow(enabled: Boolean, hours: Int, onChange: (Boolean, Int) -> Unit) {
    Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(76.dp)
                    .height(42.dp)
                    .background(if (enabled) WalkiesColors.PeeTint else MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.hours_short, hours), style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.reminders_no_pee), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.reminders_no_pee_sub, hours),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = { onChange(it, hours) }, colors = walkiesSwitchColors())
        }
        if (enabled) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.reminders_no_pee_hours),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NO_PEE_HOURS.forEach { h ->
                    FilterChip(
                        selected = h == hours,
                        onClick = { onChange(true, h) },
                        label = { Text(stringResource(R.string.hours_short, h)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun walkiesSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = WalkiesColors.Walk,
    checkedTrackColor = WalkiesColors.Ink,
    checkedBorderColor = WalkiesColors.Ink,
)
