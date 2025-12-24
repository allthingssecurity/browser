package com.example.northstarquest.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.PermissionRequest
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.Color
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(initialUrl: String) {
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var isEditing by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(initialUrl)) }

    // Sync the address bar to the current page URL when not editing
    LaunchedEffect(isEditing, currentUrl) {
        if (!isEditing) {
            textFieldValue = TextFieldValue(currentUrl)
        }
    }

    // Hold a reference to the WebView to drive navigation from UI controls
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun normalizeInputToUrlOrSearch(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "https://www.google.com"
        val lower = trimmed.lowercase()
        val hasScheme = lower.startsWith("http://") || lower.startsWith("https://")
        // Treat single-label hosts too (e.g., "intranet") to avoid disabling Go
        val looksLikeHost = Regex("^[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*(\\/.*)?$").matches(trimmed)
        return when {
            hasScheme -> trimmed
            looksLikeHost -> "https://$trimmed"
            else -> "https://www.google.com/search?q=" + URLEncoder.encode(trimmed, "UTF-8")
        }
    }

    fun normalizeForGo(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        return normalizeInputToUrlOrSearch(trimmed)
    }

    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top app bar with Chrome-like address bar
        TopAppBar(
            title = {
                val focusManager = LocalFocusManager.current
                val displayForSecurity = if (isEditing) textFieldValue.text else currentUrl
                val leadingIcon = when {
                    displayForSecurity.startsWith("https://", ignoreCase = true) -> Icons.Filled.Lock
                    displayForSecurity.startsWith("http://", ignoreCase = true) -> Icons.Filled.Warning
                    else -> Icons.Filled.Public
                }
                val canGo = textFieldValue.text.trim().isNotEmpty()

                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 36.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .onFocusChanged { state ->
                            if (state.isFocused && !isEditing) {
                                isEditing = true
                                textFieldValue = TextFieldValue("", selection = TextRange(0))
                            } else if (!state.isFocused && isEditing) {
                                isEditing = false
                            }
                        },
                    textStyle = TextStyle(fontSize = 13.sp),
                    placeholder = { Text("Search or enter address", fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Go,
                        keyboardType = KeyboardType.Uri,
                        autoCorrect = false
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            normalizeForGo(textFieldValue.text)?.let { normalized ->
                                currentUrl = normalized
                                webViewRef?.loadUrl(normalized)
                                isEditing = false
                                focusManager.clearFocus()
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    leadingIcon = { Icon(leadingIcon, contentDescription = null) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                normalizeForGo(textFieldValue.text)?.let { normalized ->
                                    currentUrl = normalized
                                    webViewRef?.loadUrl(normalized)
                                    isEditing = false
                                    focusManager.clearFocus()
                                }
                            },
                            enabled = canGo
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = "Go")
                        }
                    }
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            actions = {
                IconButton(onClick = { webViewRef?.reload() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
            }
        )

        // Removed linear top loading bar for a cleaner look

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                canGoBack = this@apply.canGoBack()
                                canGoForward = this@apply.canGoForward()
                                url?.let { currentUrl = it }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                canGoBack = this@apply.canGoBack()
                                canGoForward = this@apply.canGoForward()
                                url?.let { currentUrl = it }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val uri = request?.url ?: return false
                                // Let WebView handle http/https. External schemes go to the system.
                                return if (uri.scheme == "http" || uri.scheme == "https") {
                                    false
                                } else {
                                    runCatching {
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        context.startActivity(intent)
                                    }
                                    true
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress
                            }

                            override fun onPermissionRequest(request: PermissionRequest?) {
                                // Grant camera/mic for getUserMedia when using AR pages
                                request?.grant(request.resources)
                            }
                        }

                        loadUrl(normalizeInputToUrlOrSearch(initialUrl))
                        webViewRef = this
                    }
                },
                update = { webView ->
                    // When initialUrl changes via intent, make sure we load it once
                    if (webView.url != initialUrl) {
                        webView.loadUrl(normalizeInputToUrlOrSearch(initialUrl))
                    }
                    webViewRef = webView
                }
            )

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
