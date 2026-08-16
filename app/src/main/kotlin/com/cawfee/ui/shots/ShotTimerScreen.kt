package com.cawfee.ui.shots

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShotTimerScreen(viewModel: ShotTimerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Shot Timer") }) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        ) {
            Text(
                ShotTimerViewModel.format(state.elapsedMs),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.SemiBold,
            )
            state.preInfusionMs?.let {
                Text("Pre-infusion at ${ShotTimerViewModel.format(it)}s")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { viewModel.startOrStop() }) {
                    Text(if (state.isRunning) "Stop" else "Start")
                }
                OutlinedButton(onClick = { viewModel.markPreInfusion() }, enabled = state.isRunning) {
                    Text("Pre-infusion")
                }
                OutlinedButton(onClick = { viewModel.reset() }) { Text("Reset") }
            }
        }
    }
}
