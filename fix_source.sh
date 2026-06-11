#!/data/data/com.termux/files/usr/bin/bash
# ============================================================
# Fix Source Code Kotlin (Hilt, Imports, Syntax)
# ============================================================

set -e
cd "$(dirname "$0")"

echo "🔧 Memperbaiki Source Code..."

# 1. NetworkModule.kt - Fix Hilt Scope & CoroutineScope
cat > app/src/main/java/com/droidcode/ide/di/NetworkModule.kt << 'EOF'
package com.droidcode.ide.di

import com.droidcode.ide.lsp.LspClient
import com.droidcode.ide.lsp.LspClientImpl
import com.droidcode.ide.terminal.TerminalSession
import com.droidcode.ide.terminal.TerminalSessionImpl
import com.droidcode.ide.git.GitManager
import com.droidcode.ide.git.GitManagerImpl
import com.droidcode.ide.extensions.ExtensionHost
import com.droidcode.ide.extensions.ExtensionHostImpl
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    @Provides @Singleton
    fun provideGson(): Gson = GsonBuilder().setLenient().serializeNulls().create()
}

@Module
@InstallIn(SingletonComponent::class)
object CoreServicesModule {
    @Provides @Singleton
    fun provideLspClient(okHttp: OkHttpClient, gson: Gson): LspClient = LspClientImpl(okHttp, gson)

    @Provides @Singleton
    fun provideTerminalSession(): TerminalSession = TerminalSessionImpl()

    @Provides @Singleton
    fun provideGitManager(): GitManager = GitManagerImpl()

    @Provides @Singleton
    fun provideExtensionHost(): ExtensionHost = ExtensionHostImpl()
}
EOF

# 2. LspClientImpl.kt - Fix Constructor (remove CoroutineScope param)
cat > app/src/main/java/com/droidcode/ide/lsp/LspClientImpl.kt << 'EOF'
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
    private val gson: Gson
) : LspClient {

    companion object { private const val TAG = "LspClient" }

    private var webSocket: WebSocket? = null
    private val pendingRequests = ConcurrentHashMap<Int, (String) -> Unit>()
    private val requestId = AtomicInteger(0)
    private val messageQueue = Channel<String>(capacity = 100)
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun connect(serverUrl: String, workspaceUri: String, onConnected: (Boolean) -> Unit) {
        scope.launch {
            val request = Request.Builder().url(serverUrl).build()
            webSocket = okHttp.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: okhttp3.Response) {
                    Log.d(TAG, "LSP Connected: $serverUrl")
                    sendInitialize(workspaceUri)
                    scope.launch { flushQueue(ws) }
                    onConnected(true)
                }
                override fun onMessage(ws: WebSocket, text: String) = handleIncomingMessage(text)
                override fun onMessage(ws: WebSocket, bytes: ByteString) {}
                override fun onClosing(ws: WebSocket, code: Int, reason: String) { ws.close(1000, null); Log.d(TAG, "LSP Closing: $reason") }
                override fun onFailure(ws: WebSocket?, t: Throwable, response: okhttp3.Response?) { Log.e(TAG, "LSP Failure", t); onConnected(false) }
            })
        }
    }

    private fun sendInitialize(workspaceUri: String) {
        val params = JsonObject().apply {
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
        sendRequest("initialize", params) { Log.d(TAG, "Init Response: $it"); sendNotification("initialized", JsonObject()) }
    }

    private fun flushQueue(ws: WebSocket) { /* queue flush logic */ }

    private fun handleIncomingMessage(text: String) {
        scope.launch {
            try {
                val json = JsonParser.parseString(text).asJsonObject
                if (json.has("id")) pendingRequests.remove(json.get("id").asInt)?.invoke(text)
                else if (json.has("method")) when (json.get("method").asString) {
                    "textDocument/publishDiagnostics" -> { /* handle diag */ }
                    "window/showMessage", "window/logMessage" -> { }
                    "client/registerCapability" -> { }
                }
            } catch (e: Exception) { Log.e(TAG, "Parse LSP Error", e) }
        }
    }

    override fun notifyDidChange(uri: String, content: String, version: Int) {
        val params = JsonObject().apply {
            add("textDocument", JsonObject().apply { addProperty("uri", uri); addProperty("version", version) })
            add("contentChanges", gson.toJsonTree(listOf(JsonObject().apply { addProperty("text", content) })))
        }
        sendNotification("textDocument/didChange", params)
    }

    override fun notifyDidOpen(uri: String, content: String, languageId: String) {
        val params = JsonObject().apply {
            add("textDocument", JsonObject().apply { addProperty("uri", uri); addProperty("languageId", languageId); addProperty("version", 1); addProperty("text", content) })
        }
        sendNotification("textDocument/didOpen", params)
    }

    override fun request(method: String, params: String, callback: (String) -> Unit) {
        sendRequest(method, JsonParser.parseString(params), callback)
    }

    override fun handleRequestFromEditor(json: String) { /* forward to server */ }

    override fun disconnect() { webSocket?.close(1000, "Client Disconnect"); webSocket = null }

    private fun sendRequest(method: String, params: Any, callback: (String) -> Unit) {
        val id = requestId.incrementAndGet()
        pendingRequests[id] = callback
        sendMessage(JsonObject().apply { addProperty("jsonrpc", "2.0"); addProperty("id", id); addProperty("method", method); add("params", gson.toJsonTree(params)) }.toString())
    }

    private fun sendNotification(method: String, params: Any) {
        sendMessage(JsonObject().apply { addProperty("jsonrpc", "2.0"); addProperty("method", method); add("params", gson.toJsonTree(params)) }.toString())
    }

    private fun sendMessage(text: String) { webSocket?.send(text) ?: scope.launch { messageQueue.send(text) } }
}
EOF

