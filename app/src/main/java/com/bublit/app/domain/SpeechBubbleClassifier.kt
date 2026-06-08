package com.bublit.app.domain

class SpeechBubbleClassifier(
    private val minConfidence: Double = 0.5,
    private val minBackgroundLuma: Double = 0.58,
    private val minContrast: Double = 0.30,
    private val minWidthPx: Int = 32,
    private val minHeightPx: Int = 20,
) {
    fun isSpeechBubbleText(block: OcrTextBlock): Boolean {
        return classify(block).accepted
    }

    fun classify(block: OcrTextBlock): SpeechBubbleClassification {
        val classificationBounds = block.bubbleRegion?.bounds ?: block.bounds
        val backgroundLuma = block.bubbleRegion?.backgroundLuma ?: block.backgroundLuma
        val contrast = backgroundLuma - block.foregroundLuma
        val rejectionReasons = buildList {
            if (block.text.isBlank()) add(SpeechBubbleRejectionReason.BlankText)
            if (block.confidence < minConfidence) add(SpeechBubbleRejectionReason.LowConfidence)
            if (classificationBounds.width < minWidthPx) add(SpeechBubbleRejectionReason.TooNarrow)
            if (classificationBounds.height < minHeightPx) add(SpeechBubbleRejectionReason.TooShort)
            if (backgroundLuma < minBackgroundLuma) add(SpeechBubbleRejectionReason.DarkBackground)
            if (contrast < minContrast) add(SpeechBubbleRejectionReason.LowContrast)
        }

        return SpeechBubbleClassification(
            accepted = rejectionReasons.isEmpty(),
            rejectionReasons = rejectionReasons,
        )
    }

    fun acceptedBubbleTexts(
        blocks: List<OcrTextBlock>,
        scriptDetector: ScriptDetector = ScriptDetector(),
    ): List<AcceptedBubbleText> {
        return classifyBlocks(blocks, scriptDetector).acceptedBubbleTexts
    }

    fun classifyBlocks(
        blocks: List<OcrTextBlock>,
        scriptDetector: ScriptDetector = ScriptDetector(),
    ): SpeechBubbleClassificationResult {
        val acceptedGroups = mutableListOf<AcceptedBubbleGroup>()
        val reasonCounts = mutableMapOf<SpeechBubbleRejectionReason, Int>()
        var rejectedBlocks = 0

        blocks.forEach { block ->
            val classification = classify(block)
            if (classification.accepted) {
                val classificationBounds = block.bubbleRegion?.bounds ?: block.bounds
                val renderBounds = block.bounds
                val boundsSource = if (block.bubbleRegion == null) {
                        AcceptedBubbleBoundsSource.RawOcrTextBounds
                    } else {
                        AcceptedBubbleBoundsSource.DetectedBubbleRegion
                }
                val group = acceptedGroups.firstOrNull { existing ->
                    existing.boundsSource == boundsSource && (
                        existing.classificationBounds.iou(classificationBounds) >= SameBubbleRegionIoU ||
                            existing.renderBounds.expandedForGrouping().intersects(renderBounds)
                        )
                }

                if (group == null) {
                    acceptedGroups += AcceptedBubbleGroup(
                        entries = mutableListOf(AcceptedBubbleGroupEntry(block.text.trim(), renderBounds)),
                        classificationBounds = classificationBounds,
                        renderBounds = renderBounds,
                        boundsSource = boundsSource,
                    )
                } else {
                    group.entries += AcceptedBubbleGroupEntry(block.text.trim(), renderBounds)
                    group.classificationBounds = group.classificationBounds.union(classificationBounds)
                    group.renderBounds = group.renderBounds.union(renderBounds)
                }
            } else {
                rejectedBlocks++
                classification.rejectionReasons.forEach { reason ->
                    reasonCounts[reason] = reasonCounts.getOrDefault(reason, 0) + 1
                }
            }
        }

        return SpeechBubbleClassificationResult(
            acceptedBubbleTexts = acceptedGroups.map { group ->
                val mergedText = group.orderedTexts().filter { it.isNotBlank() }.joinToString("\n")
                AcceptedBubbleText(
                    originalText = mergedText,
                    bounds = group.renderBounds,
                    sourceLanguage = scriptDetector.detectSourceLanguage(mergedText),
                    boundsSource = group.boundsSource,
                    containingBounds = if (group.boundsSource == AcceptedBubbleBoundsSource.DetectedBubbleRegion) {
                        group.classificationBounds
                    } else {
                        null
                    },
                )
            },
            rejectedBlocks = rejectedBlocks,
            rejectionReasonCounts = reasonCounts,
        )
    }

    private data class AcceptedBubbleGroup(
        val entries: MutableList<AcceptedBubbleGroupEntry>,
        var classificationBounds: BubbleBounds,
        var renderBounds: BubbleBounds,
        val boundsSource: AcceptedBubbleBoundsSource,
    ) {
        fun orderedTexts(): List<String> {
            return if (isMostlyVertical()) {
                entries.sortedWith(
                    compareByDescending<AcceptedBubbleGroupEntry> { it.bounds.left }
                        .thenBy { it.bounds.top },
                ).map { it.text }
            } else {
                entries.sortedWith(
                    compareBy<AcceptedBubbleGroupEntry> { it.bounds.top }
                        .thenBy { it.bounds.left },
                ).map { it.text }
            }
        }

        private fun isMostlyVertical(): Boolean {
            if (entries.isEmpty()) return false
            val verticalCount = entries.count { entry -> entry.bounds.height > entry.bounds.width * 1.6 }
            return verticalCount * 2 >= entries.size
        }
    }

    private data class AcceptedBubbleGroupEntry(
        val text: String,
        val bounds: BubbleBounds,
    )

    private fun BubbleBounds.iou(other: BubbleBounds): Double {
        val intersectionLeft = maxOf(left, other.left)
        val intersectionTop = maxOf(top, other.top)
        val intersectionRight = minOf(right, other.right)
        val intersectionBottom = minOf(bottom, other.bottom)
        val intersectionWidth = (intersectionRight - intersectionLeft).coerceAtLeast(0)
        val intersectionHeight = (intersectionBottom - intersectionTop).coerceAtLeast(0)
        val intersectionArea = intersectionWidth * intersectionHeight
        val unionArea = area + other.area - intersectionArea
        return if (unionArea <= 0) 0.0 else intersectionArea.toDouble() / unionArea
    }

    private fun BubbleBounds.union(other: BubbleBounds): BubbleBounds {
        val unionLeft = minOf(left, other.left)
        val unionTop = minOf(top, other.top)
        val unionRight = maxOf(right, other.right)
        val unionBottom = maxOf(bottom, other.bottom)
        return BubbleBounds(
            left = unionLeft,
            top = unionTop,
            width = unionRight - unionLeft,
            height = unionBottom - unionTop,
        )
    }

    private fun BubbleBounds.expandedForGrouping(): BubbleBounds {
        val horizontalPadding = maxOf(18, width / 2)
        val verticalPadding = maxOf(24, height / 4)
        return BubbleBounds(
            left = left - horizontalPadding,
            top = top - verticalPadding,
            width = width + horizontalPadding * 2,
            height = height + verticalPadding * 2,
        )
    }

    private val BubbleBounds.area: Int
        get() = width.coerceAtLeast(0) * height.coerceAtLeast(0)

    private companion object {
        const val SameBubbleRegionIoU = 0.85
    }
}

private fun BubbleBounds.intersects(other: BubbleBounds): Boolean {
    return left < other.right &&
        right > other.left &&
        top < other.bottom &&
        bottom > other.top
}

data class SpeechBubbleClassification(
    val accepted: Boolean,
    val rejectionReasons: List<SpeechBubbleRejectionReason>,
)

data class SpeechBubbleClassificationResult(
    val acceptedBubbleTexts: List<AcceptedBubbleText>,
    val rejectedBlocks: Int,
    val rejectionReasonCounts: Map<SpeechBubbleRejectionReason, Int>,
)

enum class SpeechBubbleRejectionReason(
    val displayLabel: String,
) {
    DarkBackground("어두운 배경"),
    TooNarrow("너무 좁음"),
    TooShort("너무 낮음"),
    LowContrast("낮은 대비"),
    LowConfidence("낮은 OCR 신뢰도"),
    BlankText("빈 텍스트"),
}
