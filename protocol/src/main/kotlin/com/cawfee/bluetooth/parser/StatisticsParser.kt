package com.cawfee.bluetooth.parser

import com.cawfee.bluetooth.encryption.JuraCipher

/** Decoded machine statistics (§10). */
data class Statistics(
    /** counts[0] = grand total; counts[productCode] = that product's count. */
    val counts: List<Long>,
) {
    val total: Long get() = counts.firstOrNull() ?: 0L
    fun countForProduct(code: Int): Long = counts.getOrElse(code) { 0L }
}

/**
 * Parses the Statistics Data characteristic (`5a401534`) — a sequence of 3-byte
 * (24-bit) big-endian counters (§10). 0xFFFF is treated as 0; totals of 0 or
 * > 1,000,000 are considered corrupt and clamped to 0.
 */
object StatisticsParser {

    private const val MAX_VALID = 1_000_000L

    fun parseDecoded(decoded: ByteArray): Statistics {
        val counts = ArrayList<Long>(decoded.size / 3)
        var i = 0
        while (i + 3 <= decoded.size) {
            val raw = ((decoded[i].toInt() and 0xFF).toLong() shl 16) or
                ((decoded[i + 1].toInt() and 0xFF).toLong() shl 8) or
                (decoded[i + 2].toInt() and 0xFF).toLong()
            val value = if (raw == 0xFFFFL || raw > MAX_VALID) 0L else raw
            counts.add(value)
            i += 3
        }
        return Statistics(counts)
    }

    fun parse(raw: ByteArray, key: Int): Statistics =
        parseDecoded(JuraCipher.decrypt(raw, key))

    /**
     * Statistics readiness check (§8.4): the engine is busy while the (decoded) status
     * byte [1] == 0xE1 or the payload begins with 0x0E. Poll until this returns true.
     */
    fun isReady(decoded: ByteArray): Boolean {
        if (decoded.isEmpty()) return false
        if (decoded[0].toInt() and 0xFF == 0x0E) return false
        if (decoded.size > 1 && (decoded[1].toInt() and 0xFF) == 0xE1) return false
        return true
    }
}
