package com.cawfee.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowWidthSizeClass
import com.cawfee.ui.dashboard.DashboardScreen
import com.cawfee.ui.fix.FixMyCoffeeScreen
import com.cawfee.ui.machine.MachineScreen
import com.cawfee.ui.misc.PlaceholderScreen
import com.cawfee.ui.settings.SettingsScreen
import com.cawfee.ui.shots.ShotTimerScreen
import com.cawfee.ui.tools.RatioConverterScreen
import com.cawfee.ui.tools.StylePresetsScreen

/**
 * Root composable. Adapts the navigation surface to the available width: a bottom
 * [NavigationBar] on compact (phone-portrait) windows and a side [NavigationRail] on
 * medium/expanded windows (tablets, landscape) — satisfying the adaptive-layout
 * requirement.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun CawfeeApp() {
    val navController = rememberNavController()
    val widthClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    val compact = widthClass == WindowWidthSizeClass.COMPACT

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    if (compact) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    TopDestination.entries.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = { navController.navigateTop(dest.route) },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            },
        ) { padding ->
            CawfeeNavHost(navController, Modifier.padding(padding))
        }
    } else {
        Row(Modifier.fillMaxSize()) {
            NavigationRail {
                TopDestination.entries.forEach { dest ->
                    NavigationRailItem(
                        selected = currentRoute == dest.route,
                        onClick = { navController.navigateTop(dest.route) },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                    )
                }
            }
            CawfeeNavHost(navController, Modifier.fillMaxSize())
        }
    }
}

private fun NavHostController.navigateTop(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun CawfeeNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController, startDestination = TopDestination.DASHBOARD.route, modifier = modifier) {
        composable(TopDestination.DASHBOARD.route) { DashboardScreen(navController) }
        composable(TopDestination.MACHINE.route) { MachineScreen() }
        composable(TopDestination.FIX.route) { FixMyCoffeeScreen(expertMode = false) }
        composable(TopDestination.TIMER.route) { ShotTimerScreen() }
        composable(TopDestination.SETTINGS.route) { SettingsScreen() }

        composable(Routes.RATIO) { RatioConverterScreen() }
        composable(Routes.STYLES) { StylePresetsScreen() }
        composable(Routes.EXPERT) { FixMyCoffeeScreen(expertMode = true) }
        composable(Routes.BEANS) { PlaceholderScreen("Beans") }
        composable(Routes.RECIPES) { PlaceholderScreen("Recipes") }
        composable(Routes.TASTING) { PlaceholderScreen("Tasting Log") }
        composable(Routes.HISTORY) { PlaceholderScreen("History") }
        composable(Routes.WATER) { PlaceholderScreen("Water") }
        composable(Routes.MAINTENANCE) { PlaceholderScreen("Maintenance") }
    }
}
