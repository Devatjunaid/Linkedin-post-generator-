package com.inkformlabs.pdfsigner.ui.export

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.inkformlabs.pdfsigner.AppContainer
import com.inkformlabs.pdfsigner.data.DocumentRepository
import com.inkformlabs.pdfsigner.data.db.DocumentEntity
import com.inkformlabs.pdfsigner.pdf.ExportOptions
import com.inkformlabs.pdfsigner.util.Intents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ExportViewModel(private val documentId: String, private val container: AppContainer) : ViewModel() {

    val document: StateFlow<DocumentEntity?> = container.documents.observe(documentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var fileName by mutableStateOf("")
    var flatten by mutableStateOf(true)
    var removeMetadata by mutableStateOf(true)
    var protect by mutableStateOf(false)
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")

    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
    var savedCount by mutableStateOf(0)
        private set

    init {
        viewModelScope.launch {
            val doc = container.documents.get(documentId)
            if (doc != null && fileName.isEmpty()) {
                fileName = if (doc.signed && !doc.name.endsWith("signed", ignoreCase = true)) "${doc.name} (signed)" else doc.name
            }
        }
    }

    val passwordProblem: String?
        get() = when {
            !protect -> null
            password.length < 4 -> "Use at least 4 characters"
            password != confirmPassword -> "Passwords don't match"
            else -> null
        }

    val canExport: Boolean get() = !busy && passwordProblem == null && fileName.isNotBlank()

    val outputName: String get() = DocumentRepository.safeFileName(fileName.ifBlank { "Document" }) + ".pdf"

    private suspend fun build(withPassword: Boolean): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(container.app.cacheDir, "export").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val out = File(dir, outputName)
            container.pdfEngine.export(
                container.documents.workingFile(documentId),
                out,
                ExportOptions(
                    flatten = flatten,
                    removeMetadata = removeMetadata,
                    password = if (withPassword && protect) password else "",
                ),
            )
            out
        }.getOrNull()
    }

    fun saveTo(context: Context, uri: Uri, onSaved: () -> Unit) {
        if (!canExport) return
        busy = true
        viewModelScope.launch {
            val file = build(withPassword = true)
            val ok = file != null && withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { out -> file.inputStream().use { it.copyTo(out) } } != null
                }.getOrDefault(false)
            }
            busy = false
            if (ok) {
                container.documents.markExported(documentId)
                savedCount++
                message = "Saved “$outputName”"
                onSaved()
            } else {
                message = "Couldn't save the file. Please try another location."
            }
        }
    }

    fun share(context: Context) {
        if (!canExport) return
        busy = true
        viewModelScope.launch {
            val file = build(withPassword = true)
            busy = false
            if (file == null) {
                message = "Couldn't prepare the PDF."
            } else {
                container.documents.markExported(documentId)
                Intents.sharePdf(context, file, fileName)
            }
        }
    }

    /** Printing uses an unprotected copy: the print system cannot open password-protected PDFs. */
    fun print(context: Context) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            val file = build(withPassword = false)
            busy = false
            if (file == null) message = "Couldn't prepare the PDF." else Intents.printPdf(context, file, fileName)
        }
    }

    companion object {
        fun factory(documentId: String, container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { ExportViewModel(documentId, container) }
        }
    }
}
