package com.inkformlabs.pdfsigner.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.inkformlabs.pdfsigner.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
