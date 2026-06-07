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

    @Test
    fun refreshCurrentPageRequeuesAlreadyTranslatedImagesForReanalysis() {
        val state = InlineImageTranslationState().discoverImages(
            enabled = true,
            candidates = listOf(
                ImageCandidate("https://example.com/page-1.jpg", 1080, 1600, top = 120),
            ),
        ).complete(
            imageUrl = "https://example.com/page-1.jpg",
            translatedImageUri = "data:image/png;base64,AQID",
            acceptedBlocks = 2,
            rejectedBlocks = 3,
        )

        val refreshed = state.refreshCurrentPage()

        assertEquals(listOf("https://example.com/page-1.jpg"), refreshed.pendingImageUrls)
        assertEquals(emptyMap<String, String>(), refreshed.translatedImageUris)
        assertEquals(0, refreshed.debugSummary.acceptedBlocks)
        assertEquals(0, refreshed.debugSummary.rejectedBlocks)
    }

    @Test
    fun refreshCurrentPageWithoutImageCandidatesKeepsQueueEmpty() {
        val refreshed = InlineImageTranslationState().refreshCurrentPage()

        assertEquals(emptyList<String>(), refreshed.pendingImageUrls)
        assertEquals(emptyMap<String, String>(), refreshed.translatedImageUris)
    }

    @Test
    fun completedImageStoresDebugTranslationCounts() {
        val state = InlineImageTranslationState().discoverImages(
            enabled = true,
            candidates = listOf(
                ImageCandidate("https://example.com/page-1.jpg", 1080, 1600, top = 120),
            ),
        ).complete(
            imageUrl = "https://example.com/page-1.jpg",
            translatedImageUri = "data:image/png;base64,AQID",
            acceptedBlocks = 1,
            rejectedBlocks = 23,
        )

        val summary = state.debugSummary

        assertEquals(true, summary.hasResults)
        assertEquals(1, summary.translatedImages)
        assertEquals(1, summary.acceptedBlocks)
        assertEquals(23, summary.rejectedBlocks)
    }

    @Test
    fun debugSummaryIsOnlyVisibleInDebugBuilds() {
        val summary = ImageTranslationDebugSummary(
            translatedImages = 1,
            failedImages = 0,
            acceptedBlocks = 1,
            rejectedBlocks = 23,
        )

        assertEquals(true, summary.isVisibleInBuild(isDebugBuild = true))
        assertEquals(false, summary.isVisibleInBuild(isDebugBuild = false))
    }
}
