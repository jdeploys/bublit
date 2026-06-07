package com.bublit.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.bublit.app.BuildConfig
import com.bublit.app.domain.OcrScanLanguage
import com.bublit.app.pipeline.ImageProcessingService
import com.bublit.app.pipeline.ProcessedImage
import com.bublit.app.session.BrowserSessionState
import com.bublit.app.session.InlineImageTranslationState
import com.bublit.app.session.imageTranslationStatusText
import com.bublit.app.ui.BrowserScreen
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun BublitApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageProcessingService = remember { ImageProcessingService() }
    val browserSessionStore = remember(context) {
        context.getSharedPreferences("browser_session", android.content.Context.MODE_PRIVATE)
    }
    val initialBrowserSession = remember(browserSessionStore) {
        BrowserSessionState.restore(
            savedPageUrl = browserSessionStore.getString(BROWSER_SESSION_PAGE_URL, null),
            savedAddressText = browserSessionStore.getString(BROWSER_SESSION_ADDRESS_TEXT, null),
        )
    }
    var urlInput by rememberSaveable { mutableStateOf(initialBrowserSession.addressText) }
    var loadedUrl by rememberSaveable { mutableStateOf(initialBrowserSession.pageUrl) }
    var extractionStatus by rememberSaveable { mutableStateOf("") }
    var inlineTranslationState by remember { mutableStateOf(InlineImageTranslationState()) }
    var isImageTranslationEnabled by rememberSaveable { mutableStateOf(false) }
    var preferredOcrLanguage by rememberSaveable { mutableStateOf(OcrScanLanguage.English) }

    LaunchedEffect(loadedUrl, urlInput) {
        browserSessionStore.edit()
            .putString(BROWSER_SESSION_PAGE_URL, loadedUrl)
            .putString(BROWSER_SESSION_ADDRESS_TEXT, urlInput)
            .apply()
    }

    val nextPendingImageUrl = inlineTranslationState.pendingImageUrls.firstOrNull()
    LaunchedEffect(nextPendingImageUrl) {
        val imageUrl = nextPendingImageUrl ?: return@LaunchedEffect
        val remainingCount = inlineTranslationState.pendingImageUrls.size
        extractionStatus = "Translating image 1/$remainingCount..."
        val processed = processInlineImageTranslation(
            imageUrl = imageUrl,
            preferredLanguage = preferredOcrLanguage,
            processor = imageProcessingService::process,
        )

        inlineTranslationState = processed.fold(
            onSuccess = { result ->
                inlineTranslationState.complete(
                    imageUrl = imageUrl,
                    translatedImageUri = result.renderedImageUri,
                    acceptedBlocks = result.acceptedBlocks,
                    rejectedBlocks = result.rejectedBlocks,
                )
            },
            onFailure = { inlineTranslationState.fail(imageUrl) },
        )

        val translatedCount = inlineTranslationState.translatedImageUris.size
        val failedCount = inlineTranslationState.failedImageUrls.size
        extractionStatus = imageTranslationStatusText(translatedCount, failedCount)
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
        inlineTranslationState = inlineTranslationState.clear()
        extractionStatus = "Loading page..."
    }

    Surface(modifier = modifier.fillMaxSize()) {
        BrowserScreen(
            activeUrl = loadedUrl,
            urlInput = urlInput,
            status = extractionStatus,
            isImageTranslationEnabled = isImageTranslationEnabled,
            imageCandidateCount = inlineTranslationState.candidates.size,
            translatedImageUris = inlineTranslationState.translatedImageUris,
            translationProgress = inlineTranslationState.progress,
            debugSummary = inlineTranslationState.debugSummary,
            isDebugBuild = BuildConfig.DEBUG,
            preferredOcrLanguage = preferredOcrLanguage,
            onUrlInputChange = { urlInput = it },
            onActiveUrlChange = { loadedUrl = it },
            onNavigate = ::navigateToUrl,
            onRefreshCurrentPage = {
                inlineTranslationState = if (isImageTranslationEnabled) {
                    inlineTranslationState.refreshCurrentPage()
                } else {
                    inlineTranslationState.clear()
                }
                extractionStatus = if (isImageTranslationEnabled) {
                    "Refreshing page and reanalyzing images..."
                } else {
                    "Refreshing page..."
                }
            },
            onImageTranslationEnabledChange = { enabled ->
                isImageTranslationEnabled = enabled
                if (!enabled) {
                    inlineTranslationState = inlineTranslationState.clear()
                }
            },
            onStatusChange = { extractionStatus = it },
            onPreferredOcrLanguageChange = { language ->
                preferredOcrLanguage = language
                if (isImageTranslationEnabled && inlineTranslationState.candidates.isNotEmpty()) {
                    inlineTranslationState = inlineTranslationState.restart()
                    extractionStatus = "Scanning ${language.shortLabel} first..."
                }
            },
            onImagesDiscovered = { candidates ->
                inlineTranslationState = inlineTranslationState.discoverImages(
                    enabled = isImageTranslationEnabled,
                    candidates = candidates,
                )
            },
        )
    }
}

private const val BROWSER_SESSION_PAGE_URL = "page_url"
private const val BROWSER_SESSION_ADDRESS_TEXT = "address_text"

internal suspend fun processInlineImageTranslation(
    imageUrl: String,
    preferredLanguage: OcrScanLanguage,
    processor: suspend (String, OcrScanLanguage) -> ProcessedImage,
): Result<ProcessedImage> {
    return try {
        Result.success(processor(imageUrl, preferredLanguage))
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }
}
