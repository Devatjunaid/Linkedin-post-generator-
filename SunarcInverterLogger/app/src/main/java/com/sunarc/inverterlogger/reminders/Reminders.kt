package com.sunarc.inverterlogger.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sunarc.inverterlogger.MainActivity
import com.sunarc.inverterlogger.R
import com.sunarc.inverterlogger.SunarcApp
import com.sunarc.inverterlogger.domain.Aggregation
import com.sunarc.inverterlogger.domain.SolarMath
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * One reminder per day, 20 minutes before local sunset (worked out offline from the system's
 * location), plus a summary on the first day of each month. Uses WorkManager, so no exact-alarm
 * permission is needed.
 */
object ReminderScheduler {
    private const val WORK_NAME = "sunset-reminder"
    const val CHANNEL_ID = "reminders"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.notif_channel_reminders), NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = context.getString(R.string.notif_channel_reminders_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    suspend fun reschedule(context: Context) {
        val app = context.applicationContext as SunarcApp
        val profile = app.container.settings.current()
        val wm = WorkManager.getInstance(context)
        if (!profile.onboarded || (!profile.sunsetReminder && !profile.monthlySummary)) {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }
        val now = ZonedDateTime.now(profile.zone)
        var day = now.toLocalDate()
        var at: ZonedDateTime? = null
        repeat(3) {
            val (_, sunset) = SolarMath.sunriseSunset(day, profile.lat, profile.lon, profile.zone)
            val candidate = (sunset ?: day.atTime(19, 0).atZone(profile.zone)).minusMinutes(20)
            if (at == null && candidate.isAfter(now.plusMinutes(1))) at = candidate
            day = day.plusDays(1)
        }
        val delay = Duration.between(now, at ?: now.plusHours(24)).toMillis().coerceAtLeast(60_000)
        wm.enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build()
        )
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as SunarcApp
        val profile = app.container.settings.current()
        val readings = app.container.log.allReadings()
        val today = LocalDate.now(profile.zone)

        if (ReminderScheduler.canNotify(applicationContext)) {
            if (profile.sunsetReminder) {
                val scannedToday = readings.any { it.timestamp.atZone(profile.zone).toLocalDate() == today && it.todayKwh != null }
                val text = if (scannedToday) "Sun is setting. One more scan gives you the full day's total."
                else "Sun is setting. Scan your inverter to log today's energy."
                notify(1, "Time for today's reading", text)
            }
            if (profile.monthlySummary && today.dayOfMonth == 1) {
                val last = YearMonth.from(today).minusMonths(1)
                val kwh = Aggregation.month(Aggregation.dailyYields(readings, profile.zone), last).sumOf { it.kwh }
                if (kwh > 0) notify(2, "${last.month.name.lowercase().replaceFirstChar { it.uppercase() }} summary", "Your system made %.0f kWh last month.".format(kwh))
            }
        }
        ReminderScheduler.reschedule(applicationContext)
        return Result.success()
    }

    @SuppressLint("MissingPermission") // checked by ReminderScheduler.canNotify before calling
    private fun notify(id: Int, title: String, text: String) {
        val open = PendingIntent.getActivity(
            applicationContext, id,
            Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(applicationContext).notify(id, n)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and now.
        }
    }
}
