package com.bublit.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechBubbleClassifierTest {
    private val classifier = SpeechBubbleClassifier()

    @Test
    fun brightSpeechBubbleTextIsAccepted() {
        val block = OcrTextBlock(
            text = "Do not open that door!",
            bounds = BubbleBounds(left = 120, top = 220, width = 260, height = 96),
            backgroundLuma = 0.94,
            foregroundLuma = 0.08,
            confidence = 0.92,
        )

        assertTrue(classifier.isSpeechBubbleText(block))
    }

    @Test
    fun tonedSpeechBubbleTextIsAccepted() {
        val block = OcrTextBlock(
            text = "Wait a second.",
            bounds = BubbleBounds(left = 120, top = 220, width = 220, height = 72),
            backgroundLuma = 0.62,
            foregroundLuma = 0.08,
            confidence = 0.88,
        )

        assertTrue(classifier.isSpeechBubbleText(block))
    }

    @Test
    fun backgroundEffectTextIsRejected() {
        val block = OcrTextBlock(
            text = "BOOM",
            bounds = BubbleBounds(left = 24, top = 80, width = 480, height = 180),
            backgroundLuma = 0.32,
            foregroundLuma = 0.96,
            confidence = 0.89,
        )

        assertFalse(classifier.isSpeechBubbleText(block))
    }

    @Test
    fun rejectedSpeechBubbleCandidatesExplainAllFailedQualityGates() {
        val block = OcrTextBlock(
            text = "なにそれ",
            bounds = BubbleBounds(left = 40, top = 80, width = 24, height = 18),
            backgroundLuma = 0.42,
            foregroundLuma = 0.22,
            confidence = 0.41,
        )

        val classification = classifier.classify(block)

        assertEquals(false, classification.accepted)
        assertEquals(
            setOf(
                SpeechBubbleRejectionReason.LowConfidence,
                SpeechBubbleRejectionReason.TooNarrow,
                SpeechBubbleRejectionReason.TooShort,
                SpeechBubbleRejectionReason.DarkBackground,
                SpeechBubbleRejectionReason.LowContrast,
            ),
            classification.rejectionReasons.toSet(),
        )
    }

    @Test
    fun narrowDarkJapaneseTextInsideBrightBubbleRegionIsAcceptedUsingRegionBounds() {
        val block = OcrTextBlock(
            text = "恥ずかしすぎるだろー",
            bounds = BubbleBounds(left = 142, top = 88, width = 24, height = 112),
            backgroundLuma = 0.37,
            foregroundLuma = 0.08,
            confidence = 0.86,
            bubbleRegion = BubbleRegionCandidate(
                bounds = BubbleBounds(left = 108, top = 54, width = 96, height = 174),
                backgroundLuma = 0.91,
            ),
        )

        val accepted = classifier.acceptedBubbleTexts(listOf(block))

        assertEquals(1, accepted.size)
        assertEquals(BubbleBounds(left = 142, top = 88, width = 24, height = 112), accepted.single().bounds)
    }

    @Test
    fun multipleOcrBlocksInSameDetectedBubbleRegionAreMergedIntoOneBubbleText() {
        val region = BubbleRegionCandidate(
            bounds = BubbleBounds(left = 40, top = 30, width = 120, height = 150),
            backgroundLuma = 0.92,
        )
        val blocks = listOf(
            OcrTextBlock(
                text = "大丈夫ウサ！",
                bounds = BubbleBounds(left = 74, top = 52, width = 20, height = 48),
                backgroundLuma = 0.34,
                foregroundLuma = 0.08,
                confidence = 0.86,
                bubbleRegion = region,
            ),
            OcrTextBlock(
                text = "変身すれば",
                bounds = BubbleBounds(left = 98, top = 52, width = 20, height = 72),
                backgroundLuma = 0.35,
                foregroundLuma = 0.08,
                confidence = 0.86,
                bubbleRegion = region,
            ),
        )

        val accepted = classifier.acceptedBubbleTexts(blocks)

        assertEquals(1, accepted.size)
        assertEquals("変身すれば\n大丈夫ウサ！", accepted.single().originalText)
        assertEquals(BubbleBounds(left = 74, top = 52, width = 44, height = 72), accepted.single().bounds)
    }

    @Test
    fun adjacentVerticalTextColumnsWithSplitBrightRegionsAreMergedIntoOneBubbleText() {
        val blocks = listOf(
            OcrTextBlock(
                text = "そのステッキを",
                bounds = BubbleBounds(left = 132, top = 48, width = 18, height = 92),
                backgroundLuma = 0.36,
                foregroundLuma = 0.08,
                confidence = 0.86,
                bubbleRegion = BubbleRegionCandidate(
                    bounds = BubbleBounds(left = 112, top = 38, width = 40, height = 112),
                    backgroundLuma = 0.91,
                ),
            ),
            OcrTextBlock(
                text = "かまえて",
                bounds = BubbleBounds(left = 160, top = 52, width = 18, height = 78),
                backgroundLuma = 0.35,
                foregroundLuma = 0.08,
                confidence = 0.86,
                bubbleRegion = BubbleRegionCandidate(
                    bounds = BubbleBounds(left = 152, top = 38, width = 40, height = 112),
                    backgroundLuma = 0.91,
                ),
            ),
        )

        val accepted = classifier.acceptedBubbleTexts(blocks)

        assertEquals(1, accepted.size)
        assertEquals("かまえて\nそのステッキを", accepted.single().originalText)
        assertEquals(BubbleBounds(left = 132, top = 48, width = 46, height = 92), accepted.single().bounds)
    }

    @Test
    fun verticalJapaneseColumnsAreMergedRightToLeftForTranslationInput() {
        val blocks = listOf(
            OcrTextBlock(
                text = "「ラビラビ",
                bounds = BubbleBounds(left = 122, top = 88, width = 18, height = 88),
                backgroundLuma = 0.36,
                foregroundLuma = 0.08,
                confidence = 0.86,
                bubbleRegion = BubbleRegionCandidate(
                    bounds = BubbleBounds(left = 128, top = 48, width = 112, height = 170),
                    backgroundLuma = 0.91,
                ),
            ),
            OcrTextBlock(
                text = "チャームマジック",
                bounds = BubbleBounds(left = 142, top = 80, width = 18, height = 126),
                backgroundLuma = 0.35,
                foregroundLuma = 0.08,
                confidence = 0.86,
                bubbleRegion = BubbleRegionCandidate(
                    bounds = BubbleBounds(left = 128, top = 48, width = 112, height = 170),
                    backgroundLuma = 0.91,
                ),
            ),
            OcrTextBlock(
                text = "セットアップ」と",
                bounds = BubbleBounds(left = 162, top = 82, width = 18, height = 118),
                backgroundLuma = 0.35,
                foregroundLuma = 0.08,
                confidence = 0.86,
                bubbleRegion = BubbleRegionCandidate(
                    bounds = BubbleBounds(left = 128, top = 48, width = 112, height = 170),
                    backgroundLuma = 0.91,
                ),
            ),
            OcrTextBlock(
                text = "唱えるウサ!",
                bounds = BubbleBounds(left = 182, top = 88, width = 18, height = 92),
                backgroundLuma = 0.35,
                foregroundLuma = 0.08,
                confidence = 0.86,
                bubbleRegion = BubbleRegionCandidate(
                    bounds = BubbleBounds(left = 128, top = 48, width = 112, height = 170),
                    backgroundLuma = 0.91,
                ),
            ),
        )

        val accepted = classifier.acceptedBubbleTexts(blocks)

        assertEquals(1, accepted.size)
        assertEquals("唱えるウサ!\nセットアップ」と\nチャームマジック\n「ラビラビ", accepted.single().originalText)
    }
}
