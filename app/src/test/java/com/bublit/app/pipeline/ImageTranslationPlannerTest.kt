package com.bublit.app.pipeline

import com.bublit.app.domain.BubbleBounds
import com.bublit.app.domain.BubbleRegionCandidate
import com.bublit.app.domain.OcrTextBlock
import com.bublit.app.domain.SourceLanguage
import com.bublit.app.domain.SpeechBubbleRejectionReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageTranslationPlannerTest {
    @Test
    fun brightSpeechBubbleBlocksBecomeTypesetBlocks() {
        val planner = ImageTranslationPlanner(
            translator = { text, sourceLanguage ->
                when (sourceLanguage) {
                    SourceLanguage.English -> "어디에 있는 거야?"
                    SourceLanguage.Chinese -> "가자"
                    SourceLanguage.Japanese -> "가자"
                    SourceLanguage.Unknown -> text
                }
            },
        )

        val result = planner.plan(
            blocks = listOf(
                OcrTextBlock(
                    text = "Where are we?",
                    bounds = BubbleBounds(left = 20, top = 40, width = 180, height = 90),
                    backgroundLuma = 0.95,
                    foregroundLuma = 0.08,
                    confidence = 0.92,
                ),
            ),
        )

        assertEquals(1, result.blocks.size)
        assertEquals("Where are we?", result.blocks.single().sourceText)
        assertEquals("어디에 있는 거야?", result.blocks.single().translatedText)
        assertEquals(SourceLanguage.English, result.blocks.single().sourceLanguage)
        assertTrue(result.blocks.single().renderPlan.fontSizePx > 0)
    }

    @Test
    fun backgroundTextIsRejectedFromTypesetBlocks() {
        val planner = ImageTranslationPlanner(
            translator = { text, _ -> "$text translated" },
        )

        val result = planner.plan(
            blocks = listOf(
                OcrTextBlock(
                    text = "BOOM",
                    bounds = BubbleBounds(left = 10, top = 12, width = 320, height = 64),
                    backgroundLuma = 0.32,
                    foregroundLuma = 0.1,
                    confidence = 0.95,
                ),
            ),
        )

        assertEquals(emptyList<TypesetBlock>(), result.blocks)
        assertEquals(1, result.rejectedBlocks)
    }

    @Test
    fun rejectedBackgroundTextIncludesDetailedRejectionReasons() {
        val planner = ImageTranslationPlanner(
            translator = { text, _ -> "$text translated" },
        )

        val result = planner.plan(
            blocks = listOf(
                OcrTextBlock(
                    text = "ドーン",
                    bounds = BubbleBounds(left = 10, top = 12, width = 24, height = 18),
                    backgroundLuma = 0.32,
                    foregroundLuma = 0.12,
                    confidence = 0.44,
                ),
            ),
        )

        assertEquals(1, result.rejectedBlocks)
        assertEquals(1, result.rejectionReasonCounts[SpeechBubbleRejectionReason.LowConfidence])
        assertEquals(1, result.rejectionReasonCounts[SpeechBubbleRejectionReason.TooNarrow])
        assertEquals(1, result.rejectionReasonCounts[SpeechBubbleRejectionReason.TooShort])
        assertEquals(1, result.rejectionReasonCounts[SpeechBubbleRejectionReason.DarkBackground])
        assertEquals(1, result.rejectionReasonCounts[SpeechBubbleRejectionReason.LowContrast])
    }

    @Test
    fun narrowJapaneseTextUsesDetectedBrightBubbleRegionForClassificationButTextBoundsForRendering() {
        val planner = ImageTranslationPlanner(
            translator = { _, _ -> "너무 부끄럽잖아" },
        )
        val bubbleRegion = BubbleRegionCandidate(
            bounds = BubbleBounds(left = 108, top = 54, width = 96, height = 174),
            backgroundLuma = 0.91,
        )

        val result = planner.plan(
            blocks = listOf(
                OcrTextBlock(
                    text = "恥ずかしすぎるだろー",
                    bounds = BubbleBounds(left = 142, top = 88, width = 24, height = 112),
                    backgroundLuma = 0.37,
                    foregroundLuma = 0.08,
                    confidence = 0.86,
                    bubbleRegion = bubbleRegion,
                ),
            ),
        )

        assertEquals(1, result.blocks.size)
        assertTrue(result.blocks.single().renderPlan.bounds.width < bubbleRegion.bounds.width)
        assertTrue(result.blocks.single().renderPlan.bounds.height <= bubbleRegion.bounds.height)
        assertEquals(emptyMap<SpeechBubbleRejectionReason, Int>(), result.rejectionReasonCounts)
    }

    @Test
    fun multipleOcrLinesInSameDetectedBubbleBecomeSingleTypesetBlock() {
        val planner = ImageTranslationPlanner(
            translator = { text, _ -> "merged: $text" },
        )
        val bubbleRegion = BubbleRegionCandidate(
            bounds = BubbleBounds(left = 40, top = 30, width = 120, height = 150),
            backgroundLuma = 0.92,
        )

        val result = planner.plan(
            blocks = listOf(
                OcrTextBlock(
                    text = "大丈夫ウサ！",
                    bounds = BubbleBounds(left = 74, top = 52, width = 20, height = 48),
                    backgroundLuma = 0.34,
                    foregroundLuma = 0.08,
                    confidence = 0.86,
                    bubbleRegion = bubbleRegion,
                ),
                OcrTextBlock(
                    text = "変身すれば",
                    bounds = BubbleBounds(left = 98, top = 52, width = 20, height = 72),
                    backgroundLuma = 0.35,
                    foregroundLuma = 0.08,
                    confidence = 0.86,
                    bubbleRegion = bubbleRegion,
                ),
            ),
        )

        assertEquals(1, result.blocks.size)
        assertEquals("変身すれば\n大丈夫ウサ！", result.blocks.single().sourceText)
    }
}
