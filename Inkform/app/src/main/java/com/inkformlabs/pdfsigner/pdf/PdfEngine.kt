package com.inkformlabs.pdfsigner.pdf

import android.content.Context
import android.graphics.Bitmap
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDCheckBox
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDChoice
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDField
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDRadioButton
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDSignatureField
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDTerminalField
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDTextField
import com.tom_roush.pdfbox.util.Matrix
import java.io.File
import kotlin.math.max
import kotlin.math.min

/** Everything that reads or writes PDF structure. All functions are blocking: call them off the main thread. */
class PdfEngine(context: Context) {

    @Suppress("unused")
    private val appContext = context.applicationContext

    sealed interface PrepareResult {
        data object Ok : PrepareResult
        data object NeedsPassword : PrepareResult
        data class Failed(val message: String) : PrepareResult
    }

    private fun memory(): MemoryUsageSetting = MemoryUsageSetting.setupMixed(48L * 1024 * 1024)

    /**
     * Validates an imported file and writes an editable, unencrypted copy to [destination].
     * Password-protected files need the password; owner-restricted files are unlocked so they can be filled.
     */
    fun prepareImported(source: File, password: String?, destination: File): PrepareResult {
        return try {
            val document = if (password.isNullOrEmpty()) {
                PDDocument.load(source, memory())
            } else {
                PDDocument.load(source, password, memory())
            }
            document.use { doc ->
                if (doc.numberOfPages == 0) return PrepareResult.Failed("This PDF has no pages.")
                if (doc.isEncrypted) {
                    doc.isAllSecurityToBeRemoved = true
                    doc.save(destination)
                } else {
                    source.copyTo(destination, overwrite = true)
                }
            }
            PrepareResult.Ok
        } catch (e: InvalidPasswordException) {
            PrepareResult.NeedsPassword
        } catch (e: Throwable) {
            PrepareResult.Failed(e.message ?: "This file is not a valid PDF.")
        }
    }

    fun readDocument(file: File): DocumentModel {
        PDDocument.load(file, memory()).use { doc ->
            val pages = doc.pages.map { geometryOf(it) }
            val form = doc.documentCatalog.acroForm
            val fields = if (form == null) emptyList() else readFields(doc, form, pages)
            return DocumentModel(pages, fields)
        }
    }

    private fun geometryOf(page: PDPage): PageGeom {
        val box = page.cropBox
        val rotation = ((page.rotation % 360) + 360) % 360
        return PageGeom(box.lowerLeftX, box.lowerLeftY, box.width, box.height, rotation)
    }

    private fun readFields(doc: PDDocument, form: PDAcroForm, pages: List<PageGeom>): List<FormField> {
        val result = mutableListOf<FormField>()
        for (field in form.fieldTree) {
            if (field !is PDTerminalField || field.isReadOnly) continue
            val type = when (field) {
                is PDTextField -> FieldType.TEXT
                is PDCheckBox -> FieldType.CHECKBOX
                is PDRadioButton -> FieldType.RADIO
                is PDChoice -> FieldType.CHOICE
                is PDSignatureField -> FieldType.SIGNATURE
                else -> null // push buttons
            } ?: continue

            val widgets = field.widgets.mapNotNull { widgetOf(doc, it, pages) }
            if (widgets.isEmpty()) continue

            val name = field.fullyQualifiedName ?: continue
            val label = field.alternateFieldName?.takeIf { it.isNotBlank() }
                ?: prettify(field.partialName ?: name)

            val built = try {
                when (field) {
                    is PDTextField -> FormField(
                        name = name, label = label, type = type, widgets = widgets,
                        multiline = field.isMultiline, maxLength = field.maxLen,
                        required = field.isRequired, value = field.valueAsString.orEmpty(),
                    )

                    is PDCheckBox -> FormField(
                        name = name, label = label, type = type, widgets = widgets,
                        required = field.isRequired,
                        value = if (field.isChecked) field.onValue.orEmpty() else "",
                    )

                    is PDRadioButton -> FormField(
                        name = name, label = label, type = type, widgets = widgets,
                        required = field.isRequired,
                        value = field.value?.takeIf { it != "Off" }.orEmpty(),
                    )

                    is PDChoice -> {
                        val exports = field.optionsExportValues.orEmpty()
                        val displays = field.optionsDisplayValues.orEmpty()
                        val options = exports.mapIndexed { i, export ->
                            ChoiceOption(export, displays.getOrNull(i) ?: export)
                        }
                        FormField(
                            name = name, label = label, type = type, widgets = widgets,
                            options = options, required = field.isRequired,
                            value = field.value?.firstOrNull().orEmpty(),
                        )
                    }

                    else -> FormField(name = name, label = label, type = type, widgets = widgets)
                }
            } catch (e: Exception) {
                FormField(name = name, label = label, type = type, widgets = widgets)
            }
            result += built
        }
        // Natural reading order: page, then top to bottom, then left to right.
        return result.sortedWith(
            compareBy<FormField>({ it.firstWidget.page }, { (it.firstWidget.rect.top * 200).toInt() }, { it.firstWidget.rect.left })
        )
    }

