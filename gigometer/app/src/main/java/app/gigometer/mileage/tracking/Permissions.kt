package app.gigometer.mileage.tracking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object Permissions {
    private fun granted(ctx: Context, p: String) =
        ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    fun hasLocation(ctx: Context) = granted(ctx, Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasBackgroundLocation(ctx: Context) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || granted(ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    fun hasActivityRecognition(ctx: Context) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) granted(ctx, Manifest.permission.ACTIVITY_RECOGNITION)
        else true // pre-Q uses the install-time com.google.android.gms permission

    fun hasNotifications(ctx: Context) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || granted(ctx, Manifest.permission.POST_NOTIFICATIONS)

    /** Everything automatic detection needs. */
    fun autoReady(ctx: Context) = hasLocation(ctx) && hasBackgroundLocation(ctx) && hasActivityRecognition(ctx)
}
