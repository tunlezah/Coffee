package com.cawfee.bluetooth.connection

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import com.cawfee.bluetooth.protocol.JuraGatt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.util.UUID

/**
 * A single GATT connection to a Jura dongle.
 *
 * Implements the #1 Android BLE rule (§16.3): **all GATT operations are serialized** —
 * only one outstanding read/write/MTU/discover at a time — via [opMutex] + a
 * [CompletableDeferred] resolved by the [BluetoothGattCallback]. Notifications are
 * surfaced on [notifications]. The class deals only in raw bytes; obfuscation and
 * command building live in the platform-independent `:protocol` module.
 */
class JuraGattConnection(
    private val context: Context,
    private val device: BluetoothDevice,
) {
    /** (characteristic UUID, decoded-on-arrival raw value) pairs for enabled notifications. */
    private val _notifications = MutableSharedFlow<Pair<UUID, ByteArray>>(extraBufferCapacity = 64)
    val notifications: SharedFlow<Pair<UUID, ByteArray>> = _notifications

    private var gatt: BluetoothGatt? = null
    private val opMutex = Mutex()

    private var connectDeferred: CompletableDeferred<Unit>? = null
    private var servicesDeferred: CompletableDeferred<Unit>? = null
    private var mtuDeferred: CompletableDeferred<Int>? = null
    private var readDeferred: CompletableDeferred<ByteArray>? = null
    private var writeDeferred: CompletableDeferred<Unit>? = null
    private var descriptorDeferred: CompletableDeferred<Unit>? = null

    /** Invoked when the peripheral drops the link unexpectedly. */
    var onUnexpectedDisconnect: ((String) -> Unit)? = null

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> connectDeferred?.complete(Unit)
                BluetoothProfile.STATE_DISCONNECTED -> {
                    val cd = connectDeferred
                    if (cd != null && !cd.isCompleted) {
                        cd.completeExceptionally(IllegalStateException("Disconnected (status=$status)"))
                    } else {
                        onUnexpectedDisconnect?.invoke("status=$status")
                    }
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) servicesDeferred?.complete(Unit)
            else servicesDeferred?.completeExceptionally(IllegalStateException("discover failed: $status"))
        }

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            mtuDeferred?.complete(mtu)
        }

        // API 33+ read callback (carries value directly).
        override fun onCharacteristicRead(
            g: BluetoothGatt,
            c: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) readDeferred?.complete(value)
            else readDeferred?.completeExceptionally(IllegalStateException("read failed: $status"))
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return // newer overload used
            if (status == BluetoothGatt.GATT_SUCCESS) readDeferred?.complete(c.value ?: ByteArray(0))
            else readDeferred?.completeExceptionally(IllegalStateException("read failed: $status"))
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) writeDeferred?.complete(Unit)
            else writeDeferred?.completeExceptionally(IllegalStateException("write failed: $status"))
        }

        // API 33+ notification callback.
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            c: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            _notifications.tryEmit(c.uuid to value)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
            _notifications.tryEmit(c.uuid to (c.value ?: ByteArray(0)))
        }

        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) descriptorDeferred?.complete(Unit)
            else descriptorDeferred?.completeExceptionally(IllegalStateException("descriptor write failed: $status"))
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(timeoutMs: Long = 10_000L) = opMutex.withLock {
        connectDeferred = CompletableDeferred()
        // Direct connection (autoConnect=false) is faster (§16.3).
        gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        withTimeout(timeoutMs) { connectDeferred!!.await() }
    }

    @SuppressLint("MissingPermission")
    suspend fun discoverServices(timeoutMs: Long = 10_000L) = opMutex.withLock {
        servicesDeferred = CompletableDeferred()
        require(gatt?.discoverServices() == true) { "discoverServices() returned false" }
        withTimeout(timeoutMs) { servicesDeferred!!.await() }
    }

    @SuppressLint("MissingPermission")
    suspend fun requestMtu(mtu: Int = 247, timeoutMs: Long = 5_000L): Int = opMutex.withLock {
        mtuDeferred = CompletableDeferred()
        if (gatt?.requestMtu(mtu) != true) return@withLock 23
        runCatching { withTimeout(timeoutMs) { mtuDeferred!!.await() } }.getOrDefault(23)
    }

    @SuppressLint("MissingPermission")
    suspend fun read(serviceUuid: String, charUuid: String, timeoutMs: Long = 5_000L): ByteArray =
        opMutex.withLock {
            val c = characteristic(serviceUuid, charUuid)
            readDeferred = CompletableDeferred()
            require(gatt?.readCharacteristic(c) == true) { "readCharacteristic returned false" }
            withTimeout(timeoutMs) { readDeferred!!.await() }
        }

    @SuppressLint("MissingPermission")
    suspend fun write(
        serviceUuid: String,
        charUuid: String,
        value: ByteArray,
        withResponse: Boolean = true,
        timeoutMs: Long = 5_000L,
    ) = opMutex.withLock {
        val c = characteristic(serviceUuid, charUuid)
        val writeType = if (withResponse) BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        else BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        writeDeferred = CompletableDeferred()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val rc = gatt?.writeCharacteristic(c, value, writeType)
            require(rc == BluetoothGatt.GATT_SUCCESS) { "writeCharacteristic returned $rc" }
        } else {
            @Suppress("DEPRECATION")
            run {
                c.writeType = writeType
                c.value = value
                require(gatt?.writeCharacteristic(c) == true) { "writeCharacteristic returned false" }
            }
        }
        withTimeout(timeoutMs) { writeDeferred!!.await() }
    }

    /** Enable notifications and write the CCCD ENABLE value — both are required (§16.3). */
    @SuppressLint("MissingPermission")
    suspend fun enableNotifications(serviceUuid: String, charUuid: String, timeoutMs: Long = 5_000L) =
        opMutex.withLock {
            val c = characteristic(serviceUuid, charUuid)
            require(gatt?.setCharacteristicNotification(c, true) == true) { "setCharacteristicNotification failed" }
            val cccd = c.getDescriptor(UUID.fromString(JuraGatt.CCCD))
                ?: error("CCCD descriptor missing on $charUuid")
            descriptorDeferred = CompletableDeferred()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt?.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                run {
                    cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt?.writeDescriptor(cccd)
                }
            }
            withTimeout(timeoutMs) { descriptorDeferred!!.await() }
        }

    @SuppressLint("MissingPermission")
    fun close() {
        runCatching { gatt?.disconnect() }
        runCatching { gatt?.close() }
        gatt = null
    }

    private fun characteristic(serviceUuid: String, charUuid: String): BluetoothGattCharacteristic {
        val service = gatt?.getService(UUID.fromString(serviceUuid))
            ?: error("Service $serviceUuid not found — unsupported firmware?")
        return service.getCharacteristic(UUID.fromString(charUuid))
            ?: error("Characteristic $charUuid not found")
    }
}
