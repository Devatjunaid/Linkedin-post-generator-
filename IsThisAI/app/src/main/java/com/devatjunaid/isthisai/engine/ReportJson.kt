package com.devatjunaid.isthisai.engine

/** Serialises a [ScanReport] for the history database and for "Export report". */
object ReportJson {

    fun encode(r: ScanReport, pretty: Boolean = false): String {
        val obj = linkedMapOf<String, Any?>(
            "app" to "Is This AI?",
            "fileName" to r.fileName,
            "mimeType" to r.mimeType,
            "format" to r.format.name,
            "sizeBytes" to r.sizeBytes,
            "width" to r.width,
            "height" to r.height,
            "verdict" to r.verdict.name,
            "strength" to r.strength.name,
            "aiTool" to r.aiTool,
            "aiPrompt" to r.aiPrompt,
            "scannedAt" to r.scannedAt,
            "headerHex" to r.headerHex,
            "warnings" to r.warnings,
            "evidence" to r.evidence.map {
                linkedMapOf("source" to it.source, "field" to it.field, "value" to it.value, "kind" to it.kind.name, "strength" to it.strength.name)
            },
            "steps" to r.steps.map { linkedMapOf("step" to it.step.name, "status" to it.status.name, "detail" to it.detail) },
            "trail" to r.trail.map { linkedMapOf("title" to it.title, "detail" to it.detail, "tool" to it.tool, "kind" to it.kind.name) },
            "raw" to r.raw.map { s -> linkedMapOf("title" to s.title, "entries" to s.entries.map { listOf(it.first, it.second) }) },
        )
        return StringBuilder().also { write(it, obj, pretty, 0) }.toString()
    }

    fun decode(json: String): ScanReport {
        val o = Parser(json).parseValue() as Map<*, *>
        fun s(k: String) = o[k] as? String
        fun n(k: String) = (o[k] as? Number)
        fun list(k: String) = (o[k] as? List<*>).orEmpty().filterIsInstance<Map<*, *>>()
        return ScanReport(
            fileName = s("fileName") ?: "image",
            mimeType = s("mimeType"),
            format = enumOr(s("format"), ImageFormat.UNKNOWN),
            sizeBytes = n("sizeBytes")?.toLong() ?: 0L,
            width = n("width")?.toInt(),
            height = n("height")?.toInt(),
            verdict = enumOr(s("verdict"), Verdict.NO_LABEL),
            strength = enumOr(s("strength"), Strength.NONE),
            evidence = list("evidence").map {
                Evidence(it["source"] as? String ?: "", it["field"] as? String ?: "", it["value"] as? String ?: "",
                    enumOr(it["kind"] as? String, SignalKind.INFO), enumOr(it["strength"] as? String, Strength.HINT))
            },
            steps = list("steps").mapNotNull {
                val step = runCatching { ScanStep.valueOf(it["step"] as String) }.getOrNull() ?: return@mapNotNull null
                StepResult(step, enumOr(it["status"] as? String, StepStatus.NONE), it["detail"] as? String ?: "")
            },
            trail = list("trail").map {
                TrailStep(it["title"] as? String ?: "", it["detail"] as? String ?: "", it["tool"] as? String,
                    enumOr(it["kind"] as? String, TrailKind.INFO))
            },
            raw = list("raw").map { sec ->
                RawSection(sec["title"] as? String ?: "", (sec["entries"] as? List<*>).orEmpty().mapNotNull { e ->
                    val pair = e as? List<*> ?: return@mapNotNull null
                    (pair.getOrNull(0) as? String ?: "") to (pair.getOrNull(1) as? String ?: "")
                })
            },
            warnings = (o["warnings"] as? List<*>).orEmpty().filterIsInstance<String>(),
            headerHex = s("headerHex") ?: "",
            aiTool = s("aiTool"),
            aiPrompt = s("aiPrompt"),
            scannedAt = n("scannedAt")?.toLong() ?: 0L,
        )
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: fallback

    // ---------------- writer ----------------

    private fun write(sb: StringBuilder, v: Any?, pretty: Boolean, depth: Int) {
        when (v) {
            null -> sb.append("null")
            is String -> quote(sb, v)
            is Number, is Boolean -> sb.append(v.toString())
            is Map<*, *> -> {
                sb.append('{')
                var first = true
                for ((k, value) in v) {
                    if (value == null) continue
                    if (!first) sb.append(',')
                    first = false
                    newline(sb, pretty, depth + 1)
                    quote(sb, k.toString())
                    sb.append(if (pretty) ": " else ":")
                    write(sb, value, pretty, depth + 1)
                }
                if (!first) newline(sb, pretty, depth)
                sb.append('}')
            }
            is List<*> -> {
                sb.append('[')
                v.forEachIndexed { i, item ->
                    if (i > 0) sb.append(',')
                    newline(sb, pretty, depth + 1)
                    write(sb, item, pretty, depth + 1)
                }
                if (v.isNotEmpty()) newline(sb, pretty, depth)
                sb.append(']')
            }
            else -> quote(sb, v.toString())
        }
    }

    private fun newline(sb: StringBuilder, pretty: Boolean, depth: Int) {
        if (!pretty) return
        sb.append('\n')
        repeat(depth) { sb.append("  ") }
    }

    private fun quote(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) {
            when {
                c == '"' -> sb.append("\\\"")
                c == '\\' -> sb.append("\\\\")
                c == '\n' -> sb.append("\\n")
                c == '\r' -> sb.append("\\r")
                c == '\t' -> sb.append("\\t")
                c < ' ' -> sb.append(String.format("\\u%04x", c.code))
                else -> sb.append(c)
            }
        }
        sb.append('"')
    }

