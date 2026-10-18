package com.unpin.photoscrub.data

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/** One finished clean-up, as shown on the report and in History. */
data class ScrubSession(
    val id: Long,
    val timestamp: Long,
    val photoCount: Int,
    val failedCount: Int,
    val fieldsRemoved: Int,
    val locationsErased: Int,
    val bytesSaved: Long,
    val losslessCount: Int,
    val verifiedCount: Int,
    val outputs: List<Uri>,
    val failures: List<String> = emptyList(),
) {
    fun toJson() = JSONObject().apply {
        put("id", id); put("ts", timestamp); put("n", photoCount); put("failed", failedCount)
        put("fields", fieldsRemoved); put("loc", locationsErased); put("saved", bytesSaved)
        put("lossless", losslessCount); put("verified", verifiedCount)
        put("out", JSONArray(outputs.map { it.toString() }))
        put("errors", JSONArray(failures))
    }

    companion object {
        fun fromJson(o: JSONObject) = ScrubSession(
            id = o.getLong("id"),
            timestamp = o.getLong("ts"),
            photoCount = o.getInt("n"),
            failedCount = o.optInt("failed"),
            fieldsRemoved = o.getInt("fields"),
            locationsErased = o.getInt("loc"),
            bytesSaved = o.getLong("saved"),
            losslessCount = o.getInt("lossless"),
            verifiedCount = o.getInt("verified"),
            outputs = o.optJSONArray("out")?.let { a -> (0 until a.length()).map { Uri.parse(a.getString(it)) } }.orEmpty(),
            failures = o.optJSONArray("errors")?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty(),
        )

        fun from(outcomes: List<ScrubOutcome>): ScrubSession {
            val ok = outcomes.filter { it.error == null }
            val now = System.currentTimeMillis()
            return ScrubSession(
                id = now,
                timestamp = now,
                photoCount = ok.size,
                failedCount = outcomes.size - ok.size,
                fieldsRemoved = ok.sumOf { it.fieldsRemoved },
                locationsErased = ok.count { it.locationErased },
                bytesSaved = ok.sumOf { it.originalBytes - it.newBytes },
                losslessCount = ok.count { it.lossless },
                verifiedCount = ok.count { it.verified },
                outputs = ok.mapNotNull { it.outputUri },
                failures = outcomes.filter { it.error != null }.map { "${it.sourceName}: ${it.error}" },
            )
        }
    }
}

data class ScanStats(val total: Int, val withGps: Int, val withDevice: Int, val withDate: Int)

/** Small on-device store. Nothing here ever leaves the phone. */
class Storage(context: Context) {
    private val prefs = context.getSharedPreferences("unpin", Context.MODE_PRIVATE)

    var quickShare: Boolean
        get() = prefs.getBoolean("quick_share", true)
        set(v) = prefs.edit { putBoolean("quick_share", v) }

    var anonymizeNames: Boolean
        get() = prefs.getBoolean("anonymize", false)
        set(v) = prefs.edit { putBoolean("anonymize", v) }

    var askedMediaLocation: Boolean
        get() = prefs.getBoolean("asked_media_location", false)
        set(v) = prefs.edit { putBoolean("asked_media_location", v) }

    var lastScan: ScanStats?
        get() = if (!prefs.contains("scan_total")) null else ScanStats(
            prefs.getInt("scan_total", 0), prefs.getInt("scan_gps", 0),
            prefs.getInt("scan_device", 0), prefs.getInt("scan_date", 0),
        )
        set(v) = prefs.edit {
            if (v == null) {
                remove("scan_total"); remove("scan_gps"); remove("scan_device"); remove("scan_date")
            } else {
                putInt("scan_total", v.total); putInt("scan_gps", v.withGps)
                putInt("scan_device", v.withDevice); putInt("scan_date", v.withDate)
            }
        }

    fun history(): List<ScrubSession> = runCatching {
        val arr = JSONArray(prefs.getString("history", "[]"))
        (0 until arr.length()).map { ScrubSession.fromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    fun addSession(session: ScrubSession) {
        val list = (listOf(session) + history()).take(MAX_SESSIONS)
        prefs.edit { putString("history", JSONArray(list.map { it.toJson() }).toString()) }
    }

    fun clearHistory() {
        prefs.edit { remove("history"); remove("scan_total"); remove("scan_gps"); remove("scan_device"); remove("scan_date") }
    }

    private companion object {
        const val MAX_SESSIONS = 40
    }
}