    private fun widgetOf(doc: PDDocument, widget: PDAnnotationWidget, pages: List<PageGeom>): FieldWidget? {
        if (widget.isHidden || widget.isNoView) return null
        val rect = widget.rectangle ?: return null
        var pageIndex = widget.page?.let { doc.pages.indexOf(it) } ?: -1
        if (pageIndex < 0) pageIndex = findPageOf(doc, widget)
        if (pageIndex < 0 || pageIndex >= pages.size) return null
        val geom = pages[pageIndex]
        val (u1, v1) = geom.toNormalized(rect.lowerLeftX, rect.lowerLeftY)
        val (u2, v2) = geom.toNormalized(rect.upperRightX, rect.upperRightY)
        val norm = NormRect(
            left = min(u1, u2).coerceIn(0f, 1f), top = min(v1, v2).coerceIn(0f, 1f),
            right = max(u1, u2).coerceIn(0f, 1f), bottom = max(v1, v2).coerceIn(0f, 1f),
        )
        if (norm.width <= 0f || norm.height <= 0f) return null
        val onValue = widget.appearance?.normalAppearance?.let { entry ->
            if (entry.isSubDictionary) entry.subDictionary.keys.map { it.name }.firstOrNull { it != "Off" } else null
        }
        return FieldWidget(pageIndex, norm, onValue)
    }

    private fun findPageOf(doc: PDDocument, widget: PDAnnotationWidget): Int {
        for ((index, page) in doc.pages.withIndex()) {
            val found = try {
                page.annotations.any { it.cosObject == widget.cosObject }
            } catch (e: Exception) {
                false
            }
            if (found) return index
        }
        return -1
    }

    data class SaveResult(val failedFields: Int)

    /**
     * Writes field [values] (only the changed ones need to be passed) and burns [overlays] into the page content.
     */
    fun applyEdits(
        source: File,
        destination: File,
        pages: List<PageGeom>,
        values: Map<String, String>,
        overlays: List<Overlay>,
        signatureBitmap: (String) -> Bitmap?,
    ): SaveResult {
        var failed = 0
        PDDocument.load(source, memory()).use { doc ->
            val fonts = FontResolver(doc)
            val form = doc.documentCatalog.acroForm
            if (form != null && values.isNotEmpty()) {
                ensureDefaultResources(form)
                for ((name, value) in values) {
                    val field = form.getField(name) ?: continue
                    try {
                        when (field) {
                            is PDTextField -> field.setValue(value)
                            is PDCheckBox -> if (value.isNotEmpty()) field.check() else field.unCheck()
                            is PDRadioButton -> field.setValue(value.ifEmpty { "Off" })
                            is PDChoice -> if (value.isNotEmpty()) field.setValue(value)
                        }
                    } catch (e: Exception) {
                        // Usually a character the form's font cannot encode. Draw the text on the page instead.
                        if (field is PDTextField && value.isNotEmpty()) {
                            field.cosObject.removeItem(COSName.V)
                            drawTextIntoWidgets(doc, field, value, pages, fonts)
                        } else {
                            failed++
                        }
                    }
                }
            }
            overlays.groupBy { it.page }.forEach { (pageIndex, items) ->
                if (pageIndex in pages.indices) {
                    drawOverlays(doc, pageIndex, pages[pageIndex], items, fonts, signatureBitmap)
                }
            }
            doc.save(destination)
        }
        return SaveResult(failed)
    }

