package com.inkformlabs.pdfsigner.ui.home

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inkformlabs.pdfsigner.ads.BannerAd
import com.inkformlabs.pdfsigner.ui.LocalAppContainer
import com.inkformlabs.pdfsigner.ui.components.BusyOverlay
import com.inkformlabs.pdfsigner.ui.exit.ExitDialog
import com.inkformlabs.pdfsigner.ui.nav.AppViewModel
import com.inkformlabs.pdfsigner.ui.nav.MainTab
import com.inkformlabs.pdfsigner.ui.nav.Route
import com.inkformlabs.pdfsigner.ui.profile.ProfileTab
import com.inkformlabs.pdfsigner.ui.settings.SettingsTab
import com.inkformlabs.pdfsigner.ui.theme.InkIcons
import com.inkformlabs.pdfsigner.ui.theme.LocalInk

private data class TabSpec(val tab: MainTab, val label: String, val icon: ImageVector)

private val Tabs = listOf(
    TabSpec(MainTab.Documents, "Documents", InkIcons.Home),
    TabSpec(MainTab.Signatures, "Signatures", InkIcons.Signature),
    TabSpec(MainTab.Profile, "My details", InkIcons.User),
    TabSpec(MainTab.Settings, "Settings", InkIcons.Settings),
)

@Composable
fun MainScreen(appViewModel: AppViewModel, onExitApp: () -> Unit) {
    val p = LocalInk.current
    val container = LocalAppContainer.current
    val activity = LocalActivity.current
    val documentsViewModel: DocumentsViewModel = viewModel(factory = DocumentsViewModel.factory(container))
    val allDocuments by documentsViewModel.documents.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showExit by rememberSaveable { mutableStateOf(false) }

    // Back: go to the Documents tab first, then ask before leaving the app.
    BackHandler {
        if (appViewModel.tab != MainTab.Documents) appViewModel.tab = MainTab.Documents else showExit = true
    }

    // PDFs shared with / opened in Inkform from other apps.
    val incoming = appViewModel.incomingPdf
    LaunchedEffect(incoming) {
        if (incoming != null) {
            appViewModel.incomingPdf = null
            appViewModel.tab = MainTab.Documents
            documentsViewModel.importPdf(incoming) { id -> appViewModel.push(Route.Editor(id)) }
        }
    }

    val message = documentsViewModel.message
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            documentsViewModel.message = null
        }
    }

    val openEditor: (String) -> Unit = { id -> appViewModel.push(Route.Editor(id)) }
    // Converting photos/scans is a major task: a natural break for an (rate-limited) interstitial.
    val openAfterCreate: (String) -> Unit = { id ->
        container.ads.showInterstitialThen(activity) { appViewModel.push(Route.Editor(id)) }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = p.bg,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                Column {
                    NavigationBar(containerColor = p.surface, windowInsets = WindowInsets(0, 0, 0, 0)) {
                        Tabs.forEach { spec ->
                            NavigationBarItem(
                                selected = appViewModel.tab == spec.tab,
                                onClick = { appViewModel.tab = spec.tab },
                                icon = { Icon(spec.icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
                                label = { Text(spec.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = p.onAccent,
                                    selectedTextColor = p.ink,
                                    indicatorColor = p.accent,
                                    unselectedIconColor = p.muted,
                                    unselectedTextColor = p.muted,
                                ),
                            )
                        }
                    }
                    BannerAd()
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            },
        ) { padding ->
            AnimatedContent(
                targetState = appViewModel.tab,
                transitionSpec = {
                    (fadeIn(tween(260)) + slideInVertically(tween(320)) { it / 18 }) togetherWith fadeOut(tween(160))
                },
                modifier = Modifier.padding(padding),
                label = "tabs",
            ) { tab ->
                when (tab) {
                    MainTab.Documents -> DocumentsTab(
                        viewModel = documentsViewModel,
                        onOpenDocument = openEditor,
                        onCreatedFromImages = openAfterCreate,
                        onOpenSignatures = { appViewModel.tab = MainTab.Signatures },
                    )

                    MainTab.Signatures -> SignaturesTab(
                        onCreateSignature = { appViewModel.push(Route.SignaturePad(forEditor = false)) },
                        onCreateInitials = { appViewModel.push(Route.SignaturePad(forEditor = false, initials = true)) },
                    )

                    MainTab.Profile -> ProfileTab(onSaved = { documentsViewModel.message = it })
                    MainTab.Settings -> SettingsTab(onMessage = { documentsViewModel.message = it })
                }
            }
        }

        documentsViewModel.busyMessage?.let { BusyOverlay(it) }
    }

    if (showExit) {
        ExitDialog(
            documentCount = allDocuments.size,
            onStay = { showExit = false },
            onExit = {
                showExit = false
                onExitApp()
            },
        )
    }
}
