package com.unpin.photoscrub.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unpin.photoscrub.AppViewModel
import com.unpin.photoscrub.Screen
import com.unpin.photoscrub.UnpinApp
import com.unpin.photoscrub.ads.BannerAdBar
import com.unpin.photoscrub.data.Permissions
import com.unpin.photoscrub.data.ScrubSession
import com.unpin.photoscrub.ui.components.LiveLogoMark
import com.unpin.photoscrub.ui.screens.BatchScreen
import com.unpin.photoscrub.ui.screens.ExitDialog
import com.unpin.photoscrub.ui.screens.HistoryScreen
import com.unpin.photoscrub.ui.screens.HomeScreen
import com.unpin.photoscrub.ui.screens.InspectScreen
import com.unpin.photoscrub.ui.screens.ReportScreen
import com.unpin.photoscrub.ui.screens.SettingsScreen
import com.unpin.photoscrub.ui.screens.SplashScreen
import com.unpin.photoscrub.ui.theme.Card
import com.unpin.photoscrub.ui.theme.Ink
import com.unpin.photoscrub.ui.theme.Line
import com.unpin.photoscrub.ui.theme.Paper
import com.unpin.photoscrub.ui.theme.Signal
import com.unpin.photoscrub.ui.theme.UnpinText

/** Everything a screen can ask the host for: pickers, permissions, ads, intents. */
class UnpinActions(
    val pickForBatch: () -> Unit,
    val pickForInspect: () -> Unit,
    val requestMediaLocation: () -> Unit,
    val runBatch: () -> Unit,
    val scrubInspected: () -> Unit,
    val share: (List<Uri>) -> Unit,
    val view: (Uri) -> Unit,
    val openMaps: (Double, Double) -> Unit,
    val openUrl: (String) -> Unit,
    val openAppSettings: () -> Unit,
    val showPrivacyOptions: () -> Unit,
)

private const val MAX_PICK = 50

private enum class PendingPick { NONE, BATCH, INSPECT }

