package com.cawfee.bluetooth.models

/**
 * Parsed contents of the Jura BlueFrog BLE advertisement manufacturer data (§4.2).
 *
 * The single most important fields are [key] (the obfuscation key, broadcast in
 * cleartext) and [modelId] (the machine type, used to select the model definition).
 */
data class JuraAdvertisement(
    /** Obfuscation key for the nibble-shuffle codec (advert byte 0). Often 0x2A. */
    val key: Int,
    val blueFrogMajorVersion: Int,
    val blueFrogMinorVersion: Int,
    /** Article number / model id (advert bytes 4–5, LE16). E8 = 15057. 0 ⇒ invalid. */
    val modelId: Int,
    val machineNumber: Int,
    val serialNumber: Int,
    /** Feature flags (advert byte 15): bit4 incasso, bit6 master-PIN present, bit7 reset. */
    val statusBits: Int,
) {
    val isValid: Boolean get() = modelId != 0
    val hasMasterPin: Boolean get() = (statusBits and 0x40) != 0
    val isE8: Boolean get() = modelId == 15057
}
