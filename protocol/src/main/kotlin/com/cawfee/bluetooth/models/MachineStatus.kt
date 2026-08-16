package com.cawfee.bluetooth.models

/** An active machine alert (a single set bit in the status bitfield, §9). */
data class Alert(val bit: Int, val name: String) {
    /** Alerts that block brewing (hardware interlocks reported via status bits). */
    val isBlocking: Boolean get() = bit in BLOCKING_BITS

    companion object {
        // Coffee-ready (13) and the maintenance reminders (32–34) are informational,
        // not blocking. The rest of the documented E8 bits prevent a clean brew.
        val BLOCKING_BITS = setOf(0, 1, 2, 3, 4, 5, 6, 7, 10)
    }
}

/**
 * Decoded machine status (§9). Carries the raw active alerts plus convenient flags.
 */
data class MachineStatus(
    val alerts: List<Alert>,
) {
    private fun has(bit: Int) = alerts.any { it.bit == bit }

    val needsWater: Boolean get() = has(1)
    val needsEmptyGrounds: Boolean get() = has(2)
    val needsEmptyTray: Boolean get() = has(3)
    val trayMissing: Boolean get() = has(0)
    val outletMissing: Boolean get() = has(5)
    val noBeans: Boolean get() = has(10)
    val milkAlert: Boolean get() = has(7)
    val coffeeReady: Boolean get() = has(13)
    val needsDescale: Boolean get() = has(33)
    val needsCleaning: Boolean get() = has(34)
    val needsFilter: Boolean get() = has(32)

    /** True when there are no blocking alerts (machine physically ready to brew). */
    val isReadyToBrew: Boolean get() = alerts.none { it.isBlocking }
}
