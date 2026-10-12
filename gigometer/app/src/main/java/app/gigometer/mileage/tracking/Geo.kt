package app.gigometer.mileage.tracking

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import app.gigometer.mileage.data.GeoPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

object Geo {
    /** "123 Peachtree St, Atlanta" — or null when the phone's geocoder can't resolve it. */
    suspend fun shortAddress(context: Context, p: GeoPoint?): String? {
        if (p == null || !Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.US)
        val address: Address? = withTimeoutOrNull(8_000) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(p.lat, p.lng, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) { if (cont.isActive) cont.resume(addresses.firstOrNull()) }
                        override fun onError(errorMessage: String?) { if (cont.isActive) cont.resume(null) }
                    })
                }
            } else {
                @Suppress("DEPRECATION")
                runCatching { geocoder.getFromLocation(p.lat, p.lng, 1)?.firstOrNull() }.getOrNull()
            }
        }
        return address?.let(::format)
    }

    private fun format(a: Address): String? {
        val street = listOfNotNull(a.subThoroughfare, a.thoroughfare).joinToString(" ").ifBlank { null }
        val city = a.locality ?: a.subAdminArea
        return listOfNotNull(street, city).joinToString(", ").ifBlank { a.getAddressLine(0) }
    }
}
