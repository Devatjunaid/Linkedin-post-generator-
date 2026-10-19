package com.dailycash.tracker.data

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Currency
import java.util.Locale

data class Place(val countryCode: String, val countryName: String, val currency: String)

/**
 * Works out the country the phone is in from the mobile network (no location permission needed),
 * falling back to the SIM and then the device language region.
 */
object CountryDetector {

    fun detect(context: Context): Place {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val cc = listOfNotNull(
            tm?.networkCountryIso,
            tm?.simCountryIso,
            Locale.getDefault().country,
        ).map { it.uppercase(Locale.ROOT) }.firstOrNull { it.length == 2 } ?: "US"
        return placeFor(cc)
    }

    fun placeFor(countryCode: String): Place {
        val locale = Locale.Builder().setRegion(countryCode).build()
        val currency = runCatching { Currency.getInstance(locale).currencyCode }.getOrNull() ?: "USD"
        return Place(countryCode, countryName(countryCode), currency)
    }

    fun countryName(countryCode: String): String =
        runCatching { Locale.Builder().setRegion(countryCode).build().getDisplayCountry(Locale.getDefault()) }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: countryCode
}
