package com.aegis.privacy.core.engine

import android.content.Context
import com.aegis.privacy.core.database.AegisDatabase
import com.aegis.privacy.util.RootUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Root mode implementation of BlocklistBridge.
 * Modifies system hosts file directly (requires root).
 */
@Singleton
class RootHostsFileAdapter @Inject constructor(
    private val repository: BlocklistRepository,
    private val database: AegisDatabase,
    private val rootUtils: RootUtils
) : BlocklistBridge {
    
    private var initialized = false
    private var useMagiskModule = false
    
    companion object {
        private const val HOSTS_HEADER = """
# AEGIS Privacy Suite - Generated Hosts File
# DO NOT EDIT MANUALLY
# Generated: %s
# 
# Localhost entries
127.0.0.1 localhost
::1 localhost
::1 ip6-localhost
::1 ip6-loopback

# Blocked domains
"""
    }
    
    override suspend fun initialize(mode: BlocklistBridge.Mode, context: Context) {
        if (mode != BlocklistBridge.Mode.ROOT_HOSTS_FILE) {
            throw IllegalArgumentException("RootHostsFileAdapter only supports ROOT_HOSTS_FILE mode")
        }
        
        // Check root access
        if (!rootUtils.isRootAvailable()) {
            throw SecurityException("Root access not available")
        }
        
        // Prefer Magisk module if available (systemless)
        useMagiskModule = rootUtils.isMagiskAvailable()
        
        // Backup current hosts file
        rootUtils.backupHostsFile()
        
        initialized = true
        Timber.i("RootHostsFileAdapter initialized (Magisk: $useMagiskModule)")
    }
    
    override suspend fun loadBlocklists(sources: List<BlocklistSource>): Int = withContext(Dispatchers.IO) {
        var totalLoaded = 0
        
        for (source in sources) {
            if (!source.enabled) continue
            
            try {
                val domains = repository.fetchBlocklist(source)
                
                // Save to database
                repository.saveToDatabase(domains.toSet(), source.id)
                
                totalLoaded += domains.size
                Timber.i("Loaded ${domains.size} domains from ${source.name}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load blocklist: ${source.name}")
            }
        }
        
        Timber.i("Total domains loaded: $totalLoaded")
        totalLoaded
    }
    
    override suspend fun shouldBlock(domain: String, uid: Int): BlockDecision {
        // For root mode, blocking is handled by the system
        // This method is mainly for UI/logging purposes
        
        // Check if domain is in database
        val blocked = withContext(Dispatchers.IO) {
            database.blockedDomainDao().findByDomain(domain) != null
        }
        
        return if (blocked) {
            BlockDecision(
                action = BlockDecision.Action.BLOCK,
                reason = "System hosts file",
                matchedListId = "hosts"
            )
        } else {
            BlockDecision(
                action = BlockDecision.Action.ALLOW,
                reason = "Not in hosts file"
            )
        }
    }
    
    override suspend fun addCustomRule(rule: CustomRule) {
        repository.addCustomRule(rule)
        
        // For block rules, they'll be added when applyChanges() is called
        // Allow rules can't be implemented in hosts file
        if (rule.action != BlockDecision.Action.BLOCK) {
            Timber.w("Root mode only supports BLOCK rules, ignoring ${rule.action} rule")
        }
    }
    
    override suspend fun removeCustomRule(ruleId: String) {
        repository.removeCustomRule(ruleId)
        // Will be removed from hosts file on next applyChanges()
    }
    
    override suspend fun getCustomRules(): List<CustomRule> {
        return repository.getAllCustomRules().first().map { entity ->
            CustomRule(
                id = entity.id,
                domain = entity.domain,
                action = BlockDecision.Action.valueOf(entity.action),
                scope = CustomRule.RuleScope.valueOf(entity.scope),
                uid = entity.uid,
                enabled = entity.enabled,
                createdAt = entity.createdAt
            )
        }
    }
    
    override suspend fun applyChanges(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!initialized) {
                return@withContext Result.failure(Exception("Not initialized"))
            }
            
            // Build hosts file content
            val hostsContent = buildHostsFile()
            
            // Write to system
            val writeResult = if (useMagiskModule) {
                rootUtils.createMagiskModule(hostsContent)
            } else {
                rootUtils.writeHostsFile(hostsContent)
            }
            
            if (writeResult.isFailure) {
                return@withContext writeResult
            }
            
            // Restart DNS to apply changes
            rootUtils.restartDns()
            
            // Update statistics
            updateStatistics()
            
            Timber.i("Hosts file applied successfully (${if (useMagiskModule) "Magisk" else "Direct"})")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error applying hosts file")
            Result.failure(e)
        }
    }
    
    override suspend fun revert(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val result = if (useMagiskModule) {
                rootUtils.removeMagiskModule()
            } else {
                rootUtils.restoreHostsFile()
            }
            
            if (result.isSuccess) {
                // Clear database
                database.blockedDomainDao().deleteAll()
                updateStatistics()
                
                // Restart DNS
                rootUtils.restartDns()
                
                Timber.i("Hosts file reverted successfully")
                Result.success(Unit)
            } else {
                result
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reverting hosts file")
            Result.failure(e)
        }
    }
    
    override suspend fun getStatistics(): BlockingStatistics {
        val stats = database.statisticsDao().getSync()
        return BlockingStatistics(
            totalDomainsBlocked = stats?.totalDomainsBlocked ?: 0,
            totalRequestsBlocked = 0, // Not tracked in root mode
            totalRequestsAllowed = 0, // Not tracked in root mode
            blocklistCount = stats?.blocklistCount ?: 0,
            customRuleCount = stats?.customRuleCount ?: 0,
            lastUpdated = stats?.lastUpdated ?: 0
        )
    }
    
    /**
     * Build hosts file content from database.
     */
    private suspend fun buildHostsFile(): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        
        // Add header
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            .format(java.util.Date())
        sb.append(HOSTS_HEADER.format(timestamp))
        
        // Fetch all blocked domains from database
        val domains = repository.getAllBlockedDomains()
        
        // Add blocked domains
        domains.forEach { domain ->
            sb.append("0.0.0.0 $domain\n")
        }
        
        // Add custom BLOCK rules
        val customRules = getCustomRules()
        customRules.filter { it.action == BlockDecision.Action.BLOCK && it.enabled }
            .forEach { rule ->
                sb.append("0.0.0.0 ${rule.domain}\n")
            }
        
        Timber.i("Built hosts file with ${domains.size} domains + ${customRules.size} custom rules")
        sb.toString()
    }
    
    /**
     * Update statistics in database.
     */
    private suspend fun updateStatistics() = withContext(Dispatchers.IO) {
        val domainCount = database.blockedDomainDao().getCount().toLong()
        val sourceCount = database.blocklistSourceDao().getAllEnabled().first().size
        val ruleCount = database.customRuleDao().getCount()
        
        database.statisticsDao().updateCounts(
            count = domainCount,
            sources = sourceCount,
            rules = ruleCount
        )
    }
}
