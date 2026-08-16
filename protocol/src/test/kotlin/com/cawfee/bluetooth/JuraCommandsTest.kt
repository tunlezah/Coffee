package com.cawfee.bluetooth

import com.cawfee.bluetooth.commands.BrewParameters
import com.cawfee.bluetooth.commands.JuraCommands
import com.cawfee.bluetooth.encryption.JuraCipher
import com.cawfee.bluetooth.encryption.toHex
import com.cawfee.bluetooth.models.SettingKind
import com.cawfee.bluetooth.models.Temperature
import com.cawfee.bluetooth.protocol.JuraMachineCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JuraCommandsTest {

    private val e8 = JuraMachineCatalog.E8

    @Test
    fun `heartbeat matches spec vector`() {
        assertEquals("77656d", JuraCommands.heartbeat(0x2A).toHex())
    }

    @Test
    fun `start frame places code strength water temp at documented offsets`() {
        val coffee = assertNotNull(e8.product(0x03))
        val frame = JuraCommands.buildStartFrame(
            coffee,
            BrewParameters(strength = 6, waterMl = 100, temperature = Temperature.NORMAL),
            key = 0x2A,
        )
        assertEquals(18, frame.size)
        assertEquals(0x03, frame[1].toInt() and 0xFF)   // product code (byte 1)
        assertEquals(6, frame[3].toInt() and 0xFF)        // strength F3
        assertEquals(20, frame[4].toInt() and 0xFF)       // water 100ml / 5 = 20, F4
        assertEquals(1, frame[7].toInt() and 0xFF)        // temperature NORMAL, F7
        assertEquals(0x2A, frame[17].toInt() and 0xFF)    // key mirror (byte 17)
    }

    @Test
    fun `start product round-trips through the cipher and byte0 becomes key`() {
        val coffee = assertNotNull(e8.product(0x03))
        val enc = JuraCommands.startProduct(coffee, BrewParameters(strength = 5, waterMl = 90), key = 0x2A)
        val dec = JuraCipher.decrypt(enc, 0x2A)
        assertEquals(0x2A, dec[0].toInt() and 0xFF) // byte0 set to key on encode
        assertEquals(0x03, dec[1].toInt() and 0xFF)
        assertEquals(5, dec[3].toInt() and 0xFF)
        assertEquals(18, dec[4].toInt() and 0xFF) // 90/5
    }

    @Test
    fun `defaults are applied when params are null`() {
        val espresso = assertNotNull(e8.product(0x02))
        val frame = JuraCommands.buildStartFrame(espresso, BrewParameters(), key = 0x2A)
        val waterSetting = assertNotNull(espresso.setting(SettingKind.WATER))
        assertEquals(waterSetting.default / 5, frame[4].toInt() and 0xFF) // default 45ml -> 9
        assertEquals(4, frame[3].toInt() and 0xFF) // default strength 4
    }

    @Test
    fun `water amount is clamped to model range`() {
        val coffee = assertNotNull(e8.product(0x03))
        val frame = JuraCommands.buildStartFrame(coffee, BrewParameters(waterMl = 9999), key = 0x2A)
        assertEquals(240 / 5, frame[4].toInt() and 0xFF) // clamped to max 240ml
    }

    @Test
    fun `cappuccino sets milk seconds verbatim at offset 5`() {
        val capp = assertNotNull(e8.product(0x04))
        assertTrue(capp.isMilkBased)
        // Milk amounts are seconds sent verbatim (EF533 XML @Step=1).
        val frame = JuraCommands.buildStartFrame(capp, BrewParameters(milkSeconds = 50), key = 0x2A)
        assertEquals(50, frame[5].toInt() and 0xFF)
    }

    @Test
    fun `latte macchiato applies XML defaults including milk break`() {
        val latte = assertNotNull(e8.product(0x07))
        val frame = JuraCommands.buildStartFrame(latte, BrewParameters(), key = 0x2A)
        assertEquals(4, frame[3].toInt() and 0xFF)   // strength default 4, F3
        assertEquals(9, frame[4].toInt() and 0xFF)   // water default 45ml / 5, F4
        assertEquals(22, frame[5].toInt() and 0xFF)  // milk default 22s, F5
        assertEquals(2, frame[7].toInt() and 0xFF)   // temperature default High, F7
        assertEquals(30, frame[11].toInt() and 0xFF) // milk break default 30s, F11
    }

    @Test
    fun `barista lock does not set byte0 to key`() {
        val lock = JuraCommands.baristaLock(true, 0x2A)
        // raw round-trip recovers 00 01 (byte0 stays 0, not key)
        val back = JuraCipher.encDecRaw(lock, 0x2A)
        assertEquals("0001", back.toHex())
    }

    @Test
    fun `statistics request selects overall vs daily`() {
        val overall = JuraCipher.decrypt(JuraCommands.statisticsRequest(daily = false, key = 0x2A), 0x2A)
        assertEquals(0x01, overall[2].toInt() and 0xFF)
        assertEquals(0xFF, overall[3].toInt() and 0xFF)
        val daily = JuraCipher.decrypt(JuraCommands.statisticsRequest(daily = true, key = 0x2A), 0x2A)
        assertEquals(0x10, daily[2].toInt() and 0xFF)
    }
}
