package com.superdevlabs.autosubtitles.speech

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream

sealed interface ModelState {
    data object NotInstalled : ModelState
    data class Downloading(val progress: Float) : ModelState
    data object Unpacking : ModelState
    data object Installed : ModelState
    data class Failed(val message: String) : ModelState
}

/** Downloads, unpacks and deletes offline speech models under files/models/<id>. */
class ModelManager(private val context: Context, private val appScope: CoroutineScope) {

    private val root = File(context.filesDir, "models").apply { mkdirs() }
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val _states = MutableStateFlow(ModelCatalog.models.associate { it.id to initialState(it.id) })
    val states: StateFlow<Map<String, ModelState>> = _states.asStateFlow()

    fun modelDir(id: String): File = File(root, id)

    fun isInstalled(id: String): Boolean = File(modelDir(id), MARKER).exists()

    fun state(id: String): ModelState = _states.value[id] ?: ModelState.NotInstalled

    /** Fire-and-forget install that keeps running while the user moves between screens. */
    fun startInstall(id: String) {
        appScope.launch { runCatching { install(id) } }
    }

    /** Installs the model if needed. Safe to call concurrently; throws on failure. */
    suspend fun install(id: String) {
        val lock = locks.getOrPut(id) { Mutex() }
        lock.withLock {
            if (isInstalled(id)) {
                setState(id, ModelState.Installed)
                return
            }
            val model = ModelCatalog.byId(id)
            val zip = File(context.cacheDir, "${model.id}.zip.part")
            try {
                setState(id, ModelState.Downloading(0f))
                download(model.url, zip) { progress -> setState(id, ModelState.Downloading(progress)) }
                setState(id, ModelState.Unpacking)
                unpack(zip, modelDir(id))
                setState(id, ModelState.Installed)
            } catch (t: Throwable) {
                modelDir(id).deleteRecursively()
                setState(id, ModelState.Failed(t.message ?: "Download failed"))
                throw t
            } finally {
                zip.delete()
            }
        }
    }

    fun delete(id: String) {
        modelDir(id).deleteRecursively()
        setState(id, ModelState.NotInstalled)
    }

    private fun initialState(id: String): ModelState =
        if (isInstalled(id)) ModelState.Installed else ModelState.NotInstalled

    private fun setState(id: String, state: ModelState) {
        _states.update { it + (id to state) }
    }

    private suspend fun download(url: String, target: File, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 30_000
                instanceFollowRedirects = true
            }
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("Server returned $code")
            val total = connection.contentLengthLong
            var read = 0L
            var lastReported = -1
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        if (total > 0) {
                            val percent = (read * 100 / total).toInt()
                            if (percent != lastReported) {
                                lastReported = percent
                                onProgress(percent / 100f)
                            }
                        }
                    }
                }
            }
        } finally {
            connection?.disconnect()
        }
    }

    /** Unzips into [dest], flattening the single top-level folder the Vosk zips contain. */
    private suspend fun unpack(zip: File, dest: File) = withContext(Dispatchers.IO) {
        val staging = File(root, "${dest.name}.staging").apply { deleteRecursively(); mkdirs() }
        val stagingPath = staging.canonicalPath + File.separator
        ZipInputStream(zip.inputStream().buffered()).use { zis ->
            while (true) {
                ensureActive()
                val entry = zis.nextEntry ?: break
                val out = File(staging, entry.name)
                // Zip-slip protection: never write outside the staging folder.
                if (!out.canonicalPath.startsWith(stagingPath)) throw IOException("Bad zip entry")
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { zis.copyTo(it) }
                }
                zis.closeEntry()
            }
        }
        val children = staging.listFiles().orEmpty()
        val contentRoot = if (children.size == 1 && children[0].isDirectory) children[0] else staging
        dest.deleteRecursively()
        if (!contentRoot.renameTo(dest)) {
            contentRoot.copyRecursively(dest, overwrite = true)
        }
        staging.deleteRecursively()
        File(dest, MARKER).writeText("ok")
    }

    private companion object {
        const val MARKER = ".installed"
    }
}
