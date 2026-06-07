package com.bublit.app.session

import com.bublit.app.domain.ImageCandidate
import org.junit.Assert.assertEquals
import org.junit.Test

class InlineImageTranslationStateTest {
    @Test
    fun enabledImageDiscoveryQueuesInlineTranslationWithoutSeparateReaderScreen() {
        val state = InlineImageTranslationState()

        val next = state.discoverImages(
            enabled = true,
            candidates = listOf(
                ImageCandidate(
                    url = "https://example.com/page-1.jpg",
                    width = 1080,
                    height = 1600,
                    top = 120,
                ),
            ),
        )

        assertEquals(listOf("https://example.com/page-1.jpg"), next.pendingImageUrls)
    }

    @Test
    fun disabledImageDiscoveryKeepsInlineTranslationQueueEmpty() {
        val state = InlineImageTranslationState()

        val next = state.discoverImages(
            enabled = false,
            candidates = listOf(
                ImageCandidate(
                    url = "https://example.com/page-1.jpg",
                    width = 1080,
                    height = 1600,
                    top = 120,
                ),
            ),
        )

        assertEquals(emptyList<String>(), next.pendingImageUrls)
    }

    @Test
    fun activeInlineTranslationExposesBottomProgress() {
        val state = InlineImageTranslationState().discoverImages(
            enabled = true,
            candidates = listOf(
                ImageCandidate("https://example.com/page-1.jpg", 1080, 1600, top = 120),
                ImageCandidate("https://example.com/page-2.jpg", 1080, 1600, top = 1800),
            ),
        ).complete(
            imageUrl = "https://example.com/page-1.jpg",
            translatedImageUri = "file:///cache/rendered/page-1.png",
        )

        val progress = state.progress

        assertEquals(true, progress.isVisible)
        assertEquals(2, progress.totalCount)
        assertEquals(1, progress.completedCount)
        assertEquals(0.5f, progress.fraction)
    }

    @Test
    fun clearedInlineTranslationHidesBottomProgress() {
        val state = InlineImageTranslationState().discoverImages(
            enabled = false,
            candidates = listOf(
                ImageCandidate("https://example.com/page-1.jpg", 1080, 1600, top = 120),
            ),
        )

        assertEquals(false, state.progress.isVisible)
    }

    @Test
    fun restartRequeuesCurrentCandidatesAndClearsPreviousTranslationResults() {
        val state = InlineImageTranslationState().discoverImages(
            enabled = true,
            candidates = listOf(
                ImageCandidate("https://example.com/page-1.jpg", 1080, 1600, top = 120),
            ),
        ).complete(
            imageUrl = "https://example.com/page-1.jpg",
            translatedImageUri = "data:image/png;base64,AQID",
        )

        val restarted = state.restart()

        assertEquals(listOf("https://example.com/page-1.jpg"), restarted.pendingImageUrls)
        assertEquals(emptyMap<String, String>(), restarted.translatedImageUris)
    }
}
