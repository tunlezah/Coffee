package com.cawfee.bluetooth.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.cawfee.bluetooth.DiscoveredJura
import com.cawfee.bluetooth.parser.AdvertisementParser
import com.cawfee.bluetooth.protocol.JuraGatt
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Active BLE scanner for Jura Smart Connect dongles (§4). Filters on the company id
 * 0x00AB manufacturer data and parses the advertisement to recover the obfuscation key
 * and model id before any connection. Active scanning is required — passive scans miss
 * the manufacturer data.
 */
@Singleton
class JuraScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val adapter get() =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    val isBluetoothEnabled: Boolean get() = adapter?.isEnabled == true

    /** True when the runtime permission needed to scan is currently granted. */
    val hasScanPermission: Boolean
        get() {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Manifest.permission.BLUETOOTH_SCAN
            } else {
                Manifest.permission.ACCESS_FINE_LOCATION
            }
            return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

    @SuppressLint("MissingPermission") // checked via hasScanPermission below
    fun scan(): Flow<DiscoveredJura> = callbackFlow {
        // Permissions can be revoked at any time from system settings, so the UI-layer
        // check is not enough — fail the flow instead of throwing SecurityException.
        if (!hasScanPermission) {
            close(SecurityException("Bluetooth scan permission not granted"))
            return@callbackFlow
        }
        val scanner = adapter?.bluetoothLeScanner
            ?: run { close(IllegalStateException("Bluetooth is off or unavailable")); return@callbackFlow }

        // Match Jura by manufacturer-data company id OR by advertised service UUID.
        val filters = listOf(
            ScanFilter.Builder()
                .setManufacturerData(JuraGatt.COMPANY_ID, ByteArray(0))
                .build(),
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString(JuraGatt.SERVICE_CONTROL))
                .build(),
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // active scan
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val mfg = result.scanRecord?.getManufacturerSpecificData(JuraGatt.COMPANY_ID) ?: return
                val advertisement = AdvertisementParser.parse(mfg) ?: return
                if (!advertisement.isValid) return
                trySend(
                    DiscoveredJura(
                        address = result.device.address,
                        name = result.scanRecord?.deviceName ?: runCatching { result.device.name }.getOrNull(),
                        rssi = result.rssi,
                        advertisement = advertisement,
                    )
                )
            }

            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException("BLE scan failed: code $errorCode"))
            }
        }

        scanner.startScan(filters, settings, callback)
        awaitClose { runCatching { scanner.stopScan(callback) } }
    }
}
