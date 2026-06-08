package com.bublit.app.session

import com.bublit.app.domain.ImageCandidate
import com.bublit.app.domain.SpeechBubbleRejectionReason

data class InlineImageTranslationState(
    val candidates: List<ImageCandidate> = emptyList(),
    val pendingImageUrls: List<String> = emptyList(),
    val translatedImageUris: Map<String, String> = emptyMap(),
    val failedImageUrls: Set<String> = emptySet(),
    val debugResults: Map<String, ImageTranslationDebugResult> = emptyMap(),
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

    val debugSummary: ImageTranslationDebugSummary
        get() {
            return ImageTranslationDebugSummary(
                translatedImages = debugResults.size,
                failedImages = failedImageUrls.size,
                acceptedBlocks = debugResults.values.sumOf { it.acceptedBlocks },
                rejectedBlocks = debugResults.values.sumOf { it.rejectedBlocks },
                rejectionReasonCounts = debugResults.values
                    .flatMap { it.rejectionReasonCounts.entries }
                    .groupingBy { it.key }
                    .fold(0) { total, entry -> total + entry.value },
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

    fun complete(
        imageUrl: String,
        translatedImageUri: String,
        acceptedBlocks: Int = 0,
        rejectedBlocks: Int = 0,
        rejectionReasonCounts: Map<SpeechBubbleRejectionReason, Int> = emptyMap(),
    ): InlineImageTranslationState {
        return copy(
            pendingImageUrls = pendingImageUrls - imageUrl,
            translatedImageUris = translatedImageUris + (imageUrl to translatedImageUri),
            failedImageUrls = failedImageUrls - imageUrl,
            debugResults = debugResults + (
                imageUrl to ImageTranslationDebugResult(
                    acceptedBlocks = acceptedBlocks,
                    rejectedBlocks = rejectedBlocks,
                    rejectionReasonCounts = rejectionReasonCounts,
                )
                ),
        )
    }

    fun fail(imageUrl: String): InlineImageTranslationState {
        return copy(
            pendingImageUrls = pendingImageUrls - imageUrl,
            failedImageUrls = failedImageUrls + imageUrl,
            debugResults = debugResults - imageUrl,
        )
    }

    fun restart(): InlineImageTranslationState {
        return copy(
            pendingImageUrls = candidates
                .mapNotNull { candidate -> candidate.url.takeIf { it.isNotBlank() } }
                .distinct(),
            translatedImageUris = emptyMap(),
            failedImageUrls = emptySet(),
            debugResults = emptyMap(),
        )
    }

    fun refreshCurrentPage(): InlineImageTranslationState = restart()

    fun clear(): InlineImageTranslationState = InlineImageTranslationState()
}

data class InlineImageTranslationProgress(
    val isVisible: Boolean,
    val totalCount: Int,
    val completedCount: Int,
    val fraction: Float,
)

data class ImageTranslationDebugResult(
    val acceptedBlocks: Int,
    val rejectedBlocks: Int,
    val rejectionReasonCounts: Map<SpeechBubbleRejectionReason, Int> = emptyMap(),
)

data class ImageTranslationDebugSummary(
    val translatedImages: Int,
    val failedImages: Int,
    val acceptedBlocks: Int,
    val rejectedBlocks: Int,
    val rejectionReasonCounts: Map<SpeechBubbleRejectionReason, Int> = emptyMap(),
) {
    val hasResults: Boolean
        get() = translatedImages > 0 || failedImages > 0

    fun isVisibleInBuild(isDebugBuild: Boolean): Boolean {
        return isDebugBuild && hasResults
    }

    fun rejectionReasonSummaryText(): String {
        val segments = SpeechBubbleRejectionReason.entries.mapNotNull { reason ->
            val count = rejectionReasonCounts[reason] ?: return@mapNotNull null
            if (count <= 0) return@mapNotNull null
            "${reason.displayLabel} ${count}개"
        }
        return if (segments.isEmpty()) {
            ""
        } else {
            "제외 사유: ${segments.joinToString(" / ")}"
        }
    }
}
