package com.devatjunaid.isthisai.engine

/** Overall answer shown on the verdict screen. */
enum class Verdict { AI_GENERATED, AI_EDITED, CAMERA, OTHER_LABEL, NO_LABEL }

/** How much weight a label carries, strongest first. */
enum class Strength(val rank: Int) { SIGNED(3), EMBEDDED(2), HINT(1), NONE(0) }

enum class SignalKind { AI_GENERATED, AI_EDITED, CAMERA, DIGITAL, EDIT, INFO }

/** One piece of evidence found in the file. */
data class Evidence(
    val source: String,
    val field: String,
    val value: String,
    val kind: SignalKind,
    val strength: Strength,
)

enum class ScanStep(val title: String, val subtitle: String) {
    STRUCTURE("File structure", "Container · segments"),
    C2PA("Content Credentials", "C2PA manifest · JUMBF"),
    IPTC("IPTC source type", "DigitalSourceType · IIM"),
    XMP("XMP packet", "CreatorTool · history"),
    EXIF("EXIF tags", "Make · Model · Software"),
    GENERATOR("Generator text", "PNG text · prompt / parameters"),
}

enum class StepStatus { FOUND, NONE }

data class StepResult(val step: ScanStep, val status: StepStatus, val detail: String)

enum class TrailKind { AI, EDIT, CAPTURE, SIGNED, WARNING, DEVICE, INFO }

data class TrailStep(val title: String, val detail: String, val tool: String?, val kind: TrailKind)

data class RawSection(val title: String, val entries: List<Pair<String, String>>)

data class ScanReport(
    val fileName: String,
    val mimeType: String?,
    val format: ImageFormat,
    val sizeBytes: Long,
    val width: Int?,
    val height: Int?,
    val verdict: Verdict,
    val strength: Strength,
    val evidence: List<Evidence>,
    val steps: List<StepResult>,
    val trail: List<TrailStep>,
    val raw: List<RawSection>,
    val warnings: List<String>,
    val headerHex: String,
    val aiTool: String?,
    val aiPrompt: String?,
    val scannedAt: Long,
)
