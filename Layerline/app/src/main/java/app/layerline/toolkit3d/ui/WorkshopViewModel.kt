package app.layerline.toolkit3d.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.layerline.toolkit3d.data.CalibrationRecord
import app.layerline.toolkit3d.data.MaintenanceTask
import app.layerline.toolkit3d.data.PrintJob
import app.layerline.toolkit3d.data.Printer
import app.layerline.toolkit3d.data.Quote
import app.layerline.toolkit3d.data.Repository
import app.layerline.toolkit3d.data.Spool
import app.layerline.toolkit3d.data.Workshop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Shared screen ViewModel: exposes every table as state and wraps writes in [viewModelScope].
 * Each screen gets its own instance (scoped to its nav entry); Room keeps them in sync.
 */
class WorkshopViewModel(private val repo: Repository) : ViewModel() {

    private fun <T> Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val workshop: StateFlow<Workshop?> = repo.workshop.state(null)
    val printers: StateFlow<List<Printer>> = repo.printers.state(emptyList())
    val maintenance: StateFlow<List<MaintenanceTask>> = repo.maintenance.state(emptyList())
    val spools: StateFlow<List<Spool>> = repo.spools.state(emptyList())
    val jobs: StateFlow<List<PrintJob>> = repo.jobs.state(emptyList())
    val quotes: StateFlow<List<Quote>> = repo.quotes.state(emptyList())
    val calibrations: StateFlow<List<CalibrationRecord>> = repo.calibrations.state(emptyList())

    private fun io(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    fun saveJob(job: PrintJob) = io { repo.saveJob(job) }
    fun deleteJob(job: PrintJob) = io { repo.deleteJob(job) }

    fun saveSpool(spool: Spool) = io { repo.saveSpool(spool) }
    fun deleteSpool(spool: Spool) = io { repo.deleteSpool(spool) }

    fun addPrinter(printer: Printer, tasks: List<Pair<String, Double>>) = io { repo.addPrinter(printer, tasks) }
    fun updatePrinter(printer: Printer) = io { repo.updatePrinter(printer) }
    fun deletePrinter(printer: Printer) = io { repo.deletePrinter(printer) }
    fun markServiced(task: MaintenanceTask, printer: Printer) = io { repo.markServiced(task, printer) }
    fun addTask(printer: Printer, name: String, interval: Double) = io { repo.addTask(printer, name, interval) }
    fun deleteTask(task: MaintenanceTask) = io { repo.deleteTask(task) }

    fun saveQuote(quote: Quote) = io { repo.saveQuote(quote) }
    fun deleteQuote(quote: Quote) = io { repo.deleteQuote(quote) }

    fun saveCalibration(record: CalibrationRecord) = io { repo.saveCalibration(record) }
    fun deleteCalibration(record: CalibrationRecord) = io { repo.deleteCalibration(record) }

    fun setRegion(code: String) = io { repo.setRegion(code) }
    fun saveWorkshop(workshop: Workshop) = io { repo.saveWorkshop(workshop) }

    companion object {
        val Factory = repoFactory { WorkshopViewModel(it) }
    }
}
