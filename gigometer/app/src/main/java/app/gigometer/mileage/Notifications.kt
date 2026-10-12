package app.gigometer.mileage

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.gigometer.mileage.tracking.Permissions

object Notifications {
    const val CHANNEL_TRACKING = "tracking"
    const val CHANNEL_TRIPS = "trips"
    const val ID_TRACKING = 42
    const val ID_SAVED = 43
    const val ID_REMINDER = 44

    const val EXTRA_OPEN = "open"
    const val OPEN_LIVE = "live"
    const val OPEN_TRIPS = "trips"

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_TRACKING, ctx.getString(R.string.channel_tracking), NotificationManager.IMPORTANCE_LOW).apply {
                description = ctx.getString(R.string.channel_tracking_desc)
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_TRIPS, ctx.getString(R.string.channel_trips), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = ctx.getString(R.string.channel_trips_desc)
            }
        )
    }

    fun openAppIntent(ctx: Context, open: String, requestCode: Int): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_OPEN, open)
        return PendingIntent.getActivity(ctx, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    @SuppressLint("MissingPermission")
    fun post(ctx: Context, id: Int, title: String, text: String, open: String) {
        if (!Permissions.hasNotifications(ctx)) return
        val n = NotificationCompat.Builder(ctx, CHANNEL_TRIPS)
            .setSmallIcon(R.drawable.ic_stat_gauge)
            .setColor(0xFF1B6B45.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(ctx, open, id))
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }
}
