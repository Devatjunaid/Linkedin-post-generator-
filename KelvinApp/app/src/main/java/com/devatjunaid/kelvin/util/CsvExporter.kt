package com.devatjunaid.kelvin.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.devatjunaid.kelvin.domain.Moods
import com.devatjunaid.kelvin.domain.SessionRecord
import com.devatjunaid.kelvin.domain.Units
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Writes the session log to a CSV in the app cache and opens the system share sheet. No storage permission needed. */
object CsvExporter {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun exportAndShare(context: Context, sessions: List<SessionRecord>) {
        val uri = write(context, sessions)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Kelvin session log")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Export session log").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun write(context: Context, sessions: List<SessionRecord>): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "kelvin-sessions.csv")
        val zone = ZoneId.systemDefault()
        file.bufferedWriter().use { w ->
            w.write("date,type,title,total,cold_seconds,heat_seconds,rounds,water_temp_c,sauna_temp_c,mood,note\n")
            sessions.forEach { s ->
                val date = formatter.format(Instant.ofEpochMilli(s.startedAt).atZone(zone))
                val cells = listOf(
                    date,
                    s.kind.name.lowercase(),
                    s.title,
                    Units.clock(s.totalSec),
                    s.coldSec.toString(),
                    s.heatSec.toString(),
                    s.rounds.toString(),
                    s.coldTempC?.toString().orEmpty(),
                    s.heatTempC?.toString().orEmpty(),
                    Moods.label(s.mood).orEmpty(),
                    s.note,
                )
                w.write(cells.joinToString(",") { escape(it) })
                w.write("\n")
            }
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
}
