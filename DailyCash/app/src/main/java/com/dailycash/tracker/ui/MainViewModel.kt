package com.dailycash.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dailycash.tracker.DailyCashApp
import com.dailycash.tracker.data.CashEntry
import com.dailycash.tracker.data.Categories
import com.dailycash.tracker.data.Category
import com.dailycash.tracker.data.CountryDetector
import com.dailycash.tracker.data.CurrencyBalance
import com.dailycash.tracker.data.CustomCategory
import com.dailycash.tracker.data.Money
import com.dailycash.tracker.data.Place
import com.dailycash.tracker.data.Rates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: DailyCashApp) : ViewModel() {
    private val dao = app.database.cashDao()
    private val prefs = app.prefs
    private val ratesRepo = app.rates

    val place: Place = CountryDetector.detect(app)

    val entries: StateFlow<List<CashEntry>> =
        dao.observeEntries().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val balances: StateFlow<List<CurrencyBalance>> =
        dao.observeBalances().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val customCategories: StateFlow<List<CustomCategory>> =
        dao.observeCustomCategories().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val homeCurrency: StateFlow<String> =
        prefs.homeCurrency.map { it ?: place.currency }.stateIn(viewModelScope, SharingStarted.Eagerly, place.currency)

    val lastCurrency: StateFlow<String?> =
        prefs.lastCurrency.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val rates: StateFlow<Rates> = ratesRepo.rates

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        if (ratesRepo.isStale()) refreshRates()
    }

    fun refreshRates(onDone: (Boolean) -> Unit = {}) {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            val ok = ratesRepo.refresh()
            _refreshing.value = false
            onDone(ok)
        }
    }

    fun save(type: Int, amountMinor: Long, currency: String, category: Category, note: String) {
        if (amountMinor <= 0) return
        val home = homeCurrency.value
        val entry = CashEntry(
            type = type,
            amountMinor = amountMinor,
            currency = currency,
            categoryId = category.id,
            label = category.name,
            note = note.trim(),
            countryCode = place.countryCode,
            timestamp = System.currentTimeMillis(),
            homeCurrency = home,
            homeAmountMinor = rates.value.convert(amountMinor, currency, home),
        )
        viewModelScope.launch {
            dao.insert(entry)
            prefs.setLastCurrency(currency)
        }
    }

    fun delete(entry: CashEntry) = viewModelScope.launch { dao.delete(entry) }

    fun restore(entry: CashEntry) = viewModelScope.launch { dao.upsert(entry) }

    fun addCustomCategory(name: String): Category? {
        val clean = name.trim().replace(Regex("\\s+"), " ").take(32)
        if (clean.isEmpty()) return null
        viewModelScope.launch { dao.insertCustomCategory(CustomCategory(name = clean, createdAt = System.currentTimeMillis())) }
        return Categories.custom(clean)
    }

    fun removeCustomCategory(c: CustomCategory) = viewModelScope.launch { dao.deleteCustomCategory(c) }

    fun setHomeCurrency(code: String) = viewModelScope.launch { prefs.setHomeCurrency(code) }

    fun balanceOf(code: String): Long = balances.value.firstOrNull { it.currency == code }?.balance ?: 0L

    /** CSV of all entries, for sharing to a spreadsheet or email. */
    fun exportCsv(): String = buildString {
        appendLine("date,type,category,amount,currency,home_amount,home_currency,country,note")
        val fmt = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
        entries.value.sortedBy { it.timestamp }.forEach { e ->
            val date = java.time.Instant.ofEpochMilli(e.timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
            val home = e.homeAmountMinor?.let { Money.toMajor(it, e.homeCurrency).toPlainString() }.orEmpty()
            appendLine(
                listOf(
                    fmt.format(date),
                    if (e.type == CashEntry.SPEND) "spent" else "added",
                    e.label,
                    Money.toMajor(e.amountMinor, e.currency).toPlainString(),
                    e.currency,
                    home,
                    e.homeCurrency,
                    e.countryCode,
                    e.note,
                ).joinToString(",") { csv(it) }
            )
        }
    }

    private fun csv(v: String) = if (v.any { it == ',' || it == '"' || it == '\n' }) "\"" + v.replace("\"", "\"\"") + "\"" else v

    companion object {
        val Factory = viewModelFactory {
            initializer { MainViewModel(this[APPLICATION_KEY] as DailyCashApp) }
        }
    }
}
