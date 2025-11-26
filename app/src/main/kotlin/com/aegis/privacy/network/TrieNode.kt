package com.aegis.privacy.network

/**
 * Trie data structure for efficient domain matching.
 * Supports wildcard patterns (*.example.com) and O(1) lookups.
 * 
 * Used for in-memory blocklist storage in VPN mode.
 */
class TrieNode {
    
    private val children = mutableMapOf<String, TrieNode>()
    private var isEndOfDomain = false
    private var isWildcard = false
    
    /**
     * Insert a domain into the trie.
     * @param domain Domain to insert (e.g., "ads.example.com")
     */
    fun insert(domain: String) {
        var current = this
        val parts = domain.lowercase().split('.').reversed() // Reverse for TLD-first
        
        for (part in parts) {
            if (part == "*") {
                current.isWildcard = true
                return
            }
            current = current.children.getOrPut(part) { TrieNode() }
        }
        current.isEndOfDomain = true
    }
    
    /**
     * Check if a domain matches any entry in the trie.
     * Supports wildcards: *.example.com matches ads.example.com
     * 
     * @param domain Domain to check
     * @return true if domain should be blocked
     */
    fun matchesDomain(domain: String): Boolean {
        val parts = domain.lowercase().split('.').reversed()
        return matchesRecursive(parts, 0)
    }
    
    private fun matchesRecursive(parts: List<String>, index: Int): Boolean {
        // Reached end of domain parts
        if (index >= parts.size) {
            return isEndOfDomain
        }
        
        val part = parts[index]
        
        // Check for wildcard match at this level
        if (isWildcard) {
            return true
        }
        
        // Check exact match
        val child = children[part]
        if (child != null) {
            if (child.matchesRecursive(parts, index + 1)) {
                return true
            }
        }
        
        // Check if any parent has wildcard
        if (isEndOfDomain && index < parts.size) {
            return false
        }
        
        return false
    }
    
    /**
     * Get the number of domains in this trie.
     */
    fun count(): Int {
        var total = if (isEndOfDomain) 1 else 0
        for (child in children.values) {
            total += child.count()
        }
        return total
    }
    
    /**
     * Clear all entries.
     */
    fun clear() {
        children.clear()
        isEndOfDomain = false
        isWildcard = false
    }
    
    /**
     * Get all domains in this trie.
     * Useful for debugging.
     */
    fun getAllDomains(): List<String> {
        val result = mutableListOf<String>()
        collectDomains("", result)
        return result
    }
    
    private fun collectDomains(prefix: String, result: MutableList<String>) {
        if (isEndOfDomain) {
            result.add(prefix)
        }
        if (isWildcard) {
            result.add("$prefix*")
        }
        for ((part, child) in children) {
            val newPrefix = if (prefix.isEmpty()) part else "$part.$prefix"
            child.collectDomains(newPrefix, result)
        }
    }
}
