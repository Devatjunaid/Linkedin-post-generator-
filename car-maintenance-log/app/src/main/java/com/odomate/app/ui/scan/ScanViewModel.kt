package com.odomate.app.ui.scan

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odomate.app.AppContainer
import com.odomate.app.data.VehicleSnapshot
import com.odomate.app.data.db.ReadingSource
import com.odomate.app.ocr.OcrToken
import com.odomate.app.ocr.OdometerParser
import com.odomate.app.ocr.PhotoScanner
import com.odomate.app.util.Formats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScanState(
    val liveValue: Int? = null,
    val hits: Int = 0,
    val locked: Boolean = false,
    val text: String = "",
    val fromPhoto: Boolean = false,
    val processingPhoto: Boolean = false,
    val photoFailed: Boolean = false,
)

class ScanViewModel(private val container: AppContainer, handle: SavedStateHandle) : ViewModel() {
    val pickMode: Boolean = handle.get<String>("mode") == "pick"

    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state.asStateFlow()

    var snapshot: VehicleSnapshot? = null
        private set
    private val recent = ArrayDeque<Int?>()

    init {
        viewModelScope.launch { snapshot = container.repository.activeSnapshot.first() }
    }

    val lastReading: Int? get() = snapshot?.currentOdometer

    fun onFrame(tokens: List<OcrToken>, width: Int, height: Int) {
        if (_state.value.locked) return
        val best = OdometerParser.best(tokens, width, height, lastReading)
        recent.addLast(best?.value)
        while (recent.size > WINDOW) recent.removeFirst()
        val counts = recent.filterNotNull().groupingBy { it }.eachCount()
        val top = counts.maxByOrNull { it.value }
        _state.update { it.copy(liveValue = top?.key, hits = top?.value ?: 0) }
        if (top != null && top.value >= LOCK_HITS) lock(top.key)
    }

    fun lock(value: Int?) {
        _state.update { it.copy(locked = true, text = value?.toString() ?: "", fromPhoto = false, photoFailed = false) }
    }

    fun retake() {
        recent.clear()
        _state.value = ScanState()
    }

    fun setText(text: String) {
        _state.update { it.copy(text = text.filter(Char::isDigit).take(8)) }
    }

    fun scanPhoto(context: Context, uri: Uri) {
        _state.update { it.copy(processingPhoto = true, photoFailed = false) }
        viewModelScope.launch {
            val result = runCatching { PhotoScanner.scan(context.applicationContext, uri, lastReading) }.getOrNull()
            _state.update {
                it.copy(
                    processingPhoto = false,
                    locked = true,
                    fromPhoto = true,
                    photoFailed = result == null,
                    text = result?.value?.toString() ?: "",
                )
            }
        }
    }

    val value: Int? get() = Formats.parseInt(_state.value.text)

    /** Saves the reading for the active vehicle. Returns the saved value or null. */
    suspend fun save(): Int? {
        val v = value ?: return null
        val vehicleId = snapshot?.vehicle?.id ?: container.repository.activeVehicle.first()?.id ?: return null
        container.repository.addReading(vehicleId, v, System.currentTimeMillis(), ReadingSource.SCAN)
        return v
    }

    companion object {
        private const val WINDOW = 6
        private const val LOCK_HITS = 3
    }
}
