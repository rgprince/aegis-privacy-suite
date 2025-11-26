package com.aegis.privacy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aegis.privacy.ui.screens.*

/**
 * Navigation graph for the app.
 */
@Composable
fun AegisNavHost(
    navController: NavHostController,
    vpnRunning: Boolean = false,
    onStartVpn: () -> Unit = {},
    onStopVpn: () -> Unit = {},
    startDestination: String = Screen.Dashboard.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Bottom nav screens
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                vpnRunning = vpnRunning,
                onStartVpn = onStartVpn,
                onStopVpn = onStopVpn,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToBlocklists = {
                    navController.navigate(Screen.Blocklists.route)
                },
                onNavigateToLogs = {
                    navController.navigate(Screen.Logs.route)
                }
            )
        }
        
        composable(Screen.Blocklists.route) {
            BlocklistManagementScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Firewall.route) {
            FirewallScreen()
        }
        
        composable(Screen.Logs.route) {
            ConnectionLogsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToModeSelection = {
                    navController.navigate(Screen.ModeSelection.route)
                },
                onNavigateToRootManagement = {
                    navController.navigate(Screen.RootManagement.route)
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Sub-screens
        composable(Screen.ModeSelection.route) {
            // TODO: Get actual values from ViewModel
            ModeSelectionScreen(
                currentMode = null,
                availableModes = emptyList(),
                recommendedMode = null,
                onModeSelected = { mode ->
                    // TODO: Update mode
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.RootManagement.route) {
            // TODO: Get actual values from ViewModel
            RootManagementScreen(
                isRootAvailable = false,
                isMagiskAvailable = false,
                isMagiskModuleActive = false,
                hasBackup = false,
                onRequestRoot = { /* TODO */ },
                onToggleMagiskModule = { /* TODO */ },
                onCreateBackup = { /* TODO */ },
                onRestoreBackup = { /* TODO */ },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
