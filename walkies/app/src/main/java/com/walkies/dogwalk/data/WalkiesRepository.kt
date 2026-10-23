package com.walkies.dogwalk.data

import com.walkies.dogwalk.reminders.ReminderScheduler
import kotlinx.coroutines.flow.Flow

/** Single entry point for reading and changing the log, walks and reminders. */
class WalkiesRepository(
    private val db: WalkiesDatabase,
    private val scheduler: ReminderScheduler,
) {
    private val logDao = db.logDao()
    private val walkDao = db.walkDao()
    private val reminderDao = db.reminderDao()

    // --- Log ---------------------------------------------------------------------------------

    fun entriesBetween(from: Long, to: Long): Flow<List<LogEntry>> = logDao.observeBetween(from, to)

    suspend fun entriesBetweenOnce(from: Long, to: Long): List<LogEntry> = logDao.between(from, to)

    fun latest(type: LogType): Flow<LogEntry?> = logDao.observeLatest(type.name)

    suspend fun log(
        type: LogType,
        timestamp: Long = System.currentTimeMillis(),
        consistency: Consistency? = null,
        walkId: Long? = null,
    ): LogEntry {
        val entry = LogEntry(type = type, timestamp = timestamp, consistency = consistency, walkId = walkId)
        val saved = entry.copy(id = logDao.insert(entry))
        if (type == LogType.PEE) scheduler.scheduleNoPeeAlert()
        return saved
    }

    suspend fun update(entry: LogEntry) = logDao.update(entry)

    suspend fun delete(entry: LogEntry) {
        logDao.delete(entry)
        if (entry.type == LogType.PEE) scheduler.scheduleNoPeeAlert()
    }

    /** Puts back an entry removed with [delete] (snackbar undo). */
    suspend fun restore(entry: LogEntry) {
        logDao.insert(entry)
        if (entry.type == LogType.PEE) scheduler.scheduleNoPeeAlert()
    }

    // --- Walks -------------------------------------------------------------------------------

    val activeWalk: Flow<Walk?> = walkDao.observeActive()

    fun walkEntries(walkId: Long): Flow<List<LogEntry>> = logDao.observeForWalk(walkId)

    fun walkCounts(): Flow<List<WalkCount>> = logDao.observeWalkCounts()

    fun recentWalks(limit: Int = 10): Flow<List<Walk>> = walkDao.observeRecent(limit)

    fun walksBetween(from: Long, to: Long): Flow<List<Walk>> = walkDao.observeFinishedBetween(from, to)

    suspend fun walksBetweenOnce(from: Long, to: Long): List<Walk> = walkDao.finishedBetween(from, to)

    suspend fun startWalk(): Walk {
        walkDao.active()?.let { return it }
        val walk = Walk(startTime = System.currentTimeMillis())
        return walk.copy(id = walkDao.insert(walk))
    }

    suspend fun setWalkStartSteps(walk: Walk, startSteps: Long) =
        walkDao.update(walk.copy(startSteps = startSteps))

    suspend fun finishWalk(walk: Walk, steps: Long?) =
        walkDao.update(walk.copy(endTime = System.currentTimeMillis(), steps = steps))

    suspend fun deleteWalk(walk: Walk) {
        logDao.deleteForWalk(walk.id)
        walkDao.delete(walk)
        // The walk may have held the most recent pee, so the no-pee alert time can change.
        scheduler.scheduleNoPeeAlert()
    }

    // --- Reminders ---------------------------------------------------------------------------

    val reminders: Flow<List<Reminder>> = reminderDao.observeAll()

    suspend fun saveReminder(reminder: Reminder): Reminder {
        val id = reminderDao.save(reminder)
        val saved = reminder.copy(id = id)
        scheduler.schedule(saved)
        return saved
    }

    suspend fun deleteReminder(reminder: Reminder) {
        scheduler.cancel(reminder.id)
        reminderDao.delete(reminder)
    }

    suspend fun seedDefaultReminders(defaults: List<Reminder>) {
        reminderDao.insertAll(defaults)
        scheduler.rescheduleAll()
    }
}