@Composable
fun UnpinRoot(vm: AppViewModel) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val ads = (context.applicationContext as UnpinApp).ads
    val consentDone by ads.consentFlowDone.collectAsStateWithLifecycle()
    var showExit by rememberSaveable { mutableStateOf(false) }
    var pendingPick by remember { mutableStateOf(PendingPick.NONE) }
    var pendingAfterWrite by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Status bar icons: light on the ink splash, dark on paper screens.
    val view = LocalView.current
    val window = activity?.window
    SideEffect {
        if (window != null) {
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = vm.splashDone
                isAppearanceLightNavigationBars = vm.splashDone
            }
        }
    }

    val pickMany = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK)) { uris ->
        if (uris.isNotEmpty()) vm.addToBatch(uris)
    }
    val pickOne = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.inspect(uri)
    }
    val imagesOnly = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

    fun launchPicker(which: PendingPick) {
        when (which) {
            PendingPick.BATCH -> pickMany.launch(imagesOnly)
            PendingPick.INSPECT -> pickOne.launch(imagesOnly)
            PendingPick.NONE -> Unit
        }
    }

    val mediaLocationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.askedMediaLocation = true
        val next = pendingPick
        pendingPick = PendingPick.NONE
        if (next == PendingPick.NONE) {
            if (granted) vm.reloadInspected()
        } else {
            launchPicker(next)
        }
    }
    val writeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val next = pendingAfterWrite
        pendingAfterWrite = null
        if (granted) next?.invoke()
        else Toast.makeText(context, "Unpin needs storage access to save clean copies on this Android version.", Toast.LENGTH_LONG).show()
    }

    fun pick(which: PendingPick) {
        val perm = Permissions.mediaLocation
        if (perm != null && !Permissions.hasMediaLocation(context) && !vm.askedMediaLocation) {
            pendingPick = which
            mediaLocationLauncher.launch(perm)
        } else {
            launchPicker(which)
        }
    }

    fun withWriteAccess(block: () -> Unit) {
        val perm = Permissions.legacyWrite
        if (perm == null || Permissions.hasLegacyWrite(context)) block()
        else {
            pendingAfterWrite = block
            writeLauncher.launch(perm)
        }
    }

    // Interstitial only at a natural break: after a clean-up finished, before its report.
    fun finished(session: ScrubSession) {
        val host = activity
        val inForeground = (host as? LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true
        if (host == null || host.isFinishing || !inForeground) vm.openReport(session)
        else ads.interstitial.showIfAllowed(host) { vm.openReport(session) }
    }

    val actions = remember(vm, activity) {
        UnpinActions(
            pickForBatch = { pick(PendingPick.BATCH) },
            pickForInspect = { pick(PendingPick.INSPECT) },
            requestMediaLocation = {
                Permissions.mediaLocation?.let { pendingPick = PendingPick.NONE; mediaLocationLauncher.launch(it) }
            },
            runBatch = { withWriteAccess { vm.runBatch(::finished) } },
            scrubInspected = { withWriteAccess { vm.scrubInspected(::finished) } },
            share = { uris -> shareUris(context, uris) },
            view = { uri -> startSafely(context, Intent(Intent.ACTION_VIEW).setDataAndType(uri, "image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) },
            openMaps = { lat, lon -> startSafely(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon(Photo%20location)"))) },
            openUrl = { url -> startSafely(context, Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
            openAppSettings = {
                startSafely(context, Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
            },
            showPrivacyOptions = { activity?.let { a -> ads.showPrivacyOptions(a) {} } },
        )
    }

    // Quick share mode hands clean copies straight back to the share sheet.
    val pendingShare = vm.pendingShare
    LaunchedEffect(pendingShare) {
        if (pendingShare != null) {
            shareUris(context, pendingShare)
            vm.consumePendingShare()
        }
    }

    BackHandler(enabled = vm.splashDone && vm.progress == null) {
        if (!vm.back()) showExit = true
    }

    Box(Modifier.fillMaxSize().background(if (vm.splashDone) Paper else Ink)) {
        if (!vm.splashDone) {
            SplashScreen(consentDone = consentDone, onFinished = { vm.splashDone = true })
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = vm.current,
                        transitionSpec = {
                            val forward = targetState.ordinal >= initialState.ordinal
                            (slideInHorizontally { w -> if (forward) w / 6 else -w / 6 } + fadeIn()) togetherWith
                                (slideOutHorizontally { w -> if (forward) -w / 6 else w / 6 } + fadeOut())
                        },
                        label = "screens",
                    ) { screen ->
                        when (screen) {
                            Screen.HOME -> HomeScreen(vm, actions)
                            Screen.INSPECT -> InspectScreen(vm, actions)
                            Screen.BATCH -> BatchScreen(vm, actions)
                            Screen.REPORT -> ReportScreen(vm, actions)
                            Screen.HISTORY -> HistoryScreen(vm, actions)
                            Screen.SETTINGS -> SettingsScreen(vm, actions)
                        }
                    }
                }
                BannerAdBar()
                Spacer(Modifier.fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars).background(Paper))
            }
        }

        val progress = vm.progress
        AnimatedVisibility(progress != null, enter = fadeIn(), exit = fadeOut()) {
            ProcessingOverlay(progress?.label ?: "", if (progress == null || progress.total == 0) 0f else progress.done / progress.total.toFloat())
        }
    }

    if (showExit) {
        ExitDialog(
            cleaned = vm.totalCleaned,
            locations = vm.totalLocations,
            onStay = { showExit = false },
            onExit = {
                showExit = false
                activity?.finish()
            },
        )
    }
}

@Composable
private fun ProcessingOverlay(label: String, fraction: Float) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Ink.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Card)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LiveLogoMark(Modifier.size(88.dp))
            Spacer(Modifier.height(16.dp))
            Text(label, style = UnpinText.section, color = Ink)
            Spacer(Modifier.height(4.dp))
            Text("Everything happens on this phone", style = UnpinText.small, color = com.unpin.photoscrub.ui.theme.Muted)
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Signal,
                trackColor = Line,
            )
        }
    }
}

private fun shareUris(context: Context, uris: List<Uri>) {
    if (uris.isEmpty()) return
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).setType("image/*").putExtra(Intent.EXTRA_STREAM, uris.first())
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).setType("image/*").putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    val clip = android.content.ClipData.newRawUri(null, uris.first())
    uris.drop(1).forEach { clip.addItem(android.content.ClipData.Item(it)) }
    intent.clipData = clip
    startSafely(context, Intent.createChooser(intent, "Share clean copies"))
}

private fun startSafely(context: Context, intent: Intent) {
    try {
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app found to open this", Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, "That app can't open this", Toast.LENGTH_SHORT).show()
    }
}
