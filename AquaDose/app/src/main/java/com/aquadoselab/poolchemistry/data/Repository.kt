package com.aquadoselab.poolchemistry.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Stores everything in one small JSON file in the app's private storage.
 * Reads happen once at start; every change is written in the background.
 */
class Repository(context: Context) {

    private val file = File(context.filesDir, "aquadose_data.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Mutex()

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private fun load(): AppData = try {
        if (file.exists()) json.decodeFromString(AppData.serializer(), file.readText()) else AppData()
    } catch (e: Exception) {
        // Corrupt file: keep a copy for support and start fresh rather than crash.
        runCatching { file.copyTo(File(file.parentFile, "aquadose_data.bak"), overwrite = true) }
        AppData()
    }

    fun update(transform: (AppData) -> AppData) {
        _data.update(transform)
        scope.launch {
            writeLock.withLock {
                // Always write the latest state, so out-of-order writes can't lose data.
                val snapshot = _data.value
                val tmp = File(file.parentFile, "aquadose_data.tmp")
                tmp.writeText(json.encodeToString(AppData.serializer(), snapshot))
                if (!tmp.renameTo(file)) {
                    file.writeText(tmp.readText())
                    tmp.delete()
                }
            }
        }
    }
}
