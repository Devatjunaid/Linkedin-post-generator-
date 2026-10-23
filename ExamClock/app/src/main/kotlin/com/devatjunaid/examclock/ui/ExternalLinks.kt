package com.devatjunaid.examclock.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.devatjunaid.examclock.R

object ExternalLinks {
    /** Opens the Play Store listing (falls back to the web page when the Play Store is missing). */
    fun openStoreListing(context: Context) {
        val pkg = context.packageName.removeSuffix(".debug")
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(market)
        } catch (_: ActivityNotFoundException) {
            val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(web)
            } catch (_: ActivityNotFoundException) {
                // No browser either; nothing sensible to do.
            }
        }
    }

    fun shareApp(context: Context) {
        val pkg = context.packageName.removeSuffix(".debug")
        val text = context.getString(R.string.share_text, "https://play.google.com/store/apps/details?id=$pkg")
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(Intent.createChooser(send, context.getString(R.string.settings_share)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
