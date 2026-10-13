package com.odomate.app.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.odomate.app.AppContainer
import com.odomate.app.data.AppSettings

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }
val LocalSnackbar = staticCompositionLocalOf<SnackbarHostState> { error("Snackbar not provided") }
val LocalSettings = staticCompositionLocalOf { AppSettings() }

/** Shows a snackbar from an app-wide scope, so it survives the screen that triggered it closing. */
val LocalMessenger = staticCompositionLocalOf<(String) -> Unit> { {} }

/** Creates a ViewModel scoped to the current nav entry, with access to the container and nav arguments. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline create: (AppContainer, SavedStateHandle) -> VM,
): VM {
    val container = LocalAppContainer.current
    return viewModel(
        key = key,
        factory = viewModelFactory {
            initializer { create(container, createSavedStateHandle()) }
        },
    )
}
