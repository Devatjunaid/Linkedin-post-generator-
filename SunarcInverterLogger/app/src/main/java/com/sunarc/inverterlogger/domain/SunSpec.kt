package com.sunarc.inverterlogger.domain

import java.io.DataInputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.pow

class ModbusException(message: String) : IOException(message)

/** Modbus TCP "read holding registers" (function 3) framing. */
object ModbusTcp {
    fun readHoldingRequest(transactionId: Int, unitId: Int, address: Int, count: Int): ByteArray {
        require(count in 1..125) { "Modbus can read 1..125 registers at a time" }
        return byteArrayOf(
            (transactionId shr 8).toByte(), transactionId.toByte(),
            0, 0, // protocol id
            0, 6, // remaining length: unit + function + address(2) + count(2)
            unitId.toByte(),
            3,
            (address shr 8).toByte(), address.toByte(),
            (count shr 8).toByte(), count.toByte()
        )
    }

    /** [frame] is a full response including the 7-byte MBAP header. */
    fun parseResponse(frame: ByteArray, transactionId: Int, count: Int): IntArray {
        if (frame.size < 9) throw ModbusException("Short Modbus reply")
        val tx = ((frame[0].toInt() and 0xFF) shl 8) or (frame[1].toInt() and 0xFF)
        if (tx != transactionId and 0xFFFF) throw ModbusException("Unexpected transaction id $tx")
        val function = frame[7].toInt() and 0xFF
        if (function == 0x83) throw ModbusException("Inverter refused the request (exception ${frame[8].toInt() and 0xFF})")
        if (function != 3) throw ModbusException("Unexpected function $function")
        val byteCount = frame[8].toInt() and 0xFF
        if (byteCount != count * 2 || frame.size < 9 + byteCount) throw ModbusException("Wrong register count in reply")
        return IntArray(count) { i -> ((frame[9 + i * 2].toInt() and 0xFF) shl 8) or (frame[10 + i * 2].toInt() and 0xFF) }
    }
}

fun interface RegisterReader {
    /** Returns [count] unsigned 16-bit holding registers starting at [address]. */
    fun read(address: Int, count: Int): IntArray
}

enum class InverterState(val label: String) {
    OFF("Off"), SLEEPING("Sleeping"), STARTING("Starting"), MPPT("Producing"), THROTTLED("Limited"),
    SHUTTING_DOWN("Shutting down"), FAULT("Fault"), STANDBY("Standby"), UNKNOWN("Unknown");

    companion object {
        fun of(code: Int) = entries.getOrNull(code - 1)?.takeIf { it != UNKNOWN } ?: UNKNOWN
    }
}

data class SunSpecReading(
    val manufacturer: String?,
    val model: String?,
    val serial: String?,
    val powerW: Double?,
    val lifetimeKwh: Double?,
    val dcVoltage: Double?,
    val acFrequency: Double?,
    val temperatureC: Double?,
    val state: InverterState
)

/**
 * Reads SunSpec inverter models over Modbus TCP on the home network. SMA, Fronius, SolarEdge,
 * Kostal, Sungrow and FIMER/ABB can expose this once Modbus TCP is enabled on the inverter.
 * No cloud account or internet connection is involved.
 */
object SunSpec {
    private val baseAddresses = listOf(40000, 0, 50000)
    private const val MARKER_HI = 0x5375 // "Su"
    private const val MARKER_LO = 0x6E53 // "nS"

    fun read(reader: RegisterReader): SunSpecReading {
        val base = baseAddresses.firstOrNull { addr ->
            runCatching { reader.read(addr, 2) }.getOrNull()?.let { it[0] == MARKER_HI && it[1] == MARKER_LO } == true
        } ?: throw ModbusException("No SunSpec data found. Is Modbus TCP (SunSpec) switched on in the inverter?")

        var address = base + 2
        var common: IntArray? = null
        var inverter: Pair<Int, IntArray>? = null
        var models = 0
        while (models++ < 60 && inverter == null) {
            val header = reader.read(address, 2)
            val id = header[0]
            val length = header[1]
            if (id == 0xFFFF || length == 0) break
            if (id == 1 && common == null) common = readBlock(reader, address + 2, length)
            if (id in 101..103 || id in 111..113) inverter = id to readBlock(reader, address + 2, length)
            address += 2 + length
        }
        val inv = inverter ?: throw ModbusException("The inverter did not report a SunSpec inverter model.")
        val c = common
        val values = if (inv.first >= 111) parseFloatModel(inv.second) else parseIntModel(inv.second)
        return values.copy(
            manufacturer = c?.let { regsToString(it, 0, 16) },
            model = c?.let { regsToString(it, 16, 16) },
            serial = c?.let { if (it.size >= 64) regsToString(it, 48, 16) else null }
        )
    }

