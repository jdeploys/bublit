package com.bublit.app.quality

import com.bublit.app.domain.BubbleBounds
import com.bublit.app.pipeline.ImageTranslationPlan

data class VisualQualityFixture(
    val name: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val expectedSpeechBubbles: List<BubbleBounds>,
    val protectedRegions: List<BubbleBounds> = emptyList(),
)

data class VisualQualityReport(
    val fixtureName: String,
    val passed: Boolean,
    val translatedBubbleCount: Int,
    val protectedPixelsChanged: Int,
    val protectedPixelChangeRatio: Double,
    val lowestBubbleIoU: Double,
    val issues: List<VisualQualityIssue>,
)

data class VisualQualityIssue(
    val id: VisualQualityIssueId,
    val message: String,
)

enum class VisualQualityIssueId {
    PixelBufferSizeMismatch,
    MissingExpectedBubble,
    UnexpectedTranslatedBubble,
    BubbleOverlapTooLow,
    BubblePixelsUnchanged,
    TextOverflow,
    ProtectedRegionChanged,
}

class VisualQualityVerifier(
    private val minBubbleIoU: Double = 0.50,
    private val minChangedBubblePixelRatio: Double = 0.01,
    private val maxProtectedPixelChangeRatio: Double = 0.0,
) {
    fun evaluate(
        fixture: VisualQualityFixture,
        plan: ImageTranslationPlan,
        sourcePixels: IntArray,
        renderedPixels: IntArray,
    ): VisualQualityReport {
        val expectedPixelCount = fixture.imageWidth * fixture.imageHeight
        val issues = mutableListOf<VisualQualityIssue>()

        if (sourcePixels.size != expectedPixelCount || renderedPixels.size != expectedPixelCount) {
            issues += VisualQualityIssue(
                id = VisualQualityIssueId.PixelBufferSizeMismatch,
                message = "Fixture ${fixture.name} expects $expectedPixelCount pixels.",
            )
            return VisualQualityReport(
                fixtureName = fixture.name,
                passed = false,
                translatedBubbleCount = plan.blocks.size,
                protectedPixelsChanged = 0,
                protectedPixelChangeRatio = 0.0,
                lowestBubbleIoU = 0.0,
                issues = issues,
            )
        }

        if (plan.blocks.size < fixture.expectedSpeechBubbles.size) {
            issues += VisualQualityIssue(
                id = VisualQualityIssueId.MissingExpectedBubble,
                message = "Expected ${fixture.expectedSpeechBubbles.size} bubbles but found ${plan.blocks.size}.",
            )
        }

        if (fixture.expectedSpeechBubbles.isEmpty() && plan.blocks.isNotEmpty()) {
            issues += VisualQualityIssue(
                id = VisualQualityIssueId.UnexpectedTranslatedBubble,
                message = "Expected no translated bubbles but found ${plan.blocks.size}.",
            )
        }

        val bubbleIoUs = fixture.expectedSpeechBubbles.map { expectedBubble ->
            val bestIoU = plan.blocks.maxOfOrNull { block ->
                expectedBubble.iou(block.renderPlan.patchBounds, fixture.imageWidth, fixture.imageHeight)
            } ?: 0.0

            if (bestIoU < minBubbleIoU) {
                issues += VisualQualityIssue(
                    id = VisualQualityIssueId.BubbleOverlapTooLow,
                    message = "Bubble overlap was $bestIoU for fixture ${fixture.name}.",
                )
            }

            val changedRatio = changedPixelRatio(
                bounds = expectedBubble,
                fixture = fixture,
                sourcePixels = sourcePixels,
                renderedPixels = renderedPixels,
            )
            if (changedRatio < minChangedBubblePixelRatio) {
                issues += VisualQualityIssue(
                    id = VisualQualityIssueId.BubblePixelsUnchanged,
                    message = "Expected bubble pixels were not visibly changed.",
                )
            }

            bestIoU
        }

        plan.blocks.forEach { block ->
            val renderPlan = block.renderPlan
            if (
                renderPlan.lines.isEmpty() ||
                renderPlan.estimatedWidthPx > renderPlan.bounds.width ||
                renderPlan.estimatedHeightPx > renderPlan.bounds.height
            ) {
                issues += VisualQualityIssue(
                    id = VisualQualityIssueId.TextOverflow,
                    message = "Translated text does not fit inside its render bounds.",
                )
            }
        }

        val protectedPixels = fixture.protectedRegions.sumOf { region ->
            region.clampedArea(fixture.imageWidth, fixture.imageHeight)
        }
        val protectedPixelsChanged = fixture.protectedRegions.sumOf { region ->
            changedPixelCount(
                bounds = region,
                fixture = fixture,
                sourcePixels = sourcePixels,
                renderedPixels = renderedPixels,
            )
        }
        val protectedPixelChangeRatio = if (protectedPixels == 0) {
            0.0
        } else {
            protectedPixelsChanged.toDouble() / protectedPixels
        }

        if (protectedPixelChangeRatio > maxProtectedPixelChangeRatio) {
            issues += VisualQualityIssue(
                id = VisualQualityIssueId.ProtectedRegionChanged,
                message = "Protected regions changed by $protectedPixelsChanged pixels.",
            )
        }

        return VisualQualityReport(
            fixtureName = fixture.name,
            passed = issues.isEmpty(),
            translatedBubbleCount = plan.blocks.size,
            protectedPixelsChanged = protectedPixelsChanged,
            protectedPixelChangeRatio = protectedPixelChangeRatio,
            lowestBubbleIoU = bubbleIoUs.minOrNull() ?: 1.0,
            issues = issues,
        )
    }

    private fun changedPixelRatio(
        bounds: BubbleBounds,
        fixture: VisualQualityFixture,
        sourcePixels: IntArray,
        renderedPixels: IntArray,
    ): Double {
        val area = bounds.clampedArea(fixture.imageWidth, fixture.imageHeight)
        if (area == 0) return 0.0
        return changedPixelCount(bounds, fixture, sourcePixels, renderedPixels).toDouble() / area
    }

    private fun changedPixelCount(
        bounds: BubbleBounds,
        fixture: VisualQualityFixture,
        sourcePixels: IntArray,
        renderedPixels: IntArray,
    ): Int {
        val left = bounds.left.coerceIn(0, fixture.imageWidth)
        val top = bounds.top.coerceIn(0, fixture.imageHeight)
        val right = bounds.right.coerceIn(left, fixture.imageWidth)
        val bottom = bounds.bottom.coerceIn(top, fixture.imageHeight)
        var changed = 0

        for (y in top until bottom) {
            for (x in left until right) {
                val index = y * fixture.imageWidth + x
                if (sourcePixels[index] != renderedPixels[index]) {
                    changed++
                }
            }
        }

        return changed
    }
}

