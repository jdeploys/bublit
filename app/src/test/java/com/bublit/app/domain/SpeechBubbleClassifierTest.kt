package com.bublit.app.domain

import org.junit.Assert.assertFalse
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
}
