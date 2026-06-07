package com.bublit.app

import com.bublit.app.domain.OcrScanLanguage
import com.bublit.app.pipeline.ProcessedImage
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InlineImageProcessingTest {
    @Test
    fun inlineImageProcessingFailureReturnsFailureResult() = runBlocking {
        val result = processInlineImageTranslation(
            imageUrl = "https://example.com/page.jpg",
            preferredLanguage = OcrScanLanguage.English,
            processor = { _, _ -> throw IllegalStateException("download failed") },
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun inlineImageProcessingCancellationPropagatesInsteadOfFailureResult() = runBlocking {
        val result = runCatching {
            processInlineImageTranslation(
                imageUrl = "https://example.com/page.jpg",
                preferredLanguage = OcrScanLanguage.English,
                processor = { _, _ -> throw CancellationException("stale image") },
            )
        }

        assertTrue(result.exceptionOrNull() is CancellationException)
    }

    @Test
    fun inlineImageProcessingSuccessReturnsProcessedImage() = runBlocking {
        val expected = ProcessedImage(
            imageUrl = "https://example.com/page.jpg",
            renderedImageUri = "data:image/png;base64,AQID",
            acceptedBlocks = 1,
            rejectedBlocks = 2,
        )

        val result = processInlineImageTranslation(
            imageUrl = "https://example.com/page.jpg",
            preferredLanguage = OcrScanLanguage.English,
            processor = { _, _ -> expected },
        )

        assertEquals(expected, result.getOrThrow())
    }
}
