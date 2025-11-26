package com.aegis.privacy.core.engine

import com.aegis.privacy.core.database.AegisDatabase
import com.aegis.privacy.core.database.entities.BlockedDomain
import com.aegis.privacy.core.database.entities.BlocklistSourceEntity
import com.aegis.privacy.core.database.entities.CustomRuleEntity
import com.aegis.privacy.core.parser.HostsFileParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing blocklist data.
 * Handles network fetching, parsing, and database storage.
 */
@Singleton
class BlocklistRepository @Inject constructor(
    private val database: AegisDatabase,
    private val parser: HostsFileParser,
    private val httpClient: OkHttpClient
) {
    
    /**
     * Fetch blocklist from URL and parse it.
     */
    suspend fun fetchBlocklist(source: BlocklistSource): List<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(source.url)
                .build()
            
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Timber.w("Failed to fetch ${source.name}: HTTP ${response.code}")
                return@withContext emptyList()
            }
            
            val inputStream = response.body?.byteStream() ?: run {
                Timber.w("Empty response body for ${source.name}")
                return@withContext emptyList()
            }
            
            val result = parser.parse(inputStream, source.id)
            Timber.i("Fetched ${result.domains.size} domains from ${source.name}")
            
            result.domains
        } catch (e: Exception) {
            Timber.e(e, "Error fetching blocklist ${source.name}")
            emptyList()
        }
    }
    
    /**
     * Save domains to database.
     */
    suspend fun saveToDatabase(domains: Set<String>, sourceId: String) = withContext(Dispatchers.IO) {
        try {
            // Delete existing entries from this source
            database.blockedDomainDao().deleteBySource(sourceId)
            
            // Insert new entries in batches
            val entities = domains.map { domain ->
                BlockedDomain(
                    domain = domain,
                    sourceId = sourceId
                )
            }
            
            database.blockedDomainDao().insertAll(entities)
            
            // Update source stats
            val timestamp = System.currentTimeMillis()
            database.blocklistSourceDao().updateStats(sourceId, timestamp, domains.size)
            
            Timber.i("Saved ${domains.size} domains for source $sourceId")
        } catch (e: Exception) {
            Timber.e(e, "Error saving domains to database")
        }
    }
    
    /**
     * Get all blocked domains from database.
     */
    suspend fun getAllBlockedDomains(): List<String> = withContext(Dispatchers.IO) {
        database.blockedDomainDao().getAllEnabled().first().map { it.domain }
    }
    
    /**
     * Get custom rule for domain and UID.
     */
    suspend fun getCustomRule(domain: String, uid: Int): CustomRuleEntity? = withContext(Dispatchers.IO) {
        database.customRuleDao().findRule(domain, uid)
    }
    
    /**
     * Add a new blocklist source.
     */
    suspend fun addSource(source: BlocklistSource) = withContext(Dispatchers.IO) {
        val entity = BlocklistSourceEntity(
            id = source.id,
            name = source.name,
            url = source.url,
            enabled = source.enabled
        )
        database.blocklistSourceDao().insert(entity)
    }
    
    /**
     * Get all blocklist sources as Flow.
     */
    fun getAllSources(): Flow<List<BlocklistSourceEntity>> {
        return database.blocklistSourceDao().getAll()
    }
    
    /**
     * Get enabled blocklist sources as Flow.
     */
    fun getEnabledSources(): Flow<List<BlocklistSourceEntity>> {
        return database.blocklistSourceDao().getAllEnabled()
    }
    
    /**
     * Update an existing source.
     */
    suspend fun updateSource(source: BlocklistSourceEntity) = withContext(Dispatchers.IO) {
        database.blocklistSourceDao().update(source)
    }
    
    /**
     * Delete a source and all its domains.
     */
    suspend fun deleteSource(sourceId: String) = withContext(Dispatchers.IO) {
        val source = database.blocklistSourceDao().getById(sourceId)
        if (source != null) {
            database.blockedDomainDao().deleteBySource(sourceId)
            database.blocklistSourceDao().delete(source)
        }
    }
    
    /**
     * Add custom rule.
     */
    suspend fun addCustomRule(rule: CustomRule) = withContext(Dispatchers.IO) {
        val entity = CustomRuleEntity(
            id = rule.id,
            domain = rule.domain,
            action = rule.action.name,
            scope = rule.scope.name,
            uid = rule.uid,
            enabled = rule.enabled,
            createdAt = rule.createdAt
        )
        database.customRuleDao().insert(entity)
    }
    
    /**
     * Remove custom rule.
     */
    suspend fun removeCustomRule(ruleId: String) = withContext(Dispatchers.IO) {
        database.customRuleDao().deleteById(ruleId)
    }
    
    /**
     * Get all custom rules as Flow.
     */
    fun getAllCustomRules(): Flow<List<CustomRuleEntity>> {
        return database.customRuleDao().getAllEnabled()
    }
}
