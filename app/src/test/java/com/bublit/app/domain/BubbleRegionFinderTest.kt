package com.bublit.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BubbleRegionFinderTest {
    private val finder = BubbleRegionFinder()

    @Test
    fun findsBrightBubbleRegionAroundNarrowVerticalTextSeed() {
        val pixels = TestPixels(width = 160, height = 180, defaultLuma = 0.18)
        val bubble = BubbleBounds(left = 40, top = 24, width = 82, height = 126)
        pixels.fill(bubble, luma = 0.93)
        val textSeed = BubbleBounds(left = 73, top = 58, width = 14, height = 78)
        pixels.fill(textSeed, luma = 0.08)

        val region = finder.find(
            imageWidth = pixels.width,
            imageHeight = pixels.height,
            seedBounds = textSeed,
            lumaAt = pixels::lumaAt,
        )

        assertEquals(bubble, region?.bounds)
        assertEquals(0.93, region?.backgroundLuma ?: 0.0, 0.01)
    }

    @Test
    fun returnsNullWhenEffectTextHasNoBrightSurroundingRegion() {
        val pixels = TestPixels(width = 160, height = 180, defaultLuma = 0.18)
        val effectText = BubbleBounds(left = 42, top = 56, width = 90, height = 44)
        pixels.fill(effectText, luma = 0.08)

        val region = finder.find(
            imageWidth = pixels.width,
            imageHeight = pixels.height,
            seedBounds = effectText,
            lumaAt = pixels::lumaAt,
        )

        assertNull(region)
    }

    private class TestPixels(
        val width: Int,
        val height: Int,
        defaultLuma: Double,
    ) {
        private val lumas = DoubleArray(width * height) { defaultLuma }

        fun fill(bounds: BubbleBounds, luma: Double) {
            for (y in bounds.top until bounds.bottom) {
                for (x in bounds.left until bounds.right) {
                    lumas[y * width + x] = luma
                }
            }
        }

        fun lumaAt(x: Int, y: Int): Double = lumas[y * width + x]
    }
}
