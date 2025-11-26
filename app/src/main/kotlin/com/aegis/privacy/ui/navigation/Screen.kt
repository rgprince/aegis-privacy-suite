package com.aegis.privacy.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Blocklists : Screen("blocklists", "Blocklists", Icons.Default.List)
    object Firewall : Screen("firewall", "Firewall", Icons.Default.Security)
    object Logs : Screen("logs", "Logs", Icons.Default.History)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    
    // Sub-screens (not in bottom nav)
    object ModeSelection : Screen("mode_selection", "Select Mode")
    object RootManagement : Screen("root_management", "Root Management")
}

/**
 * Bottom navigation items.
 */
val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Blocklists,
    Screen.Firewall,
    Screen.Logs,
    Screen.Settings
)
