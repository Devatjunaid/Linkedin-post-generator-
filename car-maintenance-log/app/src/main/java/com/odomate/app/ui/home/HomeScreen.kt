package com.odomate.app.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Garage
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.odomate.app.AppContainer
import com.odomate.app.ads.NativeAdCard
import com.odomate.app.data.VehicleSnapshot
import com.odomate.app.data.db.ReadingSource
import com.odomate.app.data.db.Vehicle
import com.odomate.app.data.db.subtitle
import com.odomate.app.domain.Level
import com.odomate.app.domain.Stats
import com.odomate.app.reminders.Notifications
import com.odomate.app.ui.LocalAppContainer
import com.odomate.app.ui.LocalSettings
import com.odomate.app.ui.LocalMessenger
import com.odomate.app.ui.appViewModel
import com.odomate.app.ui.components.AppCard
import com.odomate.app.ui.components.EmptyState
import com.odomate.app.ui.components.IconBadge
import com.odomate.app.ui.components.OdometerDigits
import com.odomate.app.ui.components.PrimaryButton
import com.odomate.app.ui.components.ReadingDialog
import com.odomate.app.ui.components.SecondaryButton
import com.odomate.app.ui.components.SectionLabel
import com.odomate.app.ui.components.ServiceCard
import com.odomate.app.ui.theme.AppTheme
import com.odomate.app.ui.theme.Type
import com.odomate.app.util.Formats
import com.odomate.app.util.findActivity
import com.odomate.app.util.openPlayStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit
import com.odomate.app.domain.StatusCalculator

