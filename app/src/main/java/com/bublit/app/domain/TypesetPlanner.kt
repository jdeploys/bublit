package com.bublit.app.domain

import kotlin.math.ceil
import kotlin.math.floor

class TypesetPlanner(
    private val maxFontSizePx: Int = 24,
    private val minFontSizePx: Int = 10,
) {
    fun plan(bubble: AcceptedBubbleText, translatedText: String): TextRenderPlan {
        val normalizedText = translatedText.trim().ifBlank { bubble.originalText.trim() }
        val layoutBounds = bubble.containingBounds?.let { containingBounds ->
            val patchBounds = containingBounds.patchAreaAround(bubble.bounds)
            BubbleLayoutBounds(
                textBounds = patchBounds.insetForText(),
                patchBounds = patchBounds,
            )
        } ?: bubble.bounds.expandedForTypesetting().let { bounds ->
            BubbleLayoutBounds(textBounds = bounds, patchBounds = bounds)
        }
        val renderBounds = layoutBounds.textBounds

        for (fontSize in maxFontSizePx downTo minFontSizePx) {
            val lines = wrapText(normalizedText, renderBounds.width, fontSize)
            val metrics = estimateMetrics(lines, fontSize)

            if (metrics.width <= renderBounds.width && metrics.height <= renderBounds.height) {
                return TextRenderPlan(
                    text = normalizedText,
                    bounds = renderBounds,
                    lines = lines,
                    fontSizePx = fontSize,
                    estimatedWidthPx = metrics.width,
                    estimatedHeightPx = metrics.height,
                    patchBounds = layoutBounds.patchBounds,
                )
            }
        }

        val lines = wrapText(normalizedText, renderBounds.width, minFontSizePx)
        val metrics = estimateMetrics(lines, minFontSizePx)
        return TextRenderPlan(
            text = normalizedText,
            bounds = renderBounds,
            lines = lines,
            fontSizePx = minFontSizePx,
            estimatedWidthPx = minOf(metrics.width, renderBounds.width),
            estimatedHeightPx = minOf(metrics.height, renderBounds.height),
            patchBounds = layoutBounds.patchBounds,
        )
    }

    private fun wrapText(text: String, maxWidthPx: Int, fontSizePx: Int): List<String> {
        val maxCharsPerLine = maxOf(1, floor(maxWidthPx / estimatedCharWidth(fontSizePx)).toInt())
        val lines = mutableListOf<String>()
        var current = ""

        for (token in text.toWrapTokens()) {
            current = when {
                current.isEmpty() -> token
                current.length + token.length <= maxCharsPerLine -> current + token
                else -> {
                    lines += current.trim()
                    token.trimStart()
                }
            }

            while (current.length > maxCharsPerLine) {
                lines += current.take(maxCharsPerLine).trim()
                current = current.drop(maxCharsPerLine).trimStart()
            }
        }

        if (current.isNotBlank()) {
            lines += current.trim()
        }

        return lines.ifEmpty { listOf(text) }
    }

    private fun String.toWrapTokens(): List<String> {
        val tokens = mutableListOf<String>()
        var pending = StringBuilder()

        for (char in this) {
            pending.append(char)
            if (char.isWhitespace()) {
                tokens += pending.toString()
                pending = StringBuilder()
            }
        }

        if (pending.isNotEmpty()) {
            tokens += pending.toString()
        }

        return tokens
    }

    private fun estimateMetrics(lines: List<String>, fontSizePx: Int): TextMetrics {
        val width = lines.maxOfOrNull { line ->
            ceil(line.length * estimatedCharWidth(fontSizePx)).toInt()
        } ?: 0
        val height = ceil(lines.size * fontSizePx * LineHeightMultiplier).toInt()

        return TextMetrics(width = width, height = height)
    }

    private fun estimatedCharWidth(fontSizePx: Int): Double {
        return fontSizePx * AverageGlyphWidthMultiplier
    }

    private fun BubbleBounds.expandedForTypesetting(): BubbleBounds {
        val horizontalPadding = maxOf(14, ceil(width * 0.28).toInt())
        val verticalPadding = maxOf(12, ceil(height * 0.55).toInt())
        return BubbleBounds(
            left = left - horizontalPadding,
            top = top - verticalPadding,
            width = width + horizontalPadding * 2,
            height = height + verticalPadding * 2,
        )
    }

    private fun BubbleBounds.clampedTo(container: BubbleBounds): BubbleBounds {
        val clampedLeft = left.coerceAtLeast(container.left)
        val clampedTop = top.coerceAtLeast(container.top)
        val clampedRight = right.coerceAtMost(container.right).coerceAtLeast(clampedLeft)
        val clampedBottom = bottom.coerceAtMost(container.bottom).coerceAtLeast(clampedTop)
        return BubbleBounds(
            left = clampedLeft,
            top = clampedTop,
            width = clampedRight - clampedLeft,
            height = clampedBottom - clampedTop,
        )
    }

    private fun BubbleBounds.patchAreaAround(seed: BubbleBounds): BubbleBounds {
        if (isReasonablePatchFor(seed)) return this

        val innerHorizontalInset = minOf(width / 6, 8).coerceAtLeast(0)
        val innerVerticalInset = minOf(height / 8, 10).coerceAtLeast(0)
        val inner = BubbleBounds(
            left = left + innerHorizontalInset,
            top = top + innerVerticalInset,
            width = (width - innerHorizontalInset * 2).coerceAtLeast(1),
            height = (height - innerVerticalInset * 2).coerceAtLeast(1),
        )

        val targetWidth = minOf(
            inner.width,
            maxOf(
                seed.width,
                72,
                ceil(seed.width * 2.4).toInt(),
                ceil(seed.height * 1.15).toInt(),
            ),
        )
        val targetHeight = minOf(
            inner.height,
            maxOf(
                seed.height,
                80,
                ceil(seed.height * 1.75).toInt(),
            ),
        )

        val seedCenterX = seed.left + seed.width / 2
        val seedCenterY = seed.top + seed.height / 2
        val preferredLeft = seedCenterX - targetWidth / 2
        val preferredTop = seedCenterY - targetHeight / 2
        val left = preferredLeft.coerceIn(inner.left, (inner.right - targetWidth).coerceAtLeast(inner.left))
        val top = preferredTop.coerceIn(inner.top, (inner.bottom - targetHeight).coerceAtLeast(inner.top))

        return BubbleBounds(
            left = left,
            top = top,
            width = targetWidth,
            height = targetHeight,
        ).expandedToInclude(seed).clampedTo(inner)
    }

    private fun BubbleBounds.insetForText(): BubbleBounds {
        val horizontalInset = minOf(width / 8, 12).coerceAtLeast(6)
        val verticalInset = minOf(height / 6, 14).coerceAtLeast(6)
        return BubbleBounds(
            left = left + horizontalInset,
            top = top + verticalInset,
            width = (width - horizontalInset * 2).coerceAtLeast(1),
            height = (height - verticalInset * 2).coerceAtLeast(1),
        )
    }

    private fun BubbleBounds.isReasonablePatchFor(seed: BubbleBounds): Boolean {
        val seedArea = seed.width.coerceAtLeast(1) * seed.height.coerceAtLeast(1)
        val area = width.coerceAtLeast(1) * height.coerceAtLeast(1)
        return area <= seedArea * 10 &&
            width <= maxOf(96, seed.height * 2) &&
            height <= maxOf(180, seed.height * 2)
    }

    private fun BubbleBounds.expandedToInclude(other: BubbleBounds): BubbleBounds {
        val expandedLeft = minOf(left, other.left)
        val expandedTop = minOf(top, other.top)
        val expandedRight = maxOf(right, other.right)
        val expandedBottom = maxOf(bottom, other.bottom)
        return BubbleBounds(
            left = expandedLeft,
            top = expandedTop,
            width = expandedRight - expandedLeft,
            height = expandedBottom - expandedTop,
        )
    }

    private data class TextMetrics(
        val width: Int,
        val height: Int,
    )

    private data class BubbleLayoutBounds(
        val textBounds: BubbleBounds,
        val patchBounds: BubbleBounds,
    )

    private companion object {
        const val AverageGlyphWidthMultiplier = 0.58
        const val LineHeightMultiplier = 1.2
    }
}
