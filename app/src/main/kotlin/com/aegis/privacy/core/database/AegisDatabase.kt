package com.aegis.privacy.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aegis.privacy.core.database.dao.*
import com.aegis.privacy.core.database.entities.*

/**
 * Main Room database for AEGIS Privacy Suite.
 */
@Database(
    entities = [
        BlockedDomain::class,
        BlocklistSourceEntity::class,
        CustomRuleEntity::class,
        StatisticsEntity::class,
        ConnectionLog::class,
        FirewallRule::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AegisDatabase : RoomDatabase() {
    
    abstract fun blockedDomainDao(): BlockedDomainDao
    abstract fun blocklistSourceDao(): BlocklistSourceDao
    abstract fun customRuleDao(): CustomRuleDao
    abstract fun statisticsDao(): StatisticsDao
    abstract fun connectionLogDao(): ConnectionLogDao
    abstract fun firewallRuleDao(): FirewallRuleDao
    
    companion object {
        @Volatile
        private var INSTANCE: AegisDatabase? = null
        
        fun getDatabase(context: Context): AegisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AegisDatabase::class.java,
                    "aegis_database"
                )
                    .fallbackToDestructiveMigration() // TODO: Implement proper migrations
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
