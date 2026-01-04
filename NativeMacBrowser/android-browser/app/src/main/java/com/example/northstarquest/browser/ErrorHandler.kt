package com.example.northstarquest.browser

import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Error Handler - Handles WebView navigation errors
 * 
 * When a URL fails to load (DNS error, connection error, etc.),
 * this handler redirects to Google search with the failed URL as query,
 * mimicking Chrome's behavior.
 */
object ErrorHandler {
    
    private const val TAG = "BrowserError"
    
    /**
     * Error codes that should trigger a search fallback
     */
    private val NAVIGATION_ERROR_CODES = setOf(
        WebViewClient.ERROR_HOST_LOOKUP,     // DNS lookup failed
        WebViewClient.ERROR_CONNECT,         // Connection failed
        WebViewClient.ERROR_TIMEOUT,         // Connection timed out
        WebViewClient.ERROR_BAD_URL,         // Bad URL
        WebViewClient.ERROR_UNKNOWN,         // Unknown error
        WebViewClient.ERROR_UNSUPPORTED_SCHEME  // Unsupported URL scheme
    )
    
    /**
     * Handle a WebView error by redirecting to Google search if appropriate
     * 
     * @param view The WebView that encountered the error
     * @param request The failed request
     * @param error The error details
     * @return true if the error was handled (redirected to search), false otherwise
     */
    fun handleError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ): Boolean {
        // Only handle main frame errors (not subresources like images/scripts)
        if (request?.isForMainFrame != true || error == null) {
            return false
        }
        
        val errorCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            error.errorCode
        } else {
            WebViewClient.ERROR_UNKNOWN
        }
        
        val failedUrl = request.url?.toString() ?: return false
        
        // Check if this is a navigation error that should trigger search fallback
        if (isNavigationError(errorCode)) {
            android.util.Log.d(TAG, "Navigation error: $errorCode for URL: $failedUrl")
            
            // Don't redirect if already on Google (avoid infinite loop)
            if (failedUrl.contains("google.com/search")) {
                return false
            }
            
            // Redirect to Google search with the failed URL as query
            val searchUrl = createSearchUrl(failedUrl)
            view?.loadUrl(searchUrl)
            return true
        }
        
        return false
    }
    
    /**
     * Check if an error code indicates a navigation error
     */
    fun isNavigationError(errorCode: Int): Boolean {
        return errorCode in NAVIGATION_ERROR_CODES
    }
    
    /**
     * Create a Google search URL from a failed URL
     * 
     * @param failedUrl The URL that failed to load
     * @return Google search URL for the query
     */
    fun createSearchUrl(failedUrl: String): String {
        // Extract a meaningful search term from the failed URL
        val searchTerm = failedUrl
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .split("/").firstOrNull() ?: failedUrl
        
        return "https://www.google.com/search?hl=en&q=" + 
            java.net.URLEncoder.encode(searchTerm, "UTF-8")
    }
    
    /**
     * Get a human-readable description of the error
     */
    fun getErrorDescription(errorCode: Int): String {
        return when (errorCode) {
            WebViewClient.ERROR_HOST_LOOKUP -> "Domain not found"
            WebViewClient.ERROR_CONNECT -> "Connection failed"
            WebViewClient.ERROR_TIMEOUT -> "Connection timed out"
            WebViewClient.ERROR_BAD_URL -> "Invalid URL"
            WebViewClient.ERROR_UNSUPPORTED_SCHEME -> "Unsupported URL scheme"
            WebViewClient.ERROR_FILE_NOT_FOUND -> "File not found"
            WebViewClient.ERROR_AUTHENTICATION -> "Authentication required"
            WebViewClient.ERROR_PROXY_AUTHENTICATION -> "Proxy authentication required"
            WebViewClient.ERROR_REDIRECT_LOOP -> "Too many redirects"
            WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME -> "Unsupported authentication"
            WebViewClient.ERROR_TOO_MANY_REQUESTS -> "Too many requests"
            else -> "Unknown error"
        }
    }
}
