package com.example.northstarquest.browser

import android.content.Context

/**
 * Browser History Manager - Handles URL history persistence
 */
object BrowserHistory {
    
    private const val PREFS_NAME = "browser_prefs"
    private const val KEY_HISTORY = "history"
    private const val MAX_HISTORY_SIZE = 50
    
    /**
     * Load history from SharedPreferences
     */
    fun load(context: Context): MutableList<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_HISTORY, "") ?: ""
        return if (raw.isBlank()) {
            mutableListOf()
        } else {
            raw.split('\n').filter { it.isNotBlank() }.toMutableList()
        }
    }
    
    /**
     * Save history to SharedPreferences
     */
    fun save(context: Context, list: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HISTORY, list.joinToString("\n")).apply()
    }
    
    /**
     * Add a URL to history (at the beginning, removing duplicates)
     * 
     * @param context Application context
     * @param url URL to add
     * @param currentHistory Current history list (will be modified in place)
     * @return Updated history list
     */
    fun addUrl(context: Context, url: String, currentHistory: MutableList<String>): List<String> {
        if (url.isBlank()) return currentHistory
        
        val clean = url.trim()
        
        // Remove existing entry if present
        currentHistory.removeAll { it.equals(clean, ignoreCase = true) }
        
        // Add to beginning
        currentHistory.add(0, clean)
        
        // Cap the size
        val capped = currentHistory.take(MAX_HISTORY_SIZE)
        
        // Update the list in place
        currentHistory.clear()
        currentHistory.addAll(capped)
        
        // Persist
        save(context, capped)
        
        return capped
    }
    
    /**
     * Clear all history
     */
    fun clear(context: Context) {
        save(context, emptyList())
    }
}
