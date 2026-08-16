package com.cawfee.repository

import com.cawfee.bluetooth.ConnectionState
import com.cawfee.bluetooth.DiscoveredJura
import com.cawfee.bluetooth.JuraBleClient
import com.cawfee.bluetooth.MachineSnapshot
import com.cawfee.bluetooth.commands.BrewParameters
import com.cawfee.bluetooth.models.MachineModel
import com.cawfee.bluetooth.models.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository-pattern facade over the BLE client. ViewModels depend on this interface
 * rather than the Android Bluetooth stack directly, which keeps them testable.
 */
@Singleton
class MachineRepository @Inject constructor(
    private val client: JuraBleClient,
) {
    val connectionState: StateFlow<ConnectionState> get() = client.connectionState
    val machine: StateFlow<MachineSnapshot> get() = client.machine
    val model: MachineModel get() = client.currentModel
    val isBluetoothEnabled: Boolean get() = client.isBluetoothEnabled

    fun scan(): Flow<DiscoveredJura> = client.scan()
    fun connect(device: DiscoveredJura) = client.connect(device)
    fun disconnect() = client.disconnect()

    suspend fun refreshStatus() = client.refreshStatus()
    suspend fun refreshStatistics(daily: Boolean = false) = client.refreshStatistics(daily)
    suspend fun brew(product: Product, params: BrewParameters = BrewParameters()) = client.brew(product, params)
    suspend fun setBaristaLock(locked: Boolean) = client.setBaristaLock(locked)
}
