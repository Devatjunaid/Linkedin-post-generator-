package app.layerline.toolkit3d.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class Repository(private val db: AppDatabase, val settings: SettingsStore) {

    val workshop: Flow<Workshop> = settings.workshop
    val printers: Flow<List<Printer>> = db.printers().observeAll()
    val maintenance: Flow<List<MaintenanceTask>> = db.printers().observeMaintenance()
    val spools: Flow<List<Spool>> = db.spools().observeAll()
    val jobs: Flow<List<PrintJob>> = db.jobs().observeAll()
    val quotes: Flow<List<Quote>> = db.quotes().observeAll()
    val calibrations: Flow<List<CalibrationRecord>> = db.calibrations().observeRecent()

    // ---- Printers -------------------------------------------------------------------------

    suspend fun addPrinter(printer: Printer, tasks: List<Pair<String, Double>>): Long = db.withTransaction {
        val id = db.printers().insert(printer)
        db.printers().insertTasks(
            tasks.map { (name, interval) ->
                MaintenanceTask(printerId = id, name = name, intervalHours = interval, hoursAtService = printer.hoursRun)
            },
        )
        id
    }

    suspend fun updatePrinter(printer: Printer) = db.printers().update(printer)

    suspend fun deletePrinter(printer: Printer) = db.printers().delete(printer)

    suspend fun markServiced(task: MaintenanceTask, printer: Printer) =
        db.printers().updateTask(task.copy(hoursAtService = printer.hoursRun))

    suspend fun addTask(printer: Printer, name: String, interval: Double) {
        db.printers().insertTask(
            MaintenanceTask(printerId = printer.id, name = name, intervalHours = interval, hoursAtService = printer.hoursRun),
        )
    }

    suspend fun deleteTask(task: MaintenanceTask) = db.printers().deleteTask(task)

    // ---- Spools ---------------------------------------------------------------------------

    suspend fun saveSpool(spool: Spool): Long =
        if (spool.id == 0L) db.spools().insert(spool) else {
            db.spools().update(spool)
            spool.id
        }

    suspend fun deleteSpool(spool: Spool) = db.spools().delete(spool)

    // ---- Print log ------------------------------------------------------------------------

    /**
     * Saves a job and keeps stock and machine hours consistent: a finished or failed job takes
     * its filament off the linked spool and adds its run time to the linked printer exactly once.
     * Editing a finished job first reverses what the old version booked.
     */
    suspend fun saveJob(job: PrintJob): Long = db.withTransaction {
        val previous = if (job.id != 0L) db.jobs().get(job.id) else null
        if (previous != null && previous.accounted) book(previous, sign = -1.0)

        val finished = job.jobStatus != JobStatus.PRINTING
        val toSave = if (finished) {
            book(job, sign = 1.0)
            job.copy(accounted = true, finishedAt = job.finishedAt ?: System.currentTimeMillis())
        } else {
            job.copy(accounted = false, finishedAt = null)
        }

        if (toSave.id == 0L) db.jobs().insert(toSave) else {
            db.jobs().update(toSave)
            toSave.id
        }
    }

    suspend fun deleteJob(job: PrintJob) = db.jobs().delete(job)

    private suspend fun book(job: PrintJob, sign: Double) {
        val share = job.usedFraction
        job.spoolId?.let { db.spools().consume(it, sign * job.grams * share) }
        job.printerId?.let { db.printers().addHours(it, sign * job.minutes / 60.0 * share) }
    }

    // ---- Quotes & calibration history -----------------------------------------------------

    suspend fun saveQuote(quote: Quote): Long = db.quotes().insert(quote)

    suspend fun deleteQuote(quote: Quote) = db.quotes().delete(quote)

    suspend fun saveCalibration(record: CalibrationRecord): Long = db.calibrations().insert(record)

    suspend fun deleteCalibration(record: CalibrationRecord) = db.calibrations().delete(record)

    // ---- Settings -------------------------------------------------------------------------

    suspend fun setRegion(code: String) = settings.setRegion(code)

    suspend fun saveWorkshop(workshop: Workshop) = settings.save(workshop)
}
