package com.aegis.privacy.util

import android.content.Context
import com.aegis.privacy.core.database.AegisDatabase
import com.aegis.privacy.core.database.entities.BlocklistSourceEntity
import com.aegis.privacy.core.database.entities.StatisticsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Utility class to initialize the database on first launch.
 */
object DatabaseInitializer {
    
    /**
     * Initialize database with default blocklists and statistics.
     */
    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        val database = AegisDatabase.getDatabase(context)
        
        try {
            // Check if already initialized
            val existingSources = database.blocklistSourceDao().getAll()
            if (existingSources.first().isNotEmpty()) {
                Timber.i("Database already initialized, skipping...")
                return@withContext
            }
            
            Timber.i("Initializing database with default sources...")
            
            // Add default blocklist sources
            val defaultSources = listOf(
                BlocklistSourceEntity(
                    id = Constants.DefaultSources.STEVEN_BLACK.id,
                    name = Constants.DefaultSources.STEVEN_BLACK.name,
                    url = Constants.DefaultSources.STEVEN_BLACK.url,
                    enabled = false
                ),
                BlocklistSourceEntity(
                    id = Constants.DefaultSources.ADAWAY.id,
                    name = Constants.DefaultSources.ADAWAY.name,
                    url = Constants.DefaultSources.ADAWAY.url,
                    enabled = false
                ),
                BlocklistSourceEntity(
                    id = Constants.DefaultSources.OISD_BASIC.id,
                    name = Constants.DefaultSources.OISD_BASIC.name,
                    url = Constants.DefaultSources.OISD_BASIC.url,
                    enabled = true // Enable by default for immediate functionality
                )
            )
            
            defaultSources.forEach { source ->
                database.blocklistSourceDao().insert(source)
            }
            
            // Initialize statistics with zero values
            database.statisticsDao().insert(
                StatisticsEntity(
                    id = 1,
                    totalDomainsBlocked = 0,
                    totalRequestsBlocked = 0,
                    totalRequestsAllowed = 0,
                    blocklistCount = 0,
                    customRuleCount = 0,
                    lastUpdated = System.currentTimeMillis()
                )
            )
            
            Timber.i("Database initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Error initializing database")
        }
    }
}