# 3. TerminalSessionImpl.kt - Fix Constructor
cat > app/src/main/java/com/droidcode/ide/terminal/TerminalSessionImpl.kt << 'EOF'
package com.droidcode.ide.terminal

import android.util.Log
import com.hierynomus.sshj.SSHClient
import com.hierynomus.sshj.transport.verification.PromiscuousVerification
import com.hierynomus.sshj.userauth.keyprovider.FileKeyProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStream

class TerminalSessionImpl : TerminalSession {

    companion object { private const val TAG = "TerminalSession" }

    private var localProcess: Process? = null
    private var localOutputStream: OutputStream? = null
    private var sshClient: SSHClient? = null
    private var sshShell: com.hierynomus.sshj.connection.channel.direct.Session? = null
    private var sshOutputStream: OutputStream? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun startLocal(onOutput: (String) -> Unit, onExit: (Int) -> Unit) {
        scope.launch {
            try {
                val shell = if (isTermuxAvailable()) "/data/data/com.termux/files/usr/bin/bash" else "/system/bin/sh"
                localProcess = Runtime.getRuntime().exec(arrayOf(shell, "-i", "-l"))
                localOutputStream = localProcess!!.outputStream
                Thread(startReader(localProcess!!.inputStream, onOutput, "STDOUT")).start()
                Thread(startReader(localProcess!!.errorStream, onOutput, "STDERR")).start()
                onExit(localProcess!!.waitFor())
            } catch (e: Exception) { Log.e(TAG, "Local Shell Error", e); onExit(-1) }
        }
    }

    private fun isTermuxAvailable(): Boolean = try { Runtime.getRuntime().exec("ls /data/data/com.termux/files/usr/bin/bash").waitFor() == 0 } catch (e: Exception) { false }

    private fun startReader(inputStream: java.io.InputStream, onOutput: (String) -> Unit, prefix: String): Runnable = Runnable {
        inputStream.bufferedReader().use { reader -> var line: String?; while (reader.readLine().also { line = it } != null) onOutput("$prefix: $line\n") }
    }

    override fun startSsh(host: String, port: Int, user: String, password: String?, keyPath: String?, onOutput: (String) -> Unit, onExit: (Int) -> Unit) {
        scope.launch {
            sshClient = SSHClient()
            try {
                sshClient!!.addHostKeyVerifier(PromiscuousVerification.INSTANCE)
                sshClient!!.connect(host, port)
                if (keyPath != null && keyPath.isNotBlank()) sshClient!!.authPublickey(user, FileKeyProvider(keyPath))
                else if (password != null) sshClient!!.authPassword(user, password)
                else throw IllegalArgumentException("SSH Auth required")
                sshShell = sshClient!!.startSession().apply { allocateDefaultPTY(); startShell() }
                sshOutputStream = sshShell!!.outputStream
                Thread(startReader(sshShell!!.inputStream, onOutput)).start()
                Thread(startReader(sshShell!!.errorStream, onOutput)).start()
                onExit(sshShell!!.join()?.exitStatus ?: 0)
            } catch (e: Exception) { Log.e(TAG, "SSH Error", e); onOutput("\r\n[SSH Error] ${e.message}\r\n"); onExit(-1) }
            finally { disconnectSsh() }
        }
    }

    override fun write(input: String) = scope.launch { try { localOutputStream?.write(input.toByteArray()); localOutputStream?.flush(); sshOutputStream?.write(input.toByteArray()); sshOutputStream?.flush() } catch (e: Exception) { Log.e(TAG, "Write failed", e) } }

    override fun resize(cols: Int, rows: Int) { sshShell?.resize(cols, rows) }

