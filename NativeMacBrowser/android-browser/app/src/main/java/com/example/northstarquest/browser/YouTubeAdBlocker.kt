package com.example.northstarquest.browser

/**
 * YouTube Ad Blocker - JavaScript injection for YouTube-specific ad blocking
 * 
 * This object contains JavaScript code that runs on YouTube pages to:
 * 1. Auto-click "Skip Ad" buttons
 * 2. Hide ad overlays and banners (but NOT player controls)
 * 
 * Note: Video player controls are preserved to maintain functionality.
 */
object YouTubeAdBlocker {
    
    /**
     * Check if a URL is a YouTube page
     */
    fun isYouTubeUrl(url: String): Boolean {
        return url.contains("youtube.com") || url.contains("youtu.be")
    }
    
    /**
     * JavaScript code to inject for ad blocking on YouTube
     */
    val script: String = """
(function() {
    'use strict';
    
    // Prevent multiple injections
    if (window.__ytAdBlockerInjected) return;
    window.__ytAdBlockerInjected = true;
    
    // Configuration
    const CONFIG = {
        skipButtonInterval: 250,  // Check for skip button every 250ms
        adCheckInterval: 500,     // Check for ads every 500ms
        debug: false
    };
    
    function log(msg) {
        if (CONFIG.debug) console.log('[YT-AdBlock] ' + msg);
    }
    
    // CSS to hide ONLY ad overlay elements (NOT player controls)
    const adBlockCSS = `
        /* Hide overlay ads and banners - but NOT player controls */
        .ytp-ad-overlay-slot,
        .ytp-ad-text-overlay,
        .ytp-ad-overlay-container,
        .ytp-ad-overlay-close-button,
        .ytp-ad-image-overlay,
        .ytp-ad-preview-container,
        .ytp-ad-message-container,
        .ytd-action-companion-ad-renderer,
        ytd-promoted-sparkles-web-renderer,
        ytd-companion-slot-renderer,
        ytd-promoted-video-renderer,
        ytd-engagement-panel-section-list-renderer[target-id="engagement-panel-ads"],
        #masthead-ad,
        #player-ads,
        .ytd-banner-promo-renderer,
        ytd-in-feed-ad-layout-renderer,
        ytd-ad-slot-renderer,
        .ytp-featured-product,
        .ytp-suggested-action,
        .iv-branding,
        ytd-mealbar-promo-renderer,
        yt-mealbar-promo-renderer,
        ytd-statement-banner-renderer,
        .ytd-merch-shelf-renderer {
            display: none !important;
            visibility: hidden !important;
            height: 0 !important;
            opacity: 0 !important;
        }
    `;
    
    // Inject CSS
    function injectCSS() {
        if (document.getElementById('yt-adblock-css')) return;
        const style = document.createElement('style');
        style.id = 'yt-adblock-css';
        style.textContent = adBlockCSS;
        (document.head || document.documentElement).appendChild(style);
        log('CSS injected');
    }
    
    // Click skip button if available
    function clickSkipButton() {
        const skipButtons = [
            '.ytp-ad-skip-button',
            '.ytp-ad-skip-button-modern',
            '.ytp-skip-ad-button',
            'button.ytp-ad-skip-button-modern',
            '.ytp-ad-skip-button-container button',
            '.videoAdUiSkipButton',
            '.ytp-ad-skip-button-text'
        ];
        
        for (const selector of skipButtons) {
            const btn = document.querySelector(selector);
            if (btn && btn.offsetParent !== null) {
                btn.click();
                log('Clicked skip button: ' + selector);
                return true;
            }
        }
        
        // Also try clicking by text content
        const allButtons = document.querySelectorAll('button, .ytp-ad-button');
        for (const btn of allButtons) {
            const text = btn.textContent?.toLowerCase() || '';
            if (text.includes('skip') && btn.offsetParent !== null) {
                btn.click();
                log('Clicked skip button by text');
                return true;
            }
        }
        
        return false;
    }
    
    // Skip ad by seeking to end (gentle approach - doesn't mute or change speed)
    function trySkipAd() {
        const adShowing = document.querySelector('.ad-showing');
        if (!adShowing) return false;
        
        const video = document.querySelector('.html5-main-video');
        if (!video) return false;
        
        // Only try to skip short ads by seeking to end
        // This preserves audio state
        if (video.duration && video.duration < 120 && video.duration > 0) {
            // Don't seek if we're already near the end
            if (video.currentTime < video.duration - 1) {
                video.currentTime = video.duration - 0.1;
                log('Seeked to end of ad');
            }
        }
        
        return true;
    }
    
    // Remove ad elements from DOM (banner ads, not video player elements)
    function removeAdElements() {
        const adSelectors = [
            'ytd-promoted-sparkles-web-renderer',
            'ytd-promoted-video-renderer',
            'ytd-ad-slot-renderer',
            'ytd-in-feed-ad-layout-renderer',
            'ytd-banner-promo-renderer',
            '#masthead-ad'
        ];
        
        adSelectors.forEach(selector => {
            document.querySelectorAll(selector).forEach(el => {
                el.remove();
                log('Removed: ' + selector);
            });
        });
    }
    
    // Close any ad-related overlay/popup
    function closeAdOverlays() {
        // Close overlay ads
        const closeButtons = [
            '.ytp-ad-overlay-close-button',
            '.ytp-ad-skip-button-icon'
        ];
        
        closeButtons.forEach(selector => {
            const btn = document.querySelector(selector);
            if (btn && btn.offsetParent !== null) {
                btn.click();
                log('Closed overlay: ' + selector);
            }
        });
    }
    
    // Main ad blocking loop
    function blockAds() {
        injectCSS();
        
        // Try skip button first (safest)
        if (!clickSkipButton()) {
            // If no skip button, try seeking
            trySkipAd();
        }
        
        closeAdOverlays();
        removeAdElements();
    }
    
    // Initialize
    function init() {
        log('Initializing YouTube Ad Blocker v2');
        
        // Initial run
        blockAds();
        
        // Set up intervals
        setInterval(clickSkipButton, CONFIG.skipButtonInterval);
        setInterval(blockAds, CONFIG.adCheckInterval);
        
        // Also run on DOM changes (for dynamic content)
        let lastCheck = Date.now();
        const observer = new MutationObserver(() => {
            // Throttle to avoid performance issues
            if (Date.now() - lastCheck > 200) {
                lastCheck = Date.now();
                clickSkipButton();
            }
        });
        
        observer.observe(document.body || document.documentElement, {
            childList: true,
            subtree: true
        });
        
        log('YouTube Ad Blocker v2 initialized');
    }
    
    // Start when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
    
    // Also run on page navigation (YouTube is a SPA)
    let lastUrl = location.href;
    new MutationObserver(() => {
        if (location.href !== lastUrl) {
            lastUrl = location.href;
            log('Page navigation detected');
            setTimeout(blockAds, 500);
        }
    }).observe(document, { subtree: true, childList: true });
    
})();
"""
    
    /**
     * JavaScript to spoof visibility state (prevents YouTube from pausing when backgrounded)
     */
    val visibilitySpoofScript: String = """
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
"""
}
