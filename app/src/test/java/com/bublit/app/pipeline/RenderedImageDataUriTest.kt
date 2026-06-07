package com.bublit.app.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RenderedImageDataUriTest {
    @Test
    fun pngBytesAreEncodedAsImageDataUriForInlineWebViewReplacement() {
        val uri = pngBytesToDataUri(byteArrayOf(1, 2, 3))

        assertTrue(uri.startsWith("data:image/png;base64,"))
        assertEquals("data:image/png;base64,AQID", uri)
    }
}
