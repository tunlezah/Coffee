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

    /**
     * Decrypt and parse a Statistics Data read. Unlike Machine Status / Product
     * Progress, the decoded statistics payload does NOT begin with the key echo — the
     * 3-byte counters start at byte 0. This matches the Jutta-Proto protocol doc
     * (example response `00014E …` where `0x00014E` is the total product count) and the
     * AlexxIT/Jura implementation, both verified against real machines.
     */
    fun parse(raw: ByteArray, key: Int): Statistics =
        parseDecoded(JuraCipher.decrypt(raw, key))

    /**
     * Statistics readiness probe. After the request is written, poll the Statistics
     * Command characteristic and pass the RAW (still-encoded) read here: byte 1 equal
     * to 0xE1 means the statistics engine is still busy. This is the AlexxIT/Jura
     * behavior, field-proven on real machines (Jutta-Proto's README and code disagree
     * with each other on this check, so the working implementation wins).
     */
    fun isReady(raw: ByteArray): Boolean =
        raw.size > 1 && (raw[1].toInt() and 0xFF) != 0xE1
}
