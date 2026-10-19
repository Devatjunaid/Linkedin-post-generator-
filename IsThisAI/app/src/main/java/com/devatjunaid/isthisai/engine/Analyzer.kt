package com.devatjunaid.isthisai.engine

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Turns raw file bytes into a [ScanReport]. Everything runs on the device; nothing is uploaded.
 * The analyzer only reports what the file itself declares: a missing label never proves an
 * image is real, and a label could in theory be forged unless its signature checks out.
 */
object Analyzer {

    fun analyze(bytes: ByteArray, fileName: String, mimeType: String?, now: Long = System.currentTimeMillis()): ScanReport {
        val raw = Containers.read(bytes)
        val evidence = ArrayList<Evidence>()
        val warnings = ArrayList<String>()
        val trail = ArrayList<TrailStep>()
        val sections = ArrayList<RawSection>()
        var aiTool: String? = null
        var aiPrompt: String? = null

        // ---------- Content Credentials ----------
        val stores = raw.jumbf.mapNotNull { (_, data) -> runCatching { C2pa.parse(data, bytes) }.getOrNull() }
        val manifests = stores.flatMap { it.manifests }
        for (m in manifests) {
            val sig = m.signature
            val strength = if (sig?.status == CheckStatus.PASSED && m.hashStatus != CheckStatus.FAILED) Strength.SIGNED else Strength.EMBEDDED
            for (a in m.actions) {
                val ds = a.digitalSourceType?.let(::shortCode) ?: continue
                val kind = classifySource(ds, a.action)
                evidence += Evidence("Content Credentials", "digitalSourceType", "${a.action} · $ds", kind, strength)
                if (kind == SignalKind.AI_GENERATED || kind == SignalKind.AI_EDITED) {
                    aiTool = aiTool ?: a.softwareAgent ?: m.claimGenerator
                }
            }
            for (tool in listOfNotNull(m.claimGenerator) + m.actions.mapNotNull { it.softwareAgent }) {
                KnownTools.match(tool)?.let { name ->
                    evidence += Evidence("Content Credentials", "software", tool, KnownTools.kindOf(tool), Strength.HINT)
                    aiTool = aiTool ?: name
                }
            }
            if (m.isActive && sig != null) {
                evidence += Evidence(
                    "Content Credentials", "signature",
                    listOfNotNull(sig.signer?.let { "Signed by $it" }, sig.note).joinToString(" · "),
                    SignalKind.INFO, if (sig.status == CheckStatus.PASSED) Strength.SIGNED else Strength.EMBEDDED,
                )
            }
            if (sig?.status == CheckStatus.FAILED) warnings += "The Content Credentials signature does not match. The label may have been tampered with."
            if (m.isActive && m.hashStatus == CheckStatus.FAILED) warnings += "The image was changed after its Content Credentials were signed."
        }

        // ---------- XMP ----------
        val xmp = Xmp.parse(raw.xmpPackets)
        if (xmp != null) {
            xmp.fields["DigitalSourceType"]?.forEach { v ->
                val ds = shortCode(v)
                evidence += Evidence("XMP / IPTC", "DigitalSourceType", ds, classifySource(ds, null), Strength.EMBEDDED)
            }
            xmp.first("AISystemUsed")?.let { v ->
                evidence += Evidence("XMP / IPTC", "AISystemUsed", v, SignalKind.AI_GENERATED, Strength.EMBEDDED)
                aiTool = aiTool ?: listOfNotNull(v, xmp.first("AISystemVersionUsed")).joinToString(" ")
            }
            xmp.first("AIPromptInformation")?.let { aiPrompt = aiPrompt ?: it }
            xmp.first("AIGC")?.let { v -> evidence += aigcEvidence("XMP", v) }
            for (field in listOf("CreatorTool", "Software", "Credit", "Description", "Title")) {
                xmp.fields[field]?.forEach { v -> phraseOrToolEvidence("XMP", field, v)?.let { e ->
                    evidence += e
                    if (e.kind != SignalKind.INFO) aiTool = aiTool ?: KnownTools.match(v)
                } }
            }
            xmp.history.forEach { h -> KnownTools.match(h)?.let {
                evidence += Evidence("XMP", "History", h, KnownTools.kindOf(h), Strength.HINT)
                aiTool = aiTool ?: it
            } }
        }

        // ---------- IPTC IIM ----------
        for ((field, v) in raw.iptc) {
            phraseOrToolEvidence("IPTC", field, v)?.let { evidence += it }
        }

        // ---------- EXIF ----------
        val exif = raw.exifTiff?.let { runCatching { Exif.parse(it) }.getOrNull() }
        if (exif != null) {
            exif.software?.let { s -> phraseOrToolEvidence("EXIF", "Software", s)?.let { e ->
                evidence += e; if (e.kind != SignalKind.INFO) aiTool = aiTool ?: KnownTools.match(s)
            } }
            for (field in listOf("ImageDescription", "UserComment", "XPComment", "Artist")) {
                val v = exif.tags[field] ?: continue
                if (GeneratorText.looksLikeSdParameters(v)) {
                    evidence += Evidence("EXIF", field, "Stable Diffusion settings (Steps, Sampler, Seed…)", SignalKind.AI_GENERATED, Strength.EMBEDDED)
                    aiTool = aiTool ?: GeneratorText.sdToolName(v)
                    aiPrompt = aiPrompt ?: GeneratorText.sdPrompt(v)
                } else {
                    phraseOrToolEvidence("EXIF", field, v)?.let { evidence += it }
                }
            }
            if (exif.make != null && exif.model != null && exif.hasExposure) {
                evidence += Evidence("EXIF", "Camera", "${exif.make} ${exif.model}".trim(), SignalKind.CAMERA, Strength.HINT)
            }
        }

        // ---------- PNG text / comments ----------
        var generatorFound: String? = null
        for ((key, value) in raw.textChunks) {
            val g = GeneratorText.detect(key, value, raw.textChunks)
            if (g != null) {
                if (generatorFound == null) {
                    evidence += Evidence("PNG text", key, "${g.tool} settings found", SignalKind.AI_GENERATED, Strength.EMBEDDED)
                }
                generatorFound = generatorFound ?: g.tool
                aiTool = aiTool ?: g.tool
                aiPrompt = aiPrompt ?: g.prompt
            } else if (key.equals("AIGC", true)) {
                evidence += aigcEvidence("PNG text", value)
            } else {
                phraseOrToolEvidence("PNG text", key, value)?.let { evidence += it }
            }
        }
        for (c in raw.comments) {
            if (GeneratorText.looksLikeSdParameters(c)) {
                evidence += Evidence("Comment", "Comment", "Stable Diffusion settings found", SignalKind.AI_GENERATED, Strength.EMBEDDED)
                generatorFound = generatorFound ?: GeneratorText.sdToolName(c)
                aiPrompt = aiPrompt ?: GeneratorText.sdPrompt(c)
            } else {
                phraseOrToolEvidence("Comment", "Comment", c)?.let { evidence += it }
            }
        }

        val dedup = evidence.distinctBy { listOf(it.source, it.field, it.value, it.kind) }
        val (verdict, strength) = decide(dedup, manifests)

        // ---------- Steps ----------
        val steps = listOf(
            StepResult(ScanStep.STRUCTURE, StepStatus.FOUND, "${raw.format.label} · ${raw.segments.size} parts"),
            if (manifests.isNotEmpty()) {
                StepResult(ScanStep.C2PA, StepStatus.FOUND, "${manifests.size} manifest${if (manifests.size > 1) "s" else ""} at ${raw.jumbf.first().first.hexOffset()}")
            } else StepResult(ScanStep.C2PA, StepStatus.NONE, "No manifest"),
            run {
                val ds = xmp?.first("DigitalSourceType")
                when {
                    ds != null -> StepResult(ScanStep.IPTC, StepStatus.FOUND, shortCode(ds))
                    raw.iptc.isNotEmpty() -> StepResult(ScanStep.IPTC, StepStatus.FOUND, "${raw.iptc.size} IIM fields")
                    else -> StepResult(ScanStep.IPTC, StepStatus.NONE, "No source type")
                }
            },
            if (xmp != null) StepResult(ScanStep.XMP, StepStatus.FOUND, "${xmp.fields.size} fields" + if (raw.xmpOffset >= 0) " at ${raw.xmpOffset.hexOffset()}" else "")
            else StepResult(ScanStep.XMP, StepStatus.NONE, "No XMP"),
            if (exif != null) StepResult(ScanStep.EXIF, StepStatus.FOUND, "${exif.tags.size} tags" + (exif.make?.let { " · $it" } ?: ""))
            else StepResult(ScanStep.EXIF, StepStatus.NONE, "No EXIF"),
            if (generatorFound != null) StepResult(ScanStep.GENERATOR, StepStatus.FOUND, generatorFound!!)
            else StepResult(ScanStep.GENERATOR, StepStatus.NONE, if (raw.textChunks.isEmpty()) "None found" else "${raw.textChunks.size} text chunks, no generator"),
        )

        // ---------- Trail ----------
        if (manifests.isNotEmpty()) {
            for (m in manifests) {
                if (m.actions.isEmpty()) {
                    trail += TrailStep("Content Credentials added", m.title ?: m.label, m.claimGenerator, TrailKind.INFO)
                }
                for (a in m.actions) {
                    val ds = a.digitalSourceType?.let(::shortCode)
                    val kind = ds?.let { classifySource(it, a.action) }
                    val trailKind = when (kind) {
                        SignalKind.AI_GENERATED, SignalKind.AI_EDITED -> TrailKind.AI
                        SignalKind.CAMERA -> TrailKind.CAPTURE
                        else -> if (a.action == "c2pa.created") TrailKind.CAPTURE else TrailKind.EDIT
                    }
                    trail += TrailStep(
                        actionTitle(a.action, kind),
                        listOfNotNull(a.action, ds, a.whenTime?.let(::shortTime)).joinToString(" · "),
                        a.softwareAgent ?: m.claimGenerator,
                        trailKind,
                    )
                }
                m.signature?.let { s ->
                    trail += TrailStep(
                        if (s.status == CheckStatus.FAILED) "Signature mismatch" else "Signed",
                        listOfNotNull(s.algorithm, s.note).joinToString(" · "),
                        listOfNotNull(s.signer?.let { "Signer: $it" }, s.certificateIssuer?.let { "Issued by $it" }).joinToString(" · ").ifEmpty { null },
                        if (s.status == CheckStatus.FAILED) TrailKind.WARNING else TrailKind.SIGNED,
                    )
                }
                if (m.isActive && m.hashStatus == CheckStatus.FAILED) {
                    trail += TrailStep("Changed after signing", m.hashNote, null, TrailKind.WARNING)
                }
            }
        } else {
            if (exif?.make != null || exif?.model != null) {
                trail += TrailStep(
                    "Captured",
                    listOfNotNull(listOfNotNull(exif.make, exif.model).joinToString(" "), exif.tags["DateTimeOriginal"]).joinToString(" · "),
                    exif.tags["LensModel"], TrailKind.CAPTURE,
                )
            }
            if (generatorFound != null) {
                trail += TrailStep("Generated with AI", "Generator settings stored in the file", generatorFound, TrailKind.AI)
            }
            xmp?.fields?.get("DigitalSourceType")?.firstOrNull()?.let { v ->
                val ds = shortCode(v)
                val k = classifySource(ds, null)
                trail += TrailStep(sourceTitle(ds), "DigitalSourceType · $ds", xmp.first("CreatorTool"),
                    if (k == SignalKind.AI_GENERATED || k == SignalKind.AI_EDITED) TrailKind.AI else TrailKind.INFO)
            }
            xmp?.history?.forEach { h ->
                trail += TrailStep("Edited", h, null, if (KnownTools.match(h) != null) TrailKind.AI else TrailKind.EDIT)
            }
            (exif?.software ?: xmp?.first("CreatorTool"))?.let { sw ->
                trail += TrailStep("Processed by software", sw, null, if (KnownTools.match(sw) != null) TrailKind.AI else TrailKind.EDIT)
            }
            if (trail.isEmpty()) {
                trail += TrailStep("No history recorded", "This file does not describe where it came from", null, TrailKind.INFO)
            }
        }
        trail += TrailStep("Checked on your phone", "Read offline · nothing uploaded", null, TrailKind.DEVICE)

        // ---------- Raw sections ----------
        sections += RawSection("File", listOfNotNull(
            "Name" to fileName,
            "Format" to raw.format.label,
            mimeType?.let { "MIME type" to it },
            "Size" to formatSize(bytes.size.toLong()),
            if (raw.width != null && raw.height != null) "Dimensions" to "${raw.width} × ${raw.height}" else null,
        ))
        manifests.forEachIndexed { i, m ->
            val e = ArrayList<Pair<String, String>>()
            e += "Manifest" to m.label
            e += "Active" to if (m.isActive) "yes (describes this file)" else "no (an ingredient)"
            m.claimGenerator?.let { e += "Claim generator" to it }
            m.title?.let { e += "Title" to it }
            m.format?.let { e += "Format" to it }
            m.signature?.let { s ->
                s.signer?.let { e += "Signed by" to it }
                s.certificateIssuer?.let { e += "Certificate issuer" to it }
                s.algorithm?.let { e += "Algorithm" to it }
                s.validUntil?.let { e += "Certificate valid until" to SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it)) }
                e += "Signature" to s.note
            }
            if (m.isActive) e += "Image hash" to m.hashNote
            m.aiTraining?.let { e += "Use for AI training" to it }
            if (m.authors.isNotEmpty()) e += "Author" to m.authors.joinToString(", ")
            m.actions.forEachIndexed { k, a ->
                e += "Action ${k + 1}" to listOfNotNull(a.action, a.digitalSourceType?.let(::shortCode), a.softwareAgent, a.whenTime).joinToString(" · ")
            }
            m.ingredients.forEach { ing ->
                e += "Ingredient" to listOfNotNull(ing.title, ing.format, ing.relationship).joinToString(" · ")
            }
            e += "Assertions" to m.assertionLabels.joinToString(", ")
            sections += RawSection(if (manifests.size > 1) "Content Credentials ${i + 1}" else "Content Credentials", e)
        }
        if (xmp != null) {
            sections += RawSection("XMP", xmp.fields.map { (k, v) -> k to v.joinToString(" | ") } +
                xmp.history.mapIndexed { i, h -> "History ${i + 1}" to h })
        }
        if (raw.iptc.isNotEmpty()) sections += RawSection("IPTC (IIM)", raw.iptc.map { it.key to it.value })
        if (exif != null) {
            sections += RawSection("EXIF", exif.tags.map { it.key to it.value } +
                if (exif.hasGps) listOf("GPS location" to "Present (not shown)") else emptyList())
        }
        if (raw.textChunks.isNotEmpty()) sections += RawSection("PNG text", raw.textChunks.map { it.first to it.second.clean(500) })
        if (raw.comments.isNotEmpty()) sections += RawSection("Comments", raw.comments.map { "Comment" to it })
        sections += RawSection("File structure", raw.segments.take(40).map { it.name to "${it.offset.hexOffset()} · ${formatSize(it.length.toLong())}" })

        val labelOffset = raw.jumbf.firstOrNull()?.first ?: raw.xmpOffset.takeIf { it >= 0 } ?: raw.exifOffset.takeIf { it >= 0 }
        val headerHex = bytes.slice(0, 32).toHex() + (labelOffset?.let { "  " + bytes.slice(it, 32).toHex() } ?: "")

        return ScanReport(
            fileName = fileName,
            mimeType = mimeType,
            format = raw.format,
            sizeBytes = bytes.size.toLong(),
            width = raw.width,
            height = raw.height,
            verdict = verdict,
            strength = strength,
            evidence = dedup.sortedWith(compareBy<Evidence> { kindOrder(it.kind, verdict) }.thenByDescending { it.strength.rank }),
            steps = steps,
            trail = trail,
            raw = sections,
            warnings = warnings.distinct(),
            headerHex = headerHex,
            aiTool = aiTool?.clean(80),
            aiPrompt = aiPrompt?.clean(600),
            scannedAt = now,
        )
    }

    private fun decide(e: List<Evidence>, manifests: List<C2paManifest>): Pair<Verdict, Strength> {
        fun best(kind: SignalKind) = e.filter { it.kind == kind }.maxOfOrNull { it.strength.rank } ?: 0
        fun strengthOf(rank: Int) = Strength.entries.first { it.rank == rank }
        val gen = best(SignalKind.AI_GENERATED)
        val edit = best(SignalKind.AI_EDITED)
        val cam = best(SignalKind.CAMERA)
        return when {
            gen > 0 && gen >= edit -> Verdict.AI_GENERATED to strengthOf(gen)
            edit > 0 -> Verdict.AI_EDITED to strengthOf(edit)
            cam > 0 -> Verdict.CAMERA to strengthOf(cam)
            manifests.isNotEmpty() -> Verdict.OTHER_LABEL to
                if (manifests.last().signature?.status == CheckStatus.PASSED) Strength.SIGNED else Strength.EMBEDDED
            e.any { it.kind == SignalKind.DIGITAL || it.kind == SignalKind.EDIT } ->
                Verdict.OTHER_LABEL to strengthOf(maxOf(best(SignalKind.DIGITAL), best(SignalKind.EDIT)))
            else -> Verdict.NO_LABEL to Strength.NONE
        }
    }

    private fun kindOrder(k: SignalKind, v: Verdict): Int {
        val primary = when (v) {
            Verdict.AI_GENERATED -> SignalKind.AI_GENERATED
            Verdict.AI_EDITED -> SignalKind.AI_EDITED
            Verdict.CAMERA -> SignalKind.CAMERA
            else -> SignalKind.DIGITAL
        }
        return when (k) {
            primary -> 0
            SignalKind.AI_GENERATED, SignalKind.AI_EDITED -> 1
            SignalKind.CAMERA, SignalKind.DIGITAL -> 2
            SignalKind.EDIT -> 3
            SignalKind.INFO -> 4
        }
    }

    /** IPTC digital source type codes, from the vocabulary at cv.iptc.org/newscodes/digitalsourcetype. */
    fun shortCode(v: String): String = v.trim().trimEnd('/').substringAfterLast('/').substringAfterLast('#')

    internal fun classifySource(ds: String, action: String?): SignalKind = when (ds) {
        "trainedAlgorithmicMedia" -> if (action == null || action == "c2pa.created") SignalKind.AI_GENERATED else SignalKind.AI_EDITED
        "compositeWithTrainedAlgorithmicMedia", "algorithmicallyEnhanced" -> SignalKind.AI_EDITED
        "digitalCapture", "negativeFilm", "positiveFilm", "print", "compositeCapture" -> SignalKind.CAMERA
        "algorithmicMedia", "compositeSynthetic", "digitalArt", "digitalCreation", "virtualRecording",
        "dataDrivenMedia", "screenCapture", "softwareImage" -> SignalKind.DIGITAL
        "humanEdits", "minorHumanEdits", "composite" -> SignalKind.EDIT
        else -> SignalKind.INFO
    }

    private fun sourceTitle(ds: String) = when (classifySource(ds, null)) {
        SignalKind.AI_GENERATED -> "Labeled as made by generative AI"
        SignalKind.AI_EDITED -> "Labeled as edited with AI"
        SignalKind.CAMERA -> "Labeled as a camera capture"
        SignalKind.DIGITAL -> "Labeled as made on a computer"
        else -> "Source type label"
    }

    private fun actionTitle(action: String, kind: SignalKind?): String {
        val base = when (action) {
            "c2pa.created" -> return when (kind) {
                SignalKind.AI_GENERATED -> "Created by generative AI"
                SignalKind.CAMERA -> "Captured by a camera"
                SignalKind.DIGITAL -> "Created digitally"
                else -> "Created"
            }
            "c2pa.opened" -> "Opened"
            "c2pa.edited", "c2pa.edited.metadata" -> "Edited"
            "c2pa.cropped" -> "Cropped"
            "c2pa.color_adjustments" -> "Color adjusted"
            "c2pa.resized" -> "Resized"
            "c2pa.placed" -> "Element placed"
            "c2pa.removed" -> "Element removed"
            "c2pa.converted" -> "Converted"
            "c2pa.transcoded" -> "Re-encoded"
            "c2pa.published" -> "Published"
            "c2pa.filtered" -> "Filter applied"
            "c2pa.drawing" -> "Drawn on"
            "c2pa.orientation" -> "Rotated"
            "c2pa.redacted" -> "Redacted"
            "c2pa.repackaged" -> "Repackaged"
            "c2pa.watermarked" -> "Watermarked"
            else -> action.substringAfterLast('.').replace('_', ' ').replaceFirstChar { it.uppercase() }
        }
        return if (kind == SignalKind.AI_EDITED || kind == SignalKind.AI_GENERATED) "$base with generative AI" else base
    }

    private val AI_PHRASES = listOf(
        "made with ai", "made with google ai", "generated by ai", "ai-generated", "ai generated", "created with ai",
        "generated with ai", "created by ai", "imagined with ai", "made with meta ai", "imagined with meta",
    )
    private val AI_EDIT_PHRASES = listOf("edited with ai", "edited with google ai", "ai-edited", "ai edited", "modified with ai")

    /** Software-type fields where a tool name is meaningful; free-text captions only match phrases. */
    private val TOOL_FIELDS = setOf("CreatorTool", "Software", "Credit", "Source", "Artist")

    private fun phraseOrToolEvidence(source: String, field: String, value: String): Evidence? {
        val lower = value.lowercase()
        if (AI_EDIT_PHRASES.any { lower.contains(it) }) return Evidence(source, field, value.clean(160), SignalKind.AI_EDITED, Strength.EMBEDDED)
        if (AI_PHRASES.any { lower.contains(it) }) return Evidence(source, field, value.clean(160), SignalKind.AI_GENERATED, Strength.EMBEDDED)
        if (field in TOOL_FIELDS && KnownTools.match(value) != null) return Evidence(source, field, value.clean(160), KnownTools.kindOf(value), Strength.HINT)
        return null
    }

    /** China's AIGC labelling standard stores a small JSON object with a "Label" code. */
    private fun aigcEvidence(source: String, value: String): Evidence {
        val label = Regex("\"Label\"\\s*:\\s*\"?(\\d)").find(value)?.groupValues?.get(1)
        val strength = if (label == "1") Strength.EMBEDDED else Strength.HINT
        return Evidence(source, "AIGC", "AIGC label ${label ?: "present"}", SignalKind.AI_GENERATED, strength)
    }

    private fun shortTime(t: String) = t.take(16).replace('T', ' ')

    fun formatSize(n: Long): String = when {
        n >= 1_048_576 -> String.format(Locale.US, "%.1f MB", n / 1_048_576.0)
        n >= 1024 -> String.format(Locale.US, "%.1f KB", n / 1024.0)
        else -> "$n B"
    }
}

