package com.aegis.privacy.core.database.dao

import androidx.room.*
import com.aegis.privacy.core.database.entities.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for blocked domains.
 */
@Dao
interface BlockedDomainDao {
    @Query("SELECT * FROM blocked_domains WHERE enabled = 1")
    fun getAllEnabled(): Flow<List<BlockedDomain>>
    
    @Query("SELECT * FROM blocked_domains WHERE sourceId = :sourceId")
    suspend fun getBySource(sourceId: String): List<BlockedDomain>
    
    @Query("SELECT * FROM blocked_domains WHERE domain = :domain LIMIT 1")
    suspend fun findByDomain(domain: String): BlockedDomain?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(domain: BlockedDomain): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(domains: List<BlockedDomain>)
    
    @Query("DELETE FROM blocked_domains WHERE sourceId = :sourceId")
    suspend fun deleteBySource(sourceId: String)
    
    @Query("DELETE FROM blocked_domains")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM blocked_domains WHERE enabled = 1")
    suspend fun getCount(): Int
}

/**
 * DAO for blocklist sources.
 */
@Dao
interface BlocklistSourceDao {
    @Query("SELECT * FROM blocklist_sources ORDER BY name ASC")
    fun getAll(): Flow<List<BlocklistSourceEntity>>
    
    @Query("SELECT * FROM blocklist_sources WHERE enabled = 1")
    fun getAllEnabled(): Flow<List<BlocklistSourceEntity>>
    
    @Query("SELECT * FROM blocklist_sources WHERE id = :id")
    suspend fun getById(id: String): BlocklistSourceEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(source: BlocklistSourceEntity)
    
    @Update
    suspend fun update(source: BlocklistSourceEntity)
    
    @Delete
    suspend fun delete(source: BlocklistSourceEntity)
    
    @Query("UPDATE blocklist_sources SET lastUpdated = :timestamp, domainCount = :count WHERE id = :sourceId")
    suspend fun updateStats(sourceId: String, timestamp: Long, count: Int)
}

/**
 * DAO for custom rules.
 */
@Dao
interface CustomRuleDao {
    @Query("SELECT * FROM custom_rules WHERE enabled = 1 ORDER BY createdAt DESC")
    fun getAllEnabled(): Flow<List<CustomRuleEntity>>
    
    @Query("SELECT * FROM custom_rules WHERE enabled = 1 AND scope = 'GLOBAL'")
    suspend fun getGlobalRules(): List<CustomRuleEntity>
    
    @Query("SELECT * FROM custom_rules WHERE enabled = 1 AND scope = 'PER_APP' AND uid = :uid")
    suspend fun getRulesForApp(uid: Int): List<CustomRuleEntity>
    
    @Query("SELECT * FROM custom_rules WHERE domain = :domain AND uid = :uid LIMIT 1")
    suspend fun findRule(domain: String, uid: Int): CustomRuleEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: CustomRuleEntity)
    
    @Delete
    suspend fun delete(rule: CustomRuleEntity)
    
    @Query("DELETE FROM custom_rules WHERE id = :ruleId")
    suspend fun deleteById(ruleId: String)
    
    @Query("SELECT COUNT(*) FROM custom_rules WHERE enabled = 1")
    suspend fun getCount(): Int
}

/**
 * DAO for statistics.
 */
@Dao
interface StatisticsDao {
    @Query("SELECT * FROM statistics WHERE id = 1")
    fun get(): Flow<StatisticsEntity?>
    
    @Query("SELECT * FROM statistics WHERE id = 1")
    suspend fun getSync(): StatisticsEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stats: StatisticsEntity)
    
    @Query("UPDATE statistics SET totalRequestsBlocked = totalRequestsBlocked + 1, lastUpdated = :timestamp WHERE id = 1")
    suspend fun incrementBlocked(timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE statistics SET totalRequestsAllowed = totalRequestsAllowed + 1 WHERE id = 1")
    suspend fun incrementAllowed()
    
    @Query("UPDATE statistics SET totalDomainsBlocked = :count, blocklistCount = :sources, customRuleCount = :rules WHERE id = 1")
    suspend fun updateCounts(count: Long, sources: Int, rules: Int)
}

/**
 * DAO for firewall rules.
 */
@Dao
interface FirewallRuleDao {
    @Query("SELECT * FROM firewall_rules ORDER BY appName ASC")
    fun getAll(): Flow<List<FirewallRule>>
    
    @Query("SELECT * FROM firewall_rules WHERE blocked = 1")
    fun getBlocked(): Flow<List<FirewallRule>>
    
    @Query("SELECT blocked FROM firewall_rules WHERE uid = :uid")
    suspend fun isBlocked(uid: Int): Boolean?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: FirewallRule)
    
    @Delete
    suspend fun delete(rule: FirewallRule)
    
    @Query("DELETE FROM firewall_rules WHERE uid = :uid")
    suspend fun deleteByUid(uid: Int)
}

/**
 * DAO for connection logs.
 */
@Dao
interface ConnectionLogDao {
    @Query("SELECT * FROM connection_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int = 100): Flow<List<ConnectionLog>>
    
    @Query("SELECT * FROM connection_logs WHERE blocked = 1 ORDER BY timestamp DESC LIMIT :limit")
    fun getBlocked(limit: Int = 100): Flow<List<ConnectionLog>>
    
    @Query("SELECT * FROM connection_logs WHERE uid = :uid ORDER BY timestamp DESC LIMIT :limit")
    fun getByApp(uid: Int, limit: Int = 100): Flow<List<ConnectionLog>>
    
    @Insert
    suspend fun insert(log: ConnectionLog)
    
    @Query("DELETE FROM connection_logs WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
    
    @Query("DELETE FROM connection_logs")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM connection_logs")
    suspend fun getCount(): Int
}
