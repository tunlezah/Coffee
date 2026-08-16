package com.cawfee.bluetooth.parser

import com.cawfee.bluetooth.encryption.JuraCipher

/** Brew progress state reported by `5a401527` (§9, PROGRESS_STATE_INTAKE). */
data class BrewProgress(val state: Int, val raw: ByteArray) {
    val isCoffeeReady: Boolean get() = state == 0x24
    val isPMode: Boolean get() = state == 0xFF

    override fun equals(other: Any?): Boolean =
        other is BrewProgress && state == other.state && raw.contentEquals(other.raw)

    override fun hashCode(): Int = 31 * state + raw.contentHashCode()
}

object ProgressParser {
    fun parseDecoded(decoded: ByteArray): BrewProgress {
        // Byte 0 is the key echo; the progress state byte follows.
        val state = if (decoded.size > 1) decoded[1].toInt() and 0xFF else 0
        return BrewProgress(state, decoded)
    }

    fun parse(raw: ByteArray, key: Int): BrewProgress = parseDecoded(JuraCipher.decrypt(raw, key))
}
