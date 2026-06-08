package com.bublit.app.pipeline

import com.bublit.app.domain.OcrScanLanguage
import com.bublit.app.domain.BubbleBounds
import com.bublit.app.domain.BubbleRegionCandidate
import com.bublit.app.domain.OcrTextBlock
import org.junit.Assert.assertEquals
import org.junit.Test

class OcrScanLanguageTest {
    @Test
    fun japanesePreferenceScansJapaneseBeforeOtherLanguages() {
        val order = ocrScanOrder(OcrScanLanguage.Japanese)

        assertEquals(
            listOf(OcrScanLanguage.Japanese, OcrScanLanguage.English, OcrScanLanguage.Chinese),
            order,
        )
    }

    @Test
    fun englishPreferenceKeepsEnglishBeforeOtherLanguages() {
        val order = ocrScanOrder(OcrScanLanguage.English)

        assertEquals(
            listOf(OcrScanLanguage.English, OcrScanLanguage.Chinese, OcrScanLanguage.Japanese),
            order,
        )
    }

    @Test
    fun bestOcrBlocksPreferLanguageWithMoreAcceptedSpeechBubbles() {
        val englishBlocks = listOf(
            ocrBlock(
                text = "Kt%%,",
                bounds = BubbleBounds(left = 187, top = 86, width = 19, height = 108),
                bubbleRegion = BubbleBounds(left = 130, top = 51, width = 98, height = 209),
            ),
        )
        val japaneseBlocks = listOf(
            ocrBlock(
                text = "大丈夫ウサ!",
                bounds = BubbleBounds(left = 98, top = 85, width = 22, height = 90),
                bubbleRegion = BubbleBounds(left = 77, top = 51, width = 151, height = 209),
            ),
            ocrBlock(
                text = "変身すれば",
                bounds = BubbleBounds(left = 124, top = 85, width = 22, height = 110),
                bubbleRegion = BubbleBounds(left = 77, top = 51, width = 151, height = 209),
            ),
            ocrBlock(
                text = "そのステッキを",
                bounds = BubbleBounds(left = 602, top = 86, width = 20, height = 146),
                bubbleRegion = BubbleBounds(left = 579, top = 43, width = 185, height = 253),
            ),
        )

        val selected = selectBestOcrBlocks(listOf(englishBlocks, japaneseBlocks))

        assertEquals(japaneseBlocks, selected)
    }

    @Test
    fun bestOcrBlocksKeepPreferredWhenScoresTie() {
        val preferredBlocks = listOf(
            ocrBlock(
                text = "Where are we?",
                bounds = BubbleBounds(left = 20, top = 40, width = 180, height = 90),
            ),
        )
        val fallbackBlocks = listOf(
            ocrBlock(
                text = "どこ",
                bounds = BubbleBounds(left = 30, top = 50, width = 180, height = 90),
            ),
        )

        val selected = selectBestOcrBlocks(listOf(preferredBlocks, fallbackBlocks))

        assertEquals(preferredBlocks, selected)
    }

    private fun ocrBlock(
        text: String,
        bounds: BubbleBounds,
        bubbleRegion: BubbleBounds? = null,
    ): OcrTextBlock {
        return OcrTextBlock(
            text = text,
            bounds = bounds,
            backgroundLuma = 0.92,
            foregroundLuma = 0.08,
            confidence = 0.86,
            bubbleRegion = bubbleRegion?.let {
                BubbleRegionCandidate(bounds = it, backgroundLuma = 0.92)
            },
        )
    }
}