/** AI image tools whose names show up in Software / CreatorTool / claim generator fields. */
internal object KnownTools {
    private val GENERATORS = listOf(
        "adobe firefly" to "Adobe Firefly", "firefly" to "Adobe Firefly", "dall-e" to "DALL·E", "dall·e" to "DALL·E",
        "dalle" to "DALL·E", "openai" to "OpenAI", "chatgpt" to "ChatGPT", "gpt-4o" to "ChatGPT", "sora" to "Sora",
        "midjourney" to "Midjourney", "stable diffusion" to "Stable Diffusion", "stability ai" to "Stability AI",
        "dreamstudio" to "DreamStudio", "novelai" to "NovelAI", "comfyui" to "ComfyUI", "invokeai" to "InvokeAI",
        "automatic1111" to "Stable Diffusion WebUI", "fooocus" to "Fooocus", "leonardo.ai" to "Leonardo.Ai",
        "ideogram" to "Ideogram", "imagen" to "Google Imagen", "gemini" to "Google Gemini", "google ai" to "Google AI",
        "bing image creator" to "Bing Image Creator", "microsoft designer" to "Microsoft Designer",
        "image creator" to "Microsoft Image Creator", "copilot" to "Microsoft Copilot", "meta ai" to "Meta AI",
        "imagine with meta" to "Meta AI", "runway" to "Runway", "black forest labs" to "FLUX", "flux.1" to "FLUX",
        "grok" to "Grok", "krea" to "Krea", "playground ai" to "Playground", "nightcafe" to "NightCafe",
        "craiyon" to "Craiyon", "artbreeder" to "Artbreeder", "dreamina" to "Dreamina", "jimeng" to "Jimeng",
        "kling" to "Kling", "hailuo" to "Hailuo", "seedream" to "Seedream", "recraft" to "Recraft",
        "nano banana" to "Google Gemini", "generative fill" to "Generative Fill", "generative expand" to "Generative Expand",
        "magic editor" to "Magic Editor", "magic eraser" to "Magic Eraser", "galaxy ai" to "Galaxy AI",
    )
    private val EDIT_TOOLS = setOf("Generative Fill", "Generative Expand", "Magic Editor", "Magic Eraser", "Galaxy AI")