private fun BubbleBounds.iou(other: BubbleBounds, imageWidth: Int, imageHeight: Int): Double {
    val first = clamped(imageWidth, imageHeight)
    val second = other.clamped(imageWidth, imageHeight)
    val intersectionLeft = maxOf(first.left, second.left)
    val intersectionTop = maxOf(first.top, second.top)
    val intersectionRight = minOf(first.right, second.right)
    val intersectionBottom = minOf(first.bottom, second.bottom)
    val intersectionWidth = (intersectionRight - intersectionLeft).coerceAtLeast(0)
    val intersectionHeight = (intersectionBottom - intersectionTop).coerceAtLeast(0)
    val intersectionArea = intersectionWidth * intersectionHeight
    val unionArea = first.area + second.area - intersectionArea

    return if (unionArea <= 0) 0.0 else intersectionArea.toDouble() / unionArea
}

private fun BubbleBounds.clampedArea(imageWidth: Int, imageHeight: Int): Int {
    return clamped(imageWidth, imageHeight).area
}

private fun BubbleBounds.clamped(imageWidth: Int, imageHeight: Int): BubbleBounds {
    val clampedLeft = left.coerceIn(0, imageWidth)
    val clampedTop = top.coerceIn(0, imageHeight)
    val clampedRight = right.coerceIn(clampedLeft, imageWidth)
    val clampedBottom = bottom.coerceIn(clampedTop, imageHeight)
    return BubbleBounds(
        left = clampedLeft,
        top = clampedTop,
        width = clampedRight - clampedLeft,
        height = clampedBottom - clampedTop,
    )
}

private val BubbleBounds.area: Int
    get() = width.coerceAtLeast(0) * height.coerceAtLeast(0)
