package com.cawfee.bluetooth

import android.Manifest
import android.os.Build
import com.cawfee.bluetooth.models.JuraAdvertisement
import com.cawfee.bluetooth.models.MachineStatus
import com.cawfee.bluetooth.parser.Statistics

/** A Jura machine discovered during scanning. */
data class DiscoveredJura(
    val address: String,
    val name: String?,
    val rssi: Int,
    val advertisement: JuraAdvertisement,
)

/** High-level connection lifecycle, surfaced to the UI as StateFlow. */
sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Scanning : ConnectionState
    data class Connecting(val device: DiscoveredJura) : ConnectionState
    data class Connected(val device: DiscoveredJura) : ConnectionState
    data class Reconnecting(val device: DiscoveredJura, val attempt: Int) : ConnectionState
    data class Disconnected(val reason: String? = null) : ConnectionState
    data class Failed(val message: String) : ConnectionState
}

/** Aggregated live view of the connected machine. */
data class MachineSnapshot(
    val device: DiscoveredJura? = null,
    val status: MachineStatus? = null,
    val statistics: Statistics? = null,
    val baristaLocked: Boolean = false,
    val lastUpdatedMillis: Long = 0L,
)

/** Bluetooth permission sets that differ across Android versions (§16.3). */
object BluetoothPermissions {
    val required: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        @Suppress("DEPRECATION")
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }
}
