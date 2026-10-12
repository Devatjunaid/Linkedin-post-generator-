package app.gigometer.mileage.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import app.gigometer.mileage.R
import app.gigometer.mileage.tracking.Permissions

private enum class Step { NONE, LOCATION_DISCLOSURE, BACKGROUND_DISCLOSURE, DENIED }

class PermissionController internal constructor(
    val startAutoSetup: () -> Unit,
    val startManual: (onReady: () -> Unit) -> Unit,
)

/**
 * Walks the user through the permissions automatic tracking needs, with the
 * "prominent disclosure" Google Play requires before asking for location.
 */
@Composable
fun rememberPermissionController(onFinished: () -> Unit): PermissionController {
    val ctx = LocalContext.current
    var step by remember { mutableStateOf(Step.NONE) }
    var manualCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    var autoFlow by remember { mutableStateOf(false) }

    lateinit var next: () -> Unit

    val bgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { next() }
    val activityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { next() }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { next() }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (!Permissions.hasLocation(ctx)) step = Step.DENIED else next()
    }

    next = {
        when {
            !Permissions.hasLocation(ctx) -> step = Step.LOCATION_DISCLOSURE
            Build.VERSION.SDK_INT >= 33 && !Permissions.hasNotifications(ctx) && !asked(ctx, "notif") -> {
                markAsked(ctx, "notif"); notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            manualCallback != null -> { manualCallback?.invoke(); manualCallback = null; onFinished() }
            autoFlow && Build.VERSION.SDK_INT >= 29 && !Permissions.hasActivityRecognition(ctx) && !asked(ctx, "activity") -> {
                markAsked(ctx, "activity"); activityLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            autoFlow && !Permissions.hasBackgroundLocation(ctx) && !asked(ctx, "bg") -> step = Step.BACKGROUND_DISCLOSURE
            else -> { autoFlow = false; clearAsked(ctx); onFinished() }
        }
    }

    when (step) {
        Step.LOCATION_DISCLOSURE -> AlertDialog(
            onDismissRequest = { step = Step.NONE; manualCallback = null; autoFlow = false },
            title = { Text(stringResource(R.string.disclosure_title), fontWeight = FontWeight.ExtraBold) },
            text = { Text(stringResource(R.string.disclosure_location)) },
            confirmButton = {
                TextButton(onClick = {
                    step = Step.NONE
                    locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }) { Text(stringResource(R.string.continue_)) }
            },
            dismissButton = { TextButton(onClick = { step = Step.NONE; manualCallback = null; autoFlow = false }) { Text(stringResource(R.string.not_now)) } },
        )
        Step.BACKGROUND_DISCLOSURE -> AlertDialog(
            onDismissRequest = { step = Step.NONE; markAsked(ctx, "bg"); next() },
            title = { Text(stringResource(R.string.disclosure_bg_title), fontWeight = FontWeight.ExtraBold) },
            text = { Text(stringResource(R.string.disclosure_bg)) },
            confirmButton = {
                TextButton(onClick = {
                    step = Step.NONE; markAsked(ctx, "bg")
                    if (Build.VERSION.SDK_INT >= 29) bgLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) else next()
                }) { Text(stringResource(R.string.open_settings)) }
            },
            dismissButton = { TextButton(onClick = { step = Step.NONE; markAsked(ctx, "bg"); next() }) { Text(stringResource(R.string.not_now)) } },
        )
        Step.DENIED -> AlertDialog(
            onDismissRequest = { step = Step.NONE },
            title = { Text(stringResource(R.string.denied_title), fontWeight = FontWeight.ExtraBold) },
            text = { Text(stringResource(R.string.denied_text)) },
            confirmButton = {
                TextButton(onClick = { step = Step.NONE; openAppSettings(ctx) }) { Text(stringResource(R.string.go_to_settings)) }
            },
            dismissButton = { TextButton(onClick = { step = Step.NONE }) { Text(stringResource(R.string.not_now)) } },
        )
        Step.NONE -> Unit
    }

    return remember {
        PermissionController(
            startAutoSetup = { clearAsked(ctx); autoFlow = true; manualCallback = null; next() },
            startManual = { cb ->
                if (Permissions.hasLocation(ctx)) cb() else { manualCallback = cb; autoFlow = false; next() }
            },
        )
    }
}

fun openAppSettings(ctx: Context) {
    ctx.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

// "Asked during this setup run" flags so a denied permission doesn't loop forever.
private val askedSet = mutableSetOf<String>()
private fun asked(@Suppress("UNUSED_PARAMETER") ctx: Context, key: String) = key in askedSet
private fun markAsked(@Suppress("UNUSED_PARAMETER") ctx: Context, key: String) { askedSet += key }
private fun clearAsked(@Suppress("UNUSED_PARAMETER") ctx: Context) = askedSet.clear()
