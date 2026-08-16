package com.cawfee.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector

/** Primary navigation destinations shown in the bottom bar / rail. */
enum class TopDestination(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Dashboard", Icons.Filled.Dashboard),
    MACHINE("machine", "Machine", Icons.Filled.Bluetooth),
    FIX("fix", "Fix My Coffee", Icons.Filled.Tune),
    TIMER("timer", "Shot Timer", Icons.Filled.Timer),
    SETTINGS("settings", "Settings", Icons.Filled.Settings),
}

/** Secondary routes reachable from within screens (Library / Tools). */
object Routes {
    const val RATIO = "ratio"
    const val STYLES = "styles"
    const val BEANS = "beans"
    const val RECIPES = "recipes"
    const val TASTING = "tasting"
    const val HISTORY = "history"
    const val WATER = "water"
    const val MAINTENANCE = "maintenance"
    const val EXPERT = "expert"
}
