package com.shelfguard.app.notifications

import android.Manifest
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
import com.shelfguard.app.MainActivity
import com.shelfguard.app.R
import com.shelfguard.app.domain.AlertSummary

object NotificationHelper {
    const val CHANNEL_ALERTS = "stock_alerts"
    const val EXTRA_OPEN_ALERTS = "open_alerts"
    private const val ID_DAILY = 1001
    private const val ID_LOW_STOCK_BASE = 2000

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ALERTS,
            context.getString(R.string.notif_channel_alerts),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.notif_channel_alerts_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun openAlertsIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_ALERTS, true)
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** The once-a-day summary. Posts nothing when there is nothing to report. */
    fun postDailySummary(context: Context, summary: AlertSummary) {
        if (!canPost(context) || summary.total == 0) return
        val parts = buildList {
            if (summary.expired.isNotEmpty()) add("${summary.expired.size} expired")
            if (summary.expiringSoon.isNotEmpty()) add("${summary.expiringSoon.size} expiring soon")
            if (summary.outOfStock.isNotEmpty()) add("${summary.outOfStock.size} out of stock")
            if (summary.lowStock.isNotEmpty()) add("${summary.lowStock.size} low on stock")
        }
        val style = NotificationCompat.InboxStyle()
        summary.expired.take(2).forEach { style.addLine("Expired: ${it.product.name}") }
        summary.expiringSoon.take(2).forEach { style.addLine("Expiring: ${it.product.name}") }
        summary.outOfStock.take(1).forEach { style.addLine("Out of stock: ${it.product.name}") }
        summary.lowStock.take(2).forEach { style.addLine("Low stock: ${it.product.name} (${it.sellableQty} left)") }
        style.setSummaryText("Tap to review")

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_shelfguard)
            .setColor(ContextCompat.getColor(context, R.color.brand_green))
            .setContentTitle("${summary.total} products need attention")
            .setContentText(parts.joinToString(" · "))
            .setStyle(style)
            .setContentIntent(openAlertsIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ID_DAILY, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post.
        }
    }

    /** Instant heads-up when a sale pushes a product to (or below) its minimum. */
    fun postLowStock(context: Context, productId: Long, name: String, remaining: Int, unit: String) {
        if (!canPost(context)) return
        val text = if (remaining == 0) "$name is now out of stock" else "$name is low: $remaining $unit left"
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_shelfguard)
            .setColor(ContextCompat.getColor(context, R.color.brand_green))
            .setContentTitle(if (remaining == 0) "Out of stock" else "Low stock")
            .setContentText(text)
            .setContentIntent(openAlertsIntent(context))
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ID_LOW_STOCK_BASE + (productId % 1000).toInt(), notification)
        } catch (_: SecurityException) {
        }
    }
}
