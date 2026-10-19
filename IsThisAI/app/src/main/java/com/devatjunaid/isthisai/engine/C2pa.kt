package com.devatjunaid.isthisai.engine

import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PSSParameterSpec
import javax.security.auth.x500.X500Principal

enum class CheckStatus { PASSED, FAILED, NOT_CHECKED }

data class C2paAction(
    val action: String,
    val digitalSourceType: String?,
    val softwareAgent: String?,
    val whenTime: String?,
    val description: String?,
)

data class C2paIngredient(
    val title: String?,
    val format: String?,
    val relationship: String?,
    val manifestLabel: String?,
)

data class C2paSignature(
    val signer: String?,
    val certificateIssuer: String?,
    val algorithm: String?,
    val validUntil: Long?,
    val status: CheckStatus,
    val note: String,
)

data class C2paManifest(
    val label: String,
    val claimGenerator: String?,
    val title: String?,
    val format: String?,
    val actions: List<C2paAction>,
    val ingredients: List<C2paIngredient>,
    val authors: List<String>,
    val assertionLabels: List<String>,
    val signature: C2paSignature?,
    val hashStatus: CheckStatus,
    val hashNote: String,
    val aiTraining: String?,
    val isActive: Boolean,
)

data class C2paStore(val manifests: List<C2paManifest>) {
    val active: C2paManifest? get() = manifests.lastOrNull()
}

/**
 * Reads a C2PA (Content Credentials) manifest store from raw JUMBF bytes and checks, fully
 * offline, that the claim signature matches its certificate and that the image bytes still match
 * the signed hash. Trust in the certificate authority itself is not evaluated.
 */
internal object C2pa {

    fun parse(jumbf: ByteArray, asset: ByteArray?): C2paStore? {
        val roots = Jumbf.parseBoxes(jumbf)
        val store = roots.firstOrNull { it.isSuper && (it.label == "c2pa" || Jumbf.uuidCode(it.typeUuid) == "c2pa") }
            ?: return null
        val manifestBoxes = store.superChildren().filter {
            Jumbf.uuidCode(it.typeUuid).let { c -> c == "c2ma" || c == "c2um" } || it.label?.startsWith("urn:") == true
        }
        if (manifestBoxes.isEmpty()) return null
        val manifests = manifestBoxes.mapIndexed { index, box ->
            parseManifest(box, asset, isActive = index == manifestBoxes.lastIndex)
        }
        return C2paStore(manifests)
    }

    private fun baseLabel(label: String?): String = (label ?: "").replace(Regex("__\\d+$"), "")

    private fun parseManifest(box: JumbfBox, asset: ByteArray?, isActive: Boolean): C2paManifest {
        val claimBox = box.superChildren().firstOrNull { baseLabel(it.label).startsWith("c2pa.claim") }
        val claimBytes = claimBox?.content("cbor")?.payload()
        val claim = claimBytes?.let { runCatching { CborReader.decode(it).asMap() }.getOrNull() }

        val assertionsBox = box.superChildren().firstOrNull { it.label == "c2pa.assertions" }
        val assertions = assertionsBox?.superChildren().orEmpty()

        val actions = ArrayList<C2paAction>()
        val ingredients = ArrayList<C2paIngredient>()
        val authors = ArrayList<String>()
        var aiTraining: String? = null
        var hashStatus = CheckStatus.NOT_CHECKED
        var hashNote = "No image hash in this manifest"

        for (a in assertions) {
            val label = baseLabel(a.label)
            when {
                label.startsWith("c2pa.actions") -> decodeContent(a).asMap()?.let { actions += parseActions(it) }
                label.startsWith("c2pa.ingredient") -> decodeContent(a).asMap()?.let { ingredients += parseIngredient(it) }
                label == "stds.schema-org.CreativeWork" -> authors += parseAuthors(a)
                label.endsWith("training-mining") -> aiTraining = parseTraining(decodeContent(a).asMap())
                label == "c2pa.hash.data" && isActive -> {
                    val alg = (claim?.text("alg"))
                    val result = checkDataHash(decodeContent(a).asMap(), alg, asset)
                    hashStatus = result.first
                    hashNote = result.second
                }
                label.startsWith("c2pa.hash.bmff") || label == "c2pa.hash.boxes" || label == "c2pa.hash.collection.data" -> {
                    if (isActive) hashNote = "Image hash uses a format this app does not check yet"
                }
            }
        }

        val signatureBox = box.superChildren().firstOrNull { baseLabel(it.label) == "c2pa.signature" }
        val signature = signatureBox?.content("cbor")?.payload()?.let { verifySignature(it, claimBytes) }

        return C2paManifest(
            label = box.label ?: "manifest",
            claimGenerator = claim?.let { claimGenerator(it) },
            title = claim?.text("dc:title") ?: claim?.text("title"),
            format = claim?.text("dc:format"),
            actions = actions,
            ingredients = ingredients,
            authors = authors.distinct(),
            assertionLabels = assertions.mapNotNull { it.label },
            signature = signature,
            hashStatus = hashStatus,
            hashNote = hashNote,
            aiTraining = aiTraining,
            isActive = isActive,
        )
    }

