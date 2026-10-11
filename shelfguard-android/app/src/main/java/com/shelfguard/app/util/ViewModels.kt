package com.shelfguard.app.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shelfguard.app.AppContainer
import com.shelfguard.app.ShelfGuardApp

@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as ShelfGuardApp).container

/** Creates a ViewModel with access to the app container, scoped to the current nav destination. */
@Composable
inline fun <reified VM : ViewModel> containerViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = appContainer()
    return viewModel(key = key, factory = viewModelFactory { initializer { create(container) } })
}