    fun match(text: String): String? {
        val lower = text.lowercase()
        return GENERATORS.firstOrNull { (needle, _) -> containsWord(lower, needle) }?.second
    }

    fun kindOf(text: String): SignalKind =
        if (match(text) in EDIT_TOOLS) SignalKind.AI_EDITED else SignalKind.AI_GENERATED

    private fun containsWord(hay: String, needle: String): Boolean {
        var i = hay.indexOf(needle)
        while (i >= 0) {
            val before = if (i == 0) ' ' else hay[i - 1]
            val afterIdx = i + needle.length
            val after = if (afterIdx >= hay.length) ' ' else hay[afterIdx]
            if (!before.isLetterOrDigit() && !after.isLetter()) return true
            i = hay.indexOf(needle, i + 1)
        }
        return false
    }
}

/** Recognises the text that popular local image generators write into PNG text chunks. */
internal object GeneratorText {
    data class Hit(val tool: String, val prompt: String?)

    fun looksLikeSdParameters(v: String) = v.contains("Steps:") && (v.contains("Sampler:") || v.contains("Seed:") || v.contains("CFG scale:"))

    fun sdToolName(v: String) = when {
        v.contains("Version: f", ignoreCase = false) && v.contains("Forge") -> "Stable Diffusion WebUI Forge"
        v.contains("Civitai", true) -> "Stable Diffusion (Civitai)"
        else -> "Stable Diffusion WebUI"
    }