    private fun readBlock(reader: RegisterReader, address: Int, length: Int): IntArray {
        val out = IntArray(length)
        var done = 0
        while (done < length) {
            val n = minOf(125, length - done)
            reader.read(address + done, n).copyInto(out, done)
            done += n
        }
        return out
    }

    internal fun parseIntModel(r: IntArray): SunSpecReading {
        fun s16(i: Int) = r.getOrNull(i)?.let { if (it >= 0x8000) it - 0x10000 else it }
        fun scaled(i: Int, sfIndex: Int, signed: Boolean): Double? {
            val raw = r.getOrNull(i) ?: return null
            if (signed && raw == 0x8000) return null
            if (!signed && raw == 0xFFFF) return null
            val sf = s16(sfIndex) ?: return null
            if (sf == -0x8000) return null
            val v = if (signed) s16(i)!! else raw
            return v * 10.0.pow(sf)
        }
        val wh = if (r.size > 24) {
            val acc = (r[22].toLong() shl 16) or r[23].toLong()
            val sf = s16(24) ?: 0
            if (acc == 0L) null else acc * 10.0.pow(sf)
        } else null
        return SunSpecReading(
            manufacturer = null, model = null, serial = null,
            powerW = scaled(12, 13, signed = true),
            lifetimeKwh = wh?.div(1000.0),
            dcVoltage = scaled(27, 28, signed = false),
            acFrequency = scaled(14, 15, signed = false),
            temperatureC = scaled(31, 35, signed = true),
            state = InverterState.of(r.getOrNull(36) ?: 0)
        )
    }

    internal fun parseFloatModel(r: IntArray): SunSpecReading {
        fun f32(i: Int): Double? {
            if (r.size < i + 2) return null
            val bits = (r[i] shl 16) or r[i + 1]
            val v = Float.fromBits(bits)
            return if (v.isNaN()) null else v.toDouble()
        }
        return SunSpecReading(
            manufacturer = null, model = null, serial = null,
            powerW = f32(20),
            lifetimeKwh = f32(30)?.div(1000.0),
            dcVoltage = f32(34),
            acFrequency = f32(22),
            temperatureC = f32(38),
            state = InverterState.of(r.getOrNull(46) ?: 0)
        )
    }

    private fun regsToString(r: IntArray, from: Int, count: Int): String? {
        val sb = StringBuilder()
        for (i in from until minOf(from + count, r.size)) {
            val hi = (r[i] shr 8) and 0xFF
            val lo = r[i] and 0xFF
            if (hi != 0) sb.append(hi.toChar())
            if (lo != 0) sb.append(lo.toChar())
        }
        return sb.toString().trim().ifEmpty { null }
    }
}

/** Plain-socket Modbus TCP client. Call from a background thread. */
class ModbusTcpClient(
    private val host: String,
    private val port: Int = 502,
    private val unitId: Int = 1,
    private val timeoutMillis: Int = 4000
) : RegisterReader, AutoCloseable {
    private var socket: Socket? = null
    private var tx = 0

    private fun connection(): Socket = socket ?: Socket().also {
        it.connect(InetSocketAddress(host, port), timeoutMillis)
        it.soTimeout = timeoutMillis
        socket = it
    }

    override fun read(address: Int, count: Int): IntArray {
        val s = connection()
        tx = (tx + 1) and 0xFFFF
        s.getOutputStream().apply { write(ModbusTcp.readHoldingRequest(tx, unitId, address, count)); flush() }
        val input = DataInputStream(s.getInputStream())
        val header = ByteArray(7)
        input.readFully(header)
        val length = ((header[4].toInt() and 0xFF) shl 8) or (header[5].toInt() and 0xFF)
        if (length !in 2..260) throw ModbusException("Bad reply length $length")
        val rest = ByteArray(length - 1)
        input.readFully(rest)
        return ModbusTcp.parseResponse(header + rest, tx, count)
    }

    override fun close() {
        runCatching { socket?.close() }
        socket = null
    }
}
