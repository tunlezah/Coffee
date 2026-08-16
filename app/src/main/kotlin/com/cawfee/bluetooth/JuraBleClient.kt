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
import com.cawfee.bluetooth.parser.ProgressParser
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
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The application-facing Bluetooth API. Orchestrates scanning, connection, the ≤9 s
 * heartbeat, command transmission, response parsing and reconnection (Objective 3).
 * All wire-level protocol details are delegated to the platform-independent `:protocol`
 * module, so this same strategy mirrors the macOS implementation.
 *
 * Threading: fields are written from [scope] coroutines and read from the Binder thread
 * running the GATT callbacks (via [handleDrop]), hence `@Volatile` throughout and an
 * [AtomicBoolean] guarding reconnection re-entrancy.
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

    @Volatile private var connection: JuraGattConnection? = null
    @Volatile private var heartbeatJob: Job? = null
    @Volatile private var connectJob: Job? = null
    @Volatile private var progressJob: Job? = null
    @Volatile private var device: DiscoveredJura? = null
    @Volatile private var key: Int = 0x2A
    @Volatile private var model: MachineModel = JuraMachineCatalog.E8
    private val reconnecting = AtomicBoolean(false)

    val isBluetoothEnabled: Boolean get() = scanner.isBluetoothEnabled

    /** Active scan for nearby Jura machines. Surfaces [ConnectionState.Scanning]. */
    fun scan(): Flow<DiscoveredJura> = scanner.scan()
        .onStart {
            val s = _connectionState.value
            if (s is ConnectionState.Idle || s is ConnectionState.Disconnected || s is ConnectionState.Failed) {
                _connectionState.value = ConnectionState.Scanning
            }
        }
        .onCompletion {
            if (_connectionState.value is ConnectionState.Scanning) {
                _connectionState.value = ConnectionState.Idle
            }
        }

    /** Connect to [target]: GATT connect → discover → MTU → notifications → heartbeat. */
    @SuppressLint("MissingPermission")
    fun connect(target: DiscoveredJura) {
        // A second Connect tap (or a connect racing a reconnect) must not leak the
        // previous BluetoothGatt client — Android caps them per app.
        connectJob?.cancel()
        heartbeatJob?.cancel()
        cleanup()
        connectJob = scope.launch {
            device = target
            key = target.advertisement.key
            model = JuraMachineCatalog.forModelId(target.advertisement.modelId) ?: JuraMachineCatalog.E8
            _connectionState.value = ConnectionState.Connecting(target)
            try {
                establish(target)
                _machine.value = MachineSnapshot(device = target)
                onConnected(target)
            } catch (t: Throwable) {
                cleanup()
                _connectionState.value = ConnectionState.Failed(t.message ?: "Connection failed")
            }
        }
    }

    /** Shared post-[establish] wiring for both first connects and reconnects. */
    private suspend fun onConnected(target: DiscoveredJura) {
        _connectionState.value = ConnectionState.Connected(target)
        // Keep the heartbeat alive under Doze/backgrounding via the foreground service.
        runCatching { JuraConnectionService.start(context) }
        startHeartbeat()
        startProgressCollector()
        refreshStatus()
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

    /** Decode brew-progress notifications into [machine] as they arrive. */
    private fun startProgressCollector() {
        val conn = connection ?: return
        val progressUuid = UUID.fromString(JuraGatt.CHAR_PRODUCT_PROGRESS)
        progressJob?.cancel()
        progressJob = scope.launch {
            conn.notifications.collect { (uuid, value) ->
                if (uuid == progressUuid) {
                    runCatching { ProgressParser.parse(value, key) }.onSuccess { progress ->
                        _machine.value = _machine.value.copy(
                            progress = progress,
                            lastUpdatedMillis = System.currentTimeMillis(),
                        )
                    }
                }
            }
        }
    }

    /** Read + decode machine status and update [machine]. */
    suspend fun refreshStatus(): Result<Unit> {
        val conn = connection ?: return Result.failure(IllegalStateException("Not connected"))
        return runCatching {
            val raw = conn.read(JuraGatt.SERVICE_CONTROL, JuraGatt.CHAR_MACHINE_STATUS)
            val status = MachineStatusParser.parse(raw, key, model)
            _machine.value = _machine.value.copy(status = status, lastUpdatedMillis = System.currentTimeMillis())
        }
    }

    /** Brew [product] with [params] if the machine reports ready. */
    suspend fun brew(product: Product, params: BrewParameters = BrewParameters()): Result<Unit> {
        val conn = connection ?: return Result.failure(IllegalStateException("Not connected"))
        if (_machine.value.status?.isReadyToBrew == false) {
            return Result.failure(IllegalStateException("Machine is not ready to brew"))
        }
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
        connectJob?.cancel()
        heartbeatJob?.cancel()
        device = null
        cleanup()
        runCatching { JuraConnectionService.stop(context) }
        _connectionState.value = ConnectionState.Disconnected()
    }

    private fun handleDrop(reason: String) {
        // Heartbeat failure and the GATT-callback path can both land here; only one
        // reconnect attempt may run at a time.
        if (!reconnecting.compareAndSet(false, true)) return
        val target = device
        heartbeatJob?.cancel()
        cleanup()
        if (target == null) {
            runCatching { JuraConnectionService.stop(context) }
            _connectionState.value = ConnectionState.Disconnected(reason)
            reconnecting.set(false)
            return
        }
        // App-layer reconnection (§7.4).
        scope.launch {
            try {
                _connectionState.value = ConnectionState.Reconnecting(target, 1)
                try {
                    establish(target)
                    onConnected(target)
                } catch (t: Throwable) {
                    cleanup()
                    runCatching { JuraConnectionService.stop(context) }
                    _connectionState.value = ConnectionState.Disconnected(t.message)
                }
            } finally {
                reconnecting.set(false)
            }
        }
    }

    private fun cleanup() {
        progressJob?.cancel()
        progressJob = null
        connection?.let {
            // Detach the drop handler first so an intentional close is never
            // misread as an unexpected disconnect (which would trigger a reconnect).
            it.onUnexpectedDisconnect = null
            it.close()
        }
        connection = null
    }
}
