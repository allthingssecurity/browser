package com.example.northstarquest.browser

/**
 * Ad Blocker - Blocks requests to known ad networks and tracking services
 * 
 * This object contains a comprehensive list of ad/tracking domains and provides
 * a method to check if a URL should be blocked.
 */
object AdBlocker {
    
    /**
     * List of URL patterns to block for ads and tracking
     * Comprehensive list of ad networks, tracking services, and analytics
     * Note: YouTube internal ad URLs are NOT blocked to avoid breaking the player
     */
    private val AD_BLOCK_PATTERNS = listOf(
        // === Google Ads ===
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "pagead2.googlesyndication.com",
        "adservice.google.com",
        "www.googletagmanager.com",     // GTM (optional - can break some sites)
        
        // === Major Ad Exchanges ===
        "2mdn.net",
        "adnxs.com",                    // AppNexus
        "adsrvr.org",                   // The Trade Desk
        "pubmatic.com",
        "rubiconproject.com",
        "openx.net",
        "casalemedia.com",
        "contextweb.com",
        "indexww.com",                  // Index Exchange
        "smartadserver.com",
        "advertising.com",              // AOL/Verizon
        "bidswitch.net",
        "sharethrough.com",
        "triplelift.com",
        "33across.com",
        "media.net",
        "yieldmo.com",
        "sovrn.com",
        "lijit.com",                    // Sovrn
        "gumgum.com",
        "teads.tv",
        "spotxchange.com",
        "brightroll.com",
        
        // === Header Bidding / Prebid ===
        "ck-ie.com",
        "omnitagjs.com",
        "prebid.org",
        "prebidjs.com",
        "id5-sync.com",
        "liveintent.com",
        
        // === Tracking / Analytics ===
        "scorecardresearch.com",
        "moatads.com",
        "doubleverify.com",
        "adsafeprotected.com",          // IAS (Integral Ad Science)
        "quantserve.com",
        "quantcount.com",
        "bluekai.com",
        "krxd.net",                     // Krux/Salesforce DMP
        "exelator.com",
        "rlcdn.com",                    // LiveRamp
        "demdex.net",                   // Adobe Audience Manager
        "omtrdc.net",                   // Adobe Analytics
        "adobedtm.com",
        "hotjar.com",
        "mouseflow.com",
        "fullstory.com",
        "crazyegg.com",
        "clicktale.net",
        "newrelic.com",                 // (optional - performance monitoring)
        
        // === Retargeting / Remarketing ===
        "criteo.com",
        "criteo.net",
        "amazon-adsystem.com",
        "facebook.net/tr",              // Facebook Pixel
        "connect.facebook.net/signals",
        "adsymptotic.com",
        "adroll.com",
        "perfectaudience.com",
        "ml314.com",                    // MediaMath
        
        // === Content Recommendation / Native Ads ===
        "taboola.com",
        "outbrain.com",
        "revcontent.com",
        "mgid.com",
        "zergnet.com",
        "contentad.net",
        "dianomi.com",
        "nativo.com",
        
        // === Social Tracking ===
        "addthis.com",
        "sharethis.com",
        "addtoany.com",
        
        // === Pop-ups / Interstitials ===
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "adcash.com",
        "exoclick.com",
        "juicyads.com",
        "trafficjunky.com",
        
        // === Mobile Ad Networks ===
        "applovin.com",
        "unityads.unity3d.com",
        "mopub.com",
        "inmobi.com",
        "chartboost.com",
        "ironsrc.com",                  // ironSource
        "vungle.com",
        "adcolony.com",
        "tapjoy.com",
        "startapp.com",
        
        // === Video Ad Networks ===
        "innovid.com",
        "springserve.com",
        "jwpltx.com",                   // JW Player analytics
        "extreme-dm.com",
        
        // === Fingerprinting / Device ID ===
        "tapad.com",
        "drawbridge.com",
        "crossinstall.com",
        
        // === Other Common Ad/Tracking Domains ===
        "adform.net",
        "adtech.de",
        "bidvertiser.com",
        "buzzcity.net",
        "clicksor.com",
        "cpmstar.com",
        "cpxinteractive.com",
        "e-planning.net",
        "exponential.com",
        "flashtalking.com",
        "intellitxt.com",
        "legolas-media.com",
        "mathtag.com",
        "mediaplex.com",
        "serving-sys.com",              // Sizmek
        "specificclick.net",
        "statcounter.com",
        "tradedoubler.com",
        "turn.com",
        "undertone.com",
        "valueclick.com",
        "yieldlab.net",
        "zedo.com"
        
        // Note: YouTube internal ad URLs are NOT blocked
        // as they can break the video player, scrubber, and audio
    )
    
    /**
     * Check if a URL should be blocked
     * @param url The URL to check
     * @return true if the URL matches an ad/tracking pattern and should be blocked
     */
    fun shouldBlock(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return AD_BLOCK_PATTERNS.any { pattern ->
            if (pattern.contains("*")) {
                // Simple wildcard matching
                val regex = pattern.replace(".", "\\.").replace("*", ".*")
                lowerUrl.matches(Regex(".*$regex.*"))
            } else {
                lowerUrl.contains(pattern)
            }
        }
    }
    
    /**
     * Get the count of blocked patterns
     */
    val patternCount: Int
        get() = AD_BLOCK_PATTERNS.size
}
