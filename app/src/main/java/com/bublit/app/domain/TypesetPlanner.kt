package com.bublit.app.domain

import kotlin.math.ceil
import kotlin.math.floor

class TypesetPlanner(
    private val maxFontSizePx: Int = 24,
    private val minFontSizePx: Int = 10,
) {
    fun plan(bubble: AcceptedBubbleText, translatedText: String): TextRenderPlan {
        val normalizedText = translatedText.trim().ifBlank { bubble.originalText.trim() }
        val renderBounds = bubble.bounds.expandedForTypesetting()

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

    private data class TextMetrics(
        val width: Int,
        val height: Int,
    )

    private companion object {
        const val AverageGlyphWidthMultiplier = 0.58
        const val LineHeightMultiplier = 1.2
    }
}
