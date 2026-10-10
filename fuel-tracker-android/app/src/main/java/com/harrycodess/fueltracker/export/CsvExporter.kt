package com.harrycodess.fueltracker.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.harrycodess.fueltracker.data.local.Trip
import com.harrycodess.fueltracker.data.settings.UserSettings
import com.harrycodess.fueltracker.domain.FuelMath
import java.io.File
import java.time.LocalDate
import java.util.Locale

object CsvExporter {

    /** Writes [trips] to a CSV in the cache dir (in the user's units) and returns a share intent. Call off the main thread. */
    fun createShareIntent(context: Context, trips: List<Trip>, settings: UserSettings): Intent {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "fuel-trips-${LocalDate.now()}.csv")
        file.bufferedWriter().use { writer ->
            writer.write(buildCsv(trips, settings))
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Fuel trips export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Export trips")
    }

    fun buildCsv(trips: List<Trip>, settings: UserSettings): String {
        val u = settings.units
        val header = listOf(
            "Date",
            "Vehicle",
            "Note",
            "Distance (${u.distanceUnit})",
            "Fuel (${u.volumeUnit})",
            "Fuel price (${settings.currency}/${u.volumeUnit})",
            "Efficiency (${u.efficiencyUnit})",
            "Total cost (${settings.currency})",
        )
        val rows = trips.map { t ->
            listOf(
                LocalDate.ofEpochDay(t.dateEpochDay).toString(),
                t.vehicleName,
                t.note,
                num(FuelMath.kmToDistance(t.distanceKm, u.system)),
                num(FuelMath.litersToVolume(t.fuelLiters, u.system)),
                num(FuelMath.perLiterToPrice(t.fuelPricePerLiter, u.system), 3),
                num(FuelMath.kmPerLToEfficiency(t.efficiencyKmPerL, u)),
                num(t.totalCost),
            )
        }
        return (listOf(header) + rows).joinToString("\n") { row -> row.joinToString(",") { escape(it) } } + "\n"
    }

    private fun num(value: Double, decimals: Int = 2) = String.format(Locale.US, "%.${decimals}f", value)

    private fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
}
