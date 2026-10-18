package com.inkformlabs.pdfsigner.ui.home

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.inkformlabs.pdfsigner.AppContainer
import com.inkformlabs.pdfsigner.data.DocumentRepository.ImportResult
import com.inkformlabs.pdfsigner.data.db.DocumentEntity
import com.inkformlabs.pdfsigner.util.Intents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentsViewModel(private val container: AppContainer) : ViewModel() {

    val query = MutableStateFlow("")

    val documents: StateFlow<List<DocumentEntity>> = container.documents.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val visibleDocuments: StateFlow<List<DocumentEntity>> = combine(documents, query) { docs, q ->
        val needle = q.trim()
        if (needle.isEmpty()) docs else docs.filter { it.name.contains(needle, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Non-null while a long operation runs; shown in a blocking overlay. */
    var busyMessage by mutableStateOf<String?>(null)
        private set

    /** File waiting for its password. */
    var passwordRequestUri by mutableStateOf<Uri?>(null)
        private set
    var passwordWasWrong by mutableStateOf(false)
        private set

    /** One-off message for the snackbar. */
    var message by mutableStateOf<String?>(null)

    /** Pages captured with the camera in the current scan session. */
    val capturedPages = mutableStateListOf<Uri>()
    var pendingCaptureUri: Uri? = null
        private set

    fun importPdf(uri: Uri, password: String? = null, onImported: (String) -> Unit) {
        if (busyMessage != null) return
        busyMessage = "Preparing your PDF…"
        viewModelScope.launch {
            val result = container.documents.importPdf(uri, password)
            busyMessage = null
            when (result) {
                is ImportResult.Success -> {
                    passwordRequestUri = null
                    passwordWasWrong = false
                    onImported(result.id)
                }

                ImportResult.NeedsPassword -> {
                    passwordWasWrong = password != null
                    passwordRequestUri = uri
                }

                is ImportResult.Failed -> {
                    passwordRequestUri = null
                    message = result.message
                }
            }
        }
    }

    fun cancelPassword() {
        passwordRequestUri = null
        passwordWasWrong = false
    }

    fun createFromImages(images: List<Uri>, fromCamera: Boolean, onCreated: (String) -> Unit) {
        if (images.isEmpty() || busyMessage != null) return
        busyMessage = if (images.size == 1) "Creating your PDF…" else "Creating a ${images.size}-page PDF…"
        val stamp = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        val title = if (fromCamera) "Scan $stamp" else "Photos $stamp"
        viewModelScope.launch {
            val result = container.documents.createFromImages(images, title)
            busyMessage = null
            when (result) {
                is ImportResult.Success -> {
                    if (fromCamera) discardCapturedPages()
                    onCreated(result.id)
                }
                is ImportResult.Failed -> message = result.message
                ImportResult.NeedsPassword -> message = "Couldn't create the PDF."
            }
        }
    }

    /** Creates a file in cache/camera for the camera app to write into. */
    fun newCaptureUri(context: Context): Uri {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "page_${System.currentTimeMillis()}.jpg")
        val uri = Intents.fileProviderUri(context, file)
        pendingCaptureUri = uri
        return uri
    }

    fun onCaptureResult(success: Boolean) {
        val uri = pendingCaptureUri ?: return
        pendingCaptureUri = null
        if (success) capturedPages += uri
    }

    fun discardCapturedPages() {
        capturedPages.clear()
    }

    fun rename(id: String, name: String) {
        viewModelScope.launch { container.documents.rename(id, name) }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            container.documents.delete(id)
            message = "Document deleted"
        }
    }

    fun withShareableCopy(id: String, action: (File, String) -> Unit) {
        viewModelScope.launch {
            val doc = container.documents.get(id) ?: return@launch
            val file = container.documents.shareableCopy(id) ?: return@launch
            action(file, doc.name)
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { DocumentsViewModel(container) }
        }
    }
}
