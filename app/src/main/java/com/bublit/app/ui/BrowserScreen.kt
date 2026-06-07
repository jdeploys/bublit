package com.bublit.app.ui

import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
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
import com.bublit.app.web.BrowserWebView

@Composable
fun BrowserScreen(
    activeUrl: String,
    urlInput: String,
    status: String,
    isImageTranslationEnabled: Boolean,
    imageCandidateCount: Int,
    onUrlInputChange: (String) -> Unit,
    onActiveUrlChange: (String) -> Unit,
    onNavigate: () -> Unit,
    onImageTranslationEnabledChange: (Boolean) -> Unit,
    onStatusChange: (String) -> Unit,
    onImagesDiscovered: (List<ImageCandidate>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var imageScanRequestId by remember { mutableIntStateOf(0) }

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
            )
        },
    ) { innerPadding ->
        BrowserWebView(
            url = activeUrl,
            imageTranslationEnabled = isImageTranslationEnabled,
            imageScanRequestId = imageScanRequestId,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
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
    onUrlInputChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onNavigate: () -> Unit,
    onToggleImageTranslation: () -> Unit,
) {
    var isAddressFocused by remember { mutableStateOf(false) }

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
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
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
                        modifier = Modifier.size(44.dp),
                    ) {
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher_round),
                            contentDescription = if (isImageTranslationEnabled) {
                                "Disable image translation"
                            } else {
                                "Enable image translation"
                            },
                            modifier = Modifier
                                .size(if (isImageTranslationEnabled) 34.dp else 31.dp)
                                .alpha(if (isImageTranslationEnabled) 1f else 0.62f),
                        )
                    }
                }
            }

            if (status.isNotBlank() && !isAddressFocused) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 96.dp, end = 56.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = status,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF52606A),
                    )
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
            IconButton(
                onClick = onNavigate,
                modifier = Modifier.size(34.dp),
            ) {
                Text(
                    text = "Go",
                    color = Color(0xFF25686F),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
            }
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
            .size(40.dp)
            .background(Color.Transparent, CircleShape),
    ) {
        Text(
            text = label,
            color = if (enabled) Color(0xFF24313A) else Color(0xFFB4BDC4),
            fontSize = if (label.length == 1) 18.sp else 13.sp,
            maxLines = 1,
        )
    }
}
