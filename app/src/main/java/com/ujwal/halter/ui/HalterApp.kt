// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ujwal.halter.ui.screens.AppDetailScreen
import com.ujwal.halter.ui.screens.AppListScreen
import com.ujwal.halter.ui.screens.DashboardScreen
import com.ujwal.halter.ui.screens.FocusSessionScreen
import com.ujwal.halter.ui.screens.JournalScreen
import com.ujwal.halter.halterPermissionState
import com.ujwal.halter.ui.screens.OnboardingScreen
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ujwal.halter.ui.screens.ReportsPage
import com.ujwal.halter.ui.screens.RoutineScreen
import com.ujwal.halter.ui.screens.SettingsScreen

private enum class TopRoute(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Dashboard("dashboard", "Today", Icons.Outlined.Home),
    Apps("apps", "Apps", Icons.AutoMirrored.Outlined.List),
    Focus("focus", "Focus", Icons.Outlined.SelfImprovement),
    Settings("settings", "Settings", Icons.Outlined.Settings)
}

@Composable
fun HalterApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val permissionState = remember { context.halterPermissionState() }
    val startDest = if (permissionState.allSpecialPermissionsGranted) "dashboard" else "onboarding"
    val currentBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry.value?.destination
    val topRoutes = TopRoute.entries

    Scaffold(
        bottomBar = {
            NavigationBar {
                topRoutes.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                // Pop up to the destination route if it's already on the back stack,
                                // otherwise just navigate there. Using the route string avoids
                                // surprising behavior when the graph's start destination varies.
                                popUpTo(item.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(padding)
        ) {
            composable("onboarding") { OnboardingScreen(onContinue = { navController.navigate("dashboard") { popUpTo("onboarding") { inclusive = true } } }) }
            composable("dashboard") { DashboardScreen(onOpenApps = { navController.navigate("apps") }, onStartFocus = { navController.navigate("focus") }) }
            composable("apps") { AppListScreen(onOpenApp = { navController.navigate("app/$it") }) }
            composable("app/{packageName}") { entry ->
                AppDetailScreen(packageName = entry.arguments?.getString("packageName").orEmpty(), onBack = { navController.popBackStack() })
            }
            composable("focus") { FocusSessionScreen() }
            composable("settings") { SettingsScreen(onOpenJournal = { navController.navigate("journal") }, onOpenReports = { navController.navigate("reports-page") }, onOpenRoutines = { navController.navigate("routines") }) }
            composable("reports-page") { ReportsPage(onBack = { navController.popBackStack() }) }
            composable("routines") { RoutineScreen(onBack = { navController.popBackStack() }) }
            composable("journal") { JournalScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
