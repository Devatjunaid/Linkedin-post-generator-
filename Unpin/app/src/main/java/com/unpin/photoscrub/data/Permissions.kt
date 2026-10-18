package com.unpin.photoscrub.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object Permissions {
    /** Only exists (and only matters) on Android 10+. */
    val mediaLocation: String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACCESS_MEDIA_LOCATION else null

    /** Only needed on Android 8-9 to write into Pictures/Unpin. */
    val legacyWrite: String? =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) Manifest.permission.WRITE_EXTERNAL_STORAGE else null

    fun hasMediaLocation(context: Context) = granted(context, mediaLocation)
    fun hasLegacyWrite(context: Context) = granted(context, legacyWrite)

    private fun granted(context: Context, permission: String?) =
        permission == null ||
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
