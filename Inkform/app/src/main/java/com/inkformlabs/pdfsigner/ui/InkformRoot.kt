package com.inkformlabs.pdfsigner.ui

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.inkformlabs.pdfsigner.data.ThemeChoice
import com.inkformlabs.pdfsigner.ui.editor.EditorScreen
import com.inkformlabs.pdfsigner.ui.export.ExportScreen
import com.inkformlabs.pdfsigner.ui.home.MainScreen
import com.inkformlabs.pdfsigner.ui.nav.AppViewModel
import com.inkformlabs.pdfsigner.ui.nav.NavEntry
import com.inkformlabs.pdfsigner.ui.nav.Route
import com.inkformlabs.pdfsigner.ui.signature.SignaturePadScreen
import com.inkformlabs.pdfsigner.ui.splash.SplashScreen
import com.inkformlabs.pdfsigner.ui.theme.InkformTheme
import com.inkformlabs.pdfsigner.ui.theme.LocalInk

@Composable
fun InkformRoot(appViewModel: AppViewModel, onExitApp: () -> Unit) {
    val container = LocalAppContainer.current
    val theme by container.settings.theme.collectAsStateWithLifecycle(initialValue = ThemeChoice.CITRUS)

    InkformTheme(theme) {
        val palette = LocalInk.current
        val activity = LocalActivity.current as? ComponentActivity
        LaunchedEffect(palette.isDark) {
            val style = if (palette.isDark) {
                SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            } else {
                SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
            }
            activity?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        }

        BackHandler(enabled = appViewModel.stack.size > 1) { appViewModel.pop() }

        val saveableHolder = rememberSaveableStateHolder()
        Box(Modifier.fillMaxSize().background(palette.bg)) {
            AnimatedContent(
                targetState = appViewModel.top,
                contentKey = { it.id },
                transitionSpec = { transitionFor(initialState, targetState, appViewModel.lastNavigationWasPop) },
                label = "screens",
            ) { entry ->
                CompositionLocalProvider(LocalViewModelStoreOwner provides entry) {
                    saveableHolder.SaveableStateProvider(entry.id) {
                        when (val route = entry.route) {
                            Route.Splash -> SplashScreen(onFinished = { appViewModel.resetTo(Route.Main) })
                            Route.Main -> MainScreen(appViewModel = appViewModel, onExitApp = onExitApp)
                            is Route.Editor -> EditorScreen(documentId = route.documentId, appViewModel = appViewModel)
                            is Route.SignaturePad -> SignaturePadScreen(route = route, appViewModel = appViewModel)
                            is Route.Export -> ExportScreen(documentId = route.documentId, appViewModel = appViewModel)
                        }
                    }
                }
            }
        }
    }
}

/** Each destination has its own motion: editor pushes sideways, signature pad rises, export fades (it reveals itself). */
private fun transitionFor(from: NavEntry, to: NavEntry, isPop: Boolean): ContentTransform {
    val slow = tween<Float>(420, easing = FastOutSlowInEasing)
    if (from.route == Route.Splash) {
        return fadeIn(tween(500)) + scaleIn(tween(600, easing = FastOutSlowInEasing), initialScale = 0.94f) togetherWith
            fadeOut(tween(250))
    }
    val subject = if (isPop) from.route else to.route
    val transform = when (subject) {
        is Route.SignaturePad -> if (!isPop) {
            slideInVertically(tween(480, easing = FastOutSlowInEasing)) { it } + fadeIn(slow) togetherWith fadeOut(tween(300))
        } else {
            fadeIn(tween(300)) togetherWith slideOutVertically(tween(420, easing = FastOutSlowInEasing)) { it } + fadeOut(slow)
        }

        is Route.Export -> if (!isPop) {
            fadeIn(tween(120)) togetherWith fadeOut(tween(500)) + scaleOut(tween(500), targetScale = 0.96f)
        } else {
            fadeIn(tween(350)) + scaleIn(tween(400), initialScale = 0.96f) togetherWith fadeOut(tween(300))
        }

        else -> if (!isPop) {
            slideInHorizontally(tween(450, easing = FastOutSlowInEasing)) { it } + fadeIn(slow) togetherWith
                slideOutHorizontally(tween(450, easing = FastOutSlowInEasing)) { -it / 4 } + fadeOut(slow)
        } else {
            slideInHorizontally(tween(450, easing = FastOutSlowInEasing)) { -it / 4 } + fadeIn(slow) togetherWith
                slideOutHorizontally(tween(450, easing = FastOutSlowInEasing)) { it } + fadeOut(slow)
        }
    }
    transform.targetContentZIndex = if (isPop) -1f else 1f
    return transform
}
