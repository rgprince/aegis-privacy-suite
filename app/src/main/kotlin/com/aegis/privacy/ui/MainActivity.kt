package com.aegis.privacy.ui

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.aegis.privacy.service.AegisVpnService
import com.aegis.privacy.ui.navigation.AegisNavHost
import com.aegis.privacy.ui.navigation.Screen
import com.aegis.privacy.ui.navigation.bottomNavItems
import com.aegis.privacy.ui.theme.AegisTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Main activity for AEGIS Privacy Suite with bottom navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private var vpnRunning by mutableStateOf(false)
    
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        } else {
            Timber.w("VPN permission denied")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AegisTheme {
                MainScreen(
                    vpnRunning = vpnRunning,
                    onStartVpn = { requestVpnPermission() },
                    onStopVpn = { stopVpnService() }
                )
            }
        }
    }
    
    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startVpnService()
        }
    }
    
    private fun startVpnService() {
        val intent = Intent(this, AegisVpnService::class.java).apply {
            action = AegisVpnService.ACTION_START
        }
        startService(intent)
        vpnRunning = true
        Timber.i("VPN service started")
    }
    
    private fun stopVpnService() {
        val intent = Intent(this, AegisVpnService::class.java).apply {
            action = AegisVpnService.ACTION_STOP
        }
        startService(intent)
        vpnRunning = false
        Timber.i("VPN service stopped")
    }
}

@Composable
fun MainScreen(
    vpnRunning: Boolean,
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Show bottom nav only on main screens  
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }
    
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { 
                                Icon(
                                    screen.icon!!,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Dashboard.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            AegisNavHost(
                navController = navController,
                vpnRunning = vpnRunning,
                onStartVpn = onStartVpn,
                onStopVpn = onStopVpn
            )
        }
    }
}
