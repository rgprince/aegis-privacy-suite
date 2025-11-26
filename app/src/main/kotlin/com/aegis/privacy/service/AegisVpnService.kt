package com.aegis.privacy.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.aegis.privacy.R
import com.aegis.privacy.core.engine.BlocklistBridge
import com.aegis.privacy.network.DnsInterceptor
import com.aegis.privacy.network.UidResolver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject

/**
 * AEGIS VPN Service - Non-root blocking via VPN tunnel.
 * 
 * Modes supported:
 * - DNS-Only: Intercepts port 53 UDP packets
 * - Full Firewall: Intercepts all traffic (future)
 */
@AndroidEntryPoint
class AegisVpnService : VpnService() {
    
    @Inject
    lateinit var blocklistBridge: BlocklistBridge
    
    @Inject
    lateinit var dnsInterceptor: DnsInterceptor
    
    @Inject
    lateinit var uidResolver: UidResolver
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var vpnInterface: ParcelFileDescriptor? = null
    private var running = false
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "aegis_vpn"
        private const val VPN_ADDRESS = "10.1.10.1"
        private const val VPN_ROUTE = "0.0.0.0"
        private const val DNS_SERVER = "1.1.1.1" // Cloudflare DNS
        private const val MTU = 1500
        
        const val ACTION_START = "com.aegis.privacy.START_VPN"
        const val ACTION_STOP = "com.aegis.privacy.STOP_VPN"
    }
    
    override fun onCreate() {
        super.onCreate()
        Timber.i("AEGIS VPN Service created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP -> stopVpn()
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
        scope.cancel()
        Timber.i("AEGIS VPN Service destroyed")
    }
    
    /**
     * Start the VPN tunnel.
     */
    private fun startVpn() {
        if (running) {
            Timber.w("VPN already running")
            return
        }
        
        Timber.i("Starting AEGIS VPN...")
        
        // Start foreground service
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Establish VPN
        vpnInterface = establishVpn()
        if (vpnInterface == null) {
            Timber.e("Failed to establish VPN")
            stopSelf()
            return
        }
        
        running = true
        
        // Start packet processing loop
        scope.launch {
            runVpnLoop()
        }
        
        Timber.i("AEGIS VPN started successfully")
    }
    
    /**
     * Stop the VPN tunnel.
     */
    private fun stopVpn() {
        if (!running) return
        
        Timber.i("Stopping AEGIS VPN...")
        running = false
        
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Timber.e(e, "Error closing VPN interface")
        }
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    /**
     * Establish the VPN tunnel using VpnService.Builder.
     */
    private fun establishVpn(): ParcelFileDescriptor? {
        return try {
            Builder()
                .setSession("AEGIS Privacy")
                .addAddress(VPN_ADDRESS, 32)
                .addRoute(VPN_ROUTE, 0)
                .addDnsServer(DNS_SERVER)
                .setMtu(MTU)
                .setBlocking(false)
                .establish()
        } catch (e: Exception) {
            Timber.e(e, "Failed to establish VPN")
            null
        }
    }
    
    /**
     * Main VPN packet processing loop.
     */
    private suspend fun runVpnLoop() = withContext(Dispatchers.IO) {
        val vpnFd = vpnInterface ?: return@withContext
        
        val inputStream = FileInputStream(vpnFd.fileDescriptor)
        val outputStream = FileOutputStream(vpnFd.fileDescriptor)
        val buffer = ByteBuffer.allocate(32767)
        
        Timber.i("VPN packet loop started")
        
        while (running) {
            try {
                // Read packet from VPN interface
                val length = inputStream.read(buffer.array())
                if (length <= 0) {
                    continue
                }
                
                buffer.limit(length)
                
                // Process the packet
                processPacket(buffer.array(), 0, length, outputStream)
                
                buffer.clear()
            } catch (e: Exception) {
                if (running) {
                    Timber.e(e, "Error in VPN loop")
                    delay(100) // Prevent tight error loop
                }
            }
        }
        
        Timber.i("VPN packet loop stopped")
    }
    
    /**
     * Process a single IP packet.
     */
    private suspend fun processPacket(
        packet: ByteArray,
        offset: Int,
        length: Int,
        outputStream: FileOutputStream
    ) {
        try {
            // Parse IP header (simplified)
            val ipVersion = (packet[offset].toInt() shr 4) and 0xF
            if (ipVersion != 4) {
                // Drop IPv6 for now
                return
            }
            
            val protocol = packet[offset + 9].toInt() and 0xFF
            
            // Only intercept UDP for DNS-only mode
            if (protocol != 17) { // 17 = UDP
                // Forward non-UDP packets
                outputStream.write(packet, offset, length)
                return
            }
            
            // Extract ports
            val ipHeaderLength = (packet[offset].toInt() and 0xF) * 4
            val udpOffset = offset + ipHeaderLength
            val dstPort = ((packet[udpOffset + 2].toInt() and 0xFF) shl 8) or 
                          (packet[udpOffset + 3].toInt() and 0xFF)
            
            // Only intercept DNS (port 53)
            if (dstPort != 53) {
                outputStream.write(packet, offset, length)
                return
            }
            
            // Extract DNS payload
            val udpHeaderLength = 8
            val dnsOffset = udpOffset + udpHeaderLength
            val dnsLength = length - ipHeaderLength - udpHeaderLength
            val dnsPacket = packet.copyOfRange(dnsOffset, dnsOffset + dnsLength)
            
            // TODO: Get actual UID from packet
            val uid = -1
            
            // Process DNS query
            val response = dnsInterceptor.processDnsPacket(dnsPacket, uid)
            
            if (response != null) {
                // Send blocked response
                writeBlockedResponse(packet, offset, ipHeaderLength, response, outputStream)
            }
            // If null, drop the packet (blocking)
            
        } catch (e: Exception) {
            Timber.e(e, "Error processing packet")
        }
    }
    
    /**
     * Write a blocked DNS response back to the VPN interface.
     */
    private fun writeBlockedResponse(
        originalPacket: ByteArray,
        offset: Int,
        ipHeaderLength: Int,
        dnsResponse: ByteArray,
        outputStream: FileOutputStream
    ) {
        // TODO: Build proper IP + UDP + DNS response packet
        // For now, just drop (which appears as timeout to the app)
    }
    
    /**
     * Create notification channel for VPN service.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AEGIS VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "VPN-based ad and tracker blocking"
                setShowBadge(false)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create foreground service notification.
     */
    private fun createNotification(): Notification {
        val stopIntent = Intent(this, AegisVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AEGIS Protection Active")
            .setContentText("Blocking ads and trackers")
            .setSmallIcon(R.drawable.ic_shield) // TODO: Add icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent) // TODO: Add icon
            .build()
    }
}