    private fun decodeContent(box: JumbfBox): Any? {
        val cbor = box.content("cbor") ?: return null
        return runCatching { CborReader.decode(cbor.payload()) }.getOrNull()
    }

    private fun claimGenerator(claim: Map<Any?, Any?>): String? {
        val info = claim["claim_generator_info"]
        val first = info.asList()?.firstOrNull().asMap() ?: info.asMap()
        if (first != null) {
            val name = first.text("name")
            val version = first.text("version")
            if (name != null) return listOfNotNull(name, version).joinToString(" ")
        }
        return claim.text("claim_generator")
    }

    private fun agentName(v: Any?): String? = when (v) {
        is String -> v
        else -> v.asMap()?.let { m ->
            val name = m.text("name") ?: return@let null
            listOfNotNull(name, m.text("version")).joinToString(" ")
        }
    }

    private fun parseActions(m: Map<Any?, Any?>): List<C2paAction> {
        val agents = m.list("softwareAgents")
        return m.list("actions").orEmpty().mapNotNull { it.asMap() }.map { a ->
            val params = a.map("parameters")
            val agent = agentName(a["softwareAgent"])
                ?: (a["softwareAgentIndex"] as? Long)?.let { idx -> agentName(agents?.getOrNull(idx.toInt())) }
            C2paAction(
                action = a.text("action") ?: "unknown",
                digitalSourceType = a.text("digitalSourceType") ?: params?.text("digitalSourceType"),
                softwareAgent = agent,
                whenTime = a.text("when"),
                description = a.text("description") ?: params?.text("description"),
            )
        }
    }

    private fun parseIngredient(m: Map<Any?, Any?>): C2paIngredient {
        val url = m.map("c2pa_manifest")?.text("url") ?: m.map("activeManifest")?.text("url")
        val label = url?.substringAfter("c2pa/", "")?.substringBefore('/')?.takeIf { it.isNotEmpty() }
        return C2paIngredient(
            title = m.text("dc:title") ?: m.text("title"),
            format = m.text("dc:format") ?: m.text("format"),
            relationship = m.text("relationship"),
            manifestLabel = label,
        )
    }

    private fun parseAuthors(box: JumbfBox): List<String> {
        val json = box.content("json")?.payload()?.toString(Charsets.UTF_8) ?: return emptyList()
        val authorPart = json.substringAfter("\"author\"", "")
        if (authorPart.isEmpty()) return emptyList()
        return Regex("\"name\"\\s*:\\s*\"([^\"]{1,120})\"").findAll(authorPart).map { it.groupValues[1] }.take(5).toList()
    }

