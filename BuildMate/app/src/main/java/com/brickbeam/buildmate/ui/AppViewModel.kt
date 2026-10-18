package com.brickbeam.buildmate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.brickbeam.buildmate.AppContainer
import com.brickbeam.buildmate.calc.DefaultPrices
import com.brickbeam.buildmate.calc.Markets
import com.brickbeam.buildmate.calc.PriceKey
import com.brickbeam.buildmate.calc.PriceZone
import com.brickbeam.buildmate.data.AppSettings
import com.brickbeam.buildmate.data.DraftItem
import com.brickbeam.buildmate.data.ThemeId
import com.brickbeam.buildmate.data.UnitPreference
import com.brickbeam.buildmate.data.db.ProjectSummary
import com.brickbeam.buildmate.ui.components.Fmt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Activity-scoped state shared by every screen. */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(private val container: AppContainer) : ViewModel() {
    /** null until DataStore has been read once (the splash waits for it). */
    val settings: StateFlow<AppSettings?> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val zoneFlow = container.settings.settings.map { it.region.zone }

    val prices: StateFlow<Map<PriceKey, Double>> = zoneFlow
        .flatMapLatest { container.settings.prices(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DefaultPrices.forZone(PriceZone.USD))

    val customizedPrices: StateFlow<Set<PriceKey>> = zoneFlow
        .flatMapLatest { container.settings.customizedKeys(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val projects: StateFlow<List<ProjectSummary>> = container.projects.summaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectRepo get() = container.projects

    fun setTheme(id: ThemeId) = viewModelScope.launch { container.settings.setTheme(id) }
    fun setMotion(on: Boolean) = viewModelScope.launch { container.settings.setMotion(on) }
    fun setRegion(code: String) = viewModelScope.launch { container.settings.setRegion(code) }
    fun setUnits(pref: UnitPreference) = viewModelScope.launch { container.settings.setUnits(pref) }

    fun setPrice(key: PriceKey, value: Double) = viewModelScope.launch {
        val zone = (settings.value ?: AppSettings()).region.zone
        container.settings.setPrice(zone, key, value)
    }

    fun resetPrices() = viewModelScope.launch {
        container.settings.resetPrices((settings.value ?: AppSettings()).region.zone)
    }

    /** Saves calculator lines into an existing project, or a new one when [projectId] is null. */
    fun saveDrafts(projectId: Long?, newName: String, drafts: List<DraftItem>, onSaved: (Long, String) -> Unit) =
        viewModelScope.launch {
            val region = (settings.value ?: container.settings.settings.first()).region
            val id = projectId ?: container.projects.createProject(newName, region.code)
            container.projects.addItems(id, drafts)
            val name = container.projects.getProject(id)?.name ?: newName
            onSaved(id, name)
        }

    fun createProject(name: String, onCreated: (Long) -> Unit) = viewModelScope.launch {
        val region = (settings.value ?: container.settings.settings.first()).region
        onCreated(container.projects.createProject(name, region.code))
    }

    fun deleteProject(id: Long) = viewModelScope.launch { container.projects.deleteProject(id) }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { AppViewModel(container) }
        }
    }
}

/** Everything a calculator needs to format and cost its results. */
data class CalcContext(
    val settings: AppSettings,
    val prices: Map<PriceKey, Double>,
) {
    val region get() = settings.region
    val market get() = region.market
    val spec get() = Markets.spec(market)
    val system get() = settings.unitSystem
    val fmt = Fmt(settings.unitSystem, settings.region)
    fun price(key: PriceKey) = prices[key] ?: 0.0
    fun unitOf(key: PriceKey) = key.unit(market)
}
