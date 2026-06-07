package com.bublit.app.pipeline

import com.bublit.app.domain.BubbleBounds
import com.bublit.app.domain.OcrTextBlock
import kotlin.math.max
import kotlin.math.min

internal fun deduplicateOcrBlocks(blocks: List<OcrTextBlock>): List<OcrTextBlock> {
    val accepted = mutableListOf<OcrTextBlock>()

    blocks.forEach { block ->
        val duplicateIndex = accepted.indexOfFirst { existing ->
            existing.bounds.intersectionOverUnion(block.bounds) >= MIN_DUPLICATE_IOU &&
                existing.text.normalizedTextSimilarity(block.text) >= MIN_DUPLICATE_TEXT_SIMILARITY
        }

        if (duplicateIndex == -1) {
            accepted += block
        } else if (block.confidence > accepted[duplicateIndex].confidence) {
            accepted[duplicateIndex] = block
        }
    }

    return accepted
}

private const val MIN_DUPLICATE_IOU = 0.55
private const val MIN_DUPLICATE_TEXT_SIMILARITY = 0.82

private fun BubbleBounds.intersectionOverUnion(other: BubbleBounds): Double {
    val intersectionLeft = max(left, other.left)
    val intersectionTop = max(top, other.top)
    val intersectionRight = min(right, other.right)
    val intersectionBottom = min(bottom, other.bottom)
    val intersectionWidth = (intersectionRight - intersectionLeft).coerceAtLeast(0)
    val intersectionHeight = (intersectionBottom - intersectionTop).coerceAtLeast(0)
    val intersectionArea = intersectionWidth * intersectionHeight
    if (intersectionArea == 0) return 0.0

    val thisArea = width * height
    val otherArea = other.width * other.height
    val unionArea = thisArea + otherArea - intersectionArea
    return if (unionArea <= 0) 0.0 else intersectionArea.toDouble() / unionArea.toDouble()
}

private fun String.normalizedTextSimilarity(other: String): Double {
    val left = normalizeForOcrComparison()
    val right = other.normalizeForOcrComparison()
    if (left.isBlank() || right.isBlank()) return 0.0
    if (left == right) return 1.0

    val maxLength = max(left.length, right.length)
    if (maxLength == 0) return 1.0
    val distance = levenshteinDistance(left, right)
    return 1.0 - (distance.toDouble() / maxLength.toDouble())
}

private fun String.normalizeForOcrComparison(): String {
    return lowercase()
        .filter { char -> char.isLetterOrDigit() }
}

private fun levenshteinDistance(left: String, right: String): Int {
    if (left == right) return 0
    if (left.isEmpty()) return right.length
    if (right.isEmpty()) return left.length

    var previous = IntArray(right.length + 1) { it }
    var current = IntArray(right.length + 1)

    left.forEachIndexed { leftIndex, leftChar ->
        current[0] = leftIndex + 1
        right.forEachIndexed { rightIndex, rightChar ->
            val insertCost = current[rightIndex] + 1
            val deleteCost = previous[rightIndex + 1] + 1
            val replaceCost = previous[rightIndex] + if (leftChar == rightChar) 0 else 1
            current[rightIndex + 1] = minOf(insertCost, deleteCost, replaceCost)
        }
        val nextPrevious = previous
        previous = current
        current = nextPrevious
    }

    return previous[right.length]
}
