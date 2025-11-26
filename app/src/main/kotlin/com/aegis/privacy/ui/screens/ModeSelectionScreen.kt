package com.aegis.privacy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aegis.privacy.core.engine.BlocklistBridge
import timber.log.Timber

/**
 * Mode selection screen for choosing blocking strategy.
 */
@Composable
fun ModeSelectionScreen(
    currentMode: BlocklistBridge.Mode?,
    availableModes: List<BlocklistBridge.Mode>,
    recommendedMode: BlocklistBridge.Mode?,
    onModeSelected: (BlocklistBridge.Mode) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Blocking Mode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Choose how AEGIS should block ads and trackers:",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // VPN DNS-Only Mode
            if (availableModes.contains(BlocklistBridge.Mode.VPN_DNS_ONLY)) {
                ModeCard(
                    mode = BlocklistBridge.Mode.VPN_DNS_ONLY,
                    title = "VPN - DNS Only",
                    description = "Lightweight mode that intercepts DNS queries only. Best for battery life.",
                    icon = Icons.Default.Cloud,
                    pros = listOf(
                        "Excellent battery life",
                        "No root required",
                        "Minimal resource usage"
                    ),
                    cons = listOf(
                        "No per-app blocking",
                        "Only blocks DNS requests"
                    ),
                    isSelected = currentMode == BlocklistBridge.Mode.VPN_DNS_ONLY,
                    isRecommended = recommendedMode == BlocklistBridge.Mode.VPN_DNS_ONLY,
                    onClick = { onModeSelected(BlocklistBridge.Mode.VPN_DNS_ONLY) }
                )
            }
            
            // VPN Full Firewall Mode
            if (availableModes.contains(BlocklistBridge.Mode.VPN_FULL_FIREWALL)) {
                ModeCard(
                    mode = BlocklistBridge.Mode.VPN_FULL_FIREWALL,
                    title = "VPN - Full Firewall",
                    description = "Advanced mode with per-app firewall rules and complete packet filtering.",
                    icon = Icons.Default.Security,
                    pros = listOf(
                        "Per-app blocking",
                        "Complete traffic control",
                        "No root required"
                    ),
                    cons = listOf(
                        "Higher battery usage",
                        "More resource intensive"
                    ),
                    isSelected = currentMode == BlocklistBridge.Mode.VPN_FULL_FIREWALL,
                    isRecommended = recommendedMode == BlocklistBridge.Mode.VPN_FULL_FIREWALL,
                    onClick = { onModeSelected(BlocklistBridge.Mode.VPN_FULL_FIREWALL) }
                )
            }
            
            // Root Hosts File Mode
            if (availableModes.contains(BlocklistBridge.Mode.ROOT_HOSTS_FILE)) {
                ModeCard(
                    mode = BlocklistBridge.Mode.ROOT_HOSTS_FILE,
                    title = "Root - Hosts File",
                    description = "System-level blocking via hosts file modification. Best performance.",
                    icon = Icons.Default.AdminPanelSettings,
                    pros = listOf(
                        "Zero battery impact",
                        "Best performance",
                        "System-wide blocking",
                        "Magisk systemless support"
                    ),
                    cons = listOf(
                        "Requires root access",
                        "No per-app blocking",
                        "No connection logs"
                    ),
                    isSelected = currentMode == BlocklistBridge.Mode.ROOT_HOSTS_FILE,
                    isRecommended = recommendedMode == BlocklistBridge.Mode.ROOT_HOSTS_FILE,
                    onClick = { onModeSelected(BlocklistBridge.Mode.ROOT_HOSTS_FILE) }
                )
            }
            
            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "You can change the blocking mode at any time in settings. The recommended mode is based on your device capabilities.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun ModeCard(
    mode: BlocklistBridge.Mode,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    pros: List<String>,
    cons: List<String>,
    isSelected: Boolean,
    isRecommended: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isRecommended) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isRecommended) {
                            Text(
                                text = "RECOMMENDED",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Description
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            HorizontalDivider()
            
            // Pros
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Advantages:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                pros.forEach { pro ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = pro,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Cons
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Limitations:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                cons.forEach { con ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = con,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
