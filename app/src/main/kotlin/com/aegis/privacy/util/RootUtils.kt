package com.aegis.privacy.util

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for root access detection and management.
 * Uses LibSu for root operations.
 */
@Singleton
class RootUtils @Inject constructor() {
    
    companion object {
        private const val HOSTS_PATH = "/system/etc/hosts"
        private const val HOSTS_BACKUP_PATH = "/data/local/tmp/aegis_hosts_backup"
        private const val MAGISK_MODULE_PATH = "/data/adb/modules/aegis-privacy"
    }
    
    private var rootAvailable: Boolean? = null
    private var magiskAvailable: Boolean? = null
    
    init {
        // Initialize LibSu shell
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )
    }
    
    /**
     * Check if device is rooted and we have root access.
     */
    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        if (rootAvailable != null) return@withContext rootAvailable!!
        
        try {
            val result = Shell.cmd("id").exec()
            rootAvailable = result.isSuccess && result.out.any { it.contains("uid=0") }
            Timber.i("Root available: $rootAvailable")
            rootAvailable!!
        } catch (e: Exception) {
            Timber.e(e, "Error checking root access")
            rootAvailable = false
            false
        }
    }
    
    /**
     * Check if Magisk is installed.
     */
    suspend fun isMagiskAvailable(): Boolean = withContext(Dispatchers.IO) {
        if (magiskAvailable != null) return@withContext magiskAvailable!!
        
        try {
            val result = Shell.cmd("which magisk").exec()
            magiskAvailable = result.isSuccess && result.out.isNotEmpty()
            Timber.i("Magisk available: $magiskAvailable")
            magiskAvailable!!
        } catch (e: Exception) {
            Timber.e(e, "Error checking Magisk")
            magiskAvailable = false
            false
        }
    }
    
    /**
     * Mount /system as read-write.
     */
    suspend fun mountSystemRW(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val result = Shell.cmd("mount -o remount,rw /system").exec()
            if (result.isSuccess) {
                Timber.i("Mounted /system as RW")
                Result.success(Unit)
            } else {
                val error = result.err.joinToString("\n")
                Timber.e("Failed to mount /system as RW: $error")
                Result.failure(Exception("Mount failed: $error"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error mounting /system")
            Result.failure(e)
        }
    }
    
    /**
     * Mount /system as read-only.
     */
    suspend fun mountSystemRO(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val result = Shell.cmd("mount -o remount,ro /system").exec()
            if (result.isSuccess) {
                Timber.i("Mounted /system as RO")
                Result.success(Unit)
            } else {
                val error = result.err.joinToString("\n")
                Timber.e("Failed to mount /system as RO: $error")
                Result.failure(Exception("Mount failed: $error"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error mounting /system")
            Result.failure(e)
        }
    }
    
    /**
     * Backup current hosts file.
     */
    suspend fun backupHostsFile(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val result = Shell.cmd("cp $HOSTS_PATH $HOSTS_BACKUP_PATH").exec()
            if (result.isSuccess) {
                Timber.i("Backed up hosts file")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Backup failed"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error backing up hosts file")
            Result.failure(e)
        }
    }
    
    /**
     * Restore hosts file from backup.
     */
    suspend fun restoreHostsFile(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Check if backup exists
            val checkResult = Shell.cmd("test -f $HOSTS_BACKUP_PATH && echo exists").exec()
            if (!checkResult.out.contains("exists")) {
                return@withContext Result.failure(Exception("No backup found"))
            }
            
            mountSystemRW()
            val result = Shell.cmd("cp $HOSTS_BACKUP_PATH $HOSTS_PATH").exec()
            mountSystemRO()
            
            if (result.isSuccess) {
                Timber.i("Restored hosts file from backup")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Restore failed"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error restoring hosts file")
            Result.failure(e)
        }
    }
    
    /**
     * Write content to hosts file.
     */
    suspend fun writeHostsFile(content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Write to temp location first
            val tempPath = "/data/local/tmp/aegis_hosts_temp"
            val writeResult = Shell.cmd("echo '$content' > $tempPath").exec()
            
            if (!writeResult.isSuccess) {
                return@withContext Result.failure(Exception("Failed to write temp file"))
            }
            
            // Mount system as RW
            mountSystemRW()
            
            // Copy to system
            val copyResult = Shell.cmd(
                "cp $tempPath $HOSTS_PATH",
                "chmod 644 $HOSTS_PATH",
                "chown root:root $HOSTS_PATH"
            ).exec()
            
            // Remount as RO
            mountSystemRO()
            
            if (copyResult.isSuccess) {
                Timber.i("Successfully wrote hosts file")
                Result.success(Unit)
            } else {
                val error = copyResult.err.joinToString("\n")
                Result.failure(Exception("Write failed: $error"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error writing hosts file")
            Result.failure(e)
        }
    }
    
    /**
     * Create a systemless Magisk module for hosts file.
     * This modifies hosts without touching /system partition.
     */
    suspend fun createMagiskModule(hostsContent: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!isMagiskAvailable()) {
                return@withContext Result.failure(Exception("Magisk not available"))
            }
            
            val commands = listOf(
                "mkdir -p $MAGISK_MODULE_PATH",
                "mkdir -p $MAGISK_MODULE_PATH/system/etc",
                "echo 'id=aegis-privacy' > $MAGISK_MODULE_PATH/module.prop",
                "echo 'name=AEGIS Privacy Suite' >> $MAGISK_MODULE_PATH/module.prop",
                "echo 'version=1.0' >> $MAGISK_MODULE_PATH/module.prop",
                "echo 'versionCode=1' >> $MAGISK_MODULE_PATH/module.prop",
                "echo 'author=AEGIS' >> $MAGISK_MODULE_PATH/module.prop",
                "echo 'description=System hosts file blocker' >> $MAGISK_MODULE_PATH/module.prop",
                "echo '$hostsContent' > $MAGISK_MODULE_PATH/system/etc/hosts",
                "chmod 644 $MAGISK_MODULE_PATH/system/etc/hosts"
            )
            
            val result = Shell.cmd(*commands.toTypedArray()).exec()
            
            if (result.isSuccess) {
                Timber.i("Created Magisk module successfully")
                Result.success(Unit)
            } else {
                val error = result.err.joinToString("\n")
                Result.failure(Exception("Magisk module creation failed: $error"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error creating Magisk module")
            Result.failure(e)
        }
    }
    
    /**
     * Remove Magisk module.
     */
    suspend fun removeMagiskModule(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val result = Shell.cmd("rm -rf $MAGISK_MODULE_PATH").exec()
            if (result.isSuccess) {
                Timber.i("Removed Magisk module")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to remove module"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error removing Magisk module")
            Result.failure(e)
        }
    }
    
    /**
     * Restart DNS service to apply hosts file changes.
     */
    suspend fun restartDns(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Different methods for different Android versions
            val commands = listOf(
                "killall -HUP dnsmasq",  // Kill dnsmasq
                "setprop net.dns1 8.8.8.8", // Reset DNS
                "ndc resolver flushdefaultif", // Flush DNS cache
                "ndc resolver clearnetdns wlan0" // Clear network DNS
            )
            
            Shell.cmd(*commands.toTypedArray()).exec()
            Timber.i("DNS restart commands sent")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error restarting DNS")
            Result.failure(e)
        }
    }
}
