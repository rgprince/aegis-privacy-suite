import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    exportSchema = false
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
        
        // Migration from v1 to v2 (adds FirewallRule table)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create firewall_rules table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS firewall_rules (
                        uid INTEGER PRIMARY KEY NOT NULL,
                        packageName TEXT NOT NULL,
                        appName TEXT NOT NULL,
                        blocked INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
        
        fun getDatabase(context: Context): AegisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AegisDatabase::class.java,
                    "aegis_database"
                )
                    .addMigrations(MIGRATION_1_2) // Proper migration - data persists!
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
