package com.bublit.app.web

import org.junit.Assert.assertEquals
import org.junit.Test

class WebImageExtractorTest {
    @Test
    fun webViewImageDiscoveryParsesLargeImagesWithoutRegexInitializerCrash() {
        val json = """
            [
              {"src":"https://example.com/page-1.jpg","width":900,"height":1400,"naturalWidth":900,"naturalHeight":1400,"left":12,"top":90},
              {"src":"https://example.com/icon.png","width":48,"height":48,"naturalWidth":48,"naturalHeight":48,"left":0,"top":0},
              {"src":"","width":900,"height":1200}
            ]
        """.trimIndent()

        val candidates = WebImageExtractor().parseCandidates(json)

        assertEquals(1, candidates.size)
        assertEquals("https://example.com/page-1.jpg", candidates.single().url)
        assertEquals(900, candidates.single().width)
        assertEquals(1400, candidates.single().height)
    }

    @Test
    fun webViewImageDiscoveryStillIgnoresMalformedInput() {
        val candidates = WebImageExtractor().parseCandidates("not-json")

        assertEquals(emptyList<Any>(), candidates)
    }

    @Test
    fun webViewImageDiscoveryStillIgnoresSmallImages() {
        val json = """
            [
              {"src":"https://example.com/icon.png","width":48,"height":48,"naturalWidth":48,"naturalHeight":48,"left":0,"top":0}
            ]
        """.trimIndent()

        val candidates = WebImageExtractor().parseCandidates(json)

        assertEquals(emptyList<Any>(), candidates)
    }
}
