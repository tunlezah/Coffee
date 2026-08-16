package com.cawfee.ui.dashboard

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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.cawfee.navigation.Routes

private data class Shortcut(val title: String, val subtitle: String, val route: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavHostController) {
    val shortcuts = listOf(
        Shortcut("Ratio Converter", "Dial dose / yield / ratio", Routes.RATIO),
        Shortcut("Style Presets", "Australian cafe benchmarks", Routes.STYLES),
        Shortcut("Expert Mode", "Full rule reasoning", Routes.EXPERT),
        Shortcut("Beans", "Your bean library", Routes.BEANS),
        Shortcut("Recipes", "Saved dial-ins", Routes.RECIPES),
        Shortcut("Tasting Log", "Sensory notes", Routes.TASTING),
        Shortcut("History", "Past adjustments", Routes.HISTORY),
        Shortcut("Water", "Mineral profiles", Routes.WATER),
        Shortcut("Maintenance", "Cleaning & descale", Routes.MAINTENANCE),
    )
    Scaffold(topBar = { TopAppBar(title = { Text("Dialed In") }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Welcome back", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
            }
            items(shortcuts, key = { it.route }) { s ->
                Card(
                    onClick = { navController.navigate(s.route) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(s.title, fontWeight = FontWeight.SemiBold)
                        Text(s.subtitle)
                    }
                }
            }
        }
    }
}
