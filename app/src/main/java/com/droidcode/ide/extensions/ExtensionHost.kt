package com.droidcode.ide.extensions

interface ExtensionHost {
    fun initialize(context: android.content.Context)
    fun loadExtension(extensionJson: String)
    fun unloadExtension(extensionId: String)
    fun executeCommand(command: String, args: String)
    fun onFileOpen(uri: String, content: String)
    fun onFileSave(uri: String, content: String)
    fun shutdown()
} 