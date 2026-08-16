package com.cawfee.bluetooth.commands

import com.cawfee.bluetooth.encryption.JuraCipher
import com.cawfee.bluetooth.models.Product
import com.cawfee.bluetooth.models.SettingKind
import com.cawfee.bluetooth.models.Temperature

/**
 * User-chosen brew parameters. Any null value falls back to the product's default.
 * Units are natural (millilitres for water/milk, 1–8 for strength).
 */
data class BrewParameters(
    val strength: Int? = null,
    val waterMl: Int? = null,
    val temperature: Temperature? = null,
    val milkMl: Int? = null,
    val milkBreak: Int? = null,
)

/**
 * Builds the obfuscated payloads written to each Jura characteristic (§8). Every method
 * returns bytes ready to be written to GATT. Pure and deterministic — fully unit-tested.
 */
object JuraCommands {

    /** Length of a Start Product frame (§8.1). */
    const val START_FRAME_SIZE = 18

    /**
     * Build the 18-byte Start Product plaintext frame for [product] with [params]
     * (§8.1). Byte 1 = product code; settings are placed at their model-defined offsets
     * (value ÷ step); byte 17 mirrors the key. Byte 0 is left as the key placeholder and
     * is overwritten by [JuraCipher.encrypt]. Framing bytes (0/9/16) must be present, so
     * the full 18-byte buffer is always produced.
     */
    fun buildStartFrame(product: Product, params: BrewParameters, key: Int): ByteArray {
        val data = ByteArray(START_FRAME_SIZE)
        data[1] = product.code.toByte()
        for (setting in product.settings) {
            val natural: Int = when (setting.kind) {
                SettingKind.STRENGTH -> params.strength ?: setting.default
                SettingKind.WATER -> params.waterMl ?: setting.default
                SettingKind.TEMPERATURE -> params.temperature?.raw ?: setting.default
                SettingKind.MILK -> params.milkMl ?: setting.default
                SettingKind.MILK_BREAK -> params.milkBreak ?: setting.default
            }
            data[setting.argument] = setting.toByte(natural).toByte()
        }
        data[17] = key.toByte()
        return data
    }

    /** Start a product: build the frame and encrypt it for [com.cawfee.bluetooth.protocol.JuraGatt.CHAR_START_PRODUCT]. */
    fun startProduct(product: Product, params: BrewParameters, key: Int): ByteArray =
        JuraCipher.encrypt(buildStartFrame(product, params, key), key)

    /** Heartbeat payload `00 7F 80` for the P-Mode characteristic (§8.2). Send ≤9 s. */
    fun heartbeat(key: Int): ByteArray =
        JuraCipher.encrypt(byteArrayOf(0x00, 0x7F, 0x80.toByte()), key)

    /**
     * Barista lock (`0x0001`) / unlock (`0x0000`) for `5a401530`. NOTE: written WITHOUT
     * the byte-0=key step (§8.3), so the raw codec path is used.
     */
    fun baristaLock(locked: Boolean, key: Int): ByteArray =
        JuraCipher.encDecRaw(if (locked) byteArrayOf(0x00, 0x01) else byteArrayOf(0x00, 0x00), key)

    /**
     * Statistics request for `5a401533` (§8.4): `<key> 00 <01|10> FF FF`. Byte 0 is set
     * to the key on encode; bytes 1–2 select overall (`00 01`) vs daily (`00 10`); bytes
     * 3–4 request all products.
     */
    fun statisticsRequest(daily: Boolean = false, key: Int): ByteArray {
        val frame = byteArrayOf(0x00, 0x00, if (daily) 0x10 else 0x01, 0xFF.toByte(), 0xFF.toByte())
        return JuraCipher.encrypt(frame, key)
    }
}
