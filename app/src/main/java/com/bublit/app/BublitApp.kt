package com.bublit.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.bublit.app.pipeline.ImageProcessingService
import com.bublit.app.ui.HomeScreen
import com.bublit.app.ui.ImageProcessingStage
import com.bublit.app.ui.ReaderImageItem
import com.bublit.app.ui.ReaderMode
import com.bublit.app.ui.ReaderScreen
import com.bublit.app.ui.WebLoadingScreen

private enum class BublitScreen {
    Home,
    Loading,
    Reader,
}

@Composable
fun BublitApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageProcessingService = remember(context) {
        ImageProcessingService(context.applicationContext)
    }
    var currentScreen by rememberSaveable { mutableStateOf(BublitScreen.Home) }
    var urlInput by rememberSaveable { mutableStateOf("https://example.com/webtoon/episode-1") }
    var loadedUrl by rememberSaveable { mutableStateOf("") }
    var extractionStatus by rememberSaveable {
        mutableStateOf("Waiting for a URL. Sample processing will run locally for this preview.")
    }
    var readerMode by rememberSaveable { mutableStateOf(ReaderMode.Continuous) }
    var showTranslated by rememberSaveable { mutableStateOf(true) }
    var focusedIndex by rememberSaveable { mutableIntStateOf(0) }
    var readerItems by remember { mutableStateOf(emptyList<ReaderImageItem>()) }

    LaunchedEffect(currentScreen, loadedUrl) {
        if (currentScreen != BublitScreen.Reader) return@LaunchedEffect
        val processableItems = readerItems.filter { item ->
            !item.imageUrl.isNullOrBlank() &&
                item.translatedImageUri == null &&
                item.stage == ImageProcessingStage.Queued
        }

        processableItems.forEach { item ->
            readerItems = readerItems.updateItem(item.id) {
                it.copy(
                    stage = ImageProcessingStage.ExtractingText,
                    translatedCaption = "OCR 처리 중",
                )
            }
            val processed = runCatching {
                readerItems = readerItems.updateItem(item.id) {
                    it.copy(
                        stage = ImageProcessingStage.Translating,
                        translatedCaption = "로컬 번역 및 식자 생성 중",
                    )
                }
                imageProcessingService.process(requireNotNull(item.imageUrl))
            }

            readerItems = readerItems.updateItem(item.id) { current ->
                processed.fold(
                    onSuccess = { result ->
                        current.copy(
                            translatedImageUri = result.renderedImageUri,
                            translatedCaption = "식자 완료: ${result.acceptedBlocks}개 말풍선",
                            stage = ImageProcessingStage.Ready,
                            acceptedBlocks = result.acceptedBlocks,
                            rejectedBlocks = result.rejectedBlocks,
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            translatedCaption = "처리 실패: ${error.message ?: "unknown error"}",
                            stage = ImageProcessingStage.Failed,
                        )
                    },
                )
            }
        }
    }

    fun beginPageLoad() {
        val normalizedUrl = urlInput.trim().ifBlank { "https://example.com/webtoon/episode-1" }
        loadedUrl = normalizedUrl
        extractionStatus = "Loading page..."
        currentScreen = BublitScreen.Loading
    }

    Surface(modifier = modifier.fillMaxSize()) {
        when (currentScreen) {
            BublitScreen.Home -> HomeScreen(
                urlInput = urlInput,
                extractionStatus = extractionStatus,
                canOpenReader = readerItems.isNotEmpty(),
                onUrlInputChange = { urlInput = it },
                onLoadClick = ::beginPageLoad,
                onOpenReaderClick = { currentScreen = BublitScreen.Reader },
            )

            BublitScreen.Loading -> WebLoadingScreen(
                url = loadedUrl.ifBlank { urlInput },
                status = extractionStatus,
                onBackClick = { currentScreen = BublitScreen.Home },
                onStatusChange = { extractionStatus = it },
                onImagesExtracted = { candidates ->
                    readerItems = if (candidates.isEmpty()) {
                        sampleReaderItems(loadedUrl.ifBlank { urlInput })
                    } else {
                        candidates.mapIndexed { index, candidate ->
                            ReaderImageItem(
                                id = "dom-$index",
                                title = "Image ${index + 1}",
                                sourceUrl = candidate.url,
                                imageUrl = candidate.url,
                                originalCaption = "Detected speech bubble",
                                translatedCaption = "로컬 번역 준비 중",
                                stage = ImageProcessingStage.Queued,
                                paletteColor = samplePalette(index),
                            )
                        }
                    }
                    focusedIndex = 0
                    extractionStatus = if (candidates.isEmpty()) {
                        "No large DOM comic images found. Showing local sample reader."
                    } else {
                        "Extracted ${candidates.size} DOM image candidates."
                    }
                    currentScreen = BublitScreen.Reader
                },
            )

            BublitScreen.Reader -> ReaderScreen(
                sourceUrl = loadedUrl.ifBlank { urlInput },
                extractionStatus = extractionStatus,
                readerMode = readerMode,
                showTranslated = showTranslated,
                items = readerItems,
                focusedIndex = focusedIndex,
                onBackClick = { currentScreen = BublitScreen.Home },
                onReaderModeChange = { readerMode = it },
                onShowTranslatedChange = { showTranslated = it },
                onPreviousImageClick = {
                    focusedIndex = (focusedIndex - 1).coerceAtLeast(0)
                },
                onNextImageClick = {
                    focusedIndex = (focusedIndex + 1).coerceAtMost((readerItems.size - 1).coerceAtLeast(0))
                },
            )
        }
    }
}

private fun sampleReaderItems(sourceUrl: String): List<ReaderImageItem> = listOf(
    ReaderImageItem(
        id = "sample-hero",
        title = "Panel 1",
        sourceUrl = "$sourceUrl#image-1",
        originalCaption = "Where are we?",
        translatedCaption = "Where are we? (KR preview)",
        stage = ImageProcessingStage.Ready,
        paletteColor = 0xFF25686F,
    ),
    ReaderImageItem(
        id = "sample-market",
        title = "Panel 2",
        sourceUrl = "$sourceUrl#image-2",
        originalCaption = "Keep moving.",
        translatedCaption = "Keep moving. (KR preview)",
        stage = ImageProcessingStage.Translating,
        paletteColor = 0xFF7A5B2E,
    ),
    ReaderImageItem(
        id = "sample-rooftop",
        title = "Panel 3",
        sourceUrl = "$sourceUrl#image-3",
        originalCaption = "Almost there.",
        translatedCaption = "Almost there. (KR preview)",
        stage = ImageProcessingStage.ExtractingText,
        paletteColor = 0xFF7C5265,
    ),
)

private fun samplePalette(index: Int): Long {
    val colors = listOf(0xFF25686F, 0xFF7A5B2E, 0xFF7C5265, 0xFF4E6740)
    return colors[index % colors.size]
}

private fun List<ReaderImageItem>.updateItem(
    id: String,
    update: (ReaderImageItem) -> ReaderImageItem,
): List<ReaderImageItem> {
    return map { item -> if (item.id == id) update(item) else item }
}
