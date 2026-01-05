package com.example.northstarquest.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.content.Context
import android.net.http.SslError
import android.os.Build
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.PermissionRequest
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.northstarquest.BackgroundAudioService
import android.webkit.WebResourceResponse

// Import modular browser components
import com.example.northstarquest.browser.AdBlocker
import com.example.northstarquest.browser.SslHelper
import com.example.northstarquest.browser.YouTubeAdBlocker
import com.example.northstarquest.browser.BrowserHistory
import com.example.northstarquest.browser.UrlUtils
import com.example.northstarquest.browser.ErrorHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(initialUrl: String) {
    val context = LocalContext.current
    val appContext = context.applicationContext

    // Loading & Progress
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }

    // Navigation state
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // URL & editing state
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var isEditing by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf(initialUrl) }

    // Find in page state
    var isFindMode by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var findMatchCount by remember { mutableIntStateOf(0) }
    var findCurrentMatch by remember { mutableIntStateOf(0) }
    val findFocusRequester = remember { FocusRequester() }
    
    // Address bar focus requester
    val addressFocusRequester = remember { FocusRequester() }

    // SSL error dialog state
    var showSslDialog by remember { mutableStateOf(false) }
    var sslErrorMessage by remember { mutableStateOf("") }
    var pendingSslHandler by remember { mutableStateOf<SslErrorHandler?>(null) }

    // URL history
    val history = remember { mutableStateListOf<String>() }

    // Single WebView reference (using custom class for background audio support)
    var webViewRef by remember { mutableStateOf<BackgroundAudioWebView?>(null) }
    
    // Background audio is always enabled by default
    val isBackgroundAudioEnabled = true
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Helper functions for background audio service
    fun startBackgroundAudioService(ctx: Context) {
        val serviceIntent = Intent(ctx, BackgroundAudioService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(ctx, serviceIntent)
        } else {
            ctx.startService(serviceIntent)
        }
    }
    
    fun stopBackgroundAudioService(ctx: Context) {
        ctx.stopService(Intent(ctx, BackgroundAudioService::class.java))
    }
    
    // Handle lifecycle for background audio
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // App going to background - start foreground service to keep audio alive
                    if (isBackgroundAudioEnabled) {
                        startBackgroundAudioService(appContext)
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    // App coming to foreground - stop foreground service
                    stopBackgroundAudioService(appContext)
                }
                Lifecycle.Event.ON_DESTROY -> {
                    stopBackgroundAudioService(appContext)
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopBackgroundAudioService(appContext)
        }
    }

    // History management using modular BrowserHistory class
    fun addToHistory(ctx: Context, url: String) {
        BrowserHistory.addUrl(ctx, url, history)
    }

    LaunchedEffect(Unit) {
        history.clear(); history.addAll(BrowserHistory.load(appContext))
    }

    // Sync address bar with current URL when not editing
    LaunchedEffect(currentUrl) {
        if (!isEditing) {
            textFieldValue = UrlUtils.cleanForDisplay(currentUrl)
        }
    }

    // Focus find input when entering find mode
    LaunchedEffect(isFindMode) {
        if (isFindMode) {
            findFocusRequester.requestFocus()
        } else {
            webViewRef?.clearMatches()
            findQuery = ""
            findMatchCount = 0
            findCurrentMatch = 0
        }
    }

    // URL normalization using modular UrlUtils class
    fun normalizeForGo(input: String): String? = UrlUtils.normalizeForGo(input)

    // Handle back press
    BackHandler(enabled = canGoBack || isFindMode) {
        when {
            isFindMode -> isFindMode = false
            canGoBack -> webViewRef?.goBack()
        }
    }

    // SSL Error Dialog
    if (showSslDialog) {
        AlertDialog(
            onDismissRequest = {
                pendingSslHandler?.cancel()
                showSslDialog = false
                pendingSslHandler = null
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Security Warning") },
            text = { Text("This site's security certificate is not trusted.\n\n$sslErrorMessage\n\nDo you want to proceed anyway?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingSslHandler?.proceed()
                    showSslDialog = false
                    pendingSslHandler = null
                }) {
                    Text("Proceed", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingSslHandler?.cancel()
                    showSslDialog = false
                    pendingSslHandler = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        // Progress bar (subtle, at the very top)
        val animatedProgress by animateFloatAsState(
            targetValue = if (isLoading) progress else 0f,
            animationSpec = tween(durationMillis = 150),
            label = "progress"
        )
        if (isLoading && animatedProgress > 0f) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        } else {
            Spacer(modifier = Modifier.height(2.dp))
        }


        // Find in page bar (conditionally shown)
        if (isFindMode) {
            Surface(
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = findQuery,
                        onValueChange = { query ->
                            findQuery = query
                            if (query.isNotEmpty()) {
                                webViewRef?.findAllAsync(query)
                            } else {
                                webViewRef?.clearMatches()
                                findMatchCount = 0
                                findCurrentMatch = 0
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(findFocusRequester),
                        placeholder = { Text("Find in page", fontSize = 14.sp) },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { webViewRef?.findNext(true) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    if (findQuery.isNotEmpty()) {
                        Text(
                            text = if (findMatchCount > 0) "$findCurrentMatch/$findMatchCount" else "0/0",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { webViewRef?.findNext(false) }) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous")
                    }
                    IconButton(onClick = { webViewRef?.findNext(true) }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next")
                    }
                    IconButton(onClick = { isFindMode = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close find")
                    }
                }
            }
        }

        // Elegant Chrome-style address bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF202124) // Chrome's dark toolbar color
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Elegant address bar
                val focusManager = LocalFocusManager.current
                val displayUrl = if (isEditing) textFieldValue else currentUrl
                val securityIcon = when {
                    displayUrl.startsWith("https://", ignoreCase = true) -> Icons.Default.Lock
                    displayUrl.startsWith("http://", ignoreCase = true) -> Icons.Default.Warning
                    else -> Icons.Default.Public
                }
                
                // Display URL - strip protocol for cleaner look
                val displayText = remember(currentUrl) {
                    currentUrl
                        .removePrefix("https://")
                        .removePrefix("http://")
                        .removePrefix("www.")
                        .trimEnd('/')
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clickable {
                            // Always clear and enter edit mode when address bar is tapped
                            isEditing = true
                            textFieldValue = ""
                            // Request focus after a short delay to ensure state is updated
                            addressFocusRequester.requestFocus()
                        },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF303134) // Chrome's address bar pill color
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Security icon
                        Icon(
                            securityIcon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (displayUrl.startsWith("https://")) 
                                       Color(0xFF9AA0A6) // Chrome's gray icon color
                                   else Color(0xFFEA4335) // Chrome's red for warnings
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        // Single TextField - always visible
                        androidx.compose.foundation.text.BasicTextField(
                            value = textFieldValue,
                            onValueChange = { textFieldValue = it },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(addressFocusRequester)
                                .onFocusChanged { state ->
                                    if (state.isFocused) {
                                        // Always clear when gaining focus (whether from Surface click or direct text tap)
                                        isEditing = true
                                        textFieldValue = ""
                                    } else if (isEditing) {
                                        // When unfocused, exit edit mode and restore URL
                                        isEditing = false
                                        textFieldValue = displayText
                                    }
                                },
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                color = Color(0xFFE8EAED) // Chrome's text color
                            ),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Go,
                                keyboardType = KeyboardType.Uri
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    normalizeForGo(textFieldValue)?.let { normalized ->
                                        currentUrl = normalized
                                        webViewRef?.loadUrl(normalized)
                                        isEditing = false
                                        focusManager.clearFocus()
                                    }
                                }
                            ),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (textFieldValue.isEmpty() && isEditing) {
                                        Text(
                                            "Search or enter address",
                                            style = TextStyle(
                                                fontSize = 14.sp,
                                                color = Color(0xFF9AA0A6) // Chrome's placeholder color
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        
                        if (isEditing && textFieldValue.isNotEmpty()) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { textFieldValue = "" },
                                tint = Color(0xFF9AA0A6)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        
                        // Go button when editing
                        if (isEditing) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Go",
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable {
                                        normalizeForGo(textFieldValue)?.let { normalized ->
                                            currentUrl = normalized
                                            webViewRef?.loadUrl(normalized)
                                            isEditing = false
                                            focusManager.clearFocus()
                                        }
                                    },
                                tint = Color(0xFF8AB4F8) // Chrome's blue accent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Refresh button
                IconButton(
                    onClick = { webViewRef?.reload() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
            }
        }

        // Suggestions list from history while editing - Chrome style
        val suggestions = remember(isEditing, textFieldValue, history) {
            if (isEditing) history.filter { it.contains(textFieldValue.trim(), ignoreCase = true) }.take(8) else emptyList()
        }
        if (isEditing && suggestions.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF303134), // Chrome's dropdown background
                shadowElevation = 8.dp
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(suggestions) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    textFieldValue = item
                                    currentUrl = item
                                    webViewRef?.loadUrl(item)
                                    isEditing = false
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // History/Globe icon
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF3C4043), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(0xFF9AA0A6)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            // URL text
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item
                                        .removePrefix("https://")
                                        .removePrefix("http://")
                                        .removePrefix("www.")
                                        .trimEnd('/'),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE8EAED),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            // Arrow icon
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(start = 8.dp),
                                tint = Color(0xFF9AA0A6)
                            )
                        }
                        
                        // Subtle divider
                        if (suggestions.indexOf(item) < suggestions.size - 1) {
                            HorizontalDivider(
                                color = Color(0xFF3C4043),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 60.dp)
                            )
                        }
                    }
                }
            }
        }

        // WebView container
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    BackgroundAudioWebView(ctx).apply {
                        // Enable background audio by default when toggle is on
                        setBackgroundAudioEnabled(isBackgroundAudioEnabled)
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
                        
                        // Set User Agent to remove "wv" to look like a real browser (enables Google Login)
                        settings.userAgentString = settings.userAgentString.replace("; wv", "")

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                canGoBack = canGoBack()
                                canGoForward = canGoForward()
                                url?.let { currentUrl = it }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                canGoBack = canGoBack()
                                canGoForward = canGoForward()
                                url?.let {
                                    currentUrl = it
                                    addToHistory(context.applicationContext, it)

                                    // Inject JavaScript to force "visible" state
                                    // This prevents YouTube (and others) from pausing video when screen assumes background state
                                    view?.evaluateJavascript("""
                                        (function() {
                                            try {
                                                console.log("Applying visibility spoof...");
                                                Object.defineProperty(document, 'hidden', { get: function() { return false; } });
                                                Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; } });
                                                Object.defineProperty(document, 'webkitVisibilityState', { get: function() { return 'visible'; } });
                                                
                                                // Stop visibility events
                                                window.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
                                                document.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
                                                window.addEventListener('webkitvisibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
                                            } catch(e) {
                                                console.error("Visibility spoof failed", e);
                                            }
                                        })();
                                    """.trimIndent(), null)
                                    
                                    // Inject YouTube Ad Blocker script on YouTube pages
                                    if (YouTubeAdBlocker.isYouTubeUrl(it)) {
                                        view?.evaluateJavascript(YouTubeAdBlocker.script, null)
                                    }
                                }
                            }
                            
                            // Block ad-related network requests
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val url = request?.url?.toString() ?: return null
                                
                                // Check if this URL should be blocked
                                if (AdBlocker.shouldBlock(url)) {
                                    // Return empty response to block the request
                                    return WebResourceResponse(
                                        "text/plain",
                                        "UTF-8",
                                        null  // Empty body
                                    )
                                }
                                
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val uri = request?.url ?: return false
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

                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: SslErrorHandler?,
                                error: SslError?
                            ) {
                                // Use modular SSL helper for Chrome-like certificate validation
                                SslHelper.handleSslError(handler, error) { errorMessage ->
                                    sslErrorMessage = errorMessage
                                    pendingSslHandler = handler
                                    showSslDialog = true
                                }
                            }
                            
                            // Handle page load errors - redirect to Google search like Chrome
                            override fun onReceivedError(
                                view: WebView?,
                                request: android.webkit.WebResourceRequest?,
                                error: android.webkit.WebResourceError?
                            ) {
                                ErrorHandler.handleError(view, request, error)
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress / 100f
                            }

                            override fun onPermissionRequest(request: PermissionRequest?) {
                                request?.grant(request.resources)
                            }
                        }

                        // Set find listener for find in page
                        setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
                            findMatchCount = numberOfMatches
                            findCurrentMatch = if (numberOfMatches > 0) activeMatchOrdinal + 1 else 0
                        }

                        loadUrl(UrlUtils.normalizeToUrlOrSearch(initialUrl))
                        webViewRef = this
                    }
                },
                update = { webView ->
                    webViewRef = webView
                    canGoBack = webView.canGoBack()
                    canGoForward = webView.canGoForward()
                    // Sync background audio state with WebView
                    webView.setBackgroundAudioEnabled(isBackgroundAudioEnabled)
                }
            )

            // Center loading spinner (for initial loads)
            if (isLoading && progress < 0.1f) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
