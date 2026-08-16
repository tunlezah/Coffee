package com.cawfee.bluetooth.parser

import com.cawfee.bluetooth.encryption.JuraCipher
import com.cawfee.bluetooth.models.Alert
import com.cawfee.bluetooth.models.MachineModel
import com.cawfee.bluetooth.models.MachineStatus

/**
 * Decodes the Machine Status characteristic (`5a401524`) into a [MachineStatus] (§9).
 *
 * Byte 0 is the key echo; from byte 1 onward each bit is one alert, walked MSB-first
 * within each byte. The bit→name mapping comes from the per-model [MachineModel].
 */
object MachineStatusParser {

    /** Parse an already-decoded status payload (byte 0 == key). */
    fun parseDecoded(decoded: ByteArray, model: MachineModel): MachineStatus {
        val alerts = mutableListOf<Alert>()
        val totalBits = (decoded.size - 1) * 8
        for (i in 0 until totalBits) {
            val byteIndex = (i shr 3) + 1          // skip byte 0
            val bitInByte = 7 - (i and 0b111)      // MSB first
            if ((decoded[byteIndex].toInt() shr bitInByte) and 1 == 1) {
                val name = model.alertNames[i] ?: "Unknown alert $i"
                alerts.add(Alert(i, name))
            }
        }
        return MachineStatus(alerts)
    }

    /** Decrypt the raw characteristic read with [key], then parse. */
    fun parse(raw: ByteArray, key: Int, model: MachineModel): MachineStatus =
        parseDecoded(JuraCipher.decrypt(raw, key), model)
}
