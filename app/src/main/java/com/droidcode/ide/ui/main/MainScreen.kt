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
import androidx.compose.material3.icons.filled.FolderOpen
import androidx.compose.material3.icons.filled.Terminal
import androidx.compose.material3.icons.filled.Source
import androidx.compose.material3.icons.filled.VersionControl
import androidx.compose.material3.icons.filled.Extensions
import androidx.compose.material3.icons.filled.Settings
import androidx.compose.material3.icons.filled.Close
import androidx.compose.material3.icons.filled.Search
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

    val configuration = context.resources.configuration
    val isTablet = configuration.smallestScreenWidthDp >= 600

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                modifier = Modifier.fillMaxWidth(),
                title = { 
                    Text(text = activeTab?.label ?: "DroidCode IDE", maxLines = 1, overflow = androidx.compose.ui.text.TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    if (!isTablet) {
                        IconButton(onClick = { viewModel.showCommandPalette() }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Global Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { /* TODO: Toggle Panel */ }) {
                        Icon(Icons.Default.Terminal, contentDescription = "Terminal")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )

            Row(modifier = Modifier
                .fillMaxSize()
                .weight(1f)
            ) {
                if (isTablet) {
                    ActivityBar(
                        selectedView = viewModel.selectedSideBarView.value,
                        onViewClick = viewModel::onSideBarViewClick
                    )
                }

                SideBar(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight(),
                    workspaceUri = currentWorkspace,
                    gitManager = gitManager,
                    onFileClick = { uri, content, lang ->
                        editorViewModel.openFile(uri, content, lang)
                        lspClient.notifyDidOpen(uri, content, lang)
                    },
                    onNewFile = { /* TODO */ },
                    onOpenFolder = onOpenFolder
                )

                EditorArea(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    tabs = tabs,
                    activeTabId = editorViewModel.activeTabId.value,
                    onTabClick = { id -> editorViewModel._activeTabId.value = id },
                    onTabClose = editorViewModel::closeTab,
                    onContentChange = { id, content, version -> editorViewModel.updateContent(id, content, version) },
                    onCursorChange = { id, line, col -> editorViewModel.updateCursor(id, line, col) },
                    lspClient = lspClient,
                    extensionHost = extensionHost
                )

                PanelContainer(
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth(),
                    terminalSession = terminalSession,
                    gitManager = gitManager,
                    currentWorkspace = currentWorkspace
                )
            }

            StatusBar(
                modifier = Modifier.fillMaxWidth(),
                activeTab = activeTab,
                gitBranch = "main",
                lspStatus = "Connected",
                encoding = "UTF-8",
                lineEnding = "LF"
            )
        }

        if (isCommandPaletteOpen) {
            CommandPalette(onDismiss = viewModel::hideCommandPalette)
        }
        if (isSettingsOpen) {
            SettingsScreen(onDismiss = viewModel::hideSettings)
        }
    }
}

@Composable fun ActivityBar(selectedView: String, onViewClick: (String) -> Unit) = Box(Modifier.width(48.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainer)) { Text("Activity Bar", modifier = Modifier.fillMaxSize().padding(16.dp)) }
@Composable fun SideBar(modifier: Modifier, workspaceUri: android.net.Uri?, gitManager: GitManager, onFileClick: (String, String, String) -> Unit, onNewFile: () -> Unit, onOpenFolder: (android.net.Uri) -> Unit) = Box(modifier.background(MaterialTheme.colorScheme.surfaceContainer)) { Text("Side Bar: File Explorer", modifier = modifier.padding(16.dp)) }
@Composable fun EditorArea(modifier: Modifier, tabs: List<EditorTab>, activeTabId: String?, onTabClick: (String) -> Unit, onTabClose: (String) -> Unit, onContentChange: (String, String, Int) -> Unit, onCursorChange: (String, Int, Int) -> Unit, lspClient: LspClient, extensionHost: ExtensionHost) {
    Box(modifier.background(MaterialTheme.colorScheme.surface)) {
        tabs.find { it.id == activeTabId }?.let { tab ->
            EditorWebView(
                modifier = modifier,
                uri = tab.uri,
                initialContent = tab.content,
                language = tab.language,
                onContentChange = { content, version -> onContentChange(tab.id, content, version) },
                onCursorChange = { line, col -> onCursorChange(tab.id, line, col) },
                lspClient = lspClient
            )
        } ?: Text("No Editor Open. Open a file from Sidebar.", modifier = modifier.fillMaxSize().padding(16.dp), style = MaterialTheme.typography.bodyLarge)
    }
}
@Composable fun PanelContainer(modifier: Modifier, terminalSession: TerminalSession, gitManager: GitManager, currentWorkspace: android.net.Uri?) = Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) { Text("Panel: Terminal / Problems", modifier = modifier.padding(16.dp)) }
@Composable fun StatusBar(modifier: Modifier, activeTab: EditorTab?, gitBranch: String, lspStatus: String, encoding: String, lineEnding: String) = Box(modifier.height(24.dp).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 16.dp)) { Row(modifier = modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) { Text("Ln ${activeTab?.cursorPosition.line}, Col ${activeTab?.cursorPosition.column}"); androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f)); Text("$gitBranch | $lspStatus | $encoding | $lineEnding") } }
@Composable fun CommandPalette(onDismiss: () -> Unit) = Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))) { Box(Modifier.size(600.dp, 400.dp).align(Alignment.Center).background(MaterialTheme.colorScheme.surface)) { Text("Command Palette (Ctrl+Shift+P)", modifier = Modifier.fillMaxSize().padding(16.dp)) } }
@Composable fun SettingsScreen(onDismiss: () -> Unit) = Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))) { Box(Modifier.size(600.dp, 800.dp).align(Alignment.Center).background(MaterialTheme.colorScheme.surface)) { Text("Settings (JSON)", modifier = Modifier.fillMaxSize().padding(16.dp)) } } 