package com.bublit.app.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebAdBlockerTest {
    @Test
    fun webViewAdBlockingBlocksKnownAdAndTrackerRequests() {
        val blocker = WebAdBlocker()

        assertTrue(
            blocker.shouldBlock(
                pageUrl = "https://comic.example/episode/1",
                requestUrl = "https://securepubads.g.doubleclick.net/tag/js/gpt.js",
            ),
        )
        assertTrue(
            blocker.shouldBlock(
                pageUrl = "https://comic.example/episode/1",
                requestUrl = "https://www.google-analytics.com/analytics.js",
            ),
        )
    }

    @Test
    fun webViewAdBlockingStillAllowsComicImageRequests() {
        val blocker = WebAdBlocker()

        assertFalse(
            blocker.shouldBlock(
                pageUrl = "https://comic.example/episode/1",
                requestUrl = "https://cdn.comic.example/episodes/1/page-001.jpg",
            ),
        )
    }

    @Test
    fun webViewAdBlockingDoesNotBlockMainFrameNavigation() {
        val blocker = WebAdBlocker()

        assertFalse(
            blocker.shouldBlock(
                pageUrl = "https://comic.example/episode/1",
                requestUrl = "https://doubleclick.net/",
                isForMainFrame = true,
            ),
        )
    }
}
