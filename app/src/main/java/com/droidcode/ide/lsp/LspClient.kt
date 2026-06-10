package com.droidcode.ide.lsp

interface LspClient {
    fun connect(serverUrl: String, workspaceUri: String, onConnected: (Boolean) -> Unit)
    fun notifyDidChange(uri: String, content: String, version: Int)
    fun notifyDidOpen(uri: String, content: String, languageId: String)
    fun request(method: String, params: String, callback: (String) -> Unit)
    fun handleRequestFromEditor(json: String)
    fun disconnect()
} 