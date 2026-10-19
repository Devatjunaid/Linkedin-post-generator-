package com.sunarc.inverterlogger.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sunarc.inverterlogger.SunarcApp
import com.sunarc.inverterlogger.data.Inverter
import com.sunarc.inverterlogger.domain.Countries
import com.sunarc.inverterlogger.domain.Dashboard
import com.sunarc.inverterlogger.domain.DashboardBuilder
import com.sunarc.inverterlogger.domain.InverterBrands
import com.sunarc.inverterlogger.domain.ModbusTcpClient
import com.sunarc.inverterlogger.domain.ParseContext
import com.sunarc.inverterlogger.domain.ParseResult
import com.sunarc.inverterlogger.domain.Reading
import com.sunarc.inverterlogger.domain.ReadingSource
import com.sunarc.inverterlogger.domain.SolarMath
import com.sunarc.inverterlogger.domain.SunSpec
import com.sunarc.inverterlogger.domain.SunSpecReading
import com.sunarc.inverterlogger.domain.SystemProfile
import com.sunarc.inverterlogger.reminders.ReminderScheduler
import com.sunarc.inverterlogger.widget.TodayWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

/** What the scanner (or a network read, or manual entry) hands to the confirm screen. */
data class ScanDraft(
    val inverterId: Long,
    val source: ReadingSource,
    val result: ParseResult,
    val photoPath: String? = null,
    val takenAt: Instant = Instant.now()
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as SunarcApp).container
    val ads = container.ads
    val exports = container.exports
    val ocr = container.ocr

    val profile: StateFlow<SystemProfile?> = container.settings.profile
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val inverters: StateFlow<List<Inverter>> = container.log.inverters
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val readings: StateFlow<List<Reading>> = container.log.readings
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val minuteTick = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000)
        }
    }

    val dashboard: StateFlow<Dashboard?> = combine(container.settings.profile, container.log.readings, minuteTick) { p, r, _ ->
        DashboardBuilder.build(p, r, ZonedDateTime.now(p.zone))
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val draft = MutableStateFlow<ScanDraft?>(null)

    /** Which inverter the next scan is for. */
    val activeInverterId = MutableStateFlow<Long?>(null)

    /** Set when the scanner should open the photo picker straight away (scan a screenshot). */
    val pendingGallery = MutableStateFlow(false)

    /** Opens the confirm screen with empty fields so a reading can be typed in. */
    fun startManualEntry(): Boolean {
        val inverter = activeInverter() ?: return false
        draft.value = ScanDraft(inverter.id, ReadingSource.MANUAL, ParseResult.EMPTY)
        return true
    }

    fun activeInverter(): Inverter? {
        val list = inverters.value
        return list.firstOrNull { it.id == activeInverterId.value } ?: list.firstOrNull()
    }

    suspend fun parseContext(inverter: Inverter?): ParseContext {
        val p = profile.value ?: SystemProfile()
        val last = inverter?.let { container.log.latestFor(it.id) }
        return ParseContext(
            brand = inverter?.brand,
            systemKwp = p.kwp.takeIf { it > 0 },
            lastTotalKwh = last?.totalKwh,
            decimalStyle = p.country.decimalStyle
        )
    }

    suspend fun previousFor(inverterId: Long): Reading? = container.log.latestFor(inverterId)

    fun clearSkyToday(): Double? {
        val p = profile.value ?: return null
        val pv = p.pvSystem ?: return null
        return SolarMath.clearSkyDay(pv, LocalDate.now(p.zone), p.zone, 20).clearSkyKwh
    }

    fun completeOnboarding(
        countryCode: String,
        brandId: String,
        model: String,
        kwp: Double,
        tilt: Double,
        azimuth: Double,
        name: String
    ) = viewModelScope.launch {
        val country = Countries.byCode(countryCode)
        container.settings.update { old ->
            old.copy(
                onboarded = true, countryCode = countryCode, systemName = name.ifBlank { "My solar system" },
                kwp = kwp, tiltDeg = tilt, azimuthDeg = azimuth, tariffModel = country.tariffModel
            )
        }
        if (container.log.allInverters().isEmpty()) {
            val brand = InverterBrands.byId(brandId)
            container.log.addInverter(brand.name + if (model.isNotBlank()) " $model" else "", brandId, model)
        }
    }

    fun updateProfile(transform: (SystemProfile) -> SystemProfile) = viewModelScope.launch {
        container.settings.update(transform)
        ReminderScheduler.reschedule(getApplication<Application>())
    }

    suspend fun saveReading(reading: Reading): Long {
        val id = container.log.save(reading)
        TodayWidget.refresh(getApplication<Application>())
        return id
    }

    fun deleteReading(id: Long) = viewModelScope.launch {
        container.log.delete(id)
        TodayWidget.refresh(getApplication<Application>())
    }

    fun addInverter(inverter: Inverter) = viewModelScope.launch {
        val id = container.log.addInverter(inverter.name, inverter.brandId, inverter.model)
        if (inverter.host != null) container.log.updateInverter(inverter.copy(id = id))
    }

    fun updateInverter(inverter: Inverter) = viewModelScope.launch { container.log.updateInverter(inverter) }

    fun deleteInverter(id: Long) = viewModelScope.launch { container.log.deleteInverter(id) }

    suspend fun readOverNetwork(host: String, port: Int, unitId: Int): Result<SunSpecReading> = withContext(Dispatchers.IO) {
        runCatching { ModbusTcpClient(host.trim(), port, unitId).use { SunSpec.read(it) } }
    }

    suspend fun csv(): Uri = exports.csvFile()
    suspend fun dailyCsv(): Uri = exports.dailyCsvFile()
    suspend fun report(): Uri = exports.monthlyReportPdf()
    suspend fun backup(target: Uri) = exports.writeBackup(target)
    suspend fun restore(source: Uri): Int = exports.restoreBackup(source).also { TodayWidget.refresh(getApplication<Application>()) }
}