    private fun parseTraining(m: Map<Any?, Any?>?): String? {
        val entries = m?.map("entries") ?: return null
        val gen = entries.entries.firstOrNull { (k, _) -> (k as? String)?.contains("generative_training") == true }
        val use = gen?.value.asMap()?.text("use") ?: return null
        return when (use) {
            "notAllowed" -> "Not allowed"
            "allowed" -> "Allowed"
            "constrained" -> "Allowed with conditions"
            else -> use
        }
    }

    // ---------------- Hard binding (image hash) ----------------

    private fun checkDataHash(m: Map<Any?, Any?>?, claimAlg: String?, asset: ByteArray?): Pair<CheckStatus, String> {
        if (m == null || asset == null) return CheckStatus.NOT_CHECKED to "Image hash could not be read"
        val expected = m["hash"] as? ByteArray ?: return CheckStatus.NOT_CHECKED to "Image hash missing"
        val alg = m.text("alg") ?: claimAlg ?: "sha256"
        val digest = digestFor(alg) ?: return CheckStatus.NOT_CHECKED to "Unsupported hash algorithm $alg"
        val exclusions = m.list("exclusions").orEmpty().mapNotNull { it.asMap() }
            .mapNotNull { e ->
                val s = e.long("start") ?: return@mapNotNull null
                val l = e.long("length") ?: return@mapNotNull null
                s to l
            }.sortedBy { it.first }
        var p = 0L
        for ((start, length) in exclusions) {
            if (start < p || start + length > asset.size) return CheckStatus.FAILED to "Hash exclusions do not fit this file"
            digest.update(asset, p.toInt(), (start - p).toInt())
            p = start + length
        }
        digest.update(asset, p.toInt(), (asset.size - p).toInt())
        val actual = digest.digest()
        return if (actual.contentEquals(expected)) {
            CheckStatus.PASSED to "Image pixels and data match the signed hash"
        } else {
            CheckStatus.FAILED to "Image bytes changed after signing"
        }
    }

    private fun digestFor(alg: String): MessageDigest? = when (alg.lowercase()) {
        "sha256" -> MessageDigest.getInstance("SHA-256")
        "sha384" -> MessageDigest.getInstance("SHA-384")
        "sha512" -> MessageDigest.getInstance("SHA-512")
        else -> null
    }

    // ---------------- COSE_Sign1 signature ----------------

    private fun verifySignature(coseBytes: ByteArray, claimBytes: ByteArray?): C2paSignature {
        val cose = runCatching { CborReader.decode(coseBytes).asList() }.getOrNull()
        if (cose == null || cose.size < 4) {
            return C2paSignature(null, null, null, null, CheckStatus.NOT_CHECKED, "Signature could not be read")
        }
        val protectedBytes = cose[0] as? ByteArray ?: ByteArray(0)
        val protectedMap = if (protectedBytes.isEmpty()) emptyMap() else
            runCatching { CborReader.decode(protectedBytes).asMap() }.getOrNull().orEmpty()
        val unprotected = cose[1].asMap().orEmpty()
        val sig = cose[3] as? ByteArray

        val algId = (protectedMap[1L] ?: unprotected[1L]) as? Long
        val chainValue = protectedMap[33L] ?: unprotected[33L] ?: protectedMap["x5chain"] ?: unprotected["x5chain"]
        val leafDer = when (chainValue) {
            is ByteArray -> chainValue
            is List<*> -> chainValue.firstOrNull() as? ByteArray
            else -> null
        }
        val cert = leafDer?.let {
            runCatching {
                CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(it)) as X509Certificate
            }.getOrNull()
        }
        val signer = cert?.subjectX500Principal?.let { org(it) ?: cn(it) }
        val issuer = cert?.issuerX500Principal?.let { org(it) ?: cn(it) }
        val algName = algorithmName(algId)

