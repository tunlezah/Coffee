package com.cawfee.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cawfee.domain.model.AustralianStylePreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatioConverterScreen() {
    var doseG by remember { mutableFloatStateOf(18f) }
    var ratio by remember { mutableFloatStateOf(2.0f) }
    val yieldG = doseG * ratio

    Scaffold(topBar = { TopAppBar(title = { Text("Ratio Converter") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "%.1f g in → %.1f g out".format(doseG, yieldG),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text("1 : %.1f ratio".format(ratio))
            Text("Dose: %.1f g".format(doseG))
            Slider(value = doseG, onValueChange = { doseG = it }, valueRange = 5f..30f)
            Text("Ratio: 1 : %.1f".format(ratio))
            Slider(value = ratio, onValueChange = { ratio = it }, valueRange = 1f..4f)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylePresetsScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Style Presets") }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(AustralianStylePreset.all, key = { it.name }) { preset ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(preset.name, fontWeight = FontWeight.SemiBold)
                        Text("1:%.1f • %d ml drink • %d ml milk".format(preset.ratio, preset.beverageML, preset.milkML))
                        Text(preset.blurb)
                    }
                }
            }
        }
    }
}
