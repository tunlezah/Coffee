package com.cawfee.bluetooth

import com.cawfee.bluetooth.encryption.Hex
import com.cawfee.bluetooth.parser.AdvertisementParser
import com.cawfee.bluetooth.parser.MachineStatusParser
import com.cawfee.bluetooth.parser.StatisticsParser
import com.cawfee.bluetooth.protocol.JuraMachineCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ParserTest {

    @Test
    fun `advertisement parser extracts key and E8 model id`() {
        // byte0=key 0x2A, bytes4-5 = 0x3AD1 little-endian (D1 3A) = 15057
        val adv = Hex.decode("2A 01 02 00 D1 3A 00 00 00 00 00 00 00 00 00 40")
        val parsed = assertNotNull(AdvertisementParser.parse(adv))
        assertEquals(0x2A, parsed.key)
        assertEquals(15057, parsed.modelId)
        assertTrue(parsed.isE8)
        assertTrue(parsed.isValid)
        assertTrue(parsed.hasMasterPin) // bit6 of statusBits (0x40)
    }

    @Test
    fun `advertisement with zero model id is invalid`() {
        val adv = Hex.decode("2A 01 02 00 00 00 00 00 00 00 00 00 00 00 00 00")
        val parsed = assertNotNull(AdvertisementParser.parse(adv))
        assertFalse(parsed.isValid)
    }

    @Test
    fun `status parser walks alert bits MSB-first from byte 1`() {
        // byte0 = key echo (ignored). byte1 bit set for "fill water" (bit 1) and
        // "coffee ready" (bit 13 -> byte2 bit5).
        // bit 1 -> byteIndex 1, bitInByte 7-1=6 -> 0b01000000 = 0x40
        // bit 13 -> i=13: byteIndex 2, bitInByte 7-(13&7)=7-5=2 -> 0b00000100 = 0x04
        val decoded = Hex.decode("00 40 04")
        val status = MachineStatusParser.parseDecoded(decoded, JuraMachineCatalog.E8)
        assertTrue(status.needsWater)
        assertTrue(status.coffeeReady)
        assertFalse(status.noBeans)
    }

    @Test
    fun `status with no blocking alerts is ready to brew`() {
        // only coffee-ready (bit 13) set -> informational, not blocking
        val decoded = Hex.decode("00 00 04")
        val status = MachineStatusParser.parseDecoded(decoded, JuraMachineCatalog.E8)
        assertTrue(status.isReadyToBrew)
        assertTrue(status.coffeeReady)
    }

    @Test
    fun `status with fill water is not ready to brew`() {
        // bit 1 (fill water, Type="block" in the machine file) -> byte 1, 0x40
        val decoded = Hex.decode("00 40 00")
        val status = MachineStatusParser.parseDecoded(decoded, JuraMachineCatalog.E8)
        assertTrue(status.needsWater)
        assertFalse(status.isReadyToBrew)
    }

    @Test
    fun `no beans is informational per the EF533 machine file`() {
        // bit 10 is Type="info" in EF533/1.0.xml -> byteIndex 2, bit 0x20
        val decoded = Hex.decode("00 00 20")
        val status = MachineStatusParser.parseDecoded(decoded, JuraMachineCatalog.E8)
        assertTrue(status.noBeans)
        assertTrue(status.isReadyToBrew)
    }

    @Test
    fun `statistics parser reads 3-byte big-endian counters`() {
        // 00014E = 334 (total); 000000 = 0; 000027 = 39; 00FFFF -> 0
        val decoded = Hex.decode("00014E 000000 000027 00FFFF")
        val stats = StatisticsParser.parseDecoded(decoded)
        assertEquals(334L, stats.total)
        assertEquals(0L, stats.counts[1])
        assertEquals(39L, stats.counts[2])
        assertEquals(0L, stats.counts[3]) // 0xFFFF treated as 0
    }

    @Test
    fun `statistics parse decodes counters from byte 0 - no key echo in statistics data`() {
        // Unlike status/progress reads, Statistics Data does NOT begin with the key
        // echo: the Jutta-Proto doc's example response starts `00014E` = total count.
        // encDec is involutive, so encoding the expected decoded buffer produces the
        // raw bytes the machine would send.
        val key = 0x2A
        val decoded = Hex.decode("00014E 000000 000027")
        val raw = com.cawfee.bluetooth.encryption.JuraCipher.encDec(decoded, key)
        val stats = StatisticsParser.parse(raw, key)
        assertEquals(334L, stats.total)
        assertEquals(39L, stats.counts[2])
    }

    @Test
    fun `statistics readiness checks the raw probe byte 1 busy marker`() {
        // Matches AlexxIT/Jura: the RAW (still-encoded) Statistics Command read is
        // busy while byte 1 == 0xE1.
        assertFalse(StatisticsParser.isReady(Hex.decode("00 E1 00")))
        assertFalse(StatisticsParser.isReady(ByteArray(0)))
        assertTrue(StatisticsParser.isReady(Hex.decode("0E 00 00")))
        assertTrue(StatisticsParser.isReady(Hex.decode("00 01 4E")))
    }
}
