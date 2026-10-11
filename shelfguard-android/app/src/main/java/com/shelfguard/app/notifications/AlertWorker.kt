package com.shelfguard.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shelfguard.app.ShelfGuardApp
import com.shelfguard.app.domain.AlertSummary

/** Runs once a day and posts a summary of expired, expiring and low-stock products. */
class AlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as ShelfGuardApp).container
        if (!container.settings.current().remindersEnabled) return Result.success()
        val summary = AlertSummary.from(container.inventory.stockSnapshot())
        NotificationHelper.postDailySummary(applicationContext, summary)
        return Result.success()
    }
}
