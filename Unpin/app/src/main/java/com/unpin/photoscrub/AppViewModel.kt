package com.unpin.photoscrub

import android.app.Application
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unpin.photoscrub.data.GallerySaver
import com.unpin.photoscrub.data.MetaCategory
import com.unpin.photoscrub.data.MetadataReader
import com.unpin.photoscrub.data.PhotoInfo
import com.unpin.photoscrub.data.Preset
import com.unpin.photoscrub.data.ScanStats
import com.unpin.photoscrub.data.ScrubEngine
import com.unpin.photoscrub.data.ScrubOptions
import com.unpin.photoscrub.data.ScrubOutcome
import com.unpin.photoscrub.data.ScrubSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

enum class Screen { HOME, INSPECT, BATCH, REPORT, HISTORY, SETTINGS }

data class BatchItem(val uri: Uri, val info: PhotoInfo? = null, val selected: Boolean = true, val error: String? = null)

data class Progress(val done: Int, val total: Int, val label: String)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = (app as UnpinApp).storage

    // ---- navigation
    val stack = mutableStateListOf(Screen.HOME)
    val current: Screen get() = stack.last()
    var splashDone by mutableStateOf(false)

    fun open(screen: Screen) {
        if (current == screen) return
        if (screen == Screen.HOME || screen == Screen.HISTORY) {
            stack.clear(); stack.add(Screen.HOME)
            if (screen == Screen.HISTORY) stack.add(Screen.HISTORY)
        } else {
            stack.remove(screen)
            stack.add(screen)
        }
    }

    /** @return false when already at the root (the caller shows the exit dialog). */
    fun back(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    // ---- settings
    var quickShare by mutableStateOf(storage.quickShare)
        private set
    var anonymizeDefault by mutableStateOf(storage.anonymizeNames)
        private set

    fun updateQuickShare(v: Boolean) { quickShare = v; storage.quickShare = v }
    fun updateAnonymizeDefault(v: Boolean) { anonymizeDefault = v; anonymize = v; storage.anonymizeNames = v }

    var askedMediaLocation: Boolean
        get() = storage.askedMediaLocation
        set(v) { storage.askedMediaLocation = v }

    // ---- exposure scan (from the photos the user picked)
    var scan by mutableStateOf(storage.lastScan)
        private set

    // ---- batch
    val batch = mutableStateListOf<BatchItem>()
    var batchLoading by mutableStateOf(false)
        private set
    var preset by mutableStateOf(Preset.FULL)
    var customRemove by mutableStateOf(setOf(MetaCategory.LOCATION, MetaCategory.DEVICE))
    var anonymize by mutableStateOf(storage.anonymizeNames)
    var keepOrientation by mutableStateOf(true)
    var keepColorProfile by mutableStateOf(true)

    var progress by mutableStateOf<Progress?>(null)
        private set

    val selectedItems: List<BatchItem> get() = batch.filter { it.selected && it.error == null }

    fun addToBatch(uris: List<Uri>, openScreen: Boolean = true) {
        val fresh = uris.distinct().filter { u -> batch.none { it.uri == u } }
        fresh.forEach { batch.add(BatchItem(it)) }
        if (openScreen) open(Screen.BATCH)
        if (fresh.isEmpty()) return
        viewModelScope.launch { loadInfos(fresh) }
    }

    private suspend fun loadInfos(uris: List<Uri>) {
        batchLoading = true
        for (u in uris) {
            val result = withContext(Dispatchers.IO) { runCatching { MetadataReader.read(getApplication(), u) } }
            val i = batch.indexOfFirst { it.uri == u }
            if (i < 0) continue
            batch[i] = result.fold(
                onSuccess = { batch[i].copy(info = it) },
                onFailure = { batch[i].copy(error = it.message ?: "Can't read", selected = false) },
            )
        }
        batchLoading = false
        val infos = batch.mapNotNull { it.info }
        if (infos.isNotEmpty()) {
            val stats = ScanStats(infos.size, infos.count { it.hasLocation }, infos.count { it.hasDevice }, infos.count { it.hasDate })
            scan = stats
            storage.lastScan = stats
        }
    }

    fun toggleItem(uri: Uri) {
        val i = batch.indexOfFirst { it.uri == uri }
        if (i >= 0 && batch[i].error == null) batch[i] = batch[i].copy(selected = !batch[i].selected)
    }

    fun selectAll() {
        val target = !batch.filter { it.error == null }.all { it.selected }
        for (i in batch.indices) if (batch[i].error == null) batch[i] = batch[i].copy(selected = target)
    }

    fun clearBatch() = batch.clear()

    fun toggleCustom(category: MetaCategory) {
        customRemove = if (category in customRemove) customRemove - category else customRemove + category
    }

    private fun options() = ScrubOptions(anonymize, keepOrientation, keepColorProfile)

    fun runBatch(onFinished: (ScrubSession) -> Unit) {
        val items = selectedItems
        if (items.isEmpty() || progress != null) return
        val remove = preset.removes(customRemove)
        scrub(items.map { it.uri }, remove) { session ->
            batch.clear()
            onFinished(session)
        }
    }

    private fun scrub(uris: List<Uri>, remove: Set<MetaCategory>, onFinished: (ScrubSession) -> Unit) {
        viewModelScope.launch {
            val outcomes = ArrayList<ScrubOutcome>()
            uris.forEachIndexed { i, uri ->
                progress = Progress(i, uris.size, "Scrubbing ${i + 1} of ${uris.size}")
                outcomes += withContext(Dispatchers.IO) { ScrubEngine.scrub(getApplication(), uri, remove, options()) }
            }
            progress = Progress(uris.size, uris.size, "Verifying copies")
            val session = ScrubSession.from(outcomes)
            withContext(Dispatchers.IO) { storage.addSession(session) }
            history = storage.history()
            refreshRecent()
            progress = null
            report = session
            onFinished(session)
        }
    }

    // ---- inspector
    var inspected by mutableStateOf<PhotoInfo?>(null)
        private set
    var inspectLoading by mutableStateOf(false)
        private set
    var inspectError by mutableStateOf<String?>(null)
        private set
    var inspectRemove by mutableStateOf(emptySet<MetaCategory>())
        private set
    var placeName by mutableStateOf<String?>(null)
        private set
    var placeLookupState by mutableStateOf(LookupState.IDLE)
        private set

    enum class LookupState { IDLE, LOADING, DONE, FAILED }

    fun inspect(uri: Uri) {
        open(Screen.INSPECT)
        inspectLoading = true
        inspectError = null
        inspected = null
        placeName = null
        placeLookupState = LookupState.IDLE
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { MetadataReader.read(getApplication(), uri) } }
            result.onSuccess { info ->
                inspected = info
                inspectRemove = info.categories.keys.toSet()
            }.onFailure { inspectError = it.message ?: "Couldn't read this photo" }
            inspectLoading = false
        }
    }

    /** Re-reads the current photo, e.g. after the media-location permission was granted. */
    fun reloadInspected() {
        inspected?.uri?.let { inspect(it) }
    }

    fun toggleInspect(category: MetaCategory) {
        if (!category.optional) return
        inspectRemove = if (category in inspectRemove) inspectRemove - category else inspectRemove + category
    }

    fun scrubInspected(onFinished: (ScrubSession) -> Unit) {
        val info = inspected ?: return
        if (progress != null) return
        scrub(listOf(info.uri), inspectRemove + MetaCategory.alwaysRemoved, onFinished)
    }

    /** User-initiated only: the phone's geocoder is an online service. */
    fun lookUpPlace() {
        val lat = inspected?.latitude ?: return
        val lon = inspected?.longitude ?: return
        placeLookupState = LookupState.LOADING
        viewModelScope.launch {
            val address = withContext(Dispatchers.IO) { runCatching { geocode(lat, lon) }.getOrNull() }
            placeName = address?.let { a ->
                listOfNotNull(a.subLocality ?: a.thoroughfare, a.locality, a.adminArea, a.countryName)
                    .filter { it.isNotBlank() }.distinct().joinToString(", ")
            }?.ifBlank { null }
            placeLookupState = if (placeName != null) LookupState.DONE else LookupState.FAILED
        }
    }

    private suspend fun geocode(lat: Double, lon: Double): Address? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(getApplication(), Locale.getDefault())
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (cont.isActive) cont.resume(addresses.firstOrNull())
                    }

                    override fun onError(errorMessage: String?) {
                        if (cont.isActive) cont.resume(null)
                    }
                })
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
        }
    }

    // ---- report & history
    var report by mutableStateOf<ScrubSession?>(null)
        private set
    var history by mutableStateOf(storage.history())
        private set

    fun openReport(session: ScrubSession) {
        report = session
        open(Screen.REPORT)
    }

    fun clearHistory() {
        storage.clearHistory()
        history = emptyList()
        recentOutputs = emptyList()
        scan = null
    }

    /** Clean copies from recent sessions that still exist, newest first. */
    var recentOutputs by mutableStateOf<List<Uri>>(emptyList())
        private set

    private fun refreshRecent() {
        val candidates = history.flatMap { it.outputs }.take(12)
        viewModelScope.launch {
            recentOutputs = withContext(Dispatchers.IO) {
                candidates.filter { GallerySaver.exists(getApplication(), it) }.take(4)
            }
        }
    }

    init {
        refreshRecent()
    }

    val totalCleaned: Int get() = history.sumOf { it.photoCount }
    val totalLocations: Int get() = history.sumOf { it.locationsErased }

    // ---- share sheet ("Clean when sharing")
    var pendingShare by mutableStateOf<List<Uri>?>(null)
        private set

    fun consumePendingShare() { pendingShare = null }

    fun handleShared(uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (!quickShare) {
            batch.clear()
            addToBatch(uris)
            return
        }
        // Quick mode: full wipe straight away, then hand the clean copies back to the share sheet.
        scrub(uris.distinct(), Preset.FULL.removes(emptySet())) { session ->
            open(Screen.REPORT)
            if (session.outputs.isNotEmpty()) pendingShare = session.outputs
        }
    }
}
