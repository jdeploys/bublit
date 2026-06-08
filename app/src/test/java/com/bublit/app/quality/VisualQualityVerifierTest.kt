package com.bublit.app.quality

import com.bublit.app.domain.BubbleBounds
import com.bublit.app.domain.OcrTextBlock
import com.bublit.app.domain.SourceLanguage
import com.bublit.app.pipeline.ImageTranslationPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualQualityVerifierTest {
    private val planner = ImageTranslationPlanner(
        translator = { text, sourceLanguage ->
            when (sourceLanguage) {
                SourceLanguage.English -> "거기 누구야?"
                else -> text
            }
        },
    )
    private val verifier = VisualQualityVerifier()

    @Test
    fun brightSpeechBubbleTranslationPassesAndProtectedEffectTextStaysUnchanged() {
        val sample = SyntheticComicSample.basicSpeechBubble()
        val plan = planner.plan(sample.ocrBlocks)
        val renderedPixels = sample.sourcePixels.copyOf()
        renderedPixels.fillRegion(sample.expectedSpeechBubbles.single(), PatchColor, sample.fixture)

        val report = verifier.evaluate(
            fixture = sample.fixture,
            plan = plan,
            sourcePixels = sample.sourcePixels,
            renderedPixels = renderedPixels,
        )

        assertTrue(report.passed)
        assertEquals(1, report.translatedBubbleCount)
        assertEquals(0, report.protectedPixelsChanged)
        assertTrue(report.lowestBubbleIoU >= 0.50)
    }

    @Test
    fun protectedEffectTextPixelChangesFailVisualQualityReport() {
        val sample = SyntheticComicSample.basicSpeechBubble()
        val plan = planner.plan(sample.ocrBlocks)
        val renderedPixels = sample.sourcePixels.copyOf()
        renderedPixels.fillRegion(sample.expectedSpeechBubbles.single(), PatchColor, sample.fixture)
        renderedPixels.fillRegion(sample.protectedRegions.single(), PatchColor, sample.fixture)

        val report = verifier.evaluate(
            fixture = sample.fixture,
            plan = plan,
            sourcePixels = sample.sourcePixels,
            renderedPixels = renderedPixels,
        )

        assertFalse(report.passed)
        assertTrue(report.protectedPixelsChanged > 0)
        assertTrue(report.issues.any { it.id == VisualQualityIssueId.ProtectedRegionChanged })
    }

    @Test
    fun backgroundEffectOnlySamplePassesByPreservingOriginalImage() {
        val sample = SyntheticComicSample.effectTextOnly()
        val plan = planner.plan(sample.ocrBlocks)

        val report = verifier.evaluate(
            fixture = sample.fixture,
            plan = plan,
            sourcePixels = sample.sourcePixels,
            renderedPixels = sample.sourcePixels.copyOf(),
        )

        assertTrue(report.passed)
        assertEquals(0, report.translatedBubbleCount)
        assertEquals(0, report.protectedPixelsChanged)
    }

    private data class SyntheticComicSample(
        val fixture: VisualQualityFixture,
        val sourcePixels: IntArray,
        val ocrBlocks: List<OcrTextBlock>,
    ) {
        val expectedSpeechBubbles: List<BubbleBounds>
            get() = fixture.expectedSpeechBubbles

        val protectedRegions: List<BubbleBounds>
            get() = fixture.protectedRegions

        companion object {
            fun basicSpeechBubble(): SyntheticComicSample {
                val pixels = solidPixels(width = Width, height = Height, color = BackgroundColor)
                val expectedBubble = BubbleBounds(left = 73, top = 75, width = 174, height = 110)
                val protectedEffect = BubbleBounds(left = 270, top = 36, width = 82, height = 58)
                pixels.fillRegion(expectedBubble, BubbleColor, width = Width, height = Height)
                pixels.fillRegion(protectedEffect, EffectTextColor, width = Width, height = Height)

                return SyntheticComicSample(
                    fixture = VisualQualityFixture(
                        name = "basic-speech-bubble",
                        imageWidth = Width,
                        imageHeight = Height,
                        expectedSpeechBubbles = listOf(expectedBubble),
                        protectedRegions = listOf(protectedEffect),
                    ),
                    sourcePixels = pixels,
                    ocrBlocks = listOf(
                        OcrTextBlock(
                            text = "Who is there?",
                            bounds = BubbleBounds(left = 122, top = 108, width = 76, height = 42),
                            backgroundLuma = 0.96,
                            foregroundLuma = 0.08,
                            confidence = 0.94,
                        ),
                        OcrTextBlock(
                            text = "BANG",
                            bounds = protectedEffect,
                            backgroundLuma = 0.28,
                            foregroundLuma = 0.92,
                            confidence = 0.91,
                        ),
                    ),
                )
            }

            fun effectTextOnly(): SyntheticComicSample {
                val pixels = solidPixels(width = Width, height = Height, color = BackgroundColor)
                val protectedEffect = BubbleBounds(left = 80, top = 44, width = 210, height = 96)
                pixels.fillRegion(protectedEffect, EffectTextColor, width = Width, height = Height)

                return SyntheticComicSample(
                    fixture = VisualQualityFixture(
                        name = "effect-text-only",
                        imageWidth = Width,
                        imageHeight = Height,
                        expectedSpeechBubbles = emptyList(),
                        protectedRegions = listOf(protectedEffect),
                    ),
                    sourcePixels = pixels,
                    ocrBlocks = listOf(
                        OcrTextBlock(
                            text = "CRASH",
                            bounds = protectedEffect,
                            backgroundLuma = 0.24,
                            foregroundLuma = 0.95,
                            confidence = 0.89,
                        ),
                    ),
                )
            }
        }
    }

    private companion object {
        const val Width = 360
        const val Height = 240
        const val BackgroundColor = 0xFF877F77.toInt()
        const val BubbleColor = 0xFFFCFAF2.toInt()
        const val EffectTextColor = 0xFF1B1B1D.toInt()
        const val PatchColor = 0xFFF8F4EA.toInt()
    }
}

private fun solidPixels(width: Int, height: Int, color: Int): IntArray {
    return IntArray(width * height) { color }
}

private fun IntArray.fillRegion(bounds: BubbleBounds, color: Int, fixture: VisualQualityFixture) {
    fillRegion(bounds, color, width = fixture.imageWidth, height = fixture.imageHeight)
}

private fun IntArray.fillRegion(bounds: BubbleBounds, color: Int, width: Int, height: Int) {
    for (y in bounds.top.coerceAtLeast(0) until bounds.bottom.coerceAtMost(height)) {
        for (x in bounds.left.coerceAtLeast(0) until bounds.right.coerceAtMost(width)) {
            this[y * width + x] = color
        }
    }
}
