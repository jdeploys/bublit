package com.bublit.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.bublit.app.ui.HomeScreen
import com.bublit.app.ui.ImageProcessingStage
import com.bublit.app.ui.ReaderImageItem
import com.bublit.app.ui.ReaderMode
import com.bublit.app.ui.ReaderScreen

private enum class BublitScreen {
    Home,
    Reader,
}

@Composable
fun BublitApp(modifier: Modifier = Modifier) {
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

    fun loadSampleReader() {
        val normalizedUrl = urlInput.trim().ifBlank { "https://example.com/webtoon/episode-1" }
        loadedUrl = normalizedUrl
        readerItems = sampleReaderItems(normalizedUrl)
        focusedIndex = 0
        extractionStatus = "Sample extraction ready: 3 DOM image candidates queued with fake OCR and translation states."
        currentScreen = BublitScreen.Reader
    }

    Surface(modifier = modifier.fillMaxSize()) {
        when (currentScreen) {
            BublitScreen.Home -> HomeScreen(
                urlInput = urlInput,
                extractionStatus = extractionStatus,
                canOpenReader = readerItems.isNotEmpty(),
                onUrlInputChange = { urlInput = it },
                onLoadClick = ::loadSampleReader,
                onOpenReaderClick = { currentScreen = BublitScreen.Reader },
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
