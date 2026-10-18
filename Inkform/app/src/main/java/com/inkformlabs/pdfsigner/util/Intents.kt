package com.inkformlabs.pdfsigner.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.inkformlabs.pdfsigner.pdf.PdfPrintAdapter
import java.io.File

object Intents {

    fun fileProviderUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun sharePdf(context: Context, file: File, title: String) {
        val uri = fileProviderUri(context, file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(send, "Share PDF").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app available to share PDFs", Toast.LENGTH_SHORT).show()
        }
    }

    fun printPdf(context: Context, file: File, jobName: String) {
        val manager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (manager == null) {
            Toast.makeText(context, "Printing isn't available on this device", Toast.LENGTH_SHORT).show()
            return
        }
        manager.print(jobName, PdfPrintAdapter(file, jobName), PrintAttributes.Builder().build())
    }

    fun openUrl(context: Context, url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
        }
    }

    fun rateApp(context: Context) {
        val pkg = context.packageName
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
        } catch (e: ActivityNotFoundException) {
            openUrl(context, "https://play.google.com/store/apps/details?id=$pkg")
        }
    }

    fun shareApp(context: Context) {
        val pkg = context.packageName
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Inkform fills and signs PDF forms right on your phone, offline: https://play.google.com/store/apps/details?id=$pkg",
            )
        }
        try {
            context.startActivity(Intent.createChooser(send, "Share Inkform"))
        } catch (e: ActivityNotFoundException) {
            // Nothing to share with
        }
    }
}
