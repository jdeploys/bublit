package com.bublit.app.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.bublit.app.pipeline.ImageTranslationPlan

class BitmapTypesetRenderer {
    fun render(source: Bitmap, plan: ImageTranslationPlan): Bitmap {
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        plan.blocks.forEach { block ->
            val rect = block.renderPlan.bounds.toClampedRect(output.width, output.height)
            val fillColor = sampleAverageColor(output, rect)
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = fillColor
                style = Paint.Style.FILL
            }
            canvas.drawRect(rect, fillPaint)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(20, 20, 20)
                textAlign = Paint.Align.CENTER
                textSize = block.renderPlan.fontSizePx.toFloat()
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            }

            val lineHeight = block.renderPlan.fontSizePx * 1.22f
            val totalHeight = lineHeight * block.renderPlan.lines.size
            var baseline = rect.centerY() - totalHeight / 2f - textPaint.fontMetrics.ascent

            block.renderPlan.lines.forEach { line ->
                canvas.drawText(line, rect.centerX().toFloat(), baseline, textPaint)
                baseline += lineHeight
            }
        }

        return output
    }

    private fun com.bublit.app.domain.BubbleBounds.toClampedRect(maxWidth: Int, maxHeight: Int): Rect {
        val padding = 6
        val left = (this.left - padding).coerceIn(0, maxWidth)
        val top = (this.top - padding).coerceIn(0, maxHeight)
        val right = (this.right + padding).coerceIn(left, maxWidth)
        val bottom = (this.bottom + padding).coerceIn(top, maxHeight)
        return Rect(left, top, right, bottom)
    }

    private fun sampleAverageColor(bitmap: Bitmap, rect: Rect): Int {
        if (rect.width() <= 0 || rect.height() <= 0) return Color.WHITE

        var red = 0L
        var green = 0L
        var blue = 0L
        var samples = 0L
        val stepX = maxOf(1, rect.width() / 8)
        val stepY = maxOf(1, rect.height() / 8)

        var y = rect.top
        while (y < rect.bottom) {
            var x = rect.left
            while (x < rect.right) {
                val pixel = bitmap.getPixel(x, y)
                red += Color.red(pixel)
                green += Color.green(pixel)
                blue += Color.blue(pixel)
                samples++
                x += stepX
            }
            y += stepY
        }

        if (samples == 0L) return Color.WHITE
        return Color.rgb(
            (red / samples).toInt().coerceIn(0, 255),
            (green / samples).toInt().coerceIn(0, 255),
            (blue / samples).toInt().coerceIn(0, 255),
        )
    }
}
