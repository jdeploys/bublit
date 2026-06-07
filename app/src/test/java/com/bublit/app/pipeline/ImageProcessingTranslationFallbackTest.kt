package com.bublit.app.pipeline

import com.bublit.app.domain.SourceLanguage
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageProcessingTranslationFallbackTest {
    @Test
    fun mlKitTranslationFailureUsesFakeTranslationFallback() = runBlocking {
        val translated = translateWithFallback(
            text = "hello",
            language = SourceLanguage.English,
            mlKitTranslator = { _, _ -> throw IllegalStateException("model unavailable") },
            fakeTranslator = { _, _ -> "fake preview" },
        )

        assertEquals("fake preview", translated)
    }

    @Test
    fun mlKitTranslationCancellationPropagatesInsteadOfUsingFakeTranslation() = runBlocking {
        val translated = runCatching {
            translateWithFallback(
                text = "hello",
                language = SourceLanguage.English,
                mlKitTranslator = { _, _ -> throw CancellationException("stale image") },
                fakeTranslator = { _, _ -> "fake preview" },
            )
        }

        assertTrue(translated.exceptionOrNull() is CancellationException)
    }
}
