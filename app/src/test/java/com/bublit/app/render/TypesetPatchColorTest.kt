package com.bublit.app.render

import org.junit.Assert.assertEquals
import org.junit.Test

class TypesetPatchColorTest {
    @Test
    fun speechBubblePatchColorStaysBrightEvenWhenSampledAreaIsDark() {
        val darkSampledColor = 0xFF202226.toInt()

        assertEquals(0xFFF8F4EA.toInt(), speechBubblePatchColor(darkSampledColor))
    }
}
