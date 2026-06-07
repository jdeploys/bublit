package com.bublit.app.pipeline

import com.bublit.app.domain.BubbleBounds
import com.bublit.app.domain.OcrTextBlock
import org.junit.Assert.assertEquals
import org.junit.Test

class OcrBlockDeduplicationTest {
    @Test
    fun overlappingSimilarOcrBlocksCollapseToHigherConfidenceBlock() {
        val lowerConfidence = block(
            text = "Where are we?",
            left = 10,
            top = 20,
            width = 140,
            height = 42,
            confidence = 0.71,
        )
        val higherConfidence = block(
            text = "Where are we",
            left = 12,
            top = 22,
            width = 138,
            height = 40,
            confidence = 0.91,
        )

        val deduplicated = deduplicateOcrBlocks(listOf(lowerConfidence, higherConfidence))

        assertEquals(listOf(higherConfidence), deduplicated)
    }

    @Test
    fun sameTextInDifferentBubbleBoundsStaysSeparate() {
        val firstBubble = block(
            text = "Wait here",
            left = 10,
            top = 20,
            width = 120,
            height = 36,
        )
        val secondBubble = block(
            text = "Wait here",
            left = 10,
            top = 220,
            width = 120,
            height = 36,
        )

        val deduplicated = deduplicateOcrBlocks(listOf(firstBubble, secondBubble))

        assertEquals(listOf(firstBubble, secondBubble), deduplicated)
    }

    @Test
    fun overlappingDifferentTextBlocksStaySeparate() {
        val firstLine = block(
            text = "Go now",
            left = 10,
            top = 20,
            width = 120,
            height = 36,
        )
        val secondLine = block(
            text = "Stay here",
            left = 12,
            top = 22,
            width = 118,
            height = 34,
        )

        val deduplicated = deduplicateOcrBlocks(listOf(firstLine, secondLine))

        assertEquals(listOf(firstLine, secondLine), deduplicated)
    }

    private fun block(
        text: String,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        confidence: Double = 0.86,
    ): OcrTextBlock {
        return OcrTextBlock(
            text = text,
            bounds = BubbleBounds(
                left = left,
                top = top,
                width = width,
                height = height,
            ),
            backgroundLuma = 0.94,
            foregroundLuma = 0.08,
            confidence = confidence,
        )
    }
}
