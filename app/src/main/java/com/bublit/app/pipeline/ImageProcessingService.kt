package com.bublit.app.pipeline

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import com.bublit.app.domain.ScriptDetector
import com.bublit.app.domain.BubbleBounds
import com.bublit.app.domain.OcrTextBlock
import com.bublit.app.domain.SourceLanguage
import com.bublit.app.domain.SpeechBubbleClassifier
import com.bublit.app.domain.TypesetPlanner
import com.bublit.app.ocr.MlKitOcrEngine
import com.bublit.app.render.BitmapTypesetRenderer
import com.bublit.app.translation.FakeTranslationEngine
import com.bublit.app.translation.MlKitTranslationEngine
import com.google.mlkit.vision.text.Text
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageProcessingService(
    private val ocrEngine: MlKitOcrEngine = MlKitOcrEngine(),
    private val mlKitTranslationEngine: MlKitTranslationEngine = MlKitTranslationEngine(),
    private val fakeTranslationEngine: FakeTranslationEngine = FakeTranslationEngine(),
    private val renderer: BitmapTypesetRenderer = BitmapTypesetRenderer(),
    private val classifier: SpeechBubbleClassifier = SpeechBubbleClassifier(),
    private val scriptDetector: ScriptDetector = ScriptDetector(),
    private val typesetPlanner: TypesetPlanner = TypesetPlanner(),
) {
    suspend fun process(imageUrl: String): ProcessedImage = withContext(Dispatchers.IO) {
        val original = downloadBitmap(imageUrl)
        val ocrBlocks = recognizeBlocks(original)
        val plan = buildPlan(ocrBlocks)
        val rendered = renderer.render(original, plan)
        val renderedImageUri = encodeRenderedBitmap(rendered)

        ProcessedImage(
            imageUrl = imageUrl,
            renderedImageUri = renderedImageUri,
            acceptedBlocks = plan.blocks.size,
            rejectedBlocks = plan.rejectedBlocks,
        )
    }

    private fun downloadBitmap(imageUrl: String): Bitmap {
        val connection = URL(imageUrl).openConnection().apply {
            connectTimeout = 15_000
            readTimeout = 20_000
        }
        connection.getInputStream().use { input ->
            return requireNotNull(BitmapFactory.decodeStream(input)) {
                "Unable to decode image: $imageUrl"
            }
        }
    }

    private suspend fun recognizeBlocks(bitmap: Bitmap): List<OcrTextBlock> {
        val latin = runCatching { ocrEngine.recognizeLatin(bitmap).toOcrBlocks(bitmap) }.getOrDefault(emptyList())
        val chinese = runCatching { ocrEngine.recognizeChinese(bitmap).toOcrBlocks(bitmap) }.getOrDefault(emptyList())
        val japanese = runCatching { ocrEngine.recognizeJapanese(bitmap).toOcrBlocks(bitmap) }.getOrDefault(emptyList())
        return (latin + chinese + japanese).distinctBy { block ->
            "${block.text}:${block.bounds.left}:${block.bounds.top}:${block.bounds.width}:${block.bounds.height}"
        }
    }

    private suspend fun translateWithFallback(text: String, language: SourceLanguage): String {
        if (language == SourceLanguage.Unknown) {
            return fakeTranslationEngine.translate(text, language)
        }

        return runCatching {
            mlKitTranslationEngine.translate(text, language)
        }.getOrElse {
            fakeTranslationEngine.translate(text, language)
        }
    }

    private suspend fun buildPlan(blocks: List<OcrTextBlock>): ImageTranslationPlan {
        val accepted = classifier.acceptedBubbleTexts(blocks, scriptDetector)
        val typesetBlocks = accepted.map { bubble ->
            val translatedText = translateWithFallback(bubble.originalText, bubble.sourceLanguage)
            TypesetBlock(
                sourceText = bubble.originalText,
                translatedText = translatedText,
                sourceLanguage = bubble.sourceLanguage,
                renderPlan = typesetPlanner.plan(bubble, translatedText),
            )
        }

        return ImageTranslationPlan(
            blocks = typesetBlocks,
            rejectedBlocks = blocks.size - accepted.size,
        )
    }

    private fun encodeRenderedBitmap(bitmap: Bitmap): String {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        return pngBytesToDataUri(output.toByteArray())
    }

    private fun Text.toOcrBlocks(bitmap: Bitmap): List<OcrTextBlock> {
        return textBlocks.mapNotNull { block ->
            val rect = block.boundingBox ?: return@mapNotNull null
            if (rect.width() <= 0 || rect.height() <= 0) return@mapNotNull null
            val bounds = BubbleBounds(
                left = rect.left.coerceAtLeast(0),
                top = rect.top.coerceAtLeast(0),
                width = rect.width().coerceAtMost(bitmap.width),
                height = rect.height().coerceAtMost(bitmap.height),
            )
            OcrTextBlock(
                text = block.text,
                bounds = bounds,
                backgroundLuma = bitmap.averageLuma(rect),
                foregroundLuma = 0.08,
                confidence = 0.86,
            )
        }
    }

    private fun Bitmap.averageLuma(rect: Rect): Double {
        var total = 0.0
        var samples = 0
        val left = rect.left.coerceIn(0, width - 1)
        val top = rect.top.coerceIn(0, height - 1)
        val right = rect.right.coerceIn(left + 1, width)
        val bottom = rect.bottom.coerceIn(top + 1, height)
        val stepX = maxOf(1, (right - left) / 8)
        val stepY = maxOf(1, (bottom - top) / 8)

        var y = top
        while (y < bottom) {
            var x = left
            while (x < right) {
                val pixel = getPixel(x, y)
                val red = android.graphics.Color.red(pixel)
                val green = android.graphics.Color.green(pixel)
                val blue = android.graphics.Color.blue(pixel)
                total += ((0.299 * red) + (0.587 * green) + (0.114 * blue)) / 255.0
                samples++
                x += stepX
            }
            y += stepY
        }

        return if (samples == 0) 1.0 else total / samples
    }

}

internal fun pngBytesToDataUri(bytes: ByteArray): String {
    return "data:image/png;base64,${Base64.getEncoder().encodeToString(bytes)}"
}

data class ProcessedImage(
    val imageUrl: String,
    val renderedImageUri: String,
    val acceptedBlocks: Int,
    val rejectedBlocks: Int,
)
