package com.bublit.app.domain

data class ImageCandidate(
    val url: String,
    val width: Int,
    val height: Int,
    val naturalWidth: Int? = null,
    val naturalHeight: Int? = null,
    val left: Int = 0,
    val top: Int = 0,
)

enum class SourceLanguage {
    English,
    Chinese,
    Unknown,
}

data class BubbleBounds(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
) {
    val right: Int
        get() = left + width

    val bottom: Int
        get() = top + height
}

data class OcrTextBlock(
    val text: String,
    val bounds: BubbleBounds,
    val backgroundLuma: Double,
    val foregroundLuma: Double,
    val confidence: Double,
)

data class AcceptedBubbleText(
    val originalText: String,
    val bounds: BubbleBounds,
    val sourceLanguage: SourceLanguage,
)

data class TextRenderPlan(
    val text: String,
    val bounds: BubbleBounds,
    val lines: List<String>,
    val fontSizePx: Int,
    val estimatedWidthPx: Int,
    val estimatedHeightPx: Int,
)
