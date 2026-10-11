package com.shelfguard.app.util

import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.shelfguard.app.ads.FullScreenAdGate

/**
 * Opens Google's ready-made barcode scanner (no camera permission needed; Play services
 * hosts the camera UI). Leaving for the scanner must not count as "re-opening" the app,
 * so app-open ads are suppressed for that round trip.
 */
fun startBarcodeScan(context: Context, onResult: (String) -> Unit, onError: (String) -> Unit = {}) {
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39, Barcode.FORMAT_QR_CODE,
        )
        .enableAutoZoom()
        .build()
    FullScreenAdGate.skipNextAppOpen = true
    val clearSkip = { Handler(Looper.getMainLooper()).postDelayed({ FullScreenAdGate.skipNextAppOpen = false }, 1500) }
    GmsBarcodeScanning.getClient(context, options).startScan()
        .addOnSuccessListener { barcode ->
            clearSkip()
            barcode.rawValue?.trim()?.takeIf { it.isNotEmpty() }?.let(onResult)
        }
        .addOnCanceledListener { clearSkip() }
        .addOnFailureListener { e ->
            clearSkip()
            onError(e.message ?: "Scanner not available")
        }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
