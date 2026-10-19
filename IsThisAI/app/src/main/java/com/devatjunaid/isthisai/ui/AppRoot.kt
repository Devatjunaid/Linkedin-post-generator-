package com.devatjunaid.isthisai.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devatjunaid.isthisai.ads.AdsManager
import com.devatjunaid.isthisai.ads.BannerAdBar
import com.devatjunaid.isthisai.ads.Interstitials
import com.devatjunaid.isthisai.engine.ReportJson
import com.devatjunaid.isthisai.engine.ScanReport
import com.devatjunaid.isthisai.ui.screens.AboutScreen
import com.devatjunaid.isthisai.ui.screens.ExitDialog
import com.devatjunaid.isthisai.ui.screens.HistoryScreen
import com.devatjunaid.isthisai.ui.screens.HomeScreen
import com.devatjunaid.isthisai.ui.screens.ResultScreen
import com.devatjunaid.isthisai.ui.screens.ScanScreen
import com.devatjunaid.isthisai.ui.screens.SplashScreen
import com.devatjunaid.isthisai.ui.screens.TrailScreen
import com.devatjunaid.isthisai.ui.theme.Ink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppRoot(vm: MainViewModel) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()

    var showSplash by rememberSaveable { mutableStateOf(true) }
    var showExit by remember { mutableStateOf(false) }
    var reportToSave by remember { mutableStateOf<ScanReport?>(null) }

    val recent by vm.recent.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val privacyRequired by AdsManager.privacyOptionsRequired.collectAsStateWithLifecycle()

    // The system photo picker needs no storage permission (Google Play's photo policy).
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(vm::startScan)
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::startScan)
    }
    val saveDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val report = reportToSave
        reportToSave = null
        if (uri != null && report != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(ReportJson.encode(report, pretty = true).toByteArray()) }
                    }.isSuccess
                }
                toast(context, if (ok) "Report saved" else "Couldn't save the report")
                if (ok && activity != null) Interstitials.showThen(activity) {}
            }
        }
    }

    fun pickPhoto() = photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    fun pickFile() = filePicker.launch(arrayOf("image/*"))

    fun paste() {
        val cm = context.getSystemService(ClipboardManager::class.java)
        val clip = cm?.primaryClip
        val uri = clip?.let { c -> (0 until c.itemCount).firstNotNullOfOrNull { c.getItemAt(it).uri } }
        val type = uri?.let { context.contentResolver.getType(it) }
        if (uri != null && (type == null || type.startsWith("image/"))) {
            vm.startScan(uri)
        } else {
            toast(context, "No image on the clipboard. Copy an image first, or use Files.")
        }
    }

    fun openSaved(id: Long) {
        if (activity != null) Interstitials.showThen(activity) { vm.navigate(Route.Result(id)) } else vm.navigate(Route.Result(id))
    }

    fun share(report: ScanReport) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Is This AI? report")
            putExtra(Intent.EXTRA_TEXT, shareText(report))
        }
        context.startActivity(Intent.createChooser(send, "Share report"))
    }

    fun copy(report: ScanReport) {
        val cm = context.getSystemService(ClipboardManager::class.java) ?: return
        cm.setPrimaryClip(ClipData.newPlainText("Is This AI? report", ReportJson.encode(report, pretty = true)))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) toast(context, "Report copied")
    }

    // A file shared into the app opens as soon as the splash has finished.
    LaunchedEffect(showSplash, vm.pendingUri) {
        val pending: Uri? = vm.pendingUri
        if (!showSplash && pending != null) {
            vm.pendingUri = null
            vm.startScan(pending)
        }
    }

    BackHandler(enabled = !showSplash) {
        if (!vm.back()) showExit = true
    }

    Box(Modifier.fillMaxSize().background(Ink)) {
        if (showSplash) {
            SplashScreen(onFinished = { showSplash = false })
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).statusBarsPadding()) {
                    AnimatedContent(
                        targetState = vm.current,
                        contentKey = { it.key },
                        transitionSpec = {
                            val from = initialState
                            val to = targetState
                            when {
                                // The scan screen opens through its own iris.
                                to is Route.Scan -> fadeIn(tween(120)) togetherWith fadeOut(tween(260))
                                // The scanline "collapses" into the stamp.
                                from is Route.Scan && to is Route.Result ->
                                    (fadeIn(tween(320)) + scaleIn(tween(520), initialScale = 1.12f)) togetherWith
                                        (fadeOut(tween(260)) + scaleOut(tween(320), targetScale = 0.6f))
                                // Evidence card expands into the trail sheet.
                                to is Route.Trail -> fadeIn(tween(200)) togetherWith (fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.94f))
                                vm.forward ->
                                    (slideInHorizontally(tween(420)) { it / 3 } + fadeIn(tween(320))) togetherWith
                                        (slideOutHorizontally(tween(420)) { -it / 5 } + fadeOut(tween(260)))
                                else ->
                                    (slideInHorizontally(tween(420)) { -it / 3 } + fadeIn(tween(320))) togetherWith
                                        (slideOutHorizontally(tween(420)) { it / 5 } + fadeOut(tween(260)))
                            }
                        },
                        label = "nav",
                    ) { route ->
                        when (route) {
                            Route.Home -> HomeScreen(
                                recent = recent,
                                onPickPhoto = ::pickPhoto,
                                onPickFile = ::pickFile,
                                onPaste = ::paste,
                                onOpenScan = ::openSaved,
                                onHistory = { vm.navigate(Route.History) },
                                onAbout = { vm.navigate(Route.About) },
                            )
                            Route.Scan -> ScanScreen(
                                state = vm.scan,
                                onCancel = { vm.back() },
                                onRetry = { vm.back(); pickPhoto() },
                                onFinished = {
                                    scope.launch {
                                        val id = vm.saveFinishedScan() ?: return@launch
                                        val go = { if (vm.current == Route.Scan) vm.replaceTop(Route.Result(id)) }
                                        if (activity != null) Interstitials.showThen(activity, go) else go()
                                    }
                                },
                            )
                            is Route.Result -> {
                                LaunchedEffect(route.id) { vm.load(route.id) }
                                val open = vm.open?.takeIf { it.id == route.id }
                                ResultScreen(
                                    scan = open,
                                    onBack = { vm.back() },
                                    onShare = { open?.let { share(it.report) } },
                                    onTrail = { vm.navigate(Route.Trail(route.id)) },
                                    onScanAnother = { vm.home(); pickPhoto() },
                                )
                            }
                            is Route.Trail -> {
                                LaunchedEffect(route.id) { vm.load(route.id) }
                                val open = vm.open?.takeIf { it.id == route.id }
                                TrailScreen(
                                    scan = open,
                                    onBack = { vm.back() },
                                    onCopy = { open?.let { copy(it.report) } },
                                    onSave = {
                                        open?.let {
                                            reportToSave = it.report
                                            saveDocument.launch("is-this-ai-" + it.report.fileName.substringBeforeLast('.') + ".json")
                                        }
                                    },
                                    onShare = { open?.let { share(it.report) } },
                                )
                            }
                            Route.History -> HistoryScreen(
                                scans = history,
                                onBack = { vm.back() },
                                onOpen = ::openSaved,
                                onDelete = vm::delete,
                                onClear = vm::clearHistory,
                                onScan = ::pickPhoto,
                            )
                            Route.About -> AboutScreen(
                                onBack = { vm.back() },
                                privacyOptionsRequired = privacyRequired,
                                onPrivacyOptions = { activity?.let(AdsManager::showPrivacyOptions) },
                            )
                        }
                    }
                }
                BannerAdBar()
                Spacer(Modifier.fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }

    if (showExit) {
        ExitDialog(
            onStay = { showExit = false },
            onExit = {
                showExit = false
                activity?.finish()
            },
        )
    }
}

private fun toast(context: Context, text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
