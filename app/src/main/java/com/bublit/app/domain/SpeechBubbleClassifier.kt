package com.bublit.app.domain

class SpeechBubbleClassifier(
    private val minConfidence: Double = 0.5,
    private val minBackgroundLuma: Double = 0.58,
    private val minContrast: Double = 0.30,
    private val minWidthPx: Int = 32,
    private val minHeightPx: Int = 20,
) {
    fun isSpeechBubbleText(block: OcrTextBlock): Boolean {
        val contrast = block.backgroundLuma - block.foregroundLuma

        return block.text.isNotBlank() &&
            block.confidence >= minConfidence &&
            block.bounds.width >= minWidthPx &&
            block.bounds.height >= minHeightPx &&
            block.backgroundLuma >= minBackgroundLuma &&
            contrast >= minContrast
    }

    fun acceptedBubbleTexts(
        blocks: List<OcrTextBlock>,
        scriptDetector: ScriptDetector = ScriptDetector(),
    ): List<AcceptedBubbleText> {
        return blocks.filter(::isSpeechBubbleText)
            .map { block ->
                AcceptedBubbleText(
                    originalText = block.text.trim(),
                    bounds = block.bounds,
                    sourceLanguage = scriptDetector.detectSourceLanguage(block.text),
                )
            }
    }
}
