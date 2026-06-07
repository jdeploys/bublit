package com.bublit.app.ui

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bublit.app.R
import com.bublit.app.domain.ImageCandidate
import com.bublit.app.domain.OcrScanLanguage
import com.bublit.app.session.ImageTranslationDebugSummary
import com.bublit.app.session.InlineImageTranslationProgress
import com.bublit.app.web.BrowserWebView

@Composable
fun BrowserScreen(
    activeUrl: String,
    urlInput: String,
    status: String,
    isImageTranslationEnabled: Boolean,
    imageCandidateCount: Int,
    translatedImageUris: Map<String, String>,
    translationProgress: InlineImageTranslationProgress,
    debugSummary: ImageTranslationDebugSummary,
    isDebugBuild: Boolean,
    preferredOcrLanguage: OcrScanLanguage,
    onUrlInputChange: (String) -> Unit,
    onActiveUrlChange: (String) -> Unit,
    onNavigate: () -> Unit,
    onImageTranslationEnabledChange: (Boolean) -> Unit,
    onStatusChange: (String) -> Unit,
    onPreferredOcrLanguageChange: (OcrScanLanguage) -> Unit,
    onImagesDiscovered: (List<ImageCandidate>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var imageScanRequestId by remember { mutableIntStateOf(0) }
    var showDebugSummary by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            BrowserTopBar(
                urlInput = urlInput,
                status = status,
                isLoading = isLoading,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                isImageTranslationEnabled = isImageTranslationEnabled,
                imageCandidateCount = imageCandidateCount,
                preferredOcrLanguage = preferredOcrLanguage,
                onUrlInputChange = onUrlInputChange,
                onBackClick = { webView?.goBack() },
                onForwardClick = { webView?.goForward() },
                onNavigate = {
                    focusManager.clearFocus()
                    onNavigate()
                },
                onToggleImageTranslation = {
                    val nextEnabled = !isImageTranslationEnabled
                    onImageTranslationEnabledChange(nextEnabled)
                    if (nextEnabled) {
                        imageScanRequestId += 1
                        onStatusChange("Scanning images...")
                    } else {
                        onImagesDiscovered(emptyList())
                        onStatusChange("")
                    }
                },
                onPreferredOcrLanguageChange = onPreferredOcrLanguageChange,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            BrowserWebView(
                url = activeUrl,
                imageTranslationEnabled = isImageTranslationEnabled,
                imageScanRequestId = imageScanRequestId,
                translatedImageUris = translatedImageUris,
                modifier = Modifier.fillMaxSize(),
                onWebViewReady = { webView = it },
                onPageStarted = { url ->
                    isLoading = true
                    onActiveUrlChange(url)
                    onUrlInputChange(url)
                    onStatusChange("Loading...")
                },
                onPageFinished = { url, candidates, backAvailable, forwardAvailable, didScan ->
                    isLoading = false
                    onActiveUrlChange(url)
                    onUrlInputChange(url)
                    canGoBack = backAvailable
                    canGoForward = forwardAvailable
                    if (didScan) {
                        onImagesDiscovered(candidates)
                        onStatusChange(
                            if (candidates.isEmpty()) {
                                "No large images found"
                            } else {
                                "${candidates.size} images found"
                            },
                        )
                    } else {
                        onImagesDiscovered(emptyList())
                        onStatusChange("")
                    }
                },
            )
            if (translationProgress.isVisible) {
                LinearProgressIndicator(
                    progress = { translationProgress.fraction },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp),
                    color = Color(0xFF25686F),
                    trackColor = Color(0x3325686F),
                )
            }
            if (debugSummary.isVisibleInBuild(isDebugBuild)) {
                TranslationDebugButton(
                    summary = debugSummary,
                    expanded = showDebugSummary,
                    onClick = { showDebugSummary = !showDebugSummary },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun TranslationDebugButton(
    summary: ImageTranslationDebugSummary,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (expanded) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xE612171C),
                contentColor = Color.White,
                shadowElevation = 4.dp,
            ) {
                Text(
                    text = "이미지: 번역 ${summary.translatedImages}개 / 실패 ${summary.failedImages}개\n" +
                        "OCR: 말풍선 ${summary.acceptedBlocks}개 / 제외 ${summary.rejectedBlocks}개",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }
        TextButton(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.textButtonColors(
                containerColor = Color(0xCC12171C),
                contentColor = Color.White,
            ),
            modifier = Modifier.height(32.dp),
            contentPadding = ButtonDefaults.TextButtonContentPadding,
        ) {
            Text(
                text = "DEBUG",
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun BrowserTopBar(
    urlInput: String,
    status: String,
    isLoading: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isImageTranslationEnabled: Boolean,
    imageCandidateCount: Int,
    preferredOcrLanguage: OcrScanLanguage,
    onUrlInputChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onNavigate: () -> Unit,
    onToggleImageTranslation: () -> Unit,
    onPreferredOcrLanguageChange: (OcrScanLanguage) -> Unit,
) {
    var isAddressFocused by remember { mutableStateOf(false) }
    var isLanguageSheetVisible by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFFF7F9FB),
        contentColor = Color(0xFF101418),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(start = 6.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                if (!isAddressFocused) {
                    BrowserGlyphButton(
                        label = "‹",
                        enabled = canGoBack,
                        onClick = onBackClick,
                    )
                    BrowserGlyphButton(
                        label = "›",
                        enabled = canGoForward,
                        onClick = onForwardClick,
                    )
                }
                AddressField(
                    value = urlInput,
                    onValueChange = onUrlInputChange,
                    onNavigate = onNavigate,
                    onFocusChanged = { isAddressFocused = it },
                    modifier = Modifier.weight(1f),
                )
                if (!isAddressFocused) {
                    IconButton(
                        onClick = onToggleImageTranslation,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_image_translate_bubble),
                            contentDescription = if (isImageTranslationEnabled) {
                                "Disable image translation"
                            } else {
                                "Enable image translation"
                            },
                            tint = if (isImageTranslationEnabled) {
                                Color(0xFF25686F)
                            } else {
                                Color(0xFF65727B)
                            },
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            if ((status.isNotBlank() || isImageTranslationEnabled) && !isAddressFocused) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 96.dp, end = 56.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = status,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF52606A),
                        modifier = Modifier.weight(1f),
                    )
                    if (isImageTranslationEnabled) {
                        PreferredOcrLanguageChip(
                            selectedLanguage = preferredOcrLanguage,
                            onClick = { isLanguageSheetVisible = true },
                        )
                    }
                }
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = Color(0xFF25686F),
                    trackColor = Color.Transparent,
                )
            } else {
                HorizontalDivider(color = Color(0xFFE1E6EA))
            }
        }
    }

    if (isLanguageSheetVisible) {
        PreferredOcrLanguageBottomSheet(
            selectedLanguage = preferredOcrLanguage,
            onDismiss = { isLanguageSheetVisible = false },
            onLanguageSelected = { language ->
                onPreferredOcrLanguageChange(language)
                isLanguageSheetVisible = false
            },
        )
    }
}

