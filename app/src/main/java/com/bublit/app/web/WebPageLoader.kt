package com.bublit.app.web

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.bublit.app.domain.ImageCandidate

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPageLoader(
    url: String,
    modifier: Modifier = Modifier,
    onStatusChange: (String) -> Unit,
    onImagesExtracted: (List<ImageCandidate>) -> Unit,
) {
    val extractor = remember { WebImageExtractor() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        return false
                    }

                    override fun onPageFinished(view: WebView, loadedUrl: String) {
                        onStatusChange("Page loaded. Extracting DOM images...")
                        view.postDelayed(
                            {
                                view.evaluateJavascript(WebImageExtractor.DOM_IMAGE_SCRIPT) { rawResult ->
                                    val json = rawResult
                                        ?.removeSurrounding("\"")
                                        ?.replace("\\\"", "\"")
                                        ?.replace("\\\\", "\\")
                                        ?: "[]"
                                    val candidates = extractor.parseCandidates(json)
                                    onImagesExtracted(candidates)
                                }
                            },
                            600L,
                        )
                    }
                }
                loadUrl(url)
            }
        },
        update = { webView ->
            if (webView.url != url) {
                onStatusChange("Loading page...")
                webView.loadUrl(url)
            }
        },
    )

    DisposableEffect(url) {
        onDispose { }
    }
}
