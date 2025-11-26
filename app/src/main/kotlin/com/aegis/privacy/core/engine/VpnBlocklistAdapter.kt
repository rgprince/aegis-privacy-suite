package com.aegis.privacy.core.engine

import android.content.Context
import com.aegis.privacy.core.database.AegisDatabase
import com.aegis.privacy.core.database.entities.BlockedDomain
import com.aegis.privacy.core.database.entities.CustomRuleEntity
import com.aegis.privacy.network.TrieNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VPN mode implementation of BlocklistBridge.
 * Uses in-memory Trie for fast domain lookups.
 */
@Singleton
class VpnBlocklistAdapter @Inject constructor(
    private val repository: BlocklistRepository,
    private val database: AegisDatabase
) : BlocklistBridge {
    
    private val blocklistTrie = TrieNode()
    private var initialized = false
    private var currentMode: BlocklistBridge.Mode = BlocklistBridge.Mode.VPN_DNS_ONLY
    
    override suspend fun initialize(mode: BlocklistBridge.Mode, context: Context) {
        if (mode == BlocklistBridge.Mode.ROOT_HOSTS_FILE) {
            throw IllegalArgumentException("VpnBlocklistAdapter only supports VPN modes")
        }
        
        currentMode = mode
        initialized = true
        
        // Load existing blocklists from database into trie
        loadExistingBlocklists()
        
        Timber.i("VpnBlocklistAdapter initialized in mode: $mode")
    }
    
    override suspend fun loadBlocklists(sources: List<BlocklistSource>): Int = withContext(Dispatchers.IO) {
        var totalLoaded = 0
        
        for (source in sources) {
            if (!source.enabled) continue
            
            try {
                val domains = repository.fetchBlocklist(source)
                
                // Save to database
                repository.saveToDatabase(domains.toSet(), source.id)
                
                // Insert into trie
                domains.forEach { domain ->
                    blocklistTrie.insert(domain)
                }
                
                totalLoaded += domains.size
                Timber.i("Loaded ${domains.size} domains from ${source.name}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load blocklist: ${source.name}")
            }
        }
        
        // Update statistics
        updateStatistics()
        
        Timber.i("Total domains loaded: $totalLoaded")
        totalLoaded
    }
    
    override suspend fun shouldBlock(domain: String, uid: Int): BlockDecision {
        if (!initialized) {
            return BlockDecision(
                action = BlockDecision.Action.ALLOW,
                reason = "Not initialized"
            )
        }
        
        // Check custom rules first (per-app or global)
        val customRule = if (uid != -1) {
            repository.getCustomRule(domain, uid)
        } else {
            null
        }
        
        customRule?.let {
            return BlockDecision(
                action = BlockDecision.Action.valueOf(it.action),
                reason = "Custom rule",
                matchedRule = it.id
            )
        }
        
        // Check blocklist trie
        val isBlocked = blocklistTrie.matchesDomain(domain)
        
        return if (isBlocked) {
            BlockDecision(
                action = BlockDecision.Action.BLOCK,
                reason = "Matched blocklist",
                matchedListId = "trie"
            )
        } else {
            BlockDecision(
                action = BlockDecision.Action.ALLOW,
                reason = "Not in blocklist"
            )
        }
    }
    
    override suspend fun addCustomRule(rule: CustomRule) {
        repository.addCustomRule(rule)
        
        // If it's an allow rule, we don't need to update trie
        // Block rules should be added if not already present
        if (rule.action == BlockDecision.Action.BLOCK) {
            blocklistTrie.insert(rule.domain)
        }
    }
    
    override suspend fun removeCustomRule(ruleId: String) {
        repository.removeCustomRule(ruleId)
        
        // Rebuild trie from database
        rebuildTrie()
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
    
    override suspend fun applyChanges(): Result<Unit> {
        return try {
            // For VPN mode, just ensure trie is up-to-date
            rebuildTrie()
            updateStatistics()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error applying changes")
            Result.failure(e)
        }
    }
    
    override suspend fun revert(): Result<Unit> {
        return try {
            // Clear all blocklists
            database.blockedDomainDao().deleteAll()
            blocklistTrie.clear()
            updateStatistics()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error reverting changes")
            Result.failure(e)
        }
    }
    
    override suspend fun getStatistics(): BlockingStatistics {
        val stats = database.statisticsDao().getSync()
        return BlockingStatistics(
            totalDomainsBlocked = stats?.totalDomainsBlocked ?: 0,
            totalRequestsBlocked = stats?.totalRequestsBlocked ?: 0,
            totalRequestsAllowed = stats?.totalRequestsAllowed ?: 0,
            blocklistCount = stats?.blocklistCount ?: 0,
            customRuleCount = stats?.customRuleCount ?: 0,
            lastUpdated = stats?.lastUpdated ?: 0
        )
    }
    
    /**
     * Load existing blocklists from database into trie.
     */
    private suspend fun loadExistingBlocklists() = withContext(Dispatchers.IO) {
        try {
            val domains = database.blockedDomainDao().getAllEnabled().first()
            domains.forEach { blocklistTrie.insert(it.domain) }
            Timber.i("Loaded ${domains.size} existing domains from database")
        } catch (e: Exception) {
            Timber.e(e, "Error loading existing blocklists")
        }
    }
    
    /**
     * Rebuild trie from database.
     */
    private suspend fun rebuildTrie() = withContext(Dispatchers.IO) {
        blocklistTrie.clear()
        loadExistingBlocklists()
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
