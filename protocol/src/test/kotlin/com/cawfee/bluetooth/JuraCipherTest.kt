package com.cawfee.bluetooth

import com.cawfee.bluetooth.encryption.Hex
import com.cawfee.bluetooth.encryption.JuraCipher
import com.cawfee.bluetooth.encryption.toHex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Validates [JuraCipher] against the executed test vectors from §6.5 of
 * JURA_E8_BLUETOOTH_SPECIFICATION.md. These are the highest-confidence findings in the
 * spec (byte-identical across three independent codebases).
 */
class JuraCipherTest {

    @Test
    fun `heartbeat vector encodes to 77 65 6d`() {
        // encrypt(00 7F 80, key=0x2A): byte0 -> key (2A 7F 80), then encDec = 77 65 6D
        val out = JuraCipher.encrypt(Hex.decode("00 7F 80"), 0x2A)
        assertEquals("77656d", out.toHex())
    }

    @Test
    fun `encDec is involutive for the brew vector`() {
        val cipher = Hex.decode("77c23dd05e81d3dba32bf898a4a3faab45fd")
        val plain = "2a280006120000010001090000000000062a"
        assertEquals(plain, JuraCipher.encDec(cipher, 0x2A).toHex())
        // round-trip back to ciphertext
        assertEquals(cipher.toHex(), JuraCipher.encDec(Hex.decode(plain), 0x2A).toHex())
    }

    @Test
    fun `cappuccino vector decodes correctly`() {
        val cipher = Hex.decode("77ea3dd38981dadba32bfa98a4a3faab45fd")
        assertEquals("2a0400080c000e010001000000000000062a", JuraCipher.encDec(cipher, 0x2A).toHex())
    }

    @Test
    fun `idle machine status decodes to byte0 equal key for key 0x00`() {
        val cipher = Hex.decode("14444CC623152D9ABFE772ED1B3F65136B888DDC")
        val decoded = JuraCipher.encDec(cipher, 0x00)
        assertEquals(0x00, decoded[0].toInt() and 0xFF)
    }

    @Test
    fun `decoded byte0 always equals key`() {
        for (key in intArrayOf(0x00, 0x2A, 0x7F, 0xAB, 0xFF)) {
            val frame = ByteArray(18) { it.toByte() }
            val enc = JuraCipher.encrypt(frame, key)
            val dec = JuraCipher.decrypt(enc, key)
            assertEquals(key, dec[0].toInt() and 0xFF, "key=$key")
            assertTrue(JuraCipher.isValid(enc, key))
        }
    }

    @Test
    fun `brute force recovers the key`() {
        val enc = JuraCipher.encrypt(Hex.decode("00 7F 80"), 0x2A)
        assertEquals(0x2A, JuraCipher.bruteForceKey(enc))
    }

    @Test
    fun `barista lock raw encode does not force byte0 to key`() {
        // raw path must NOT overwrite byte 0; round-trips cleanly
        val payload = Hex.decode("0001")
        val enc = JuraCipher.encDecRaw(payload, 0x2A)
        assertEquals(payload.toHex(), JuraCipher.encDecRaw(enc, 0x2A).toHex())
    }

    @Test
    fun `brute force returns null for empty input`() {
        assertNull(JuraCipher.bruteForceKey(ByteArray(0)))
    }
}
