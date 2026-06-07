package com.bublit.app.session

import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserSessionStateTest {
    @Test
    fun restoreUsesSavedPageUrlAndAddressText() {
        val state = BrowserSessionState.restore(
            savedPageUrl = "https://example.com/chapter-7",
            savedAddressText = "example.com/chapter-7",
        )

        assertEquals("https://example.com/chapter-7", state.pageUrl)
        assertEquals("example.com/chapter-7", state.addressText)
    }

    @Test
    fun restoreFallsBackToHomeWhenSavedPageUrlIsBlank() {
        val state = BrowserSessionState.restore(
            savedPageUrl = " ",
            savedAddressText = " ",
        )

        assertEquals(BrowserSessionState.DefaultHomeUrl, state.pageUrl)
        assertEquals(BrowserSessionState.DefaultHomeUrl, state.addressText)
    }
}
