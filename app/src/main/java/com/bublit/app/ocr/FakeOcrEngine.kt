package com.bublit.app.ocr

import com.bublit.app.domain.BubbleBounds
import com.bublit.app.domain.OcrTextBlock

class FakeOcrEngine : OcrEngine {
    override fun recognizePreviewBlocks(): List<OcrTextBlock> = listOf(
        OcrTextBlock(
            text = "Where are we?",
            bounds = BubbleBounds(left = 24, top = 32, width = 180, height = 82),
            backgroundLuma = 0.94,
            foregroundLuma = 0.08,
            confidence = 0.91,
        ),
    )
}
