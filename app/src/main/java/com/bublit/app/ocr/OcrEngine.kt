package com.bublit.app.ocr

import com.bublit.app.domain.OcrTextBlock

interface OcrEngine {
    fun recognizePreviewBlocks(): List<OcrTextBlock>
}
