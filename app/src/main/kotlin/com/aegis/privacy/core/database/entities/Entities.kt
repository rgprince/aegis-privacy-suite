package com.aegis.privacy.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Database entity representing a blocked domain entry.
 */
@Entity(
    tableName = "blocked_domains",
    indices = [Index(value = ["domain"], unique = true)]
)
data class BlockedDomain(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** The blocked domain (e.g., "ads.example.com") */
    val domain: String,
    
    /** Source blocklist ID this entry came from */
    val sourceId: String,
    
    /** Timestamp when added */
    val addedAt: Long = System.currentTimeMillis(),
    
    /** Whether this entry is currently active */
    val enabled: Boolean = true
)

/**
 * Database entity for blocklist sources.
 */
@Entity(
    tableName = "blocklist_sources",
    indices = [Index(value = ["url"], unique = true)]
)
data class BlocklistSourceEntity(
    @PrimaryKey
    val id: String,
    
    /** Display name */
    val name: String,
    
    /** URL to fetch blocklist from */
    val url: String,
    
    /** Whether this source is enabled */
    val enabled: Boolean = true,
    
    /** Last successful update timestamp */
    val lastUpdated: Long = 0L,
    
    /** Number of domains from this source */
    val domainCount: Int = 0,
    
    /** ETag for HTTP caching */
    val etag: String? = null
)

/**
 * Database entity for custom user rules.
 */
@Entity(
    tableName = "custom_rules",
    indices = [Index(value = ["domain"])]
)
data class CustomRuleEntity(
    @PrimaryKey
    val id: String,
    
    /** Domain pattern (supports wildcards) */
    val domain: String,
    
    /** Action: BLOCK, ALLOW, REDIRECT */
    val action: String,
    
    /** Scope: GLOBAL or PER_APP */
    val scope: String,
    
    /** App UID if scope is PER_APP */
    val uid: Int = -1,
    
    /** Whether this rule is enabled */
    val enabled: Boolean = true,
    
    /** Creation timestamp */
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Database entity for blocking statistics.
 */
@Entity(tableName = "statistics")
data class StatisticsEntity(
    @PrimaryKey
    val id: Int = 1, // Single row
    
    /** Total domains in blocklist */
    val totalDomainsBlocked: Long = 0,
    
    /** Total requests blocked (since installation) */
    val totalRequestsBlocked: Long = 0,
    
    /** Total requests allowed */
    val totalRequestsAllowed: Long = 0,
    
    /** Number of active blocklist sources */
    val blocklistCount: Int = 0,
    
    /** Number of custom rules */
    val customRuleCount: Int = 0,
    
    /** Last update timestamp */
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Per-app firewall rule.
 */
@Entity(tableName = "firewall_rules")
data class FirewallRule(
    @PrimaryKey val uid: Int,
    val packageName: String,
    val appName: String,
    val blocked: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Database entity for connection logs (VPN mode).
 */
@Entity(
    tableName = "connection_logs",
    indices = [Index(value = ["timestamp"])]
)
data class ConnectionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Timestamp of connection */
    val timestamp: Long = System.currentTimeMillis(),
    
    /** App UID */
    val uid: Int,
    
    /** App package name */
    val packageName: String?,
    
    /** Destination domain */
    val domain: String,
    
    /** Destination IP */
    val destinationIp: String,
    
    /** Destination port */
    val destinationPort: Int,
    
    /** Protocol (TCP/UDP) */
    val protocol: String,
    
    /** Whether blocked */
    val blocked: Boolean,
    
    /** Reason for decision */
    val reason: String,
    
    /** Matched rule/list ID */
    val matchedRule: String? = null
)
