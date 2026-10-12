package app.gigometer.mileage.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.gigometer.mileage.Graph
import app.gigometer.mileage.Notifications
import app.gigometer.mileage.R
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Daily 9 PM nudge to classify the day's unreviewed trips. */
class ReviewReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val count = Graph.trips.unreviewedCount()
        if (count > 0) {
            Notifications.post(
                applicationContext, Notifications.ID_REMINDER,
                applicationContext.getString(R.string.reminder_title),
                applicationContext.resources.getQuantityString(R.plurals.reminder_text, count, count),
                Notifications.OPEN_TRIPS,
            )
        }
        return Result.success()
    }

    companion object {
        private const val NAME = "review-reminder"

        fun schedule(ctx: Context, enabled: Boolean) {
            val wm = WorkManager.getInstance(ctx)
            if (!enabled) { wm.cancelUniqueWork(NAME); return }
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(21, 0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val delay = Duration.between(now, next).toMinutes()
            val req = PeriodicWorkRequestBuilder<ReviewReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .build()
            wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
        }
    }
}
