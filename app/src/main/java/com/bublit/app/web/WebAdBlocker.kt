package com.bublit.app.web

import java.net.URI
import java.util.Locale

class WebAdBlocker(
    private val blockedHostSuffixes: Set<String> = DefaultBlockedHostSuffixes,
    private val blockedUrlTokens: Set<String> = DefaultBlockedUrlTokens,
) {
    fun shouldBlock(
        pageUrl: String?,
        requestUrl: String,
        isForMainFrame: Boolean = false,
    ): Boolean {
        if (isForMainFrame) return false

        val request = requestUrl.toUriOrNull() ?: return false
        val scheme = request.scheme?.lowercase(Locale.US)
        if (scheme != "http" && scheme != "https") return false

        val requestHost = request.host?.lowercase(Locale.US)?.trimEnd('.') ?: return false
        val pageHost = pageUrl?.toUriOrNull()?.host?.lowercase(Locale.US)?.trimEnd('.')
        if (pageHost != null && requestHost == pageHost) return false

        if (blockedHostSuffixes.any { requestHost == it || requestHost.endsWith(".$it") }) {
            return true
        }

        val normalizedUrl = requestUrl.lowercase(Locale.US)
        return blockedUrlTokens.any { token -> normalizedUrl.contains(token) }
    }

    companion object {
        private val DefaultBlockedHostSuffixes = setOf(
            "2mdn.net",
            "adservice.google.com",
            "adservice.google.co.kr",
            "adsafeprotected.com",
            "adsrvr.org",
            "amazon-adsystem.com",
            "analytics.google.com",
            "app-measurement.com",
            "criteo.com",
            "criteo.net",
            "doubleclick.net",
            "facebook.net",
            "googlesyndication.com",
            "googletagmanager.com",
            "googletagservices.com",
            "google-analytics.com",
            "googleadservices.com",
            "pagead2.googlesyndication.com",
            "scorecardresearch.com",
            "taboola.com",
            "zedo.com",
        )

        private val DefaultBlockedUrlTokens = setOf(
            "/adserver/",
            "/adsystem/",
            "/advertisement/",
            "/analytics.js",
            "/bannerad",
            "/doubleclick/",
            "/googleads.",
            "/pagead/",
            "/prebid.",
            "/track.gif",
            "adservice.",
            "ads?",
            "adsbygoogle",
            "adserver",
            "advertising",
            "googletagmanager",
            "googletagservices",
        )
    }
}

private fun String.toUriOrNull(): URI? {
    return runCatching { URI(this) }.getOrNull()
}
