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
    Japanese,
    Unknown,
}

enum class OcrScanLanguage(
    val shortLabel: String,
    val displayLabel: String,
) {
    English("EN", "English"),
    Chinese("ZH", "Chinese"),
    Japanese("JA", "Japanese"),
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
    val bubbleRegion: BubbleRegionCandidate? = null,
)

data class AcceptedBubbleText(
    val originalText: String,
    val bounds: BubbleBounds,
    val sourceLanguage: SourceLanguage,
    val boundsSource: AcceptedBubbleBoundsSource = AcceptedBubbleBoundsSource.RawOcrTextBounds,
    val containingBounds: BubbleBounds? = null,
)

data class BubbleRegionCandidate(
    val bounds: BubbleBounds,
    val backgroundLuma: Double,
)

enum class AcceptedBubbleBoundsSource {
    RawOcrTextBounds,
    DetectedBubbleRegion,
}

data class TextRenderPlan(
    val text: String,
    val bounds: BubbleBounds,
    val lines: List<String>,
    val fontSizePx: Int,
    val estimatedWidthPx: Int,
    val estimatedHeightPx: Int,
    val patchBounds: BubbleBounds = bounds,
)
