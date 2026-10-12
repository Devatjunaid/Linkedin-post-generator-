package app.gigometer.mileage.ui

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.gigometer.mileage.Graph
import app.gigometer.mileage.R
import app.gigometer.mileage.data.AppSettings
import app.gigometer.mileage.data.Category
import app.gigometer.mileage.data.IrsRates
import app.gigometer.mileage.data.RatePeriod
import app.gigometer.mileage.data.Trip
import app.gigometer.mileage.export.Exporter
import app.gigometer.mileage.reminder.ReviewReminderWorker
import app.gigometer.mileage.tracking.AutoDetect
import app.gigometer.mileage.tracking.TrackingService
import app.gigometer.mileage.tracking.TripRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class Totals(val miles: Double = 0.0, val deduction: Double = 0.0, val trips: Int = 0)

data class HomeState(
    val ytd: Totals = Totals(),
    val lastYearBusinessMiles: Double = 0.0,
    val today: Totals = Totals(),
    val week: Totals = Totals(),
    val toReview: Int = 0,
    val recent: List<Trip> = emptyList(),
    val currentRate: Double = 0.0,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = Graph.trips
    private val settingsRepo = Graph.settings
    private val ctx get() = getApplication<Application>()

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val trips: StateFlow<List<Trip>> = dao.observeFinished()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val live = TripRecorder.live

    val home: StateFlow<HomeState> = combine(trips, settings) { list, s -> computeHome(list, s) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, HomeState())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            AutoDetect.recoverOrphanedTrip()
            AutoDetect.sync(ctx)
        }
    }

    private fun computeHome(list: List<Trip>, s: AppSettings): HomeState {
        val today = LocalDate.now()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        fun totals(filter: (LocalDate) -> Boolean): Totals {
            val sel = list.filter { it.isBusiness && filter(app.gigometer.mileage.Format.localDate(it.startTime)) }
            return Totals(sel.sumOf { it.miles }, sel.sumOf { IrsRates.deduction(it, s.rates) }, sel.size)
        }
        return HomeState(
            ytd = totals { it.year == today.year },
            lastYearBusinessMiles = totals { it.year == today.year - 1 }.miles,
            today = totals { it == today },
            week = totals { !it.isBefore(weekStart) },
            toReview = list.count { it.category == Category.UNREVIEWED },
            recent = list.take(4),
            currentRate = IrsRates.rateOn(today, s.rates),
        )
    }

    // ---- Trips ----

    fun classify(trip: Trip, category: String, platform: String? = null) = viewModelScope.launch(Dispatchers.IO) {
        dao.setCategory(trip.id, category, platform)
    }

    fun saveTrip(trip: Trip) = viewModelScope.launch(Dispatchers.IO) {
        if (trip.id == 0L) dao.insert(trip) else dao.update(trip)
        if (trip.platform != null) settingsRepo.setLastPlatform(trip.platform)
    }

    fun deleteTrip(trip: Trip) = viewModelScope.launch(Dispatchers.IO) { dao.delete(trip) }

    fun startManualTrip(): Boolean = TrackingService.send(ctx, TrackingService.ACTION_START_MANUAL)
    fun endTrip() = TrackingService.send(ctx, TrackingService.ACTION_STOP)
    fun pauseTrip() = TrackingService.send(ctx, TrackingService.ACTION_PAUSE)
    fun resumeTrip() = TrackingService.send(ctx, TrackingService.ACTION_RESUME)

    // ---- Settings ----

    fun setAutoDetect(v: Boolean) = viewModelScope.launch {
        settingsRepo.setAutoDetect(v)
        AutoDetect.sync(ctx)
    }
    fun resyncAutoDetect() = viewModelScope.launch { AutoDetect.sync(ctx) }
    fun setBatterySaver(v: Boolean) = viewModelScope.launch { settingsRepo.setBatterySaver(v) }
    fun setReminder(v: Boolean) = viewModelScope.launch {
        settingsRepo.setReminder(v)
        ReviewReminderWorker.schedule(ctx, v)
    }
    fun setLookupAddresses(v: Boolean) = viewModelScope.launch { settingsRepo.setLookupAddresses(v) }
    fun setWorkAutoBusiness(v: Boolean) = viewModelScope.launch { settingsRepo.setWorkAutoBusiness(v) }
    fun setWorkHours(start: Int, end: Int) = viewModelScope.launch { settingsRepo.setWorkHours(start, end) }
    fun setPlatforms(list: List<String>) = viewModelScope.launch { settingsRepo.setPlatforms(list) }
    fun setDriverName(v: String) = viewModelScope.launch { settingsRepo.setDriverName(v) }
    fun setOnboarded() = viewModelScope.launch { settingsRepo.setOnboarded() }
    fun addRate(start: LocalDate, rate: Double) = viewModelScope.launch {
        val list = settings.value.rates.filterNot { it.start == start } + RatePeriod(start, rate)
        settingsRepo.setRates(list.sortedBy { it.start })
    }
    fun removeRate(p: RatePeriod) = viewModelScope.launch {
        val list = settings.value.rates - p
        if (list.isNotEmpty()) settingsRepo.setRates(list)
    }
    fun resetRates() = viewModelScope.launch { settingsRepo.setRates(IrsRates.DEFAULTS) }

    // ---- Reports / export ----

    fun tripsForYear(year: Int): List<Trip> {
        val zone = ZoneId.systemDefault()
        val from = LocalDate.of(year, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = LocalDate.of(year + 1, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        return trips.value.filter { it.startTime in from until to }.sortedBy { it.startTime }
    }

    fun exportPdf(year: Int) = viewModelScope.launch {
        val f = withContext(Dispatchers.IO) { Exporter.writePdf(ctx, tripsForYear(year), settings.value, year) }
        Exporter.share(ctx, f, "application/pdf")
    }

    fun exportCsv(year: Int) = viewModelScope.launch {
        val f = withContext(Dispatchers.IO) { Exporter.writeCsv(ctx, tripsForYear(year), settings.value, year) }
        Exporter.share(ctx, f, "text/csv")
    }

    fun backupTo(uri: Uri) = viewModelScope.launch {
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                val json = Exporter.backupJson(dao.allFinished())
                ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
            }.isSuccess
        }
        toast(if (ok) R.string.backup_done else R.string.backup_failed)
    }

    fun restoreFrom(uri: Uri) = viewModelScope.launch {
        val added = withContext(Dispatchers.IO) {
            runCatching {
                val json = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                val existing = dao.allFinished().map { it.startTime }.toHashSet()
                val fresh = Exporter.parseBackup(json).filter { it.startTime !in existing }
                dao.insertAll(fresh)
                fresh.size
            }.getOrNull()
        }
        if (added == null) toast(R.string.restore_failed)
        else Toast.makeText(ctx, ctx.resources.getQuantityString(R.plurals.restore_done, added, added), Toast.LENGTH_SHORT).show()
    }

    private fun toast(res: Int) = Toast.makeText(ctx, ctx.getString(res), Toast.LENGTH_SHORT).show()
}
