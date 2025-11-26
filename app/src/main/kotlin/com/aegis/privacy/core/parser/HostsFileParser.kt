package com.aegis.privacy.core.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Parser for hosts file format blocklists.
 * Based on AdAway's SourceLoader logic with Kotlin optimizations.
 * 
 * Supports standard hosts file format:
 * ```
 * 127.0.0.1 localhost
 * 0.0.0.0 ads.example.com
 * 0.0.0.0 tracker.example.com # comment
 * ```
 */
class HostsFileParser {
    
    companion object {
        // FIXED: More flexible regex that handles various formats
        // Matches: IP whitespace+ DOMAIN [optional comment]
        private val HOSTS_PATTERN = Regex("^\\s*(\\S+)\\s+(\\S+)")
        
        // Batch size for database inserts
        private const val BATCH_SIZE = 100
        
        // Valid redirection IPs
        private val VALID_REDIRECTS = setOf("0.0.0.0", "127.0.0.1", "::1", "::")
    }
    
    /**
     * Parse result containing domains and metadata.
     */
    data class ParseResult(
        val domains: List<String>,
        val totalLines: Int,
        val validLines: Int,
        val skippedLines: Int,
        val errors: List<String> = emptyList()
    )
    
    /**
     * Parse an InputStream containing hosts file data.
     * 
     * @param inputStream Input stream to read from
     * @param sourceId Identifier for this source
     * @return ParseResult with extracted domains
     */
    suspend fun parse(inputStream: InputStream, sourceId: String): ParseResult = withContext(Dispatchers.IO) {
        val domains = mutableListOf<String>()
        val errors = mutableListOf<String>()
        var totalLines = 0
        var validLines = 0
        var skippedLines = 0
        
        try {
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.lineSequence().forEach { line ->
                    totalLines++
                    
                    // Skip empty lines and comments
                    val trimmed = line.trim()
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        skippedLines++
                        return@forEach
                    }
                    
                    // Try to parse the line
                    val domain = parseLine(trimmed)
                    if (domain != null) {
                        domains.add(domain)
                        validLines++
                    } else {
                        skippedLines++
                        if (errors.size < 10) {
                            // Log first few errors for debugging
                            errors.add("Line $totalLines: Invalid format - $trimmed")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing hosts file from $sourceId")
            errors.add("Parse error: ${e.message}")
        }
        
        Timber.i("Parsed $sourceId: $validLines valid domains from $totalLines lines")
        
        ParseResult(
            domains = domains.distinct(), // Remove duplicates
            totalLines = totalLines,
            validLines = validLines,
            skippedLines = skippedLines,
            errors = errors
        )
    }
    
    /**
     * Parse a single line of hosts file.
     * 
     * @param line Line to parse
     * @return Domain name if valid, null otherwise
     */
    private fun parseLine(line: String): String? {
        val match = HOSTS_PATTERN.find(line) ?: return null
        
        val redirect = match.groupValues[1]
        val hostname = match.groupValues[2]
        
        // Validate redirect IP (must be a blocking IP)
        if (!VALID_REDIRECTS.contains(redirect)) {
            return null
        }
        
        // Skip localhost entries
        if (hostname == "localhost" || hostname == "localhost.localdomain" || hostname.startsWith("local")) {
            return null
        }
        
        // Basic hostname validation - must contain a dot and look like a domain
        if (!hostname.contains('.') || hostname.length > 253) {
            return null
        }
        
        // Reject obvious invalid patterns
        if (hostname.contains('*') || hostname.contains('?') || hostname.contains(' ')) {
            return null
        }
        
        return hostname.lowercase()
    }
    
    /**
     * Validate hostname format.
     * 
     * Checks:
     * - No wildcard characters (*, ?)
     * - Valid domain format
     * - Not excessively long
     */
    private fun isValidHostname(hostname: String): Boolean {
        // Reject wildcards (we'll handle them separately)
        if (hostname.contains('*') || hostname.contains('?')) {
            return false
        }
        
        // Length check
        if (hostname.length > 253) {
            return false
        }
        
        // Basic domain format check
        // Must contain at least one dot and valid characters
        if (!hostname.contains('.')) {
            return false
        }
        
        // Check each label
        val labels = hostname.split('.')
        return labels.all { label ->
            label.isNotEmpty() &&
            label.length <= 63 &&
            label.all { it.isLetterOrDigit() || it == '-' || it == '_' } &&
            !label.startsWith('-') &&
            !label.endsWith('-')
        }
    }
    
    /**
     * Parse multiple sources in parallel.
     */
    suspend fun parseMultiple(
        sources: List<Pair<InputStream, String>>
    ): Map<String, ParseResult> = withContext(Dispatchers.IO) {
        sources.associate { (stream, sourceId) ->
            sourceId to parse(stream, sourceId)
        }
    }
}
