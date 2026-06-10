package com.droidcode.ide.editor

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.lifecycle.LifecycleOwner
import com.droidcode.ide.lsp.LspClient
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class MonacoBridge(
    private val context: Context,
    private val webView: WebView,
    private val lspClient: LspClient,
    private val scope: CoroutineScope
) : LifecycleOwner by object : LifecycleOwner {
    override val lifecycle = androidx.lifecycle.LifecycleRegistry(this).apply { handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_CREATE) }
} {

    companion object {
        private const val TAG = "MonacoBridge"
        const val JS_INTERFACE_NAME = "MonacoBridge"
    }

    private val pendingRequests = ConcurrentHashMap<Int, (String) -> Unit>()
    private val requestId = AtomicInteger(0)
    private val editorModels = mutableMapOf<String, EditorModelState>()

    data class EditorModelState(
        val uri: String,
        var content: String = "",
        var version: Int = 0,
        val languageId: String = "plaintext"
    )

    fun initializeEditor(initialContent: String, language: String, theme: String = "vs-dark") {
        val js = """
            (function() {
                require.config({ paths: { 'vs': 'https://cdn.jsdelivr.net/npm/monaco-editor@0.48.0/min/vs' }});
                require(['vs/editor/editor.main'], function() {
                    monaco.editor.defineTheme('droidcode-custom', ${getCustomThemeJson()});
                    window.monacoEditor = monaco.editor.create(document.getElementById('editor'), {
                        value: ${escapeJsString(initialContent)},
                        language: '$language',
                        theme: 'droidcode-custom',
                        automaticLayout: true,
                        minimap: { enabled: true },
                        fontSize: 14,
                        lineNumbers: 'on',
                        scrollBeyondLastLine: false,
                        wordWrap: 'on',
                        tabSize: 4,
                        insertSpaces: true,
                        bracketPairColorization: { enabled: true },
                        guides: { bracketPairs: true },
                        renderLineHighlight: 'all',
                        cursorBlinking: 'smooth',
                        smoothScrolling: true
                    });

                    window.monacoEditor.onDidChangeModelContent(function(e) {
                        var model = window.monacoEditor.getModel();
                        if(model) {
                            var content = model.getValue();
                            var version = model.getVersionId();
                            window.MonacoBridge.onContentChanged(JSON.stringify({uri: model.uri.toString(), content: content, version: version}));
                        }
                    });

                    window.monacoEditor.onDidChangeCursorSelection(function(e) {
                        var sel = window.monacoEditor.getSelection();
                        if(sel) {
                            window.MonacoBridge.onCursorSelection(JSON.stringify({
                                startLine: sel.startLineNumber,
                                startCol: sel.startColumn,
                                endLine: sel.endLineNumber,
                                endCol: sel.endColumn
                            }));
                        }
                    });

                    window.MonacoBridge.onEditorReady('ready');
                });
            })();
        """.trimIndent()
        evaluateJavascript(js)
    }

    fun setContent(uri: String, content: String) {
        val js = "window.monacoEditor.getModel()?.setValue(${escapeJsString(content)});"
        evaluateJavascript(js)
        editorModels[uri]?.content = content
    }

    fun openFile(uri: String, content: String, language: String) {
        val js = """
            (function() {
                var model = monaco.editor.createModel(${escapeJsString(content)}, '$language', monaco.Uri.parse(${escapeJsString(uri)}));
                window.monacoEditor.setModel(model);
            })();
        """.trimIndent()
        evaluateJavascript(js)
        editorModels[uri] = EditorModelState(uri, content, 1, language)
    }

    fun setTheme(themeName: String) {
        val js = "monaco.editor.setTheme('$themeName');"
        evaluateJavascript(js)
    }

    fun triggerLspHover(line: Int, character: Int) {
        val js = "window.monacoEditor.trigger('android', 'editor.action.showHover', null);"
        evaluateJavascript(js)
    }

    fun triggerFormat() {
        evaluateJavascript("window.monacoEditor.getAction('editor.action.formatDocument').run();")
    }

    fun revealLine(line: Int, at: String = "center") {
        evaluateJavascript("window.monacoEditor.revealLine($line, 1, '$at');")
    }

    private fun evaluateJavascript(js: String, callback: ((String) -> Unit)? = null) {
        webView.post {
            if (callback != null) {
                val id = requestId.incrementAndGet()
                pendingRequests[id] = callback
            }
            webView.evaluateJavascript(js, null)
        }
    }

    @JavascriptInterface
    fun onEditorReady(status: String) {
        Log.d(TAG, "Monaco Ready: $status")
    }

    @JavascriptInterface
    fun onContentChanged(json: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val obj = JsonParser.parseString(json).asJsonObject
                val uri = obj.get("uri").asString
                val content = obj.get("content").asString
                val version = obj.get("version").asInt

                editorModels[uri]?.let { it.content = content; it.version = version } ?: run {
                    editorModels[uri] = EditorModelState(uri, content, version, detectLanguage(uri))
                }

                lspClient.notifyDidChange(uri, content, version)
            } catch (e: Exception) {
                Log.e(TAG, "Parse content change failed", e)
            }
        }
    }

    @JavascriptInterface
    fun onCursorSelection(json: String) {
        // Update UI (Status bar line/col), Trigger LSP Hover/Completion if idle
    }

    @JavascriptInterface
    fun onLspRequest(json: String) {
        scope.launch(Dispatchers.IO) {
            lspClient.handleRequestFromEditor(json)
        }
    }

    @JavascriptInterface
    fun onExtensionMessage(json: String) {
        // Message from Extension (QuickJS) running inside Monaco WebWorker? 
    }

    private fun detectLanguage(uri: String): String {
        return when {
            uri.endsWith(".kt") -> "kotlin"
            uri.endsWith(".java") -> "java"
            uri.endsWith(".js", ".jsx", ".ts", ".tsx") -> "typescript"
            uri.endsWith(".py") -> "python"
            uri.endsWith(".json") -> "json"
            uri.endsWith(".xml", ".html", ".htm") -> "html"
            uri.endsWith(".css") -> "css"
            uri.endsWith(".md") -> "markdown"
            uri.endsWith(".gradle", ".gradle.kts") -> "gradle"
            uri.endsWith(".yaml", ".yml") -> "yaml"
            uri.endsWith(".sh") -> "shell"
            uri.endsWith(".rs") -> "rust"
            uri.endsWith(".go") -> "go"
            else -> "plaintext"
        }
    }

    private fun escapeJsString(str: String): String {
        return Gson().toJson(str)
    }

    private fun getCustomThemeJson(): String = """
        {
            "base": "vs-dark",
            "inherit": true,
            "rules": [],
            "colors": {
                "editor.background": "#1E1E1E",
                "editor.foreground": "#D4D4D4",
                "editor.lineHighlightBackground": "#2D2D2D",
                "editorCursor.foreground": "#AEAFAD",
                "editor.selectionBackground": "#264F78",
                "editor.inactiveSelectionBackground": "#3A3D41",
                "editorLineNumber.foreground": "#858585",
                "editorIndentGuide.background": "#404040",
                "editorIndentGuide.activeBackground": "#707070"
            }
        }
    """.trimIndent()
} 