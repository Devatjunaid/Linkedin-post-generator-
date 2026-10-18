package com.devatjunaid.balconysolar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devatjunaid.balconysolar.data.PvgisClient
import com.devatjunaid.balconysolar.data.SetupMapper
import com.devatjunaid.balconysolar.data.db.SetupDatabase
import com.devatjunaid.balconysolar.data.db.SetupEntity
import com.devatjunaid.balconysolar.domain.Calculator
import com.devatjunaid.balconysolar.domain.CustomLocation
import com.devatjunaid.balconysolar.domain.Results
import com.devatjunaid.balconysolar.domain.SetupState
import com.devatjunaid.balconysolar.engine.YieldEngine
import com.devatjunaid.balconysolar.ui.screens.RefineStatus
import com.devatjunaid.balconysolar.ui.screens.SavedSummary
import com.devatjunaid.balconysolar.ui.screens.TiltScanState
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(FlowPreview::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = SetupDatabase.get(app).setups()

    private val _state = MutableStateFlow(SetupState.default())
    val state: StateFlow<SetupState> = _state.asStateFlow()

    private val _results = MutableStateFlow<Results?>(null)
    val results: StateFlow<Results?> = _results.asStateFlow()

    private val _refine = MutableStateFlow<RefineStatus>(RefineStatus.Idle)
    val refine: StateFlow<RefineStatus> = _refine.asStateFlow()

    private val _scan = MutableStateFlow<TiltScanState>(TiltScanState.Idle)
    val scan: StateFlow<TiltScanState> = _scan.asStateFlow()

    val saved: StateFlow<List<SavedSummary>> = dao.observeSaved()
        .map { list ->
            list.map { e ->
                val s = runCatching { SetupMapper.toState(e) }.getOrNull()
                SavedSummary(
                    id = e.id, name = e.name, place = s?.placeLabel ?: e.cityName, savedAt = e.savedAt,
                    peakW = e.panelCount * e.panelWp, annualKwh = e.resultAnnualKwh, paybackYears = e.resultPaybackYears,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var refineJob: Job? = null

    init {
        viewModelScope.launch {
            val draft = withContext(Dispatchers.IO) { runCatching { dao.get(SetupEntity.DRAFT_ID) }.getOrNull() }
            if (draft != null) runCatching { SetupMapper.toState(draft) }.onSuccess { _state.value = it }
            launch {
                _state.debounce(120).collectLatest { s ->
                    val r = withContext(Dispatchers.Default) { runCatching { Calculator.run(s) }.getOrNull() }
                    if (r != null) _results.value = r
                }
            }
            launch {
                _state.drop(1).debounce(800).collectLatest { s ->
                    withContext(Dispatchers.IO) {
                        runCatching { dao.upsert(SetupMapper.toEntity(SetupEntity.DRAFT_ID, "Draft", s, _results.value, System.currentTimeMillis())) }
                    }
                }
            }
        }
    }

    fun update(next: SetupState) {
        val prev = _state.value
        if (prev.latitude != next.latitude || prev.longitude != next.longitude) {
            refineJob?.cancel()
            _refine.value = RefineStatus.Idle
            _scan.value = TiltScanState.Idle
        }
        _state.value = next
    }

    fun useDeviceLocation(latitude: Double, longitude: Double) {
        update(_state.value.withCustom(CustomLocation("My location", latitude, longitude, ZoneId.systemDefault().id)))
    }

    fun refineWithPvgis() {
        val s = _state.value
        refineJob?.cancel()
        _refine.value = RefineStatus.Loading
        refineJob = viewModelScope.launch {
            try {
                val climate = PvgisClient.fetch(s.latitude, s.longitude, s.climateCity.city.climate.diurnalRange)
                val current = _state.value
                if (current.latitude == s.latitude && current.longitude == s.longitude) {
                    _state.value = current.copy(refinedClimate = climate)
                    _refine.value = RefineStatus.Done("Loaded: ${com.devatjunaid.balconysolar.domain.Fmt.number(climate.annualGhi())} kWh/m² per year at your exact coordinates.")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _refine.value = RefineStatus.Failed(
                    (e.message?.takeIf { it.isNotBlank() } ?: "Could not reach PVGIS.") + " The built-in table is still used.",
                )
            }
        }
    }

    fun scanTilt() {
        val s = _state.value
        _scan.value = TiltScanState.Running
        viewModelScope.launch {
            val scan = withContext(Dispatchers.Default) { runCatching { YieldEngine.scanTilt(s.site(), s.pv) }.getOrNull() }
            _scan.value = if (scan != null) TiltScanState.Ready(scan, s.pv) else TiltScanState.Idle
        }
    }

    fun save(name: String) {
        val s = _state.value
        val r = _results.value
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsert(SetupMapper.toEntity(0, name, s, r, System.currentTimeMillis()))
        }
    }

    fun load(id: Long) {
        viewModelScope.launch {
            val e = withContext(Dispatchers.IO) { dao.get(id) } ?: return@launch
            runCatching { SetupMapper.toState(e) }.onSuccess { update(it) }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { dao.delete(id) }
    }
}
