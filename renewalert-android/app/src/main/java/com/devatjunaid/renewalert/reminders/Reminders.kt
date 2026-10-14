package com.devatjunaid.renewalert.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.devatjunaid.renewalert.MainActivity
import com.devatjunaid.renewalert.R
import com.devatjunaid.renewalert.RenewAlertApp
import com.devatjunaid.renewalert.container
import com.devatjunaid.renewalert.data.SubStatus
import com.devatjunaid.renewalert.domain.Billing
import com.devatjunaid.renewalert.domain.Formats
import com.devatjunaid.renewalert.domain.Portfolio
import com.devatjunaid.renewalert.widget.WidgetUpdater
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val WORK_NAME = "renewalert-daily-reminders"

    /** Runs once a day around 9:00. WorkManager survives reboots; no exact-alarm permission needed. */
    fun schedule(context: Context) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(LocalTime.of(9, 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMinutes()
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        c.repository.convertEndedTrials()
        val settings = c.settings.current()
        val today = LocalDate.now()
        WidgetUpdater.update(applicationContext)
        if (settings.lastReminderRunDay == today.toEpochDay()) return Result.success()
        c.settings.setLastReminderRunDay(today.toEpochDay())

        val subs = c.repository.all()
        val portfolio = Portfolio(subs, c.repository.allPrices(), settings, today)
        val hide = settings.hideAmounts

        if (settings.renewalReminders) {
            subs.filter { it.status == SubStatus.ACTIVE && it.reminderDaysBefore >= 0 }.forEach { s ->
                val next = Billing.nextCharge(s, today) ?: return@forEach
                if (Billing.daysUntil(today, next) == s.reminderDaysBefore.toLong()) {
                    Notifier.show(
                        applicationContext, RenewAlertApp.CHANNEL_RENEWALS, s.id.toInt(),
                        "${s.name} renews ${Formats.relativeDay(today, next)}",
                        "${Formats.money(s.priceMinor, s.currency, hide)} on ${Formats.shortDate(next)}. Tap to review.",
                        s.id,
                    )
                }
            }
        }
        if (settings.trialAlerts) {
            subs.filter { it.status == SubStatus.TRIAL }.forEach { t ->
                val end = LocalDate.ofEpochDay(t.trialEndEpochDay ?: t.firstPaymentEpochDay)
                val d = Billing.daysUntil(today, end)
                if (d == 3L || d == 1L || d == 0L) {
                    Notifier.show(
                        applicationContext, RenewAlertApp.CHANNEL_TRIALS, 100_000 + t.id.toInt(),
                        "${t.name} free trial ends ${Formats.relativeDay(today, end)}",
                        "Then ${Formats.money(t.priceMinor, t.currency, hide)} " +
                            "${Billing.cycleLabel(t.cycleUnit, t.cycleCount).lowercase()}. Cancel now if you don't want it.",
                        t.id,
                    )
                }
            }
        }
        if (settings.weeklyDigest && today.dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            val week = portfolio.upcoming(7)
            val total = week.sumOf { it.amountMain }
            Notifier.show(
                applicationContext, RenewAlertApp.CHANNEL_DIGEST, 999_999,
                "Your week ahead",
                if (week.isEmpty()) "No subscription charges in the next 7 days."
                else "${week.size} charge${if (week.size == 1) "" else "s"} coming up, " +
                    "${Formats.money(total, settings.currency, hide)} in total.",
                null,
            )
        }
        return Result.success()
    }
}

object Notifier {
    const val EXTRA_SUBSCRIPTION_ID = "subscription_id"

    @SuppressLint("MissingPermission")
    fun show(context: Context, channel: String, id: Int, title: String, text: String, subscriptionId: Long?) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= 33
        ) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            subscriptionId?.let { putExtra(EXTRA_SUBSCRIPTION_ID, it) }
        }
        val pi = PendingIntent.getActivity(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.brand_blue))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }
}