    fun export(source: File, destination: File, options: ExportOptions) {
        PDDocument.load(source, memory()).use { doc ->
            if (options.flatten) {
                doc.documentCatalog.acroForm?.let { form ->
                    ensureDefaultResources(form)
                    try {
                        form.flatten(form.fieldTree.toList(), true)
                    } catch (e: Exception) {
                        runCatching { form.flatten() }
                    }
                }
            }
            if (options.removeMetadata) {
                doc.documentInformation = PDDocumentInformation()
                doc.documentCatalog.metadata = null
            }
            if (options.password.isNotEmpty()) {
                val policy = StandardProtectionPolicy(options.password, options.password, AccessPermission())
                policy.encryptionKeyLength = 256
                doc.protect(policy)
            }
            doc.save(destination)
        }
    }

    private fun ensureDefaultResources(form: PDAcroForm) {
        try {
            var resources = form.defaultResources
            if (resources == null) {
                resources = PDResources()
                form.defaultResources = resources
            }
            val helv = COSName.getPDFName("Helv")
            if (resources.getFont(helv) == null) resources.put(helv, PDType1Font.HELVETICA)
            if (form.defaultAppearance.isNullOrEmpty()) form.defaultAppearance = "/Helv 0 Tf 0 g"
        } catch (e: Exception) {
            // Best effort; individual fields may still carry their own fonts.
        }
    }

    private fun drawTextIntoWidgets(doc: PDDocument, field: PDTextField, value: String, pages: List<PageGeom>, fonts: FontResolver) {
        for (widget in field.widgets) {
            val info = widgetOf(doc, widget, pages) ?: continue
            val r = info.rect
            val box = r.height
            val overlay = Overlay(
                id = -1, kind = OverlayKind.TEXT, page = info.page,
                x = r.left + r.width * 0.02f, y = r.top + box * 0.1f, w = r.width, h = box * 0.8f,
                text = value.replace('\n', ' '),
            )
            drawOverlays(doc, info.page, pages[info.page], listOf(overlay), fonts) { null }
        }
    }

