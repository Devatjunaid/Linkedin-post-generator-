package com.aquadoselab.poolchemistry.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import com.aquadoselab.poolchemistry.AquaDoseApp
import com.aquadoselab.poolchemistry.chem.Units
import com.aquadoselab.poolchemistry.chem.Volume
import com.aquadoselab.poolchemistry.data.AppData
import com.aquadoselab.poolchemistry.data.AppSettings
import com.aquadoselab.poolchemistry.data.Pool
import com.aquadoselab.poolchemistry.data.ProductChoice
import com.aquadoselab.poolchemistry.data.WaterTest
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as AquaDoseApp).repository
    val data: StateFlow<AppData> = repo.data

    fun newId(): String = UUID.randomUUID().toString()

    fun savePool(pool: Pool) {
        val fixed = pool.copy(volumeGallons = Volume.gallons(pool))
        repo.update { d ->
            val exists = d.pools.any { it.id == fixed.id }
            val pools = if (exists) d.pools.map { if (it.id == fixed.id) fixed else it } else d.pools + fixed
            val active = if (!exists || d.settings.activePoolId == null) fixed.id else d.settings.activePoolId
            d.copy(pools = pools, settings = d.settings.copy(activePoolId = active, onboardingDone = true))
        }
    }

    fun deletePool(id: String) = repo.update { d ->
        val pools = d.pools.filterNot { it.id == id }
        val active = if (d.settings.activePoolId == id) pools.firstOrNull()?.id else d.settings.activePoolId
        d.copy(
            pools = pools,
            tests = d.tests.filterNot { it.poolId == id },
            settings = d.settings.copy(activePoolId = active)
        )
    }

    fun setActivePool(id: String) = repo.update { it.copy(settings = it.settings.copy(activePoolId = id)) }

    fun updateProducts(poolId: String, transform: (ProductChoice) -> ProductChoice) = repo.update { d ->
        d.copy(pools = d.pools.map { if (it.id == poolId) it.copy(products = transform(it.products)) else it })
    }

    fun saveTest(test: WaterTest) = repo.update { d -> d.copy(tests = d.tests.filterNot { it.id == test.id } + test) }

    fun deleteTest(id: String) = repo.update { d -> d.copy(tests = d.tests.filterNot { it.id == id }) }

    fun setDosesApplied(testId: String, applied: Boolean) = repo.update { d ->
        d.copy(tests = d.tests.map { if (it.id == testId) it.copy(dosesApplied = applied) else it })
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) =
        repo.update { it.copy(settings = transform(it.settings)) }

    /** Writes the pool's history as CSV and returns a share intent, or null when there is nothing to export. */
    fun exportCsvIntent(context: Context, poolId: String): Intent? {
        val d = data.value
        val pool = d.pools.firstOrNull { it.id == poolId } ?: return null
        val tests = d.testsFor(poolId).sortedBy { it.timestamp }
        if (tests.isEmpty()) return null
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        fun v(x: Double?) = x?.let { Units.num(it).replace(",", "") } ?: ""
        val sb = StringBuilder("date,free_chlorine,combined_chlorine,ph,total_alkalinity,calcium_hardness,cya,salt,temp_f,chemicals_added,note\n")
        tests.forEach { t ->
            sb.append(fmt.format(Date(t.timestamp))).append(',')
                .append(v(t.fc)).append(',').append(v(t.cc)).append(',').append(v(t.ph)).append(',')
                .append(v(t.ta)).append(',').append(v(t.ch)).append(',').append(v(t.cya)).append(',')
                .append(v(t.salt)).append(',').append(v(t.tempF)).append(',')
                .append(if (t.dosesApplied) "yes" else "no").append(',')
                .append('"').append(t.note.replace("\"", "\"\"")).append('"').append('\n')
        }
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = pool.name.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').ifBlank { "pool" }
        val file = File(dir, "${safeName}_history.csv")
        file.writeText(sb.toString())
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${pool.name} — water test history")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Export history")
    }
}
