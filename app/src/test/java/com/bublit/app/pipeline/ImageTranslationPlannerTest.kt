package com.bublit.app.pipeline

import com.bublit.app.domain.BubbleBounds
import com.bublit.app.domain.OcrTextBlock
import com.bublit.app.domain.SourceLanguage
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
}
