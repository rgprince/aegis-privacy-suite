package com.aegis.privacy.network
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber


/**
 * Resolves application UIDs from network connections.
 * 
 * On Android Q+, uses ConnectivityManager.
 * On older versions, falls back to /proc/net/tcp parsing.
 */
@Singleton
class UidResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val packageManager: PackageManager = context.packageManager
    
    /**
     * Get UID for a connection.
     * 
     * @param protocol Protocol (6=TCP, 17=UDP)
     * @param srcIp Source IP
     * @param srcPort Source port
     * @param dstIp Destination IP
     * @param dstPort Destination port
     * @return UID or -1 if not found
     */
    suspend fun getUid(
        protocol: Int,
        srcIp: String,
        srcPort: Int,
        dstIp: String,
        dstPort: Int
    ): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getUidFromConnectivityManager(protocol, srcIp, srcPort, dstIp, dstPort)
        } else {
            getUidFromProcNet(protocol, srcPort)
        }
    }
    
    /**
     * Get UID using ConnectivityManager (Android Q+).
     */
    private fun getUidFromConnectivityManager(
        protocol: Int,
        srcIp: String,
        srcPort: Int,
        dstIp: String,
        dstPort: Int
    ): Int {
        // TODO: Implement using ConnectivityManager.getConnectionOwnerUid()
        // This requires android.permission.NETWORK_SETTINGS which is signature-level
        // For now, return -1 (unknown)
        return -1
    }
    
    /**
     * Get UID from /proc/net/tcp or /proc/net/tcp6 (Legacy).
     */
    private fun getUidFromProcNet(protocol: Int, localPort: Int): Int {
        val file = when (protocol) {
            6 -> "/proc/net/tcp"  // TCP
            17 -> "/proc/net/udp" // UDP
            else -> return -1
        }
        
        return try {
            File(file).useLines { lines ->
                lines.drop(1).forEach { line -> // Skip header
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 8) {
                        val localAddress = parts[1]
                        val port = localAddress.split(":")[1].toInt(16)
                        
                        if (port == localPort) {
                            return@useLines parts[7].toInt()
                        }
                    }
                }
                -1
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reading $file")
            -1
        }
    }
    
    /**
     * Get package name for UID.
     */
    fun getPackageName(uid: Int): String? {
        return try {
            val packages = packageManager.getPackagesForUid(uid)
            packages?.firstOrNull()
        } catch (e: Exception) {
            Timber.e(e, "Error getting package for UID $uid")
            null
        }
    }
    
    /**
     * Get app name for UID.
     */
    fun getAppName(uid: Int): String? {
        val packageName = getPackageName(uid) ?: return null
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            Timber.e(e, "Error getting app name for $packageName")
            packageName
        }
    }
}
