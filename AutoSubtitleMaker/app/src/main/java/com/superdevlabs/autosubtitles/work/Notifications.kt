package com.superdevlabs.autosubtitles.work

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.superdevlabs.autosubtitles.MainActivity
import com.superdevlabs.autosubtitles.R

/** Progress + "done" notifications for background subtitle and export jobs. */
object Notifications {

    const val CHANNEL_PROGRESS = "jobs_progress"
    const val CHANNEL_DONE = "jobs_done"
    const val EXTRA_OPEN_PROJECT = "open_project"
    const val EXTRA_OPEN_TARGET = "open_target"
    const val TARGET_EDITOR = "editor"
    const val TARGET_EXPORT = "export"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_PROGRESS, "Work in progress", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows progress while subtitles are created or a video is exported"
                setShowBadge(false)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, "Finished", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Tells you when subtitles or an exported video are ready"
            },
        )
    }

    fun progress(
        context: Context,
        title: String,
        text: String,
        progress: Float,
        cancelIntent: PendingIntent?,
        projectId: String,
        target: String,
    ) = NotificationCompat.Builder(context, CHANNEL_PROGRESS)
        .setSmallIcon(R.drawable.ic_stat_subtitles)
        .setContentTitle(title)
        .setContentText(text)
        .setOnlyAlertOnce(true)
        .setOngoing(true)
        .setSilent(true)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setProgress(100, (progress * 100).toInt().coerceIn(0, 100), progress <= 0f)
        .setContentIntent(openApp(context, projectId, target, requestCode = 1))
        .apply {
            if (cancelIntent != null) addAction(0, "Cancel", cancelIntent)
        }
        .build()

    /** Shown only while the app is in the background (the screen itself shows the result otherwise). */
    fun done(context: Context, id: Int, title: String, text: String, projectId: String, target: String) {
        if (isAppVisible()) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val notification = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_stat_subtitles)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(openApp(context, projectId, target, requestCode = 2))
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun cancel(context: Context, id: Int) = NotificationManagerCompat.from(context).cancel(id)

    private fun isAppVisible(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private fun openApp(context: Context, projectId: String, target: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_PROJECT, projectId)
            putExtra(EXTRA_OPEN_TARGET, target)
        }
        return PendingIntent.getActivity(
            context,
            projectId.hashCode() * 31 + requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Stable per-project notification ids, distinct for processing and export. */
    fun processingId(projectId: String): Int = 10_000 + (projectId.hashCode() and 0xFFFF)
    fun exportId(projectId: String): Int = 80_000 + (projectId.hashCode() and 0xFFFF)
    fun processingDoneId(projectId: String): Int = 150_000 + (projectId.hashCode() and 0xFFFF)
    fun exportDoneId(projectId: String): Int = 220_000 + (projectId.hashCode() and 0xFFFF)
}
