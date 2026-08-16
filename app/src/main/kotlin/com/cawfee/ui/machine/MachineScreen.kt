package com.cawfee.ui.machine

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cawfee.bluetooth.BluetoothPermissions
import com.cawfee.bluetooth.ConnectionState
import com.cawfee.bluetooth.DiscoveredJura
import com.cawfee.bluetooth.MachineSnapshot
import com.cawfee.bluetooth.models.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineScreen(viewModel: MachineViewModel = hiltViewModel()) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val machine by viewModel.machine.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var hasPermissions by remember {
        mutableStateOf(BluetoothPermissions.required.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> hasPermissions = result.values.all { it } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Machine") },
                actions = {
                    if (connectionState is ConnectionState.Connected) {
                        IconButton(onClick = { viewModel.refreshStatus() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh status")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!hasPermissions) {
                PermissionPrompt { permissionLauncher.launch(BluetoothPermissions.required) }
                return@Column
            }
            if (!viewModel.isBluetoothEnabled) {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        "Bluetooth is off. Enable it in system settings to scan for your machine.",
                        Modifier.padding(16.dp),
                    )
                }
            }

            when (val state = connectionState) {
                is ConnectionState.Connected ->
                    ConnectedPanel(machine, viewModel.products, viewModel)

                is ConnectionState.Connecting ->
                    StatusRow("Connecting to ${state.device.name ?: "machine"}…", showSpinner = true)

                is ConnectionState.Reconnecting ->
                    StatusRow("Reconnecting (attempt ${state.attempt})…", showSpinner = true)

                else -> ScanPanel(
                    state = state,
                    devices = devices,
                    onScan = { viewModel.startScan() },
                    onStop = { viewModel.stopScan() },
                    onConnect = { viewModel.connect(it) },
                )
            }
        }
    }
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Bluetooth permission required", fontWeight = FontWeight.SemiBold)
            Text("Cawfee needs Bluetooth access to discover and control your Jura machine. " +
                "We never use it for location.")
            Button(onClick = onRequest) { Text("Grant permission") }
        }
    }
}

@Composable
private fun StatusRow(text: String, showSpinner: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showSpinner) CircularProgressIndicator(Modifier.padding(4.dp))
        Text(text)
    }
}

@Composable
private fun ScanPanel(
    state: ConnectionState,
    devices: List<DiscoveredJura>,
    onScan: () -> Unit,
    onStop: () -> Unit,
    onConnect: (DiscoveredJura) -> Unit,
) {
    val scanning = state is ConnectionState.Scanning || devices.isNotEmpty()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onScan) { Text("Scan for machines") }
        if (scanning) OutlinedButton(onClick = onStop) { Text("Stop") }
    }
    if (state is ConnectionState.Failed) {
        Text("Failed: ${state.message}")
    }
    if (state is ConnectionState.Disconnected && state.reason != null) {
        Text("Disconnected: ${state.reason}")
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(devices, key = { it.address }) { device ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(device.name ?: "Jura machine", fontWeight = FontWeight.SemiBold)
                    Text("Model id ${device.advertisement.modelId} • key 0x%02X • RSSI ${device.rssi} dBm"
                        .format(device.advertisement.key))
                    Button(onClick = { onConnect(device) }) { Text("Connect") }
                }
            }
        }
    }
}

@Composable
private fun ConnectedPanel(
    machine: MachineSnapshot,
    products: List<Product>,
    viewModel: MachineViewModel,
) {
    val status = machine.status
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Connected", fontWeight = FontWeight.SemiBold)
            when {
                status == null -> Text("Reading status…")
                status.isReadyToBrew -> Text("Ready to brew" + if (status.coffeeReady) " · coffee ready" else "")
                else -> Text("Not ready: " + status.alerts.filter { it.isBlocking }.joinToString { it.name })
            }
            if (status != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (status.needsDescale) AssistChip(onClick = {}, label = { Text("Descale") })
                    if (status.needsCleaning) AssistChip(onClick = {}, label = { Text("Clean") })
                    if (status.needsFilter) AssistChip(onClick = {}, label = { Text("Filter") })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Barista lock", Modifier.weight(1f))
                Switch(checked = machine.baristaLocked, onCheckedChange = { viewModel.setBaristaLock(it) })
            }
        }
    }

    Text("Brew", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items(products, key = { it.code }) { product ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(product.name)
                    FilledTonalButton(
                        onClick = { viewModel.brew(product, strength = null, waterMl = null, temperature = null) },
                        enabled = status?.isReadyToBrew != false,
                    ) { Text("Start") }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.refreshStatistics() }) { Text("Statistics") }
                OutlinedButton(onClick = { viewModel.disconnect() }) { Text("Disconnect") }
            }
        }
        machine.statistics?.let { stats ->
            item { Text("Total drinks: ${stats.total}") }
        }
    }
}
