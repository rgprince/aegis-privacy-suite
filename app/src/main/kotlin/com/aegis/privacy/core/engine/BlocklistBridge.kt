package com.aegis.privacy.core.engine

import android.content.Context

/**
 * Bridge interface between blocklist data and blocking engines.
 * Supports dual-mode deployment (VPN or Root).
 * 
 * Implementation uses Strategy Pattern to route blocklist data to either:
 * 1. VPN Tunnel (non-root): In-memory checks during packet inspection
 * 2. Hosts File (root): Writes to /system/etc/hosts
 */
interface BlocklistBridge {
    
    /**
     * Deployment mode for the blocking engine.
     */
    enum class Mode {
        /** Lightweight DNS interception (port 53 only) */
        VPN_DNS_ONLY,
        
        /** Full packet inspection with per-app firewall */
        VPN_FULL_FIREWALL,
        
        /** System hosts file modification (requires root) */
        ROOT_HOSTS_FILE
    }
    
    /**
     * Initialize the bridge with specified mode.
     * @param mode Deployment mode
     * @param context Application context
     */
    suspend fun initialize(mode: Mode, context: Context)
    
    /**
     * Load blocklist from sources into the active engine.
     * @param sources List of blocklist URLs or file paths
     * @return Number of entries loaded
     */
    suspend fun loadBlocklists(sources: List<BlocklistSource>): Int
    
    /**
     * Query if a domain should be blocked.
     * @param domain FQDN to check (e.g., "ads.example.com")
     * @param uid Application UID (for VPN modes), -1 for global
     * @return BlockDecision with action and reason
     */
    suspend fun shouldBlock(domain: String, uid: Int = -1): BlockDecision
    
    /**
     * Add a custom rule (whitelist or blacklist).
     */
    suspend fun addCustomRule(rule: CustomRule)
    
    /**
     * Remove a custom rule.
     */
    suspend fun removeCustomRule(ruleId: String)
    
    /**
     * Get all custom rules.
     */
    suspend fun getCustomRules(): List<CustomRule>
    
    /**
     * Apply changes to the active blocking engine.
     * - VPN Mode: Reload in-memory filter
     * - Root Mode: Write to /system/etc/hosts and restart DNS
     */
    suspend fun applyChanges(): Result<Unit>
    
    /**
     * Disable all blocking (revert to default).
     */
    suspend fun revert(): Result<Unit>
    
    /**
     * Get statistics about blocked requests.
     */
    suspend fun getStatistics(): BlockingStatistics
}

/**
 * Represents a blocklist source (URL or local file).
 */
data class BlocklistSource(
    val id: String,
    val url: String,
    val name: String,
    val enabled: Boolean = true,
    val lastUpdated: Long = 0L
)

/**
 * Result of a block check.
 */
data class BlockDecision(
    val action: Action,
    val reason: String,
    val matchedListId: String? = null,
    val matchedRule: String? = null
) {
    enum class Action {
        /** Block the request */
        BLOCK,
        
        /** Allow the request */
        ALLOW,
        
        /** Redirect to custom IP */
        REDIRECT
    }
}

/**
 * Custom user-defined rule.
 */
data class CustomRule(
    val id: String = java.util.UUID.randomUUID().toString(),
    val domain: String,
    val action: BlockDecision.Action,
    val scope: RuleScope = RuleScope.GLOBAL,
    val uid: Int = -1,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class RuleScope {
        /** Apply to all apps */
        GLOBAL,
        
        /** Apply to specific app only */
        PER_APP
    }
}

/**
 * Statistics about blocking operations.
 */
data class BlockingStatistics(
    val totalDomainsBlocked: Long = 0,
    val totalRequestsBlocked: Long = 0,
    val totalRequestsAllowed: Long = 0,
    val blocklistCount: Int = 0,
    val customRuleCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
