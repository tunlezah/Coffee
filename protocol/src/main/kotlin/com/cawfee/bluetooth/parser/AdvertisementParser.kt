package com.cawfee.bluetooth.parser

import com.cawfee.bluetooth.models.JuraAdvertisement

/**
 * Parses the little-endian manufacturer-data block advertised under company id 0x00AB
 * into a [JuraAdvertisement] (§4.2 of the protocol spec).
 */
object AdvertisementParser {

    private fun le16(data: ByteArray, offset: Int): Int =
        (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)

    /**
     * @param data the raw manufacturer-data bytes (already stripped of the company id
     *             prefix, i.e. byte 0 is the key).
     * @return parsed advertisement, or null if the buffer is too short.
     */
    fun parse(data: ByteArray): JuraAdvertisement? {
        if (data.size < 16) return null
        return JuraAdvertisement(
            key = data[0].toInt() and 0xFF,
            blueFrogMajorVersion = data[1].toInt() and 0xFF,
            blueFrogMinorVersion = data[2].toInt() and 0xFF,
            modelId = le16(data, 4),
            machineNumber = le16(data, 6),
            serialNumber = le16(data, 8),
            statusBits = data[15].toInt() and 0xFF,
        )
    }
}
