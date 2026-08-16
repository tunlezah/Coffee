package com.cawfee.ui.machine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cawfee.bluetooth.ConnectionState
import com.cawfee.bluetooth.DiscoveredJura
import com.cawfee.bluetooth.commands.BrewParameters
import com.cawfee.bluetooth.models.Product
import com.cawfee.bluetooth.models.Temperature
import com.cawfee.repository.MachineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MachineViewModel @Inject constructor(
    private val repo: MachineRepository,
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = repo.connectionState
    val machine = repo.machine

    private val _devices = MutableStateFlow<List<DiscoveredJura>>(emptyList())
    val devices: StateFlow<List<DiscoveredJura>> = _devices.asStateFlow()

    val isBluetoothEnabled: Boolean get() = repo.isBluetoothEnabled
    val products: List<Product> get() = repo.model.products

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    private var scanJob: Job? = null

    fun startScan() {
        scanJob?.cancel()
        _devices.value = emptyList()
        _scanError.value = null
        scanJob = viewModelScope.launch {
            // Scan failures (permission revoked, throttling, adapter off) must surface
            // as UI state — an uncaught exception here would crash the process.
            repo.scan()
                .catch { _scanError.value = it.message ?: "Scan failed" }
                .collect { found ->
                    _devices.update { current ->
                        (current.filterNot { it.address == found.address } + found)
                            .sortedByDescending { it.rssi }
                    }
                }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
    }

    fun connect(device: DiscoveredJura) {
        stopScan()
        repo.connect(device)
    }

    fun disconnect() = repo.disconnect()

    fun refreshStatus() = viewModelScope.launch { repo.refreshStatus() }

    fun refreshStatistics() = viewModelScope.launch { repo.refreshStatistics() }

    fun setBaristaLock(locked: Boolean) = viewModelScope.launch { repo.setBaristaLock(locked) }

    fun brew(product: Product, strength: Int?, waterMl: Int?, temperature: Temperature?) =
        viewModelScope.launch {
            repo.brew(product, BrewParameters(strength = strength, waterMl = waterMl, temperature = temperature))
        }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
}
