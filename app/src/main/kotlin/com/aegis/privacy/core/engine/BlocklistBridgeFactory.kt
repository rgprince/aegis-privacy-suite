package com.aegis.privacy.core.engine

import android.content.Context
import com.aegis.privacy.util.RootUtils
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory to create appropriate BlocklistBridge implementation
 * based on available permissions and user preference.
 */
@Singleton
class BlocklistBridgeFactory @Inject constructor(
    private val vpnAdapter: VpnBlocklistAdapter,
    private val rootAdapter: RootHostsFileAdapter,
    private val rootUtils: RootUtils
) {
    
    /**
     * Create BlocklistBridge instance for the requested mode.
     * Falls back to VPN mode if root is not available.
     */
    suspend fun create(
        mode: BlocklistBridge.Mode,
        context: Context
    ): Pair<BlocklistBridge, BlocklistBridge.Mode> {
        return when (mode) {
            BlocklistBridge.Mode.ROOT_HOSTS_FILE -> {
                if (rootUtils.isRootAvailable()) {
                    Timber.i("Using Root mode")
                    rootAdapter.initialize(mode, context)
                    Pair(rootAdapter, mode)
                } else {
                    Timber.w("Root not available, falling back to VPN DNS mode")
                    val fallbackMode = BlocklistBridge.Mode.VPN_DNS_ONLY
                    vpnAdapter.initialize(fallbackMode, context)
                    Pair(vpnAdapter, fallbackMode)
                }
            }
            
            BlocklistBridge.Mode.VPN_DNS_ONLY,
            BlocklistBridge.Mode.VPN_FULL_FIREWALL -> {
                Timber.i("Using VPN mode: $mode")
                vpnAdapter.initialize(mode, context)
                Pair(vpnAdapter, mode)
            }
        }
    }
    
    /**
     * Get recommended mode based on device capabilities.
     */
    suspend fun getRecommendedMode(): BlocklistBridge.Mode {
        return if (rootUtils.isRootAvailable() && rootUtils.isMagiskAvailable()) {
            // Prefer root with Magisk (systemless)
            BlocklistBridge.Mode.ROOT_HOSTS_FILE
        } else {
            // Default to VPN DNS-only mode
            BlocklistBridge.Mode.VPN_DNS_ONLY
        }
    }
    
    /**
     * Check which modes are available on this device.
     */
    suspend fun getAvailableModes(): List<BlocklistBridge.Mode> {
        val modes = mutableListOf<BlocklistBridge.Mode>()
        
        // VPN modes always available
        modes.add(BlocklistBridge.Mode.VPN_DNS_ONLY)
        modes.add(BlocklistBridge.Mode.VPN_FULL_FIREWALL)
        
        // Root mode only if root access
        if (rootUtils.isRootAvailable()) {
            modes.add(BlocklistBridge.Mode.ROOT_HOSTS_FILE)
        }
        
        return modes
    }
}
