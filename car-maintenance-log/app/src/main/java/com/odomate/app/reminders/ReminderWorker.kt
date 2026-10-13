package com.odomate.app.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.odomate.app.OdoMateApp
import com.odomate.app.domain.Level
import com.odomate.app.util.Formats
import kotlin.math.abs

/**
 * Runs once a day. Notifies when a service moves into "due soon" or "overdue"
 * (and repeats overdue alerts weekly), plus an optional weekly odometer-photo nudge.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as OdoMateApp).container
        val settings = container.settings.current()
        if (!settings.notificationsEnabled) return Result.success()
        val force = inputData.getBoolean(KEY_FORCE, false)
        val now = System.currentTimeMillis()
        val unit = settings.distanceUnit
        val vehicles = container.db.vehicles().getAll()
        var anySent = false

        for (vehicle in vehicles) {
            val snapshot = container.repository.snapshotOnce(vehicle.id) ?: continue
            for (status in snapshot.statuses) {
                val type = status.type
                val level = when (status.level) {
                    Level.OVERDUE -> 2
                    Level.SOON -> 1
                    else -> 0
                }
                if (level == 0) {
                    if (type.notifiedLevel != 0) container.db.serviceTypes().update(type.copy(notifiedLevel = 0, notifiedAt = 0))
                    continue
                }
                val escalated = level > type.notifiedLevel
                val repeatOverdue = level == 2 && now - type.notifiedAt > WEEK_MS
                if (!(escalated || repeatOverdue || force)) continue

                val detail = describe(status.remainingDistance, status.remainingDays, status.distanceDominant, unit)
                val title = if (level == 2) "${type.name} is overdue" else "${type.name} is due soon"
                val text = "${vehicle.name}: $detail" + (status.dueOdometer?.let { " · due at ${Formats.distance(it, unit)}" } ?: "")
                Notifications.showServiceReminder(applicationContext, type.id, vehicle.id, title, text)
                container.db.serviceTypes().update(type.copy(notifiedLevel = level, notifiedAt = now))
                anySent = true
            }

            if (settings.weeklyNudge && now - settings.lastNudgeAt > WEEK_MS) {
                val last = snapshot.lastReading?.date ?: 0L
                if (now - last > WEEK_MS) {
                    Notifications.showScanNudge(applicationContext, vehicle.name)
                    container.settings.setLastNudge(now)
                }
            }
        }

        if (force && !anySent) {
            Notifications.showServiceReminder(
                applicationContext, 0, 0, "Reminders are working",
                "You're all caught up. We'll alert you here when a service is due."
            )
        }
        return Result.success()
    }

    companion object {
        const val KEY_FORCE = "force"
        private const val WEEK_MS = 7L * 24 * 3600 * 1000

        fun describe(remainingDistance: Int?, remainingDays: Long?, distanceDominant: Boolean, unit: String): String {
            return if (distanceDominant && remainingDistance != null) {
                if (remainingDistance < 0) "${Formats.distance(abs(remainingDistance), unit)} overdue"
                else "${Formats.distance(remainingDistance, unit)} left"
            } else if (remainingDays != null) {
                when {
                    remainingDays < 0 -> "${Formats.days(remainingDays)} overdue"
                    remainingDays == 0L -> "due today"
                    else -> "${Formats.days(remainingDays)} left"
                }
            } else "check now"
        }
    }
}
