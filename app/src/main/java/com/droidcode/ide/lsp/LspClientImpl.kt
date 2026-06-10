package com.droidcode.ide.lsp

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class LspClientImpl(
    private val okHttp: OkHttpClient,
    private val gson: Gson,
    private val scope: CoroutineScope
) : LspClient {

    companion object {
        private const val TAG = "LspClient"
    }

    private var webSocket: WebSocket? = null
    private val pendingRequests = ConcurrentHashMap<Int, (String) -> Unit>()
    private val requestId = AtomicInteger(0)
    private val messageQueue = Channel<String>(capacity = 100)

    override fun connect(serverUrl: String, workspaceUri: String, onConnected: (Boolean) -> Unit) {
        scope.launch(Dispatchers.IO) {
            val request = Request.Builder().url(serverUrl).build()
            webSocket = okHttp.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: okhttp3.Response) {
                    Log.d(TAG, "LSP WebSocket Connected to $serverUrl")
                    sendInitialize(workspaceUri)
                    scope.launch { flushQueue(ws) }
                    onConnected(true)
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    handleIncomingMessage(text)
                }

                override fun onMessage(ws: WebSocket, bytes: ByteString) {}

                override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                    ws.close(1000, null)
                    Log.d(TAG, "LSP Closing: $reason")
                }

                override fun onFailure(ws: WebSocket?, t: Throwable, response: okhttp3.Response?) {
                    Log.e(TAG, "LSP Failure", t)
                    onConnected(false)
                }
            })
        }
    }

    private fun sendInitialize(workspaceUri: String) {
        val initParams = JsonObject().apply {
            addProperty("processId", android.os.Process.myPid())
            add("rootUri", gson.toJsonTree(workspaceUri))
            add("capabilities", JsonObject().apply {
                add("textDocument", JsonObject().apply {
                    add("sync", JsonObject().apply { addProperty("dynamicRegistration", true); addProperty("willSave", true); addProperty("willSaveWaitUntil", true); addProperty("didSave", true) })
                    add("completion", JsonObject().apply { addProperty("dynamicRegistration", true); addProperty("completionItem", JsonObject().apply { addProperty("snippetSupport", true) }) })
                    add("hover", JsonObject().apply { addProperty("dynamicRegistration", true) })
                    add("definition", JsonObject().apply { addProperty("dynamicRegistration", true) })
                    add("references", JsonObject().apply { addProperty("dynamicRegistration", true) })
                    add("documentHighlight", JsonObject().apply { addProperty("dynamicRegistration", true) })
                    add("documentSymbol", JsonObject().apply { addProperty("dynamicRegistration", true) })
                    add("formatting", JsonObject().apply { addProperty("dynamicRegistration", true) })
                    add("rangeFormatting", JsonObject().apply { addProperty("dynamicRegistration", true) })
                    add("onTypeFormatting", JsonObject().apply { addProperty("dynamicRegistration", true) })
                    add("rename", JsonObject().apply { addProperty("dynamicRegistration", true) })
                    add("publishDiagnostics", JsonObject().apply { addProperty("relatedInformation", true) })
                })
                add("workspace", JsonObject().apply {
                    add("applyEdit", true)
                    add("workspaceEdit", JsonObject().apply { addProperty("documentChanges", true) })
                    add("didChangeConfiguration", JsonObject().apply { addProperty("dynamicRegistration", true) })
                    add("didChangeWatchedFiles", JsonObject().apply { addProperty("dynamicRegistration", true) })
                    add("symbol", JsonObject().apply { addProperty("dynamicRegistration", true) })
                    add("executeCommand", JsonObject().apply { addProperty("dynamicRegistration", true) })
                })
            })
            addProperty("trace", "verbose")
        }
        sendRequest("initialize", initParams) { response ->
            Log.d(TAG, "Initialize Response: $response")
            sendNotification("initialized", JsonObject())
        }
    }

    private fun flushQueue(ws: WebSocket) {
        // Implementation for flushing queued messages
    }

    private fun handleIncomingMessage(text: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val json = JsonParser.parseString(text).asJsonObject
                if (json.has("id")) {
                    val id = json.get("id").asInt
                    pendingRequests.remove(id)?.invoke(text)
                } else if (json.has("method")) {
                    val method = json.get("method").asString
                    when (method) {
                        "textDocument/publishDiagnostics" -> handleDiagnostics(json.get("params").asJsonObject)
                        "window/showMessage" -> handleShowMessage(json.get("params").asJsonObject)
                        "window/logMessage" -> handleLogMessage(json.get("params").asJsonObject)
                        "client/registerCapability" -> handleRegisterCapability(json)
                        else -> Log.d(TAG, "Unhandled Server Method: $method")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Parse LSP Message Failed", e)
            }
        }
    }

    private fun handleDiagnostics(params: JsonObject) {
        // Forward to Monaco via Bridge (runOnUiThread -> evaluateJavascript)
        // monaco.editor.setModelMarkers(model, 'lsp', [...])
    }

    private fun handleShowMessage(params: JsonObject) {}
    private fun handleLogMessage(params: JsonObject) {}
    private fun handleRegisterCapability(json: JsonObject) {}

    override fun notifyDidChange(uri: String, content: String, version: Int) {
        val params = JsonObject().apply {
            add("textDocument", JsonObject().apply {
                addProperty("uri", uri)
                addProperty("version", version)
            })
            add("contentChanges", gson.toJsonTree(listOf(JsonObject().apply { addProperty("text", content) })))
        }
        sendNotification("textDocument/didChange", params)
    }

    override fun notifyDidOpen(uri: String, content: String, languageId: String) {
        val params = JsonObject().apply {
            add("textDocument", JsonObject().apply {
                addProperty("uri", uri)
                addProperty("languageId", languageId)
                addProperty("version", 1)
                addProperty("text", content)
            })
        }
        sendNotification("textDocument/didOpen", params)
    }

    override fun request(method: String, params: String, callback: (String) -> Unit) {
        val jsonParams = JsonParser.parseString(params)
        sendRequest(method, jsonParams, callback)
    }

    override fun handleRequestFromEditor(json: String) {
        // Parse JSON from Monaco (method, params, id) -> Forward to LSP Server
    }

    override fun disconnect() {
        webSocket?.close(1000, "Client Disconnect")
        webSocket = null
    }

    private fun sendRequest(method: String, params: Any, callback: (String) -> Unit) {
        val id = requestId.incrementAndGet()
        pendingRequests[id] = callback
        val msg = JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            addProperty("id", id)
            addProperty("method", method)
            add("params", gson.toJsonTree(params))
        }
        sendMessage(msg.toString())
    }

    private fun sendNotification(method: String, params: Any) {
        val msg = JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            addProperty("method", method)
            add("params", gson.toJsonTree(params))
        }
        sendMessage(msg.toString())
    }

    private fun sendMessage(text: String) {
        webSocket?.send(text) ?: scope.launch { messageQueue.send(text) }
    }
} 