    // ---------------- parser ----------------

    private class Parser(val s: String) {
        var i = 0

        fun parseValue(): Any? {
            skipWs()
            if (i >= s.length) throw IllegalArgumentException("Unexpected end of JSON")
            return when (s[i]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't' -> { expect("true"); true }
                'f' -> { expect("false"); false }
                'n' -> { expect("null"); null }
                else -> parseNumber()
            }
        }

        private fun parseObject(): Map<String, Any?> {
            val m = LinkedHashMap<String, Any?>()
            i++
            skipWs()
            if (s[i] == '}') { i++; return m }
            while (true) {
                skipWs()
                val k = parseString()
                skipWs()
                if (s[i++] != ':') throw IllegalArgumentException("Expected ':'")
                m[k] = parseValue()
                skipWs()
                when (s[i++]) {
                    ',' -> continue
                    '}' -> return m
                    else -> throw IllegalArgumentException("Expected ',' or '}'")
                }
            }
        }

        private fun parseArray(): List<Any?> {
            val l = ArrayList<Any?>()
            i++
            skipWs()
            if (s[i] == ']') { i++; return l }
            while (true) {
                l += parseValue()
                skipWs()
                when (s[i++]) {
                    ',' -> continue
                    ']' -> return l
                    else -> throw IllegalArgumentException("Expected ',' or ']'")
                }
            }
        }

        private fun parseString(): String {
            if (s[i] != '"') throw IllegalArgumentException("Expected string")
            i++
            val sb = StringBuilder()
            while (true) {
                val c = s[i++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        when (val e = s[i++]) {
                            'n' -> sb.append('\n'); 'r' -> sb.append('\r'); 't' -> sb.append('\t')
                            'b' -> sb.append('\b'); 'f' -> sb.append('\u000C')
                            'u' -> { sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                            else -> sb.append(e)
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun parseNumber(): Number {
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
            val t = s.substring(start, i)
            return if (t.contains('.') || t.contains('e') || t.contains('E')) t.toDouble() else t.toLong()
        }

        private fun expect(word: String) {
            if (!s.startsWith(word, i)) throw IllegalArgumentException("Expected $word")
            i += word.length
        }

        private fun skipWs() {
            while (i < s.length && s[i].isWhitespace()) i++
        }
    }
}
