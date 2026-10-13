package com.devatjunaid.renevo.backup

import android.content.Context
import android.net.Uri
import com.devatjunaid.renevo.data.AppSettings
import com.devatjunaid.renevo.data.CycleUnit
import com.devatjunaid.renevo.data.PricePoint
import com.devatjunaid.renevo.data.SubStatus
import com.devatjunaid.renevo.data.Subscription
import com.devatjunaid.renevo.data.ThemeMode
import com.devatjunaid.renevo.data.Usage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@Serializable
data class BackupSubscription(
    val id: Long,
    val name: String,
    val priceMinor: Long,
    val currency: String,
    val cycleUnit: String,
    val cycleCount: Int,
    val firstPaymentEpochDay: Long,
    val category: String,
    val paymentMethod: String,
    val notes: String,
    val status: String,
    val trialEndEpochDay: Long? = null,
    val reminderDaysBefore: Int,
    val usage: String,
    val createdAtMillis: Long,
    val statusChangedEpochDay: Long? = null,
)

@Serializable
data class BackupPrice(val subscriptionId: Long, val priceMinor: Long, val effectiveEpochDay: Long)

@Serializable
data class BackupSettings(
    val currency: String,
    val budgetMinor: Long,
    val showYearly: Boolean,
    val theme: String,
    val rates: Map<String, Double>,
    val renewalReminders: Boolean,
    val trialAlerts: Boolean,
    val weeklyDigest: Boolean,
    val defaultReminderDays: Int,
)

@Serializable
data class BackupFile(
    val format: String = "renevo-backup",
    val version: Int = 1,
    val exportedAtMillis: Long,
    val subscriptions: List<BackupSubscription>,
    val prices: List<BackupPrice>,
    val settings: BackupSettings? = null,
)

data class RestoredData(val subs: List<Subscription>, val prices: List<PricePoint>, val settings: AppSettings?)

class WrongPasswordException : Exception("Wrong password or damaged file")
class NotABackupException : Exception("This file is not a Renevo backup")

/**
 * Encrypted backup: "RNVB" magic + version byte + 16-byte salt + 12-byte IV + AES-256-GCM ciphertext.
 * The key is derived from the user's password with PBKDF2-HMAC-SHA256.
 */
object BackupManager {
    private val MAGIC = byteArrayOf('R'.code.toByte(), 'N'.code.toByte(), 'V'.code.toByte(), 'B'.code.toByte())
    private const val VERSION: Byte = 1
    private const val ITERATIONS = 120_000
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun toFile(subs: List<Subscription>, prices: List<PricePoint>, s: AppSettings) = BackupFile(
        exportedAtMillis = System.currentTimeMillis(),
        subscriptions = subs.map {
            BackupSubscription(
                it.id, it.name, it.priceMinor, it.currency, it.cycleUnit.name, it.cycleCount,
                it.firstPaymentEpochDay, it.category, it.paymentMethod, it.notes, it.status.name,
                it.trialEndEpochDay, it.reminderDaysBefore, it.usage.name, it.createdAtMillis, it.statusChangedEpochDay,
            )
        },
        prices = prices.map { BackupPrice(it.subscriptionId, it.priceMinor, it.effectiveEpochDay) },
        settings = BackupSettings(
            s.currency, s.budgetMinor, s.showYearly, s.theme.name, s.rates,
            s.renewalReminders, s.trialAlerts, s.weeklyDigest, s.defaultReminderDays,
        ),
    )

    fun fromFile(f: BackupFile): RestoredData {
        val subs = f.subscriptions.map {
            Subscription(
                id = it.id,
                name = it.name,
                priceMinor = it.priceMinor,
                currency = it.currency,
                cycleUnit = enumOr(it.cycleUnit, CycleUnit.MONTH),
                cycleCount = it.cycleCount,
                firstPaymentEpochDay = it.firstPaymentEpochDay,
                category = it.category,
                paymentMethod = it.paymentMethod,
                notes = it.notes,
                status = enumOr(it.status, SubStatus.ACTIVE),
                trialEndEpochDay = it.trialEndEpochDay,
                reminderDaysBefore = it.reminderDaysBefore,
                usage = enumOr(it.usage, Usage.UNKNOWN),
                createdAtMillis = it.createdAtMillis,
                statusChangedEpochDay = it.statusChangedEpochDay,
            )
        }
        val prices = f.prices.map { PricePoint(subscriptionId = it.subscriptionId, priceMinor = it.priceMinor, effectiveEpochDay = it.effectiveEpochDay) }
        val settings = f.settings?.let {
            AppSettings(
                currency = it.currency, budgetMinor = it.budgetMinor, showYearly = it.showYearly,
                theme = enumOr(it.theme, ThemeMode.SYSTEM), rates = it.rates,
                renewalReminders = it.renewalReminders, trialAlerts = it.trialAlerts,
                weeklyDigest = it.weeklyDigest, defaultReminderDays = it.defaultReminderDays,
            )
        }
        return RestoredData(subs, prices, settings)
    }

    fun encrypt(file: BackupFile, password: CharArray): ByteArray {
        val plain = json.encodeToString(BackupFile.serializer(), file).toByteArray(Charsets.UTF_8)
        val rnd = SecureRandom()
        val salt = ByteArray(16).also(rnd::nextBytes)
        val iv = ByteArray(12).also(rnd::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        cipher.updateAAD(MAGIC)
        val body = cipher.doFinal(plain)
        return MAGIC + byteArrayOf(VERSION) + salt + iv + body
    }

    fun decrypt(bytes: ByteArray, password: CharArray): BackupFile {
        if (bytes.size < 4 + 1 + 16 + 12 + 16 || !bytes.copyOfRange(0, 4).contentEquals(MAGIC)) throw NotABackupException()
        val salt = bytes.copyOfRange(5, 21)
        val iv = bytes.copyOfRange(21, 33)
        val body = bytes.copyOfRange(33, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        cipher.updateAAD(MAGIC)
        val plain = try {
            cipher.doFinal(body)
        } catch (e: AEADBadTagException) {
            throw WrongPasswordException()
        }
        val f = json.decodeFromString(BackupFile.serializer(), plain.toString(Charsets.UTF_8))
        if (f.format != "renevo-backup") throw NotABackupException()
        return f
    }

    private fun key(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(raw, "AES")
    }

    fun write(context: Context, uri: Uri, bytes: ByteArray) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
            ?: error("Couldn't open the file for writing")
    }

    fun read(context: Context, uri: Uri): ByteArray =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Couldn't open the file")

    private inline fun <reified T : Enum<T>> enumOr(name: String, fallback: T): T =
        runCatching { enumValueOf<T>(name) }.getOrDefault(fallback)
}