data class HomeState(
    val loading: Boolean = true,
    val vehicles: List<Vehicle> = emptyList(),
    val snapshot: VehicleSnapshot? = null,
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    val state = combine(container.repository.vehicles, container.repository.activeSnapshot) { vehicles, snapshot ->
        HomeState(loading = false, vehicles = vehicles, snapshot = snapshot)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun switchVehicle(id: Long) {
        viewModelScope.launch { container.repository.setActiveVehicle(id) }
    }

    suspend fun saveReading(vehicleId: Long, value: Int) {
        container.repository.addReading(vehicleId, value, System.currentTimeMillis(), ReadingSource.MANUAL)
    }

    fun markRated() {
        viewModelScope.launch { container.settings.setRateDone() }
    }
}

@Composable
fun HomeScreen(
    onScan: () -> Unit,
    onOpenService: (Long) -> Unit,
    onSeeServices: () -> Unit,
    onLogService: () -> Unit,
    onAddFuel: () -> Unit,
    onManageVehicles: () -> Unit,
    onAddVehicle: () -> Unit,
) {
    val vm = appViewModel { c, _ -> HomeViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val settings = LocalSettings.current
    val c = AppTheme.colors
    val context = LocalContext.current
    val ads = LocalAppContainer.current.ads
    val message = LocalMessenger.current
    val scope = rememberCoroutineScope()
    var showReadingDialog by remember { mutableStateOf(false) }
    var rateDismissed by rememberSaveable { mutableStateOf(false) }

    // Ask for notification permission once, so reminders can actually be delivered.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    var askedPermission by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(settings.notificationsEnabled) {
        if (!askedPermission && settings.notificationsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !Notifications.canPost(context)
        ) {
            askedPermission = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val snap = state.snapshot
    if (!state.loading && snap == null) {
        Column(Modifier.fillMaxSize().statusBarsPadding(), verticalArrangement = Arrangement.Center) {
            EmptyState(Icons.Outlined.DirectionsCar, "No vehicle yet", "Add your car to start tracking its maintenance.") {
                PrimaryButton("Add vehicle", onAddVehicle, icon = Icons.Outlined.Add)
            }
        }
        return
    }
    if (snap == null) return

    val unit = settings.distanceUnit
    val statuses = snap.statuses
    val overdue = statuses.count { it.level == Level.OVERDUE }
    val soon = statuses.count { it.level == Level.SOON }
    val ok = statuses.count { it.level == Level.OK }
    val unknown = statuses.count { it.level == Level.UNKNOWN }
    val upNext = statuses.filter { it.level == Level.OVERDUE || it.level == Level.SOON || it.level == Level.OK }.take(4)
    val economy = remember(snap.fuel) { Stats.fuelEconomy(snap.fuel) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HomeHeader(
                vehicles = state.vehicles,
                current = snap.vehicle,
                alertCount = overdue + soon,
                onSwitch = vm::switchVehicle,
                onManage = onManageVehicles,
                onAdd = onAddVehicle,
                onBell = onSeeServices,
            )
        }

        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ODOMETER", style = Type.overline, color = c.muted, modifier = Modifier.weight(1f))
                    snap.lastReading?.let { r ->
                        val days = ChronoUnit.DAYS.between(StatusCalculator.toLocalDate(r.date), java.time.LocalDate.now())
                        val whenText = when (days) {
                            0L -> "Updated today"
                            1L -> "Updated yesterday"
                            else -> "Updated ${Formats.days(days)} ago"
                        }
                        val how = when (r.source) {
                            ReadingSource.SCAN -> " · from photo"
                            ReadingSource.SERVICE -> " · at service"
                            ReadingSource.FUEL -> " · at fill-up"
                            else -> ""
                        }
                        Text(whenText + how, style = MaterialTheme.typography.bodySmall, color = if (days > 14) c.warn else c.muted)
                    }
                }
                Spacer(Modifier.height(12.dp))
                OdometerDigits(snap.currentOdometer, unit)
                Spacer(Modifier.height(10.dp))
                val avg = snap.avgPerDay
                val prev = snap.readings.sortedByDescending { it.date }.getOrNull(1)
                val line = buildList {
                    if (avg != null) add("≈ ${Formats.decimal(avg, 0)} $unit/day")
                    if (prev != null && snap.currentOdometer != null && snap.currentOdometer > prev.value) {
                        add("+${Formats.distance(snap.currentOdometer - prev.value, unit)} since ${Formats.shortDate(prev.date)}")
                    }
                }.joinToString(" · ").ifBlank { "Snap the odometer every week or two for accurate reminders" }
                Text(line, style = MaterialTheme.typography.bodySmall, color = c.muted)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton("Scan odometer", onScan, Modifier.weight(1f), icon = Icons.Outlined.PhotoCamera)
                    SecondaryButton("Type it", { showReadingDialog = true })
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("$overdue", "Overdue", c.danger, Modifier.weight(1f), onSeeServices)
                StatTile("$soon", "Due soon", c.warn, Modifier.weight(1f), onSeeServices)
                StatTile("$ok", "On track", c.ok, Modifier.weight(1f), onSeeServices)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("Log service", Icons.Outlined.Build, Modifier.weight(1f), onLogService)
                QuickAction("Add fuel", Icons.Outlined.LocalGasStation, Modifier.weight(1f), onAddFuel)
            }
        }

        if (unknown > 0) {
            item {
                AppCard(onClick = onSeeServices, color = c.accentSoft) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Outlined.Tune, c.onAccent, c.accent, 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Finish setting up", color = c.text, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "$unknown ${if (unknown == 1) "service doesn't" else "services don't"} know when they were last done yet.",
                                color = c.muted, style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionLabel("Up next") {
                TextButton(onClick = onSeeServices) {
                    Text("See all ${statuses.count { it.type.enabled }}", color = c.accentText, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (upNext.isEmpty()) {
            item {
                AppCard {
                    Text("Nothing scheduled yet", color = c.text, style = MaterialTheme.typography.titleMedium)
                    Text("Open a service and set when it was last done to start getting reminders.", color = c.muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            items(upNext, key = { it.type.id }) { status ->
                ServiceCard(status, unit) { onOpenService(status.type.id) }
            }
        }

        item { NativeAdCard() }

        if (economy.average != null) {
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Outlined.LocalGasStation, c.ok, c.okSoft)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Fuel economy", color = c.muted, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${Formats.decimal(economy.average)} $unit/${settings.volumeUnit}",
                                color = c.text, style = MaterialTheme.typography.titleLarge,
                            )
                        }
                        economy.last?.let {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Last fill", color = c.muted, style = MaterialTheme.typography.bodySmall)
                                Text(Formats.decimal(it), color = c.text, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }

        if (settings.launchCount >= 4 && !settings.ratePromptDone && !rateDismissed) {
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Outlined.StarOutline, c.accentText, c.accentSoft)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Enjoying OdoMate?", color = c.text, style = MaterialTheme.typography.titleMedium)
                            Text("A quick rating helps other drivers find it.", color = c.muted, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { rateDismissed = true }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Dismiss", tint = c.muted)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton("Rate 5 stars", { vm.markRated(); context.openPlayStore() }, Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (showReadingDialog) {
        ReadingDialog(
            lastValue = snap.currentOdometer,
            unit = unit,
            onSave = { value ->
                scope.launch {
                    vm.saveReading(snap.vehicle.id, value)
                    ads.showInterstitial(context.findActivity()) {
                        message("Odometer saved: ${Formats.distance(value, unit)}")
                    }
                }
            },
            onDismiss = { showReadingDialog = false },
        )
    }
}

@Composable
private fun HomeHeader(
    vehicles: List<Vehicle>,
    current: Vehicle,
    alertCount: Int,
    onSwitch: (Long) -> Unit,
    onManage: () -> Unit,
    onAdd: () -> Unit,
    onBell: () -> Unit,
) {
    val c = AppTheme.colors
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(top = 16.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { menu = true }
                    .padding(vertical = 4.dp)
            ) {
                Text("MY GARAGE", style = Type.overline, color = c.muted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(current.name, style = MaterialTheme.typography.titleLarge, color = c.text, maxLines = 1)
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Switch vehicle", tint = c.text)
                }
                if (current.subtitle.isNotBlank() && current.subtitle != current.name) {
                    Text(current.subtitle, style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 1)
                }
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                vehicles.forEach { v ->
                    DropdownMenuItem(
                        text = { Text(v.name, color = if (v.id == current.id) c.accentText else c.text, fontWeight = if (v.id == current.id) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = { Icon(Icons.Outlined.DirectionsCar, null, tint = c.muted) },
                        onClick = { menu = false; onSwitch(v.id) },
                    )
                }
                HorizontalDivider(color = c.divider)
                DropdownMenuItem(
                    text = { Text("Add vehicle", color = c.text) },
                    leadingIcon = { Icon(Icons.Outlined.Add, null, tint = c.muted) },
                    onClick = { menu = false; onAdd() },
                )
                DropdownMenuItem(
                    text = { Text("Manage garage", color = c.text) },
                    leadingIcon = { Icon(Icons.Outlined.Garage, null, tint = c.muted) },
                    onClick = { menu = false; onManage() },
                )
            }
        }
        Box {
            IconButton(
                onClick = onBell,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.surface),
            ) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Services needing attention: $alertCount", tint = c.text)
            }
            if (alertCount > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 9.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(c.danger)
                )
            }
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    val c = AppTheme.colors
    AppCard(modifier = modifier, onClick = onClick, padding = PaddingValues(12.dp)) {
        Text(value, color = color, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
        Text(label, color = c.muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val c = AppTheme.colors
    AppCard(modifier = modifier, onClick = onClick, padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, c.accentText, c.accentSoft, 34.dp)
            Spacer(Modifier.width(10.dp))
            Text(label, color = c.text, style = MaterialTheme.typography.titleSmall)
        }
    }
}
