package com.devatjunaid.examclock.notifications

import android.content.Context
import android.text.format.DateFormat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.devatjunaid.examclock.ExamClockApp
import com.devatjunaid.examclock.R
import com.devatjunaid.examclock.domain.Countdown
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Daily study reminder ("3 sessions planned today, first at 16:00 · Physics in 12 days") plus a
 * one-time heads-up when an exam is 30, 14, 7, 3 and 1 day(s) away.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ExamClockApp
        val prefs = app.container.preferences.current()
        if (!prefs.onboardingDone || !prefs.remindersEnabled) return Result.success()

        val repo = app.container.repository
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val upcoming = repo.upcomingExams()
        val next = upcoming.firstOrNull() ?: return Result.success()
        val nextDate = Instant.ofEpochMilli(next.examAt!!).atZone(zone).toLocalDate()
        val days = Countdown.calendarDays(today, nextDate)

        val open = repo.openSessionsOn(today)
        val ctx = applicationContext
        val examPart = if (days <= 0L) {
            ctx.getString(R.string.notif_exam_today, next.title)
        } else {
            ctx.resources.getQuantityString(R.plurals.notif_exam_in_days, days.toInt(), next.title, days.toInt())
        }
        val text = if (open.isEmpty()) {
            ctx.getString(R.string.notif_nothing_planned, examPart)
        } else {
            val first = DateFormat.getTimeFormat(ctx).format(Date(open.first().startAt))
            ctx.resources.getQuantityString(R.plurals.notif_sessions_today, open.size, open.size, first) + " · " + examPart
        }
        Notifications.show(ctx, Notifications.ID_DAILY, Notifications.CHANNEL_REMINDERS, ctx.getString(R.string.notif_daily_title), text)

        val sent = app.container.preferences.milestonesSent()
        for (exam in upcoming) {
            val d = Countdown.calendarDays(today, Instant.ofEpochMilli(exam.examAt!!).atZone(zone).toLocalDate())
            if (d !in MILESTONES) continue
            val key = "${exam.id}:$d"
            if (key in sent) continue
            Notifications.show(
                ctx,
                Notifications.milestoneId(exam.id),
                Notifications.CHANNEL_REMINDERS,
                ctx.resources.getQuantityString(R.plurals.notif_milestone_title, d.toInt(), exam.title, d.toInt()),
                ctx.getString(R.string.notif_milestone_text),
            )
            app.container.preferences.addMilestone(key)
        }
        return Result.success()
    }

    companion object {
        private val MILESTONES = setOf(30L, 14L, 7L, 3L, 1L)
    }
}

object ReminderScheduler {
    private const val WORK_NAME = "daily_study_reminder"

    fun schedule(context: Context, enabled: Boolean, minuteOfDay: Int) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(LocalTime.of(minuteOfDay / 60, minuteOfDay % 60))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMillis()
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }
}
