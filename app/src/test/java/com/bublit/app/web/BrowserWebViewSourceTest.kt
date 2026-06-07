package com.bublit.app.web

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserWebViewSourceTest {
    @Test
    fun pullRefreshWebViewLookupDoesNotAssumeSwipeRefreshLayoutChildOrder() {
        val source = String(
            Files.readAllBytes(Paths.get("src/main/java/com/bublit/app/web/BrowserWebView.kt")),
        )

        assertFalse(source.contains("getChildAt(0) as WebView"))
        assertTrue(source.contains("refreshLayout.tag = webView"))
        assertTrue(source.contains("refreshLayout.tag as? WebView"))
    }
}
