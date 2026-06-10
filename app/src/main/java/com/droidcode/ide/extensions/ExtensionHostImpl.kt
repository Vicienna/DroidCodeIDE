package com.droidcode.ide.extensions

import android.content.Context
import android.util.Log
import com.arthenica.quickjs.QuickJS
import com.arthenica.quickjs.QuickJSContext
import com.arthenica.quickjs.QuickJSFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class ExtensionHostImpl(private val scope: CoroutineScope) : ExtensionHost {

    companion object {
        private const val TAG = "ExtensionHost"
    }

    private var context: Context? = null
    private var quickJSContext: QuickJSContext? = null
    private val loadedExtensions = mutableMapOf<String, ExtensionInstance>()

    data class ExtensionInstance(
        val id: String,
        val mainScript: String,
        val exports: JSONObject
    )

    override fun initialize(ctx: Context) {
        context = ctx
        scope.launch(Dispatchers.IO) {
            QuickJS.createContext().also { qjsCtx ->
                quickJSContext = qjsCtx
                injectGlobals(qjsCtx)
                Log.d(TAG, "QuickJS Runtime Ready")
            }
        }
    }

    private fun injectGlobals(qjsCtx: QuickJSContext) {
        qjsCtx.evaluate("""
            globalThis.console = {
                log: function(...args) { AndroidLog.log(args.join(' ')); },
                error: function(...args) { AndroidLog.error(args.join(' ')); },
                warn: function(...args) { AndroidLog.warn(args.join(' ')); }
            };
            globalThis.setTimeout = function(cb, ms) { AndroidTimers.setTimeout(cb, ms); };
            globalThis.clearTimeout = function(id) { AndroidTimers.clearTimeout(id); };
            
            globalThis.vscode = {
                workspace: { 
                    onDidChangeTextDocument: (cb) => AndroidEvents.on('didChangeTextDocument', cb),
                    getConfiguration: (section) => ({ get: (key) => AndroidConfig.get(section, key) })
                },
                window: {
                    showInformationMessage: (msg) => AndroidUI.toast(msg),
                    showErrorMessage: (msg) => AndroidUI.toastError(msg),
                    createTextEditorDecorationType: (opts) => AndroidDecor.create(opts)
                },
                commands: {
                    registerCommand: (cmd, cb) => AndroidCommands.register(cmd, cb),
                    executeCommand: (cmd, ...args) => AndroidCommands.execute(cmd, args)
                },
                languages: {
                    registerCompletionItemProvider: (lang, provider) => AndroidLSP.registerCompletion(lang, provider),
                    registerHoverProvider: (lang, provider) => AndroidLSP.registerHover(lang, provider)
                }
            };
        """)
        
        qjsCtx.setGlobal("AndroidLog", QuickJSFunction { _, args ->
            Log.d("ExtJS", args.joinToString(" "))
            null
        })
    }

    override fun loadExtension(extensionJson: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val pkg = JSONObject(extensionJson)
                val id = pkg.getString("name")
                val main = pkg.optString("main", "extension.js")
                val script = "// TODO: Load script content for $main"
                
                quickJSContext?.evaluate(script)
                loadedExtensions[id] = ExtensionInstance(id, script, pkg)
                Log.d(TAG, "Loaded Extension: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Load Extension Failed", e)
            }
        }
    }

    override fun unloadExtension(extensionId: String) {
        loadedExtensions.remove(extensionId)
    }

    override fun executeCommand(command: String, args: String) {
        quickJSContext?.evaluate("vscode.commands.executeCommand('$command', $args);")
    }

    override fun onFileOpen(uri: String, content: String) {
        quickJSContext?.evaluate("vscode.workspace.emit('onDidOpenTextDocument', {uri: '$uri', getText: () => '$content'});")
    }

    override fun onFileSave(uri: String, content: String) {
        quickJSContext?.evaluate("vscode.workspace.emit('onDidSaveTextDocument', {uri: '$uri', getText: () => '$content'});")
    }

    override fun shutdown() {
        quickJSContext?.close()
        quickJSContext = null
        loadedExtensions.clear()
    }
} 