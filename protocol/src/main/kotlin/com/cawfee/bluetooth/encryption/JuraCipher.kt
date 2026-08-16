package com.cawfee.bluetooth.encryption

/**
 * The Jura BlueFrog "encryption" — actually a key-seeded 4-bit nibble-substitution
 * obfuscation over two fixed 16-entry S-boxes (§6 of the protocol spec).
 *
 * Properties:
 *  - **Involutive for a fixed key**: the same function both encodes and decodes.
 *  - The key is a single byte broadcast in cleartext in the BLE advertisement.
 *  - On a correctly decoded payload, byte 0 always equals the key — used as the
 *    validity check and as the brute-force oracle when the advert key is unknown.
 *
 * This is a faithful port of AlexxIT/Jura `encryption.py`, cross-confirmed against
 * Jutta-Proto `ByteEncDecoder.cpp`. It is verified by [JuraCipherTest] against the
 * spec's executed test vectors (e.g. `encdec(2A 7F 80, 0x2A) = 77 65 6D`).
 */
object JuraCipher {

    // Fixed S-boxes — byte-identical across all known implementations (§6.2).
    private val NUMB1 = intArrayOf(14, 4, 3, 2, 1, 13, 8, 11, 6, 15, 12, 7, 10, 5, 0, 9)
    private val NUMB2 = intArrayOf(10, 6, 13, 12, 14, 11, 1, 9, 15, 7, 0, 5, 3, 2, 4, 8)

    /**
     * Per-nibble shuffle. [cnt] is a running nibble counter (NOT reset per byte), which
     * mixes positional dependence into the substitution. `Int.mod` is used throughout
     * because Kotlin's `%` can yield negative results, whereas the reference Python `%`
     * always returns a non-negative value. Note `(x mod 256) mod 16 == x mod 16`.
     */
    private fun shuffle(src: Int, cnt: Int, key1: Int, key2: Int): Int {
        val i1 = (cnt shr 4).mod(256)
        val i2 = NUMB1[(src + cnt + key1).mod(16)]
        val i3 = NUMB2[(i2 + key2 + i1 - cnt - key1).mod(16)]
        val i4 = NUMB1[(i3 + key1 + cnt - key2 - i1).mod(16)]
        return (i4 - cnt - key1).mod(16)
    }

    /**
     * Symmetric encode/decode of [src] under the single-byte [key]. The transform is
     * involutive, so `encDec(encDec(x, key), key) == x`.
     */
    fun encDec(src: ByteArray, key: Int): ByteArray {
        val key1 = (key and 0xFF) shr 4   // high nibble of key
        val key2 = key and 0x0F           // low nibble of key
        var cnt = 0
        val out = ByteArray(src.size)
        for (i in src.indices) {
            val b = src[i].toInt() and 0xFF
            val hi = b shr 4
            val lo = b and 0x0F
            val dHi = shuffle(hi, cnt, key1, key2); cnt++
            val dLo = shuffle(lo, cnt, key1, key2); cnt++
            out[i] = ((dHi shl 4) or dLo).toByte()
        }
        return out
    }

    /**
     * Encode a command for transmission: byte 0 is overwritten with the [key] BEFORE
     * encoding (the protocol framing requirement), then the buffer is run through
     * [encDec]. This is the wrapper used for every obfuscated *write* characteristic
     * except the Barista lock (see [encDecRaw]).
     */
    fun encrypt(data: ByteArray, key: Int): ByteArray {
        val copy = data.copyOf()
        if (copy.isNotEmpty()) copy[0] = key.toByte()
        return encDec(copy, key)
    }

    /**
     * Decode a payload received from the machine. On success `result[0] == key`.
     */
    fun decrypt(data: ByteArray, key: Int): ByteArray = encDec(data, key)

    /**
     * Encode/decode WITHOUT the byte-0=key step. Used only for the Barista lock
     * characteristic (`5a401530`), which is the one documented exception (§6.4, §8.3).
     */
    fun encDecRaw(data: ByteArray, key: Int): ByteArray = encDec(data, key)

    /**
     * True when [ciphertext] decodes validly under [key] (decoded byte 0 == key).
     */
    fun isValid(ciphertext: ByteArray, key: Int): Boolean =
        ciphertext.isNotEmpty() && (encDec(ciphertext, key)[0].toInt() and 0xFF) == (key and 0xFF)

    /**
     * Recover the key from a captured [ciphertext] by trying all 256 candidates and
     * returning the one for which decoded byte 0 == key. Returns null if none match.
     */
    fun bruteForceKey(ciphertext: ByteArray): Int? {
        if (ciphertext.isEmpty()) return null
        for (k in 0..255) {
            if ((encDec(ciphertext, k)[0].toInt() and 0xFF) == k) return k
        }
        return null
    }
}
