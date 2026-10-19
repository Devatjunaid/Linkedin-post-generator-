package com.devatjunaid.isthisai.ui

import com.devatjunaid.isthisai.engine.Evidence
import com.devatjunaid.isthisai.engine.ScanReport
import com.devatjunaid.isthisai.engine.SignalKind
import com.devatjunaid.isthisai.engine.Strength
import com.devatjunaid.isthisai.engine.Verdict
import java.text.DateFormat
import java.util.Date

/** Wording for each verdict. It always says what the file *claims*, never more. */
data class VerdictCopy(
    val word: String,
    val sub: String,
    val ring: String,
    val headline: String,
    val detail: String,
    val chip: String,
    val tag: String,
)

fun strengthLabel(s: Strength) = when (s) {
    Strength.SIGNED -> "Signed label"
    Strength.EMBEDDED -> "Label in file"
    Strength.HINT -> "Clue only"
    Strength.NONE -> "Nothing declared"
}

fun verdictTag(v: Verdict) = when (v) {
    Verdict.AI_GENERATED -> "AI label"
    Verdict.AI_EDITED -> "AI edit"
    Verdict.CAMERA -> "Camera"
    Verdict.OTHER_LABEL -> "Labeled"
    Verdict.NO_LABEL -> "No label"
}

fun verdictCopy(r: ScanReport): VerdictCopy {
    val tool = r.aiTool?.let { " ($it)" } ?: ""
    return when (r.verdict) {
        Verdict.AI_GENERATED -> VerdictCopy(
            word = "AI-MADE",
            sub = if (r.strength == Strength.HINT) "TOOL NAMED" else "LABEL FOUND",
            ring = "GENERATIVE AI · CONTENT CREDENTIALS · GENERATIVE AI · HIDDEN LABEL · ",
            headline = if (r.strength == Strength.HINT) "This image names an AI image tool" else "This image says it was made with AI",
            detail = when (r.strength) {
                Strength.SIGNED -> "A signed Content Credentials label says this image was created by generative AI$tool. Its signature checks out."
                Strength.EMBEDDED -> "A label stored inside the file says this image was created by generative AI$tool."
                else -> "The file's software fields mention an AI image tool$tool, but there is no formal AI label."
            },
            chip = strengthLabel(r.strength),
            tag = verdictTag(r.verdict),
        )
        Verdict.AI_EDITED -> VerdictCopy(
            word = "AI-EDITED",
            sub = "LABEL FOUND",
            ring = "EDITED WITH AI · CONTENT CREDENTIALS · EDITED WITH AI · ",
            headline = "This image says it was edited with AI",
            detail = when (r.strength) {
                Strength.SIGNED -> "Signed Content Credentials say generative AI was used to change part of this image$tool."
                Strength.EMBEDDED -> "A label inside the file says AI was used to edit or add to this image$tool."
                else -> "The file mentions an AI editing feature$tool, but carries no formal label."
            },
            chip = strengthLabel(r.strength),
            tag = verdictTag(r.verdict),
        )
        Verdict.CAMERA -> if (r.strength == Strength.HINT) VerdictCopy(
            word = "CAMERA",
            sub = "DETAILS ONLY",
            ring = "CAMERA DETAILS · NOT SIGNED · CAMERA DETAILS · NOT SIGNED · ",
            headline = "Camera details found, no AI label",
            detail = "The file lists a camera, lens and exposure settings. These are easy to copy or fake, so they are a clue, not proof.",
            chip = strengthLabel(r.strength),
            tag = verdictTag(r.verdict),
        ) else VerdictCopy(
            word = "CAMERA",
            sub = if (r.strength == Strength.SIGNED) "SIGNED CAPTURE" else "CAPTURE LABEL",
            ring = "CAPTURED ON DEVICE · CONTENT CREDENTIALS · CAPTURED ON DEVICE · ",
            headline = "This image says it came from a camera",
            detail = if (r.strength == Strength.SIGNED) "The capturing device signed this image. No AI steps are recorded."
            else "A label inside the file says it is a camera capture. No AI steps are recorded.",
            chip = strengthLabel(r.strength),
            tag = verdictTag(r.verdict),
        )
        Verdict.OTHER_LABEL -> VerdictCopy(
            word = "LABELED",
            sub = "NO AI DECLARED",
            ring = "PROVENANCE FOUND · NO AI DECLARED · PROVENANCE FOUND · ",
            headline = "This image has a history label, but no AI is declared",
            detail = "The file describes where it came from or how it was made, without saying AI was involved. See the full trail for details.",
            chip = strengthLabel(r.strength),
            tag = verdictTag(r.verdict),
        )
        Verdict.NO_LABEL -> VerdictCopy(
            word = "NO LABEL",
            sub = "NOTHING DECLARED",
            ring = "NO PROVENANCE DATA · UNKNOWN ORIGIN · NO PROVENANCE DATA · ",
            headline = "This image doesn't say where it came from",
            detail = "No AI or camera labels were found. They may have been removed by a screenshot, a messaging app or re-saving, or never added.",
            chip = strengthLabel(r.strength),
            tag = verdictTag(r.verdict),
        )
    }
}

fun evidenceTitle(e: Evidence) = when (e.field) {
    "digitalSourceType" -> "Content Credentials"
    "signature" -> "Signature"
    "software" -> "Software named"
    "DigitalSourceType" -> "IPTC source type"
    "AISystemUsed" -> "AI system used"
    "Camera" -> "Camera details"
    else -> "${e.source} · ${e.field}"
}

fun isAiKind(k: SignalKind) = k == SignalKind.AI_GENERATED || k == SignalKind.AI_EDITED

/** Plain-text summary for the share sheet. */
fun shareText(r: ScanReport): String {
    val copy = verdictCopy(r)
    val sb = StringBuilder()
    sb.append("Is This AI? report\n")
    sb.append("File: ").append(r.fileName).append('\n')
    sb.append("Result: ").append(copy.headline).append(" (").append(copy.chip).append(")\n")
    r.aiTool?.let { sb.append("Tool: ").append(it).append('\n') }
    if (r.evidence.isNotEmpty()) {
        sb.append("\nEvidence:\n")
        r.evidence.take(8).forEach { sb.append("• ").append(evidenceTitle(it)).append(": ").append(it.value).append('\n') }
    }
    r.warnings.forEach { sb.append("⚠ ").append(it).append('\n') }
    sb.append("\nChecked on ").append(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(r.scannedAt)))
    sb.append(" by reading the labels inside the file, offline. A missing label does not prove an image is real.")
    return sb.toString()
}
