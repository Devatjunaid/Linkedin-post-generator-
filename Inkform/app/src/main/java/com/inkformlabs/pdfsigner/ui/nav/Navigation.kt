package com.inkformlabs.pdfsigner.ui.nav

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface Route {
    data object Splash : Route
    data object Main : Route
    data class Editor(val documentId: String) : Route
    /** [forEditor]: the new signature is handed back to the editor and placed on the page. */
    data class SignaturePad(val forEditor: Boolean, val initials: Boolean = false) : Route
    data class Export(val documentId: String) : Route
}

/** One back-stack entry with its own ViewModelStore, cleared when the entry leaves the stack. */
class NavEntry(val id: Long, val route: Route) : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}

enum class MainTab { Documents, Signatures, Profile, Settings }

/**
 * Activity-scoped app state: the navigation back stack, the selected tab, results passed between
 * screens and PDFs shared to the app from other apps. Survives configuration changes.
 */
class AppViewModel : ViewModel() {
    private var nextId = 0L
    val stack = mutableStateListOf(NavEntry(nextId++, Route.Splash))
    var lastNavigationWasPop by mutableStateOf(false)
        private set

    var tab by mutableStateOf(MainTab.Documents)

    /** Signature created on the pad for the editor; consumed by the editor. */
    var pendingSignatureId by mutableStateOf<String?>(null)

    /** PDF opened from another app ("Open with" / "Share"). */
    var incomingPdf by mutableStateOf<Uri?>(null)

    val top: NavEntry get() = stack.last()

    fun push(route: Route) {
        lastNavigationWasPop = false
        stack.add(NavEntry(nextId++, route))
    }

    /** @return false when there is nothing to pop (the caller decides what back means then). */
    fun pop(): Boolean {
        if (stack.size <= 1) return false
        lastNavigationWasPop = true
        val removed = stack.removeAt(stack.lastIndex)
        clearLater(removed)
        return true
    }

    /** Clears the stack and shows [route] as the only screen. */
    fun resetTo(route: Route) {
        lastNavigationWasPop = false
        val old = stack.toList()
        stack.clear()
        stack.add(NavEntry(nextId++, route))
        old.forEach { clearLater(it) }
    }

    /** Pops back to the main tabs. */
    fun popToMain() {
        val index = stack.indexOfFirst { it.route == Route.Main }
        if (index < 0) {
            resetTo(Route.Main)
            return
        }
        lastNavigationWasPop = true
        while (stack.size > index + 1) clearLater(stack.removeAt(stack.lastIndex))
    }

    /** Lets the exit animation finish before the entry's ViewModels are destroyed. */
    private val awaitingClear = mutableListOf<NavEntry>()

    private fun clearLater(entry: NavEntry) {
        awaitingClear += entry
        viewModelScope.launch {
            delay(900)
            if (awaitingClear.remove(entry)) entry.viewModelStore.clear()
        }
    }

    override fun onCleared() {
        stack.forEach { it.viewModelStore.clear() }
        awaitingClear.forEach { it.viewModelStore.clear() }
        awaitingClear.clear()
    }
}
