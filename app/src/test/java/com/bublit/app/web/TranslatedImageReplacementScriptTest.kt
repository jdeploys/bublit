package com.bublit.app.web

import org.junit.Assert.assertTrue
import org.junit.Test

class TranslatedImageReplacementScriptTest {
    @Test
    fun translatedImageScriptReplacesMatchingPageImageSourcesInline() {
        val script = buildTranslatedImageReplacementScript(
            mapOf("https://example.com/page-1.jpg" to "file:///cache/rendered/page-1.png"),
        )

        assertTrue(script.contains("\"https://example.com/page-1.jpg\""))
        assertTrue(script.contains("\"file:///cache/rendered/page-1.png\""))
        assertTrue(script.contains("img.src = replacement"))
    }

    @Test
    fun emptyTranslatedImageScriptRestoresOriginalPageImageSources() {
        val script = buildTranslatedImageReplacementScript(emptyMap())

        assertTrue(script.contains("img.dataset.bublitOriginalSrc"))
        assertTrue(script.contains("img.src = img.dataset.bublitOriginalSrc"))
    }
}
