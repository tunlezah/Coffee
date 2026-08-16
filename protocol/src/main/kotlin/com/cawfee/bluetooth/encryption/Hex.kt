package com.cawfee.bluetooth.encryption

/** Small hex helpers used by the protocol layer and its tests. */
object Hex {
    fun decode(hex: String): ByteArray {
        val clean = hex.filterNot { it.isWhitespace() || it == ':' || it == '-' }
        require(clean.length % 2 == 0) { "Hex string must have an even length" }
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    fun encode(bytes: ByteArray, separator: String = ""): String =
        bytes.joinToString(separator) { "%02x".format(it.toInt() and 0xFF) }
}

/** Convenience extensions. */
fun ByteArray.toHex(separator: String = ""): String = Hex.encode(this, separator)
fun String.hexToBytes(): ByteArray = Hex.decode(this)
