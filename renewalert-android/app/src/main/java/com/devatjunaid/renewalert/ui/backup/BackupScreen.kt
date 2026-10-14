package com.devatjunaid.renewalert.ui.backup

import android.net.Uri
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.devatjunaid.renewalert.ads.AdPlacement
import com.devatjunaid.renewalert.backup.NotABackupException
import com.devatjunaid.renewalert.backup.WrongPasswordException
import com.devatjunaid.renewalert.security.LockGate
import com.devatjunaid.renewalert.ui.AppViewModel
import com.devatjunaid.renewalert.ui.components.SectionCard
import com.devatjunaid.renewalert.ui.components.SectionLabel
import com.devatjunaid.renewalert.ui.components.SettingRow
import com.devatjunaid.renewalert.ui.theme.RenewAlertTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

private data class Status(val ok: Boolean, val text: String)

@Composable
fun BackupScreen(vm: AppViewModel, onBack: () -> Unit, afterAction: (AdPlacement, () -> Unit) -> Unit) {
    val c = RenewAlertTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<Status?>(null) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    val passwordOk = password.length >= 6

    fun run(placement: AdPlacement, block: suspend () -> Result<String>) {
        busy = true
        scope.launch {
            val r = block()
            busy = false
            status = r.fold(
                onSuccess = { Status(true, it) },
                onFailure = { e ->
                    Status(
                        false,
                        when (e) {
                            is WrongPasswordException -> "Wrong password, or the file is damaged."
                            is NotABackupException -> "That file isn't a Renew Alert backup."
                            else -> e.message ?: "Something went wrong."
                        },
                    )
                },
            )
            if (r.isSuccess) afterAction(placement) {}
        }
    }

    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        LockGate.externalUiOpen = false
        if (uri != null) run(AdPlacement.BACKUP_DONE) { vm.createBackup(context, uri, password.toCharArray()) }
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        LockGate.externalUiOpen = false
        restoreUri = uri
    }
    val createCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        LockGate.externalUiOpen = false
        if (uri != null) run(AdPlacement.EXPORT_DONE) { vm.exportCsv(context, uri) }
    }
    val createPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        LockGate.externalUiOpen = false
        if (uri != null) run(AdPlacement.EXPORT_DONE) { vm.exportPdf(context, uri) }
    }
    val openCsv = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        LockGate.externalUiOpen = false
        if (uri != null) run(AdPlacement.EXPORT_DONE) { vm.importCsv(context, uri) }
    }
    val stamp = LocalDate.now().toString()

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            Text("Backup & export", style = MaterialTheme.typography.headlineSmall, color = c.onSurface)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Your data only leaves this phone when you save a file yourself. You choose where it goes.",
                style = MaterialTheme.typography.bodyMedium, color = c.muted, modifier = Modifier.padding(horizontal = 4.dp),
            )

            status?.let { st ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(if (st.ok) c.successContainer else c.warnContainer).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = if (st.ok) c.successText else c.warnText)
                    Spacer(Modifier.width(10.dp))
                    Text(st.text, style = MaterialTheme.typography.bodyMedium, color = if (st.ok) c.successText else c.warnText)
                }
            }

            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(c.accentSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = c.onAccentSoft)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Encrypted backup", style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                        Text("AES-256 · locked with a password you choose", style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it.take(64) },
                    label = { Text("Backup password") },
                    supportingText = { Text("At least 6 characters. It can't be recovered, so keep it safe.") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = if (showPassword) "Hide password" else "Show password")
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { LockGate.externalUiOpen = true; createBackup.launch("renewalert-backup-$stamp.renewalert") },
                    enabled = passwordOk && !busy,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent),
                ) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save backup file…")
                }
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { LockGate.externalUiOpen = true; openBackup.launch(arrayOf("*/*")) },
                    enabled = passwordOk && !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Restore, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Restore from a backup file…")
                }
            }

            SectionLabel("Export & import")
            SectionCard(padding = PaddingValues(0.dp)) {
                SettingRow("Spreadsheet (CSV)", "Not encrypted · opens in Excel or Sheets", Icons.Rounded.TableChart) {
                    LockGate.externalUiOpen = true
                    createCsv.launch("renewalert-subscriptions-$stamp.csv")
                }
                SettingRow("Report (PDF)", "Totals, categories and every subscription", Icons.Rounded.PictureAsPdf, showDivider = true) {
                    LockGate.externalUiOpen = true
                    createPdf.launch("renewalert-report-$stamp.pdf")
                }
                SettingRow("Import from CSV", "Needs 'name' and 'price' columns", Icons.Rounded.FileUpload, showDivider = true) {
                    LockGate.externalUiOpen = true
                    openCsv.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/vnd.ms-excel"))
                }
            }
            if (busy) {
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    restoreUri?.let { uri ->
        var replace by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { restoreUri = null },
            title = { Text("Restore backup") },
            text = {
                Column {
                    Text("Use the password this backup was created with.")
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = replace, onClick = { replace = true })
                        Text("Replace current data")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !replace, onClick = { replace = false })
                        Text("Add to current data")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    restoreUri = null
                    run(AdPlacement.BACKUP_DONE) { vm.restoreBackup(context, uri, password.toCharArray(), replace) }
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { restoreUri = null }) { Text("Cancel") } },
        )
    }
}
