package com.inkformlabs.pdfsigner.pdf

/**
 * Geometry of one page. PDF user space has its origin at the bottom-left and may be rotated by
 * the page's /Rotate entry; the UI works in "display space": the page as the user sees it,
 * normalised to 0..1 with the origin at the top-left.
 */
data class PageGeom(
    val cropX: Float,
    val cropY: Float,
    val cropW: Float,
    val cropH: Float,
    val rotation: Int,
) {
    private val swapped: Boolean get() = rotation == 90 || rotation == 270

    /** Width/height of the page as displayed, in PDF points. */
    val displayW: Float get() = if (swapped) cropH else cropW
    val displayH: Float get() = if (swapped) cropW else cropH

    /** PDF user-space point -> normalised display coordinates (u right, v down). */
    fun toNormalized(x: Float, y: Float): Pair<Float, Float> {
        val nx = (x - cropX) / cropW
        val ny = (y - cropY) / cropH
        return when (rotation) {
            90 -> ny to nx
            180 -> (1f - nx) to ny
            270 -> (1f - ny) to (1f - nx)
            else -> nx to (1f - ny)
        }
    }

    /**
     * Affine matrix [a, b, c, d, e, f] that maps display points (origin bottom-left, y up,
     * size [displayW] x [displayH]) into PDF user space, honouring the page rotation.
     */
    fun displayMatrix(): FloatArray = when (rotation) {
        90 -> floatArrayOf(0f, 1f, -1f, 0f, cropX + cropW, cropY)
        180 -> floatArrayOf(-1f, 0f, 0f, -1f, cropX + cropW, cropY + cropH)
        270 -> floatArrayOf(0f, -1f, 1f, 0f, cropX, cropY + cropH)
        else -> floatArrayOf(1f, 0f, 0f, 1f, cropX, cropY)
    }
}

/** Rectangle in normalised display space (0..1, origin top-left). */
data class NormRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

enum class FieldType { TEXT, CHECKBOX, RADIO, CHOICE, SIGNATURE }

data class FieldWidget(
    val page: Int,
    val rect: NormRect,
    /** For check boxes / radio buttons: the export value that means "on" for this widget. */
    val onValue: String?,
)

data class ChoiceOption(val export: String, val display: String)

data class FormField(
    /** Fully qualified field name, unique within the document. */
    val name: String,
    val label: String,
    val type: FieldType,
    val widgets: List<FieldWidget>,
    val multiline: Boolean = false,
    val maxLength: Int = -1,
    val options: List<ChoiceOption> = emptyList(),
    val required: Boolean = false,
    /** Current value. TEXT: text, CHECKBOX: on-value or "", RADIO: selected export value or "", CHOICE: export value. */
    val value: String = "",
) {
    val firstWidget: FieldWidget get() = widgets.first()
}

data class DocumentModel(val pages: List<PageGeom>, val fields: List<FormField>)

enum class OverlayKind { TEXT, DATE, CHECK, CROSS, SIGNATURE }

/** An element the user placed on a page. Coordinates are normalised display space. */
data class Overlay(
    val id: Long,
    val kind: OverlayKind,
    val page: Int,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val text: String = "",
    val signatureId: String? = null,
    val color: Int = INK_BLACK,
) {
    companion object {
        const val INK_BLACK: Int = 0xFF17181C.toInt()
        const val INK_BLUE: Int = 0xFF1F3FBF.toInt()

        /** Font size = box height * this ratio (shared by the editor preview and the PDF writer). */
        const val TEXT_SIZE_RATIO = 0.72f

        /** Baseline offset from the bottom of the box, as a fraction of the box height. */
        const val TEXT_BASELINE_RATIO = 0.24f
    }
}

data class ExportOptions(
    val flatten: Boolean,
    val removeMetadata: Boolean,
    val password: String,
)
