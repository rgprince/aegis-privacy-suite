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
import timber.log.Timber

/**
 * Root management screen for rooted devices.
 */
@Composable
fun RootManagementScreen(
    isRootAvailable: Boolean,
    isMagiskAvailable: Boolean,
    isMagiskModuleActive: Boolean,
    hasBackup: Boolean,
    onRequestRoot: () -> Unit,
    onToggleMagiskModule: (Boolean) -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Root Management") },
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
            // Root Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRootAvailable) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isRootAvailable) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isRootAvailable) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                        Column {
                            Text(
                                text = "Root Access",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isRootAvailable) "Available" else "Not Available",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    if (!isRootAvailable) {
                        Button(
                            onClick = onRequestRoot,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Request Root Access")
                        }
                    }
                }
            }
            
            // Magisk Status Card
            if (isRootAvailable) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                                    Icons.Default.Extension,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "Magisk Module",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isMagiskAvailable) {
                                            if (isMagiskModuleActive) "Active (Systemless)" else "Available"
                                        } else {
                                            "Not Installed"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            if (isMagiskAvailable) {
                                Switch(
                                    checked = isMagiskModuleActive,
                                    onCheckedChange = onToggleMagiskModule
                                )
                            }
                        }
                        
                        if (isMagiskAvailable) {
                            Text(
                                text = "Systemless mode modifies hosts file without touching /system partition. Recommended for safety.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Magisk not detected. Direct /system modification will be used (requires system partition mount).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            // Backup Management
            if (isRootAvailable) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Hosts File Backup",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = if (hasBackup) {
                                "Original hosts file is backed up and can be restored at any time."
                            } else {
                                "No backup found. Create a backup before modifying system files."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCreateBackup,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Create Backup")
                            }
                            
                            Button(
                                onClick = onRestoreBackup,
                                modifier = Modifier.weight(1f),
                                enabled = hasBackup
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Restore")
                            }
                        }
                    }
                }
            }
            
            // Warning Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Modifying system files requires root access and can be risky. Always create a backup before making changes. Use Magisk systemless mode when available.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
