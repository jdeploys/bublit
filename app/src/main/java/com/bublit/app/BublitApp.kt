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
import com.bublit.app.domain.ImageCandidate
import com.bublit.app.pipeline.ImageProcessingService
import com.bublit.app.session.BrowserSessionState
import com.bublit.app.ui.BrowserScreen
import com.bublit.app.ui.ImageProcessingStage
import com.bublit.app.ui.ReaderImageItem
import com.bublit.app.ui.ReaderMode
import com.bublit.app.ui.ReaderScreen

private enum class BublitScreen {
    Browser,
    Reader,
}

@Composable
fun BublitApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageProcessingService = remember(context) {
        ImageProcessingService(context.applicationContext)
    }
    val browserSessionStore = remember(context) {
        context.getSharedPreferences("browser_session", android.content.Context.MODE_PRIVATE)
    }
    val initialBrowserSession = remember(browserSessionStore) {
        BrowserSessionState.restore(
            savedPageUrl = browserSessionStore.getString(BROWSER_SESSION_PAGE_URL, null),
            savedAddressText = browserSessionStore.getString(BROWSER_SESSION_ADDRESS_TEXT, null),
        )
    }
    var currentScreen by rememberSaveable { mutableStateOf(BublitScreen.Browser) }
    var urlInput by rememberSaveable { mutableStateOf(initialBrowserSession.addressText) }
    var loadedUrl by rememberSaveable { mutableStateOf(initialBrowserSession.pageUrl) }
    var extractionStatus by rememberSaveable { mutableStateOf("") }
    var readerMode by rememberSaveable { mutableStateOf(ReaderMode.Continuous) }
    var showTranslated by rememberSaveable { mutableStateOf(true) }
    var focusedIndex by rememberSaveable { mutableIntStateOf(0) }
    var readerItems by remember { mutableStateOf(emptyList<ReaderImageItem>()) }
    var imageCandidates by remember { mutableStateOf(emptyList<ImageCandidate>()) }
    var isImageTranslationEnabled by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(loadedUrl, urlInput) {
        browserSessionStore.edit()
            .putString(BROWSER_SESSION_PAGE_URL, loadedUrl)
            .putString(BROWSER_SESSION_ADDRESS_TEXT, urlInput)
            .apply()
    }

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

    fun normalizeUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return loadedUrl
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    fun navigateToUrl() {
        val normalizedUrl = normalizeUrl(urlInput)
        loadedUrl = normalizedUrl
        imageCandidates = emptyList()
        extractionStatus = "Loading page..."
    }

    fun openReaderFromCandidates(candidates: List<ImageCandidate>) {
        if (candidates.isEmpty()) return
        readerItems = candidates.mapIndexed { index, candidate ->
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
        focusedIndex = 0
        currentScreen = BublitScreen.Reader
    }

    Surface(modifier = modifier.fillMaxSize()) {
        when (currentScreen) {
            BublitScreen.Browser -> BrowserScreen(
                activeUrl = loadedUrl,
                urlInput = urlInput,
                status = extractionStatus,
                isImageTranslationEnabled = isImageTranslationEnabled,
                imageCandidateCount = imageCandidates.size,
                onUrlInputChange = { urlInput = it },
                onActiveUrlChange = { loadedUrl = it },
                onNavigate = ::navigateToUrl,
                onImageTranslationEnabledChange = { enabled ->
                    isImageTranslationEnabled = enabled
                    if (!enabled) {
                        imageCandidates = emptyList()
                    }
                },
                onStatusChange = { extractionStatus = it },
                onImagesDiscovered = { candidates ->
                    imageCandidates = candidates
                    if (isImageTranslationEnabled && candidates.isNotEmpty()) {
                        openReaderFromCandidates(candidates)
                    }
                },
            )

            BublitScreen.Reader -> ReaderScreen(
                sourceUrl = loadedUrl.ifBlank { urlInput },
                extractionStatus = extractionStatus,
                readerMode = readerMode,
                showTranslated = showTranslated,
                items = readerItems,
                focusedIndex = focusedIndex,
                onBackClick = { currentScreen = BublitScreen.Browser },
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

private const val BROWSER_SESSION_PAGE_URL = "page_url"
private const val BROWSER_SESSION_ADDRESS_TEXT = "address_text"

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