        if (cert == null || sig == null || claimBytes == null || algId == null) {
            return C2paSignature(signer, issuer, algName, cert?.notAfter?.time, CheckStatus.NOT_CHECKED, "Signature parts are missing")
        }
        val toBeSigned = CborWriter.array("Signature1", protectedBytes, ByteArray(0), claimBytes)
        val result = runCatching { verify(algId, cert, toBeSigned, sig) }
        val status = when (result.getOrNull()) {
            true -> CheckStatus.PASSED
            false -> CheckStatus.FAILED
            null -> CheckStatus.NOT_CHECKED
        }
        val note = when (status) {
            CheckStatus.PASSED -> "Signature matches the certificate"
            CheckStatus.FAILED -> "Signature does not match: the label may have been altered"
            CheckStatus.NOT_CHECKED -> "This phone cannot check $algName signatures"
        }
        return C2paSignature(signer, issuer, algName, cert.notAfter?.time, status, note)
    }

    private fun algorithmName(id: Long?): String = when (id) {
        -7L -> "ES256"; -35L -> "ES384"; -36L -> "ES512"
        -37L -> "PS256"; -38L -> "PS384"; -39L -> "PS512"
        -8L -> "Ed25519"
        null -> "unknown"
        else -> "alg $id"
    }

    /** Returns true/false for a completed check; throws when the algorithm is unavailable. */
    private fun verify(algId: Long, cert: X509Certificate, data: ByteArray, sig: ByteArray): Boolean {
        val key = cert.publicKey
        return when (algId) {
            -7L, -35L, -36L -> {
                val name = when (algId) { -7L -> "SHA256withECDSA"; -35L -> "SHA384withECDSA"; else -> "SHA512withECDSA" }
                Signature.getInstance(name).run { initVerify(key); update(data); verify(rawEcdsaToDer(sig)) }
            }
            -37L, -38L, -39L -> {
                val (hash, mgf, salt) = when (algId) {
                    -37L -> Triple("SHA-256", MGF1ParameterSpec.SHA256, 32)
                    -38L -> Triple("SHA-384", MGF1ParameterSpec.SHA384, 48)
                    else -> Triple("SHA-512", MGF1ParameterSpec.SHA512, 64)
                }
                val androidName = hash.replace("-", "") + "withRSA/PSS"
                val s = runCatching { Signature.getInstance(androidName) }.getOrNull()
                    ?: Signature.getInstance("RSASSA-PSS").also {
                        it.setParameter(PSSParameterSpec(hash, "MGF1", mgf, salt, 1))
                    }
                s.initVerify(key); s.update(data); s.verify(sig)
            }
            -8L -> Signature.getInstance("Ed25519").run { initVerify(key); update(data); verify(sig) }
            else -> throw UnsupportedOperationException("alg $algId")
        }
    }

    /** COSE stores ECDSA signatures as raw r||s; Java expects ASN.1 DER. */
    private fun rawEcdsaToDer(raw: ByteArray): ByteArray {
        if (raw.isEmpty() || raw.size % 2 != 0) return raw
        val half = raw.size / 2
        val r = BigInteger(1, raw.copyOfRange(0, half)).toByteArray()
        val s = BigInteger(1, raw.copyOfRange(half, raw.size)).toByteArray()
        val body = byteArrayOf(0x02, r.size.toByte()) + r + byteArrayOf(0x02, s.size.toByte()) + s
        return if (body.size < 128) {
            byteArrayOf(0x30, body.size.toByte()) + body
        } else {
            byteArrayOf(0x30, 0x81.toByte(), body.size.toByte()) + body
        }
    }

    private fun rdn(p: X500Principal, key: String): String? {
        val name = p.getName(X500Principal.RFC2253)
        val m = Regex("(?:^|,)$key=((?:\\\\.|[^,])+)").find(name) ?: return null
        return m.groupValues[1].replace("\\", "").trim().takeIf { it.isNotEmpty() }
    }

    private fun org(p: X500Principal) = rdn(p, "O")
    private fun cn(p: X500Principal) = rdn(p, "CN")
}