    private fun drawOverlays(
        doc: PDDocument,
        pageIndex: Int,
        geom: PageGeom,
        items: List<Overlay>,
        fonts: FontResolver,
        signatureBitmap: (String) -> Bitmap?,
    ) {
        val page = doc.getPage(pageIndex)
        PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
            val m = geom.displayMatrix()
            cs.transform(Matrix(m[0], m[1], m[2], m[3], m[4], m[5]))
            for (o in items) drawOverlay(doc, cs, o, geom, fonts, signatureBitmap)
        }
    }

    private fun drawOverlay(
        doc: PDDocument,
        cs: PDPageContentStream,
        o: Overlay,
        geom: PageGeom,
        fonts: FontResolver,
        signatureBitmap: (String) -> Bitmap?,
    ) {
        val dw = geom.displayW
        val dh = geom.displayH
        val left = o.x * dw
        val bw = o.w * dw
        val bh = o.h * dh
        val bottom = dh - (o.y * dh) - bh
        val r = ((o.color shr 16) and 0xFF) / 255f
        val g = ((o.color shr 8) and 0xFF) / 255f
        val b = (o.color and 0xFF) / 255f

        when (o.kind) {
            OverlayKind.TEXT, OverlayKind.DATE -> {
                val text = o.text.trim()
                if (text.isEmpty()) return
                val (font, safeText) = fonts.resolve(text)
                cs.beginText()
                cs.setFont(font, bh * Overlay.TEXT_SIZE_RATIO)
                cs.setNonStrokingColor(r, g, b)
                cs.newLineAtOffset(left, bottom + bh * Overlay.TEXT_BASELINE_RATIO)
                cs.showText(safeText)
                cs.endText()
            }

            OverlayKind.CHECK -> {
                cs.setStrokingColor(r, g, b)
                cs.setLineWidth(bh * 0.13f)
                cs.setLineCapStyle(1)
                cs.setLineJoinStyle(1)
                cs.moveTo(left + bw * 0.16f, bottom + bh * 0.45f)
                cs.lineTo(left + bw * 0.42f, bottom + bh * 0.20f)
                cs.lineTo(left + bw * 0.86f, bottom + bh * 0.78f)
                cs.stroke()
            }

            OverlayKind.CROSS -> {
                cs.setStrokingColor(r, g, b)
                cs.setLineWidth(bh * 0.13f)
                cs.setLineCapStyle(1)
                cs.moveTo(left + bw * 0.2f, bottom + bh * 0.2f)
                cs.lineTo(left + bw * 0.8f, bottom + bh * 0.8f)
                cs.moveTo(left + bw * 0.8f, bottom + bh * 0.2f)
                cs.lineTo(left + bw * 0.2f, bottom + bh * 0.8f)
                cs.stroke()
            }

            OverlayKind.SIGNATURE -> {
                val bitmap = o.signatureId?.let(signatureBitmap) ?: return
                val image = LosslessFactory.createFromImage(doc, bitmap)
                cs.drawImage(image, left, bottom, bw, bh)
            }
        }
    }

    /** Picks a font able to show the text: built-in Helvetica, else a Unicode system font. */
    private class FontResolver(private val doc: PDDocument) {
        private var unicode: PDFont? = null
        private var unicodeTried = false

        fun resolve(text: String): Pair<PDFont, String> {
            val helvetica: PDFont = PDType1Font.HELVETICA
            if (canEncode(helvetica, text)) return helvetica to text
            val system = unicodeFont()
            if (system != null && canEncode(system, text)) return system to text
            val target = system ?: helvetica
            val cleaned = text.map { c -> if (canEncode(target, c.toString())) c else '?' }.joinToString("")
            return target to cleaned
        }

        private fun canEncode(font: PDFont, text: String): Boolean = try {
            font.encode(text)
            true
        } catch (e: Exception) {
            false
        }

        private fun unicodeFont(): PDFont? {
            if (!unicodeTried) {
                unicodeTried = true
                unicode = SYSTEM_FONTS.asSequence()
                    .map { File(it) }
                    .filter { it.exists() }
                    .mapNotNull { runCatching { PDType0Font.load(doc, it) }.getOrNull() }
                    .firstOrNull()
            }
            return unicode
        }

        companion object {
            private val SYSTEM_FONTS = listOf(
                "/system/fonts/NotoSans-Regular.ttf",
                "/system/fonts/Roboto-Regular.ttf",
                "/system/fonts/DroidSans.ttf",
            )
        }
    }

    companion object {
        /** "applicant_first-name" / "ApplicantFirstName" -> "Applicant first name". */
        fun prettify(raw: String): String {
            val spaced = raw
                .substringAfterLast('.')
                .replace(Regex("[_\\-]+"), " ")
                .replace(Regex("([a-z])([A-Z])"), "$1 $2")
                .replace(Regex("\\[\\d+]"), "")
                .trim()
                .lowercase()
            return spaced.replaceFirstChar { it.uppercase() }.ifBlank { raw }
        }
    }
}
