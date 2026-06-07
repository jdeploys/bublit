package com.bublit.app.domain

import kotlin.math.ceil
import kotlin.math.floor

class TypesetPlanner(
    private val maxFontSizePx: Int = 24,
    private val minFontSizePx: Int = 10,
) {
    fun plan(bubble: AcceptedBubbleText, translatedText: String): TextRenderPlan {
        val normalizedText = translatedText.trim().ifBlank { bubble.originalText.trim() }

        for (fontSize in maxFontSizePx downTo minFontSizePx) {
            val lines = wrapText(normalizedText, bubble.bounds.width, fontSize)
            val metrics = estimateMetrics(lines, fontSize)

            if (metrics.width <= bubble.bounds.width && metrics.height <= bubble.bounds.height) {
                return TextRenderPlan(
                    text = normalizedText,
                    bounds = bubble.bounds,
                    lines = lines,
                    fontSizePx = fontSize,
                    estimatedWidthPx = metrics.width,
                    estimatedHeightPx = metrics.height,
                )
            }
        }

        val lines = wrapText(normalizedText, bubble.bounds.width, minFontSizePx)
        val metrics = estimateMetrics(lines, minFontSizePx)
        return TextRenderPlan(
            text = normalizedText,
            bounds = bubble.bounds,
            lines = lines,
            fontSizePx = minFontSizePx,
            estimatedWidthPx = minOf(metrics.width, bubble.bounds.width),
            estimatedHeightPx = minOf(metrics.height, bubble.bounds.height),
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

    private data class TextMetrics(
        val width: Int,
        val height: Int,
    )

    private companion object {
        const val AverageGlyphWidthMultiplier = 0.58
        const val LineHeightMultiplier = 1.2
    }
}
