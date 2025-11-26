package com.aegis.privacy.util

/**
 * Constants used throughout the application.
 */
object Constants {
    
    // VPN Configuration
    object Vpn {
        const val ADDRESS = "10.1.10.1"
        const val PREFIX_LENGTH = 32
        const val ROUTE = "0.0.0.0"
        const val ROUTE_PREFIX = 0
        const val DNS_SERVER = "1.1.1.1"
        const val DNS_SERVER_BACKUP = "1.0.0.1"
        const val MTU = 1500
        const val DNS_PORT = 53
    }
    
    // Root Mode Paths
    object Root {
        const val HOSTS_PATH = "/system/etc/hosts"
        const val HOSTS_BACKUP = "/data/local/tmp/aegis_hosts_backup"
        const val MAGISK_MODULE_ID = "aegis-privacy"
        const val MAGISK_MODULES_PATH = "/data/adb/modules"
        const val MAGISK_MODULE_PATH = "$MAGISK_MODULES_PATH/$MAGISK_MODULE_ID"
    }
    
    // Database Configuration
    object Database {
        const val NAME = "aegis_database"
        const val VERSION = 1
        const val BATCH_SIZE = 100
    }
    
    // Network Configuration
    object Network {
        const val CONNECT_TIMEOUT_SECONDS = 30L
        const val READ_TIMEOUT_SECONDS = 30L
        const val WRITE_TIMEOUT_SECONDS = 30L
    }
    
    // Notification
    object Notification {
        const val CHANNEL_ID = "aegis_vpn"
        const val CHANNEL_NAME = "AEGIS VPN Service"
        const val ID = 1
    }
    
    // Logging
    object Logging {
        const val MAX_LOGS = 1000
        const val LOG_RETENTION_DAYS = 7
    }
    
    // Default Blocklist Sources
    object DefaultSources {
        val STEVEN_BLACK = BlocklistSourceInfo(
            id = "steven_black",
            name = "Steven Black's Unified Hosts",
            url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
            description = "Consolidated hosts file with base adware/malware"
        )
        
        val ADAWAY = BlocklistSourceInfo(
            id = "adaway_default",
            name = "AdAway Default",
            url = "https://adaway.org/hosts.txt",
            description = "AdAway's default hosts source"
        )
        
        val OISD_BASIC = BlocklistSourceInfo(
            id = "oisd_basic",
            name = "OISD Basic",
            url = "https://small.oisd.nl/",
            description = "OISD basic list - ads, tracking, malware"
        )
    }
    
    data class BlocklistSourceInfo(
        val id: String,
        val name: String,
        val url: String,
        val description: String
    )
}
