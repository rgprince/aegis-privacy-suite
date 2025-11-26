package com.aegis.privacy.network

import com.aegis.privacy.core.database.AegisDatabase
import com.aegis.privacy.core.database.entities.ConnectionLog
import com.aegis.privacy.core.engine.BlockDecision
import com.aegis.privacy.core.engine.BlocklistBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xbill.DNS.*
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DNS packet interceptor for DNS-only VPN mode.
 * 
 * Intercepts UDP packets on port 53, parses DNS queries,
 * checks against blocklist, and returns appropriate responses.
 */
@Singleton
class DnsInterceptor @Inject constructor(
    private val blocklistBridge: BlocklistBridge,
    private val uidResolver: UidResolver,
    private val database: AegisDatabase
) {
    
    companion object {
        private const val DNS_PORT = 53
        private const val TTL_BLOCKED = 60 // 1 minute TTL for blocked responses
    }
    
    /**
     * Process a DNS packet.
     * 
     * @param packet Raw DNS packet bytes
     * @param uid Application UID making the request
     * @return Response packet or null to drop
     */
    suspend fun processDnsPacket(packet: ByteArray, uid: Int): ByteArray? = withContext(Dispatchers.IO) {
        try {
            // Parse DNS query
            val message = Message(packet)
            val query = message.question ?: return@withContext null
            
            val domain = query.name.toString(true) // Strip trailing dot
            Timber.d("DNS query for: $domain from UID $uid")
            
            // Check if should block
            val decision = blocklistBridge.shouldBlock(domain, uid)
            
            // Log the connection
            logConnection(uid, domain, decision)
            
            // Update statistics
            if (decision.action == BlockDecision.Action.BLOCK) {
                database.statisticsDao().incrementBlocked()
            } else {
                database.statisticsDao().incrementAllowed()
            }
            
            // Create response
            when (decision.action) {
                BlockDecision.Action.BLOCK -> {
                    Timber.i("Blocking DNS query: $domain (${decision.reason})")
                    createBlockedResponse(message)
                }
                BlockDecision.Action.ALLOW -> {
                    // Let the query through (return null to forward)
                    null
                }
                BlockDecision.Action.REDIRECT -> {
                    // TODO: Implement custom redirect
                    createBlockedResponse(message)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing DNS packet")
            null
        }
    }
    
    /**
     * Create a blocked DNS response (NXDOMAIN or null IP).
     */
    private fun createBlockedResponse(query: Message): ByteArray {
        val response = Message(query.header.id)
        response.header.setFlag(Flags.QR.toInt()) // Query Response
        response.header.setFlag(Flags.AA.toInt()) // Authoritative Answer
        
        // Add the original question
        query.question?.let { response.addRecord(it, Section.QUESTION) }
        
        // Return NXDOMAIN (domain doesn't exist)
        response.header.rcode = Rcode.NXDOMAIN
        
        // OR return 0.0.0.0 answer (commented out - NXDOMAIN is preferred)
        /*
        val record = ARecord(
            query.question?.name,
            DClass.IN,
            TTL_BLOCKED.toLong(),
            InetAddress.getByName("0.0.0.0")
        )
        response.addRecord(record, Section.ANSWER)
        response.header.rcode = Rcode.NOERROR
        */
        
        return response.toWire()
    }
    
    /**
     * Log the connection attempt.
     */
    private suspend fun logConnection(uid: Int, domain: String, decision: BlockDecision) {
        try {
            val packageName = uidResolver.getPackageName(uid)
            val log = ConnectionLog(
                uid = uid,
                packageName = packageName,
                domain = domain,
                destinationIp = "DNS_QUERY",
                destinationPort = DNS_PORT,
                protocol = "UDP",
                blocked = decision.action == BlockDecision.Action.BLOCK,
                reason = decision.reason,
                matchedRule = decision.matchedListId
            )
            database.connectionLogDao().insert(log)
        } catch (e: Exception) {
            Timber.e(e, "Error logging connection")
        }
    }
    
    /**
     * Extract domain from DNS packet without full parsing.
     * Faster for simple checks.
     */
    fun extractDomain(packet: ByteArray): String? {
        return try {
            val message = Message(packet)
            message.question?.name?.toString(true)
        } catch (e: Exception) {
            null
        }
    }
}