    fun sdPrompt(v: String) = v.substringBefore("Negative prompt:").substringBefore("\nSteps:").trim().takeIf { it.isNotEmpty() }

    fun detect(key: String, value: String, all: List<Pair<String, String>>): Hit? {
        val k = key.lowercase()
        return when {
            k == "parameters" && looksLikeSdParameters(value) -> Hit(sdToolName(value), sdPrompt(value))
            (k == "prompt" || k == "workflow") && (value.contains("class_type") || value.contains("\"nodes\"")) ->
                Hit("ComfyUI", comfyPrompt(all))
            k == "software" && value.contains("NovelAI", true) -> Hit("NovelAI", all.firstOrNull { it.first == "Description" }?.second)
            k == "invokeai_metadata" || k == "sd-metadata" || k == "dream" -> Hit("InvokeAI", Regex("\"positive_prompt\"\\s*:\\s*\"([^\"]+)\"").find(value)?.groupValues?.get(1))
            k == "fooocus_scheme" || (k == "comment" && value.contains("fooocus", true)) -> Hit("Fooocus", null)
            k == "generation_data" -> Hit("AI image generator", Regex("\"prompt\"\\s*:\\s*\"([^\"]+)\"").find(value)?.groupValues?.get(1))
            k == "comment" && value.contains("\"steps\"") && value.contains("\"sampler\"") -> Hit("NovelAI", null)
            else -> null
        }
    }

    private fun comfyPrompt(all: List<Pair<String, String>>): String? {
        val json = all.firstOrNull { it.first == "prompt" }?.second ?: return null
        return Regex("\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\]){3,})\"").find(json)?.groupValues?.get(1)?.replace("\\n", " ")
    }
}
