package com.cawfee.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import com.cawfee.bluetooth.commands.BrewParameters
import com.cawfee.bluetooth.commands.JuraCommands
import com.cawfee.bluetooth.connection.JuraGattConnection
import com.cawfee.bluetooth.models.MachineModel
import com.cawfee.bluetooth.models.Product
import com.cawfee.bluetooth.parser.MachineStatusParser
import com.cawfee.bluetooth.parser.StatisticsParser
import com.cawfee.bluetooth.protocol.JuraGatt
import com.cawfee.bluetooth.protocol.JuraMachineCatalog
import com.cawfee.bluetooth.scanner.JuraScanner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The application-facing Bluetooth API. Orchestrates scanning, connection, the ≤9 s
 * heartbeat, command transmission, response parsing and reconnection (Objective 3).
 * All wire-level protocol details are delegated to the platform-independent `:protocol`
 * module, so this same strategy mirrors the macOS implementation.
 */
@Singleton
class JuraBleClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanner: JuraScanner,
    private val scope: CoroutineScope,
) {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _machine = MutableStateFlow(MachineSnapshot())
    val machine: StateFlow<MachineSnapshot> = _machine.asStateFlow()

    private var connection: JuraGattConnection? = null
    private var heartbeatJob: Job? = null
    private var device: DiscoveredJura? = null
    private var key: Int = 0x2A
    private var model: MachineModel = JuraMachineCatalog.E8

    val isBluetoothEnabled: Boolean get() = scanner.isBluetoothEnabled

    /** Active scan for nearby Jura machines. */
    fun scan(): Flow<DiscoveredJura> = scanner.scan()

    /** Connect to [target]: GATT connect → discover → MTU → notifications → heartbeat. */
    @SuppressLint("MissingPermission")
    fun connect(target: DiscoveredJura) {
        scope.launch {
            device = target
            key = target.advertisement.key
            model = JuraMachineCatalog.forModelId(target.advertisement.modelId) ?: JuraMachineCatalog.E8
            _connectionState.value = ConnectionState.Connecting(target)
            try {
                establish(target)
                _connectionState.value = ConnectionState.Connected(target)
                _machine.value = MachineSnapshot(device = target)
                startHeartbeat()
                refreshStatus()
            } catch (t: Throwable) {
                _connectionState.value = ConnectionState.Failed(t.message ?: "Connection failed")
                cleanup()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun establish(target: DiscoveredJura) {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
            ?: error("Bluetooth unavailable")
        val btDevice = adapter.getRemoteDevice(target.address)
        val conn = JuraGattConnection(context, btDevice)
        conn.onUnexpectedDisconnect = { reason -> handleDrop(reason) }
        connection = conn

        // Retry connect with backoff — first attempt is often flaky (§7.4).
        var lastError: Throwable? = null
        repeat(JuraGatt.Timing.CONNECT_MAX_RETRIES) { attempt ->
            try {
                conn.connect()
                conn.discoverServices()
                conn.requestMtu(247)
                // Subscribe to brew progress notifications.
                runCatching { conn.enableNotifications(JuraGatt.SERVICE_CONTROL, JuraGatt.CHAR_PRODUCT_PROGRESS) }
                return
            } catch (t: Throwable) {
                lastError = t
                conn.close()
                delay(JuraGatt.Timing.CONNECT_RETRY_DELAY_MS * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("Could not establish connection")
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (true) {
                delay(JuraGatt.Timing.HEARTBEAT_INTERVAL_MS)
                val conn = connection ?: break
                runCatching {
                    conn.write(JuraGatt.SERVICE_CONTROL, JuraGatt.CHAR_PMODE, JuraCommands.heartbeat(key))
                }.onFailure { handleDrop(it.message ?: "heartbeat failed") }
            }
        }
    }

    /** Read + decode machine status and update [machine]. */
    suspend fun refreshStatus() {
        val conn = connection ?: return
        runCatching {
            val raw = conn.read(JuraGatt.SERVICE_CONTROL, JuraGatt.CHAR_MACHINE_STATUS)
            val status = MachineStatusParser.parse(raw, key, model)
            _machine.value = _machine.value.copy(status = status, lastUpdatedMillis = System.currentTimeMillis())
        }
    }

    /** Brew [product] with [params] if the machine reports ready. */
    suspend fun brew(product: Product, params: BrewParameters = BrewParameters()): Result<Unit> {
        val conn = connection ?: return Result.failure(IllegalStateException("Not connected"))
        return runCatching {
            val payload = JuraCommands.startProduct(product, params, key)
            conn.write(JuraGatt.SERVICE_CONTROL, JuraGatt.CHAR_START_PRODUCT, payload)
        }
    }

    suspend fun setBaristaLock(locked: Boolean): Result<Unit> {
        val conn = connection ?: return Result.failure(IllegalStateException("Not connected"))
        return runCatching {
            conn.write(JuraGatt.SERVICE_CONTROL, JuraGatt.CHAR_BARISTA, JuraCommands.baristaLock(locked, key))
            _machine.value = _machine.value.copy(baristaLocked = locked)
        }
    }

    /** Statistics: write the request, poll until ready, then read + decode (§8.4). */
    suspend fun refreshStatistics(daily: Boolean = false): Result<Unit> {
        val conn = connection ?: return Result.failure(IllegalStateException("Not connected"))
        return runCatching {
            conn.write(JuraGatt.SERVICE_CONTROL, JuraGatt.CHAR_STATISTICS_CMD, JuraCommands.statisticsRequest(daily, key))
            delay(JuraGatt.Timing.STATS_INITIAL_WAIT_MS)
            repeat(JuraGatt.Timing.STATS_MAX_POLLS) {
                val probe = conn.read(JuraGatt.SERVICE_CONTROL, JuraGatt.CHAR_STATISTICS_CMD)
                if (StatisticsParser.isReady(com.cawfee.bluetooth.encryption.JuraCipher.decrypt(probe, key))) {
                    val data = conn.read(JuraGatt.SERVICE_CONTROL, JuraGatt.CHAR_STATISTICS_DATA)
                    _machine.value = _machine.value.copy(statistics = StatisticsParser.parse(data, key))
                    return@runCatching
                }
                delay(JuraGatt.Timing.STATS_POLL_INTERVAL_MS)
            }
            error("Statistics engine stayed busy")
        }
    }

    val currentModel: MachineModel get() = model

    fun disconnect() {
        heartbeatJob?.cancel()
        cleanup()
        _connectionState.value = ConnectionState.Disconnected()
    }

    private fun handleDrop(reason: String) {
        val target = device
        heartbeatJob?.cancel()
        cleanup()
        if (target == null) {
            _connectionState.value = ConnectionState.Disconnected(reason)
            return
        }
        // App-layer reconnection (§7.4).
        scope.launch {
            _connectionState.value = ConnectionState.Reconnecting(target, 1)
            try {
                establish(target)
                _connectionState.value = ConnectionState.Connected(target)
                startHeartbeat()
                refreshStatus()
            } catch (t: Throwable) {
                _connectionState.value = ConnectionState.Disconnected(t.message)
            }
        }
    }

    private fun cleanup() {
        connection?.close()
        connection = null
    }
}
