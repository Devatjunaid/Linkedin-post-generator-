package com.voltora.evcalculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voltora.evcalculator.data.Countries
import com.voltora.evcalculator.data.Country
import com.voltora.evcalculator.data.DistanceUnit
import com.voltora.evcalculator.data.EvDatabase
import com.voltora.evcalculator.data.EvModel
import com.voltora.evcalculator.data.PrefsRepository
import com.voltora.evcalculator.data.UserPrefs
import com.voltora.evcalculator.domain.Formatter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the screens need, resolved from the saved preferences. */
data class AppState(
    val prefs: UserPrefs,
    val country: Country,
    val model: EvModel,
    val homeRate: Double,
    val acRate: Double,
    val dcRate: Double,
    val petrolPrice: Double,
    val fmt: Formatter,
) {
    val hasCustomRates: Boolean
        get() = prefs.homeRate != null || prefs.acRate != null || prefs.dcRate != null || prefs.petrolPrice != null

    companion object {
        fun from(p: UserPrefs): AppState {
            val country = Countries.byCode(p.countryCode) ?: Countries.default()
            val model = EvDatabase.byId(p.modelId) ?: EvDatabase.default
            val miles = when (p.distanceUnit) {
                DistanceUnit.AUTO -> country.useMiles
                DistanceUnit.KM -> false
                DistanceUnit.MILES -> true
            }
            return AppState(
                prefs = p,
                country = country,
                model = model,
                homeRate = p.homeRate ?: country.home,
                acRate = p.acRate ?: country.publicAc,
                dcRate = p.dcRate ?: country.dcFast,
                petrolPrice = p.petrolPrice ?: country.petrol,
                fmt = Formatter(country, miles),
            )
        }
    }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PrefsRepository(app)

    val state: StateFlow<AppState> = repo.prefs
        .map { AppState.from(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppState.from(UserPrefs()))

    fun setCountry(code: String) {
        viewModelScope.launch { repo.setCountry(code) }
    }
    fun setModel(id: String) {
        viewModelScope.launch { repo.setModel(id) }
    }
    fun setSoc(soc: Float) {
        viewModelScope.launch { repo.setSoc(soc) }
    }
    fun setHomeChargerKw(kw: Double) {
        viewModelScope.launch { repo.setHomeChargerKw(kw) }
    }
    fun setPetrolLPer100(v: Double) {
        viewModelScope.launch { repo.setPetrolLPer100(v) }
    }
    fun setDistanceUnit(u: DistanceUnit) {
        viewModelScope.launch { repo.setDistanceUnit(u) }
    }
    fun setRates(home: Double?, ac: Double?, dc: Double?, petrol: Double?) {
        viewModelScope.launch { repo.setRates(home, ac, dc, petrol) }
    }
}
