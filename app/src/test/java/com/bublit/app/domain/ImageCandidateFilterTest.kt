package com.bublit.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageCandidateFilterTest {
    private val filter = ImageCandidateFilter()

    @Test
    fun largeDomComicImagesAreRetained() {
        val candidates = listOf(
            ImageCandidate(
                url = "https://example.com/episode/001.jpg",
                width = 900,
                height = 1400,
                naturalWidth = 900,
                naturalHeight = 1400,
            ),
        )

        val filtered = filter.retainComicImages(candidates)

        assertEquals(candidates, filtered)
    }

    @Test
    fun smallUiImagesAndDuplicateUrlsAreRejected() {
        val firstPage = ImageCandidate(
            url = "https://example.com/episode/001.jpg",
            width = 900,
            height = 1300,
            naturalWidth = 900,
            naturalHeight = 1300,
        )
        val duplicateWithFragment = firstPage.copy(url = " https://example.com/episode/001.jpg#view ")
        val icon = ImageCandidate(
            url = "https://example.com/assets/logo.png",
            width = 64,
            height = 64,
            naturalWidth = 64,
            naturalHeight = 64,
        )

        val filtered = filter.retainComicImages(listOf(icon, firstPage, duplicateWithFragment))

        assertEquals(listOf(firstPage), filtered)
        assertTrue(filtered.none { it.url.contains("logo") })
    }
}
