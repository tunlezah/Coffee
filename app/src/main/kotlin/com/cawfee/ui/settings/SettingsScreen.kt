package com.cawfee.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cawfee.data.PreferencesRepository
import com.cawfee.domain.model.AppearancePreference
import com.cawfee.domain.model.DrinkType
import com.cawfee.domain.model.UserMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle(PreferencesRepository.UserPrefs())

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingSection("Mode") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    UserMode.entries.forEachIndexed { i, mode ->
                        SegmentedButton(
                            selected = prefs.userMode == mode,
                            onClick = { viewModel.setUserMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(i, UserMode.entries.size),
                        ) { Text(mode.displayName) }
                    }
                }
            }
            SettingSection("Appearance") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    AppearancePreference.entries.forEachIndexed { i, a ->
                        SegmentedButton(
                            selected = prefs.appearance == a,
                            onClick = { viewModel.setAppearance(a) },
                            shape = SegmentedButtonDefaults.itemShape(i, AppearancePreference.entries.size),
                        ) { Text(a.displayName) }
                    }
                }
            }
            SettingSection("Default drink") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DrinkType.entries.forEach { drink ->
                        FilterChip(
                            selected = prefs.defaultDrink == drink,
                            onClick = { viewModel.setDefaultDrink(drink) },
                            label = { Text(drink.displayName) },
                        )
                    }
                }
            }
            SettingSection("About") {
                Text("${prefs.machineName} • Cawfee for Android")
                Text("Works fully offline. Bluetooth control is local-only.")
            }
        }
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}