    override fun kill() = scope.launch { localProcess?.destroyForcibly(); disconnectSsh() }

    private fun disconnectSsh() { try { sshShell?.close() } catch (e: Exception) {}; try { sshClient?.disconnect() } catch (e: Exception) {}; sshShell = null; sshClient = null; sshOutputStream = null }
}
EOF

# 4. MainScreen.kt - Fix Syntax Errors (Typos, Missing Imports)
cat > app/src/main/java/com/droidcode/ide/ui/main/MainScreen.kt << 'EOF'
package com.droidcode.ide.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.Menu
import androidx.compose.material3.icons.filled.Search
import androidx.compose.material3.icons.filled.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.droidcode.ide.editor.EditorTab
import com.droidcode.ide.editor.EditorViewModel
import com.droidcode.ide.editor.EditorWebView
import com.droidcode.ide.extensions.ExtensionHost
import com.droidcode.ide.git.GitManager
import com.droidcode.ide.lsp.LspClient
import com.droidcode.ide.terminal.TerminalSession
import com.droidcode.ide.ui.theme.Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    editorViewModel: EditorViewModel,
    lspClient: LspClient,
    terminalSession: TerminalSession,
    gitManager: GitManager,
    extensionHost: ExtensionHost,
    onOpenFolder: (android.net.Uri) -> Unit
) {
    val context = LocalContext.current
    val currentWorkspace by viewModel.currentWorkspace.collectAsStateWithLifecycle()
    val activeTab by editorViewModel.activeTab.collectAsStateWithLifecycle()
    val tabs by editorViewModel.tabs.collectAsStateWithLifecycle()
    val isCommandPaletteOpen by viewModel.isCommandPaletteOpen.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val isTablet = context.resources.configuration.smallestScreenWidthDp >= 600

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                modifier = Modifier.fillMaxWidth(),
                title = { Text(text = activeTab?.label ?: "DroidCode IDE", maxLines = 1, overflow = androidx.compose.ui.text.TextOverflow.Ellipsis) },
                navigationIcon = { if (!isTablet) IconButton(onClick = { viewModel.showCommandPalette() }) { Icon(Icons.Default.Menu, contentDescription = "Menu") } },
                actions = {
                    IconButton(onClick = { }) { Icon(Icons.Default.Search, contentDescription = "Search") }
                    IconButton(onClick = { }) { Icon(Icons.Default.Terminal, contentDescription = "Terminal") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            )

            Row(modifier = Modifier.fillMaxSize().weight(1f)) {
                if (isTablet) ActivityBar(selectedView = viewModel.selectedSideBarView.value, onViewClick = viewModel::onSideBarViewClick)
                SideBar(modifier = Modifier.width(280.dp).fillMaxHeight(), workspaceUri = currentWorkspace, gitManager = gitManager, onFileClick = { uri, content, lang -> editorViewModel.openFile(uri, content, lang); lspClient.notifyDidOpen(uri, content, lang) }, onNewFile = {}, onOpenFolder = onOpenFolder)
                EditorArea(modifier = Modifier.fillMaxSize().weight(1f), tabs = tabs, activeTabId = editorViewModel.activeTabId.value, onTabClick = { id -> editorViewModel._activeTabId.value = id }, onTabClose = editorViewModel::closeTab, onContentChange = { id, content, version -> editorViewModel.updateContent(id, content, version) }, onCursorChange = { id, line, col -> editorViewModel.updateCursor(id, line, col) }, lspClient = lspClient, extensionHost = extensionHost)
                PanelContainer(modifier = Modifier.height(200.dp).fillMaxWidth(), terminalSession = terminalSession, gitManager = gitManager, currentWorkspace = currentWorkspace)
            }
            StatusBar(modifier = Modifier.fillMaxWidth(), activeTab = activeTab, gitBranch = "main", lspStatus = "Connected", encoding = "UTF-8", lineEnding = "LF")
        }
        if (isCommandPaletteOpen) CommandPalette(onDismiss = viewModel::hideCommandPalette)
        if (isSettingsOpen) SettingsScreen(onDismiss = viewModel::hideSettings)
    }
}

