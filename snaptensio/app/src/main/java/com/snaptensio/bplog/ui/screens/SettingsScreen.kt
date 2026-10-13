package com.snaptensio.bplog.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snaptensio.bplog.BuildConfig
import com.snaptensio.bplog.R
import com.snaptensio.bplog.ads.NativeAdCard
import com.snaptensio.bplog.appContainer
import com.snaptensio.bplog.data.AppSettings
import com.snaptensio.bplog.data.Guideline
import com.snaptensio.bplog.data.Profile
import com.snaptensio.bplog.data.ThemeMode
import com.snaptensio.bplog.export.Exporters
import com.snaptensio.bplog.ui.MainViewModel
import com.snaptensio.bplog.ui.components.ScreenHeader
import com.snaptensio.bplog.ui.components.SectionCard
import com.snaptensio.bplog.ui.components.Segmented
import com.snaptensio.bplog.ui.components.SmallLabel
import com.snaptensio.bplog.ui.findActivity
import kotlinx.coroutines.launch

/** Replace with the page where you host PRIVACY_POLICY.md before publishing (Play requires a public URL). */
const val PRIVACY_POLICY_URL = "https://devatjunaid.github.io/snaptensio/privacy"

private val profileColors = listOf(
    Color(0xFF1F4FD8), Color(0xFFE07A2B), Color(0xFF1E8E5A), Color(0xFF8E44AD), Color(0xFFC2272D), Color(0xFF00838F),
)

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit, onImported: () -> Unit) {
    val context = LocalContext.current
    val ads = context.appContainer.ads
    val settings by vm.settings.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val active by vm.activeProfile.collectAsStateWithLifecycle()
    val s = settings ?: AppSettings()
    var editingProfile by remember { mutableStateOf<Profile?>(null) }
    var editNumber by rememberSaveable { mutableStateOf<String?>(null) } // "emergency" | "doctor"
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            message = vm.importCsv(uri)
            onImported()
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Settings", onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallLabel("People on this phone")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                profiles.take(3).forEach { p ->
                    val selected = p.id == active?.id
                    Surface(
                        onClick = { if (selected) editingProfile = p else vm.switchProfile(p.id) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f).border(
                            if (selected) 2.dp else 1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp),
                        ),
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).clip(CircleShape).background(profileColors[p.colorIndex % profileColors.size]), contentAlignment = Alignment.Center) {
                                Text(p.name.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.labelLarge)
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(p.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                Text("${p.targetSystolic}/${p.targetDiastolic}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (profiles.size < 3) {
                    Surface(
                        onClick = { editingProfile = Profile(name = "") },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(56.dp),
                    ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, contentDescription = "Add person") } }
                }
            }
            if (profiles.size > 3) {
                profiles.drop(3).forEach { p ->
                    TextButton(onClick = { vm.switchProfile(p.id) }) { Text(p.name) }
                }
            }
            Text("Tap the selected person to edit their name and target.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            SmallLabel("Measuring")
            SectionCard(padding = 12) {
                Text("Guideline for ranges", style = MaterialTheme.typography.titleSmall)
                Segmented(Guideline.entries, s.guideline, { it.label }, { vm.setGuideline(it) }, Modifier.fillMaxWidth())
                SettingRow("Warn me on very high readings", "Shows safety steps for readings over 180/120") {
                    Switch(s.crisisAlerts, { vm.setCrisisAlerts(it) })
                }
                SettingRow("Emergency number", s.emergencyNumber) { TextButton(onClick = { editNumber = "emergency" }) { Text("Change") } }
                SettingRow("Doctor's phone", s.doctorNumber.ifBlank { "Not set" }) { TextButton(onClick = { editNumber = "doctor" }) { Text(if (s.doctorNumber.isBlank()) "Add" else "Change") } }
            }

            SmallLabel("Your data")
            SectionCard(padding = 12) {
                SettingRow("Export all readings", "CSV file you can keep as a backup or open in a spreadsheet") {
                    TextButton(onClick = { scope.launch { vm.buildCsv()?.let { Exporters.share(context, it, "text/csv", "SnapTensio backup") } } }) { Text("Export") }
                }
                SettingRow("Import from CSV", "From a SnapTensio backup or another app's export") {
                    TextButton(onClick = { importer.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel")) }) { Text("Import") }
                }
                message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }

            SmallLabel("App")
            SectionCard(padding = 12) {
                Text("Theme", style = MaterialTheme.typography.titleSmall)
                Segmented(ThemeMode.entries, s.theme, { it.label }, { vm.setTheme(it) }, Modifier.fillMaxWidth())
                if (ads.privacyOptionsRequired) {
                    SettingRow("Ad privacy choices", "Change your consent for personalised ads") {
                        TextButton(onClick = { context.findActivity()?.let { ads.showPrivacyOptions(it) } }) { Text("Open") }
                    }
                }
                SettingRow("Privacy policy", "How SnapTensio handles your data") {
                    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))) }) { Text("View") }
                }
            }

            NativeAdCard(Modifier.padding(vertical = 6.dp))

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("No account. Readings never leave this phone unless you export them. Ads never receive your health data.", style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("SnapTensio ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(4.dp))
            Spacer(Modifier.height(8.dp))
        }
    }

    editingProfile?.let { p ->
        ProfileDialog(
            initial = p,
            canDelete = p.id != 0L && profiles.size > 1,
            onDismiss = { editingProfile = null },
            onSave = { vm.saveProfile(it); editingProfile = null },
            onDelete = { vm.deleteProfile(p); editingProfile = null },
        )
    }
    editNumber?.let { which ->
        var value by rememberSaveable(which) { mutableStateOf(if (which == "emergency") s.emergencyNumber else s.doctorNumber) }
        AlertDialog(
            onDismissRequest = { editNumber = null },
            title = { Text(if (which == "emergency") "Emergency number" else "Doctor's phone") },
            text = {
                OutlinedTextField(
                    value, { v -> value = v.filter { it.isDigit() || it in "+ -()" }.take(20) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (which == "emergency") { if (value.isNotBlank()) vm.setEmergencyNumber(value) } else vm.setDoctorNumber(value)
                    editNumber = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editNumber = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String?, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Composable
private fun ProfileDialog(initial: Profile, canDelete: Boolean, onDismiss: () -> Unit, onSave: (Profile) -> Unit, onDelete: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var sys by rememberSaveable { mutableStateOf(initial.targetSystolic.toString()) }
    var dia by rememberSaveable { mutableStateOf(initial.targetDiastolic.toString()) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val sysV = sys.toIntOrNull(); val diaV = dia.toIntOrNull()
    val ok = name.isNotBlank() && sysV != null && diaV != null && sysV in 90..200 && diaV in 50..130 && sysV > diaV
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Add person" else "Edit ${initial.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it.take(24) }, label = { Text("Name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(sys, { sys = it.filter(Char::isDigit).take(3) }, label = { Text("Target SYS") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(dia, { dia = it.filter(Char::isDigit).take(3) }, label = { Text("Target DIA") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                if (canDelete) {
                    TextButton(onClick = { confirmDelete = true }) { Text("Delete ${initial.name} and all their readings", color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = { TextButton(enabled = ok, onClick = { onSave(initial.copy(name = name.trim(), targetSystolic = sysV!!, targetDiastolic = diaV!!)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${initial.name}?") },
            text = { Text("All of their readings and medications will be removed from this phone. This can't be undone.") },
            confirmButton = { TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
