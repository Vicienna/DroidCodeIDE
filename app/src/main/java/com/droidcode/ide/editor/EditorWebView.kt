package com.droidcode.ide.editor

import android.content.Context
import android.util.AttributeSet
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EditorWebView(
    modifier: Modifier = Modifier,
    uri: String,
    initialContent: String,
    language: String,
    onContentChange: (String, Int) -> Unit,
    onCursorChange: (Int, Int) -> Unit,
    lspClient: LspClient
) {
    val context = LocalContext.current
    val scope = CoroutineScope(Dispatchers.Main)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MonacoWebView(ctx).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { webView ->
            if (!webView.isInitialized) {
                webView.isInitialized = true
                setupWebView(webView, context, scope, uri, initialContent, language, lspClient) { content, version ->
                    onContentChange(content, version)
                } { line, col ->
                    onCursorChange(line, col)
                }
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            // WebView cleanup handled by system
        }
    }
}

private fun setupWebView(
    webView: MonacoWebView,
    context: Context,
    scope: CoroutineScope,
    uri: String,
    initialContent: String,
    language: String,
    lspClient: LspClient,
    onContentChange: (String, Int) -> Unit,
    onCursorChange: (Int, Int) -> Unit
) {
    val settings = webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        allowFileAccess = true
        allowContentAccess = true
        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        userAgentString += " DroidCodeIDE/1.0"
    }

    if (BuildConfig.DEBUG) {
        WebView.setWebContentsDebuggingEnabled(true)
    }

    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            val bridge = MonacoBridge(context, webView, lspClient, scope)
            webView.addJavascriptInterface(bridge, MonacoBridge.JS_INTERFACE_NAME)
            bridge.initializeEditor(initialContent, language)
        }
    }

    webView.webChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage) {
            android.util.Log.d("MonacoWebView", "[JS Console] ${consoleMessage.message()} (${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})")
        }
    }
}

class MonacoWebView(context: Context, attrs: AttributeSet? = null) : WebView(context, attrs) {
    var isInitialized = false
} 