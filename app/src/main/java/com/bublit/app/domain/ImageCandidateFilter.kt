package com.bublit.app.domain

class ImageCandidateFilter(
    private val minComicWidthPx: Int = 480,
    private val minComicHeightPx: Int = 640,
    private val minComicAreaPx: Int = 300_000,
) {
    fun retainComicImages(candidates: List<ImageCandidate>): List<ImageCandidate> {
        val seenUrls = linkedSetOf<String>()

        return candidates.filter { candidate ->
            val normalizedUrl = candidate.url.normalizedForDeduplication()
            normalizedUrl.isNotBlank() &&
                seenUrls.add(normalizedUrl) &&
                candidate.hasComicImageDimensions()
        }
    }

    private fun ImageCandidate.hasComicImageDimensions(): Boolean {
        val effectiveWidth = maxOf(width, naturalWidth ?: 0)
        val effectiveHeight = maxOf(height, naturalHeight ?: 0)
        val effectiveArea = effectiveWidth * effectiveHeight

        return effectiveWidth >= minComicWidthPx &&
            effectiveHeight >= minComicHeightPx &&
            effectiveArea >= minComicAreaPx
    }

    private fun String.normalizedForDeduplication(): String {
        return trim()
            .substringBefore("#")
            .lowercase()
    }
}
