package com.bublit.app.web

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.bublit.app.domain.ImageCandidate

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    url: String,
    imageTranslationEnabled: Boolean,
    imageScanRequestId: Int,
    translatedImageUris: Map<String, String>,
    modifier: Modifier = Modifier,
    onWebViewReady: (WebView) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String, List<ImageCandidate>, Boolean, Boolean, Boolean) -> Unit,
) {
    val extractor = remember { WebImageExtractor() }
    val currentImageTranslationEnabled by rememberUpdatedState(imageTranslationEnabled)
    val currentOnPageStarted by rememberUpdatedState(onPageStarted)
    val currentOnPageFinished by rememberUpdatedState(onPageFinished)
    var handledScanRequestId by remember { mutableIntStateOf(-1) }
    var appliedTranslationSignature by remember { mutableStateOf("") }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        return false
                    }

                    override fun onPageStarted(view: WebView, loadedUrl: String, favicon: Bitmap?) {
                        currentOnPageStarted(loadedUrl)
                    }

                    override fun onPageFinished(view: WebView, loadedUrl: String) {
                        if (currentImageTranslationEnabled) {
                            view.postDelayed(
                                { scanPageImages(view, extractor, loadedUrl, currentOnPageFinished) },
                                500L,
                            )
                        } else {
                            currentOnPageFinished(
                                loadedUrl,
                                emptyList(),
                                view.canGoBack(),
                                view.canGoForward(),
                                false,
                            )
                        }
                    }
                }
                onWebViewReady(this)
                loadUrl(url)
            }
        },
        update = { webView ->
            if (webView.url != url && url.isNotBlank()) {
                webView.loadUrl(url)
            }
            val translationSignature = translatedImageUris.entries
                .sortedBy { it.key }
                .joinToString(separator = "|") { "${it.key}=${it.value}" }
            if (translationSignature != appliedTranslationSignature) {
                appliedTranslationSignature = translationSignature
                webView.evaluateJavascript(
                    buildTranslatedImageReplacementScript(translatedImageUris),
                    null,
                )
            }
            if (
                imageTranslationEnabled &&
                imageScanRequestId > handledScanRequestId &&
                webView.url != null
            ) {
                handledScanRequestId = imageScanRequestId
                scanPageImages(
                    webView,
                    extractor,
                    webView.url ?: url,
                    currentOnPageFinished,
                )
            }
        },
    )
}

internal fun buildTranslatedImageReplacementScript(translatedImageUris: Map<String, String>): String {
    val replacementsJson = translatedImageUris.entries.joinToString(
        separator = ",",
        prefix = "{",
        postfix = "}",
    ) { (sourceUrl, translatedUri) ->
        "\"${sourceUrl.escapeJavaScriptString()}\":\"${translatedUri.escapeJavaScriptString()}\""
    }
    return """
        (function(replacements) {
          const images = Array.from(document.images || []);
          const hasReplacements = Object.keys(replacements).length > 0;
          images.forEach(function(img) {
            if (!img.dataset.bublitOriginalSrc) {
              img.dataset.bublitOriginalSrc = img.src || "";
            }
            if (!hasReplacements && img.dataset.bublitOriginalSrc) {
              img.src = img.dataset.bublitOriginalSrc;
              return;
            }
            const sources = [img.currentSrc, img.src, img.dataset.bublitOriginalSrc].filter(Boolean);
            let replacement = null;
            for (let index = 0; index < sources.length; index += 1) {
              if (replacements[sources[index]]) {
                replacement = replacements[sources[index]];
                break;
              }
            }
            if (replacement) {
              img.src = replacement;
            }
          });
        })($replacementsJson);
    """.trimIndent()
}

private fun String.escapeJavaScriptString(): String {
    return buildString {
        this@escapeJavaScriptString.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }
}

private fun scanPageImages(
    webView: WebView,
    extractor: WebImageExtractor,
    loadedUrl: String,
    onPageFinished: (String, List<ImageCandidate>, Boolean, Boolean, Boolean) -> Unit,
) {
    webView.evaluateJavascript(WebImageExtractor.DOM_IMAGE_SCRIPT) { rawResult ->
        val json = rawResult
            ?.removeSurrounding("\"")
            ?.replace("\\\"", "\"")
            ?.replace("\\\\", "\\")
            ?: "[]"
        val candidates = runCatching {
            extractor.parseCandidates(json)
        }.getOrDefault(emptyList())
        onPageFinished(
            loadedUrl,
            candidates,
            webView.canGoBack(),
            webView.canGoForward(),
            true,
        )
    }
}
