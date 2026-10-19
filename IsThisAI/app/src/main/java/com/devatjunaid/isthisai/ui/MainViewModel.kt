package com.devatjunaid.isthisai.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devatjunaid.isthisai.IsThisAiApp
import com.devatjunaid.isthisai.data.ImageSource
import com.devatjunaid.isthisai.data.ImageTooLargeException
import com.devatjunaid.isthisai.data.ScanEntity
import com.devatjunaid.isthisai.engine.Analyzer
import com.devatjunaid.isthisai.engine.ScanReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Route {
    val key: String

    data object Home : Route { override val key = "home" }
    data object Scan : Route { override val key = "scan" }
    data class Result(val id: Long) : Route { override val key = "result/$id" }
    data class Trail(val id: Long) : Route { override val key = "trail/$id" }
    data object History : Route { override val key = "history" }
    data object About : Route { override val key = "about" }
}

sealed interface ScanState {
    data object Idle : ScanState
    data class Reading(val fileName: String, val preview: Bitmap?) : ScanState
    data class Ready(val report: ScanReport, val preview: Bitmap?) : ScanState
    data class Failed(val message: String) : ScanState
}

/** A saved scan opened on the result and trail screens. */
data class OpenScan(val id: Long, val report: ScanReport, val preview: Bitmap?)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = (app as IsThisAiApp).repository

    val recent: StateFlow<List<ScanEntity>> =
        repository.observeRecent(8).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val history: StateFlow<List<ScanEntity>> =
        repository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---------------- Navigation ----------------

    val stack = mutableStateListOf<Route>(Route.Home)
    var forward by mutableStateOf(true)
        private set

    val current: Route get() = stack.last()

    fun navigate(route: Route) {
        forward = true
        stack.add(route)
    }

    fun replaceTop(route: Route) {
        forward = true
        stack[stack.lastIndex] = route
    }

    fun back(): Boolean {
        if (stack.size <= 1) return false
        forward = false
        if (stack.last() == Route.Scan) cancelScan()
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun home() {
        forward = false
        cancelScan()
        stack.clear()
        stack.add(Route.Home)
    }

    // ---------------- Scanning ----------------

    var scan by mutableStateOf<ScanState>(ScanState.Idle)
        private set
    private var scanJob: Job? = null

    /** A file shared into the app while the splash was still showing. */
    var pendingUri by mutableStateOf<Uri?>(null)

    fun startScan(uri: Uri) {
        scanJob?.cancel()
        val context = getApplication<Application>()
        scan = ScanState.Reading("image", null)
        if (current != Route.Scan) navigate(Route.Scan)
        scanJob = viewModelScope.launch {
            scan = try {
                withContext(Dispatchers.IO) {
                    val info = ImageSource.info(context, uri)
                    val bytes = ImageSource.readBytes(context, uri)
                    val preview = ImageSource.decodePreview(bytes, 1080)
                    withContext(Dispatchers.Main) { scan = ScanState.Reading(info.name, preview) }
                    var report = Analyzer.analyze(bytes, info.name, info.mimeType)
                    if (report.width == null) {
                        ImageSource.dimensions(bytes)?.let { (w, h) -> report = report.copy(width = w, height = h) }
                    }
                    ScanState.Ready(report, preview)
                }
            } catch (e: ImageTooLargeException) {
                ScanState.Failed("This file is larger than 100 MB, which is more than this app reads.")
            } catch (e: SecurityException) {
                ScanState.Failed("The app that shared this image didn't allow it to be read. Try saving it first, then pick it with Files.")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                ScanState.Failed("This file couldn't be read. ${e.message ?: ""}".trim())
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        scan = ScanState.Idle
    }

    /** Saves the finished scan to history and returns its id. */
    suspend fun saveFinishedScan(): Long? {
        val ready = scan as? ScanState.Ready ?: return null
        val thumb = ready.preview?.let { scaleDown(it, 720) }
        val id = repository.save(ready.report, thumb)
        open = OpenScan(id, ready.report, ready.preview)
        scan = ScanState.Idle
        return id
    }

    // ---------------- Saved scans ----------------

    var open by mutableStateOf<OpenScan?>(null)
        private set

    fun load(id: Long) {
        if (open?.id == id) return
        viewModelScope.launch {
            val loaded = repository.load(id) ?: return@launch
            val preview = withContext(Dispatchers.IO) { ImageSource.loadPreview(loaded.first.previewPath) }
            open = OpenScan(id, loaded.second, preview)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
        if (open?.id == id) open = null
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clear() }
        open = null
    }

    private fun scaleDown(b: Bitmap, max: Int): Bitmap {
        val longest = maxOf(b.width, b.height)
        if (longest <= max) return b
        val s = max.toFloat() / longest
        return Bitmap.createScaledBitmap(b, (b.width * s).toInt().coerceAtLeast(1), (b.height * s).toInt().coerceAtLeast(1), true)
    }
}
