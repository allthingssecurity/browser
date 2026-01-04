package com.example.northstarquest.browser

import java.net.URLEncoder

/**
 * URL Utilities - Helper functions for URL processing
 */
object UrlUtils {
    
    private const val DEFAULT_SEARCH_URL = "https://www.google.com/search?hl=en&q="
    private const val DEFAULT_HOME_URL = "https://www.google.com?hl=en"
    
    /**
     * Regex to detect if input looks like a URL/hostname
     */
    private val HOST_PATTERN = Regex("^[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*(/.*)?$")
    
    /**
     * Convert user input to a proper URL or Google search query
     * 
     * @param input User input from address bar
     * @return Normalized URL (either direct URL or Google search)
     */
    fun normalizeToUrlOrSearch(input: String): String {
        val trimmed = input.trim()
        
        if (trimmed.isEmpty()) {
            return DEFAULT_HOME_URL
        }
        
        val lower = trimmed.lowercase()
        val hasScheme = lower.startsWith("http://") || lower.startsWith("https://")
        val looksLikeHost = HOST_PATTERN.matches(trimmed)
        
        return when {
            hasScheme -> trimmed
            looksLikeHost -> "https://$trimmed"
            else -> DEFAULT_SEARCH_URL + URLEncoder.encode(trimmed, "UTF-8")
        }
    }
    
    /**
     * Normalize input for navigation (returns null if empty)
     */
    fun normalizeForGo(input: String): String? {
        val trimmed = input.trim()
        return if (trimmed.isEmpty()) null else normalizeToUrlOrSearch(trimmed)
    }
    
    /**
     * Clean URL for display in address bar (remove protocol and www)
     */
    fun cleanForDisplay(url: String): String {
        return url
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .trimEnd('/')
    }
    
    /**
     * Check if URL uses HTTPS
     */
    fun isSecure(url: String): Boolean {
        return url.startsWith("https://", ignoreCase = true)
    }
    
    /**
     * Check if URL uses HTTP (insecure)
     */
    fun isInsecure(url: String): Boolean {
        return url.startsWith("http://", ignoreCase = true)
    }
    
    /**
     * Extract the domain from a URL
     */
    fun extractDomain(url: String): String? {
        return try {
            val withoutProtocol = url
                .removePrefix("https://")
                .removePrefix("http://")
            val slashIndex = withoutProtocol.indexOf('/')
            if (slashIndex > 0) {
                withoutProtocol.substring(0, slashIndex)
            } else {
                withoutProtocol
            }
        } catch (e: Exception) {
            null
        }
    }
}
