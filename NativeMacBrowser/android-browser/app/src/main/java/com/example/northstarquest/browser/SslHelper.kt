package com.example.northstarquest.browser

import android.net.http.SslCertificate
import android.net.http.SslError
import android.os.Build
import android.webkit.SslErrorHandler
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * SSL Helper - Handles SSL certificate validation for WebView
 * 
 * Provides Chrome-like SSL handling by validating certificates against
 * the system trust store and handling date validation for false positives.
 */
object SslHelper {
    
    private const val TAG = "BrowserSSL"
    
    /**
     * Result of SSL validation
     */
    sealed class ValidationResult {
        /** Certificate is valid, proceed without warning */
        object Valid : ValidationResult()
        
        /** Certificate is invalid, show warning with message */
        data class Invalid(val message: String) : ValidationResult()
    }
    
    /**
     * Validate an SSL error and determine if we should proceed or show a warning
     * 
     * @param error The SSL error from WebView
     * @return ValidationResult indicating whether to proceed or show warning
     */
    fun validate(error: SslError?): ValidationResult {
        val cert = error?.certificate
        val primaryError = error?.primaryError ?: -1
        
        android.util.Log.d(TAG, "SSL Error received: code=$primaryError, url=${error?.url}")
        
        // Try to validate certificate - Chrome is more lenient than WebView
        if (cert != null) {
            try {
                val x509Cert = cert.getX509Cert()
                android.util.Log.d(TAG, "x509Cert extracted: ${x509Cert != null}")
                
                if (x509Cert != null) {
                    android.util.Log.d(TAG, "Got X509 cert: ${x509Cert.subjectDN}")
                    
                    // Try trust manager validation
                    if (validateWithTrustManager(x509Cert)) {
                        return ValidationResult.Valid
                    }
                    
                    // For date-related errors, check if dates are actually valid
                    if (isDateError(primaryError) && validateCertificateDates(x509Cert)) {
                        return ValidationResult.Valid
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exception during validation: ${e.message}")
            }
        } else {
            android.util.Log.d(TAG, "Certificate is null!")
        }
        
        // Return invalid with appropriate message
        android.util.Log.d(TAG, "Showing SSL warning dialog")
        return ValidationResult.Invalid(getErrorMessage(primaryError))
    }
    
    /**
     * Handle the SSL error by either proceeding or preparing for dialog
     * 
     * @param handler The SSL error handler
     * @param error The SSL error
     * @param onShowDialog Callback when dialog should be shown with error message
     */
    fun handleSslError(
        handler: SslErrorHandler?,
        error: SslError?,
        onShowDialog: (String) -> Unit
    ) {
        when (val result = validate(error)) {
            is ValidationResult.Valid -> {
                handler?.proceed()
            }
            is ValidationResult.Invalid -> {
                onShowDialog(result.message)
            }
        }
    }
    
    /**
     * Validate certificate using system trust manager
     */
    private fun validateWithTrustManager(x509Cert: X509Certificate): Boolean {
        try {
            val trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            )
            trustManagerFactory.init(null as java.security.KeyStore?)
            
            for (trustManager in trustManagerFactory.trustManagers) {
                if (trustManager is X509TrustManager) {
                    // Try RSA validation
                    try {
                        trustManager.checkServerTrusted(arrayOf(x509Cert), "RSA")
                        android.util.Log.d(TAG, "RSA validation passed, proceeding")
                        return true
                    } catch (e: java.security.cert.CertificateException) {
                        android.util.Log.d(TAG, "RSA failed: ${e.message}")
                        // Try EC validation
                        try {
                            trustManager.checkServerTrusted(arrayOf(x509Cert), "EC")
                            android.util.Log.d(TAG, "EC validation passed, proceeding")
                            return true
                        } catch (e2: java.security.cert.CertificateException) {
                            android.util.Log.d(TAG, "EC also failed: ${e2.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Trust manager validation error: ${e.message}")
        }
        return false
    }
    
    /**
     * Check if the error is date-related
     */
    private fun isDateError(primaryError: Int): Boolean {
        return primaryError == SslError.SSL_EXPIRED ||
               primaryError == SslError.SSL_NOTYETVALID ||
               primaryError == 4 // SSL_DATE_INVALID
    }
    
    /**
     * Validate certificate dates against current time
     */
    private fun validateCertificateDates(x509Cert: X509Certificate): Boolean {
        return try {
            val now = Date()
            val notBefore = x509Cert.notBefore
            val notAfter = x509Cert.notAfter
            
            android.util.Log.d(TAG, "Date check - Now: $now, NotBefore: $notBefore, NotAfter: $notAfter")
            
            if (now.after(notBefore) && now.before(notAfter)) {
                android.util.Log.d(TAG, "Certificate dates are valid! Proceeding.")
                true
            } else {
                android.util.Log.d(TAG, "Certificate dates are actually invalid")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Date validation error: ${e.message}")
            false
        }
    }
    
    /**
     * Get human-readable error message for SSL error code
     */
    fun getErrorMessage(primaryError: Int): String {
        return when (primaryError) {
            SslError.SSL_NOTYETVALID -> "The certificate is not yet valid."
            SslError.SSL_EXPIRED -> "The certificate has expired."
            SslError.SSL_IDMISMATCH -> "The certificate hostname mismatch."
            SslError.SSL_UNTRUSTED -> "The certificate authority is not trusted."
            4 -> "The certificate date is invalid."
            5 -> "A generic SSL error occurred."
            else -> "Unknown SSL error."
        }
    }
    
    /**
     * Extension function to extract X509Certificate from SslCertificate
     * Works on all Android API levels (uses Bundle for pre-API 29)
     */
    private fun SslCertificate.getX509Cert(): X509Certificate? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.util.Log.d(TAG, "Using API 29+ x509Certificate property")
                this.x509Certificate
            } else {
                android.util.Log.d(TAG, "Using pre-API 29 Bundle extraction")
                val bundle = SslCertificate.saveState(this)
                val certBytes = bundle.getByteArray("x509-certificate")
                android.util.Log.d(TAG, "certBytes is null: ${certBytes == null}, size: ${certBytes?.size ?: 0}")
                if (certBytes != null) {
                    val certFactory = CertificateFactory.getInstance("X.509")
                    certFactory.generateCertificate(ByteArrayInputStream(certBytes)) as X509Certificate
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to extract X509 cert: ${e.message}")
            null
        }
    }
}