@Composable fun ActivityBar(selectedView: String, onViewClick: (String) -> Unit) = Box(Modifier.width(48.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainer)) { Text("Activity Bar", modifier = Modifier.fillMaxSize().padding(16.dp)) }
@Composable fun SideBar(modifier: Modifier, workspaceUri: android.net.Uri?, gitManager: GitManager, onFileClick: (String, String, String) -> Unit, onNewFile: () -> Unit, onOpenFolder: (android.net.Uri) -> Unit) = Box(modifier.background(MaterialTheme.colorScheme.surfaceContainer)) { Text("Side Bar: File Explorer", modifier = modifier.padding(16.dp)) }
@Composable fun EditorArea(modifier: Modifier, tabs: List<EditorTab>, activeTabId: String?, onTabClick: (String) -> Unit, onTabClose: (String) -> Unit, onContentChange: (String, String, Int) -> Unit, onCursorChange: (String, Int, Int) -> Unit, lspClient: LspClient, extensionHost: ExtensionHost) {
    Box(modifier.background(MaterialTheme.colorScheme.surface)) {
        tabs.find { it.id == activeTabId }?.let { tab -> EditorWebView(modifier = modifier, uri = tab.uri, initialContent = tab.content, language = tab.language, onContentChange = { content, version -> onContentChange(tab.id, content, version) }, onCursorChange = { line, col -> onCursorChange(tab.id, line, col) }, lspClient = lspClient) }
        ?: Text("No Editor Open. Open a file from Sidebar.", modifier = modifier.fillMaxSize().padding(16.dp), style = MaterialTheme.typography.bodyLarge)
    }
}
@Composable fun PanelContainer(modifier: Modifier, terminalSession: TerminalSession, gitManager: GitManager, currentWorkspace: android.net.Uri?) = Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) { Text("Panel: Terminal / Problems", modifier = modifier.padding(16.dp)) }
@Composable fun StatusBar(modifier: Modifier, activeTab: EditorTab?, gitBranch: String, lspStatus: String, encoding: String, lineEnding: String) = Box(modifier.height(24.dp).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 16.dp)) { Row(modifier = modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) { Text("Ln ${activeTab?.cursorPosition?.line}, Col ${activeTab?.cursorPosition?.column}"); androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f)); Text("$gitBranch | $lspStatus | $encoding | $lineEnding") } }
@Composable fun CommandPalette(onDismiss: () -> Unit) = Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))) { Box(Modifier.size(600.dp, 400.dp).align(Alignment.Center).background(MaterialTheme.colorScheme.surface)) { Text("Command Palette (Ctrl+Shift+P)", modifier = Modifier.fillMaxSize().padding(16.dp)) } }
@Composable fun SettingsScreen(onDismiss: () -> Unit) = Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))) { Box(Modifier.size(600.dp, 800.dp).align(Alignment.Center).background(MaterialTheme.colorScheme.surface)) { Text("Settings (JSON)", modifier = Modifier.fillMaxSize().padding(16.dp)) } }
EOF

# 5. Proguard Rules - Fix QuickJS & Hilt
cat > app/proguard-rules.pro << 'EOF'
# Kotlin & Coroutines
-keep class kotlin.coroutines.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class ** { @kotlin.Metadata *; }

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp { *; }
-keep class * extends dagger.hilt.android.HiltViewModel { *; }
-dontwarn dagger.hilt.**
-keepclasseswithmembers class * { @dagger.hilt.android.* <init>(...); }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class com.droidcode.ide.data.db.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# OkHttp / Okio / Gson
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keep class com.google.gson.** { *; }
-keep class com.droidcode.ide.** { *; }
-keepclassmembers class * { @com.google.gson.annotations.SerializedName *; }

# JGit
-keep class org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**

# SSHJ / BouncyCastle
-keep class com.hierynomus.** { *; }
-dontwarn com.hierynomus.**
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# QuickJS
-keep class com.arthenica.quickjs.** { *; }
-dontwarn com.arthenica.quickjs.**

# SLF4J / Logback
-keep class org.slf4j.** { *; }
-keep class ch.qos.logback.** { *; }
-dontwarn org.slf4j.**
-dontwarn ch.qos.logback.**

# AndroidX / Compose / Navigation / Lifecycle / Work / Security
-keep class androidx.lifecycle.** { *; }
-keep class androidx.compose.** { *; }
-keep class androidx.navigation.** { *; }
-keep class androidx.hilt.** { *; }
-keep class androidx.work.** { *; }
-keep class androidx.security.** { *; }

# Monaco Bridge JS Interface
-keep class com.droidcode.ide.editor.MonacoBridge { *; }
-keepclassmembers class com.droidcode.ide.editor.MonacoBridge { @android.webkit.JavascriptInterface <methods>; }

# Serialization
-keepclassmembers class * implements java.io.Serializable { static final long serialVersionUID; private static final java.io.ObjectStreamField[] serialPersistentFields; private void writeObject(java.io.ObjectOutputStream); private void readObject(java.io.ObjectInputStream); java.lang.Object writeReplace(); java.lang.Object readResolve(); }

# Attributes
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Remove Logs in Release
-assumenosideeffects class android.util.Log { public static *** d(...); public static *** v(...); public static *** i(...); public static *** w(...); }
EOF

echo "✅ Source Code Fixed. Sekarang jalankan: ./fix_wrapper.sh"