package com.aegis.privacy.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aegis.privacy.ui.viewmodel.DashboardViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*


/**
 * Dashboard screen showing statistics and quick actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    vpnRunning: Boolean = false,
    onStartVpn: () -> Unit = {},
    onStopVpn: () -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onNavigateToBlocklists: () -> Unit,
    onNavigateToLogs: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refreshStatistics()
            pullRefreshState.endRefresh()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AEGIS Privacy") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Protection Status Card
                    ProtectionStatusCard(
                        isActive = vpnRunning,
                        onToggle = { enabled ->
                            if (enabled) {
                                onStartVpn()
                            } else {
                                onStopVpn()
                            }
                        }
                    )
                    
                    // Statistics Section
                    Text(
                        text = "Statistics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Domains",
                            value = formatNumber(uiState.totalDomains),
                            icon = Icons.Default.Block,
                            color = MaterialTheme.colorScheme.primary
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Blocked",
                            value = formatNumber(uiState.requestsBlocked),
                            icon = Icons.Default.ShieldMoon,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Block Rate",
                            value = "${String.format("%.1f", uiState.blockRate)}%",
                            icon = Icons.Default.Percent,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Blocklists",
                            value = uiState.activeBlocklists.toString(),
                            icon = Icons.Default.List,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    // Quick Actions
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionCard(
                            modifier = Modifier.weight(1f),
                            title = "Blocklists",
                            icon = Icons.Default.FormatListBulleted,
                            onClick = onNavigateToBlocklists
                        )
                        ActionCard(
                            modifier = Modifier.weight(1f),
                            title = "Logs",
                            icon = Icons.Default.History,
                            onClick = onNavigateToLogs
                        )
                    }
                    
                    // Last Updated
                    if (uiState.lastUpdated > 0) {
                        Text(
                            text = "Last updated: ${formatDate(uiState.lastUpdated)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
            
            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
        
        // Error Snackbar
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

@Composable
fun ProtectionStatusCard(
    isActive: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isActive) Icons.Default.Shield else Icons.Default.ShieldMoon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (isActive) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
                Column {
                    Text(
                        text = if (isActive) "Protected" else "Not Protected",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isActive) "Blocking active" else "Protection disabled",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Switch(
                checked = isActive,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

private fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format("%.1fK", number / 1_000.0)
        else -> number.toString()
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
