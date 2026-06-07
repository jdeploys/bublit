package com.bublit.app.session

import com.bublit.app.domain.ImageCandidate

data class InlineImageTranslationState(
    val candidates: List<ImageCandidate> = emptyList(),
    val pendingImageUrls: List<String> = emptyList(),
    val translatedImageUris: Map<String, String> = emptyMap(),
    val failedImageUrls: Set<String> = emptySet(),
) {
    val progress: InlineImageTranslationProgress
        get() {
            val totalCount = candidates
                .mapNotNull { candidate -> candidate.url.takeIf { it.isNotBlank() } }
                .distinct()
                .size
            val completedCount = translatedImageUris.size + failedImageUrls.size
            val isVisible = totalCount > 0 && completedCount < totalCount
            return InlineImageTranslationProgress(
                isVisible = isVisible,
                totalCount = totalCount,
                completedCount = completedCount,
                fraction = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat(),
            )
        }

    fun discoverImages(enabled: Boolean, candidates: List<ImageCandidate>): InlineImageTranslationState {
        if (!enabled) return clear()

        val urls = candidates
            .mapNotNull { candidate -> candidate.url.takeIf { it.isNotBlank() } }
            .distinct()
        return copy(
            candidates = candidates,
            pendingImageUrls = urls.filterNot { url ->
                translatedImageUris.containsKey(url) || failedImageUrls.contains(url)
            },
        )
    }

    fun markProcessing(imageUrl: String): InlineImageTranslationState {
        return copy(pendingImageUrls = pendingImageUrls - imageUrl)
    }

    fun complete(imageUrl: String, translatedImageUri: String): InlineImageTranslationState {
        return copy(
            pendingImageUrls = pendingImageUrls - imageUrl,
            translatedImageUris = translatedImageUris + (imageUrl to translatedImageUri),
            failedImageUrls = failedImageUrls - imageUrl,
        )
    }

    fun fail(imageUrl: String): InlineImageTranslationState {
        return copy(
            pendingImageUrls = pendingImageUrls - imageUrl,
            failedImageUrls = failedImageUrls + imageUrl,
        )
    }

    fun clear(): InlineImageTranslationState = InlineImageTranslationState()
}

data class InlineImageTranslationProgress(
    val isVisible: Boolean,
    val totalCount: Int,
    val completedCount: Int,
    val fraction: Float,
)