@Composable
private fun PreferredOcrLanguageChip(
    selectedLanguage: OcrScanLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF25686F),
        contentColor = Color.White,
        modifier = modifier
            .width(44.dp)
            .height(30.dp)
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = selectedLanguage.shortLabel,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PreferredOcrLanguageBottomSheet(
    selectedLanguage: OcrScanLanguage,
    onDismiss: () -> Unit,
    onLanguageSelected: (OcrScanLanguage) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "OCR 우선 탐색 언어",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF172027),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OcrScanLanguage.entries.forEach { language ->
                    val selected = language == selectedLanguage
                    TextButton(
                        onClick = { onLanguageSelected(language) },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (selected) Color(0xFF25686F) else Color(0xFFEFF3F5),
                            contentColor = if (selected) Color.White else Color(0xFF24313A),
                        ),
                        modifier = Modifier
                            .height(38.dp)
                            .widthIn(min = 112.dp)
                            .defaultMinSize(minWidth = 0.dp, minHeight = 0.dp),
                        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                    ) {
                        Text(
                            text = "${language.shortLabel} · ${language.displayLabel}",
                            fontSize = 13.sp,
                            lineHeight = 15.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressField(
    value: String,
    onValueChange: (String) -> Unit,
    onNavigate: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val textColor = Color(0xFF172027)
    val placeholderColor = Color(0xFF75818A)

    Surface(
        modifier = modifier
            .height(42.dp)
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(21.dp),
        color = Color(0xFFEFF3F5),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onNavigate() }),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged {
                        isFocused = it.isFocused
                        onFocusChanged(it.isFocused)
                    },
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isBlank() && !isFocused) {
                            Text(
                                text = "site or URL",
                                color = placeholderColor,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}

@Composable
private fun BrowserGlyphButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(30.dp)
            .background(Color.Transparent, CircleShape),
    ) {
        Text(
            text = label,
            color = if (enabled) Color(0xFF24313A) else Color(0xFFB4BDC4),
            fontSize = if (label.length == 1) 15.sp else 12.sp,
            maxLines = 1,
        )
    }
}
