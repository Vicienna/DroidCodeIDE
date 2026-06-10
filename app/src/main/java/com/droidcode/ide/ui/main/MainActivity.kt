package com.droidcode.ide.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droidcode.ide.DroidCodeApplication
import com.droidcode.ide.editor.EditorViewModel
import com.droidcode.ide.editor.MonacoBridge
import com.droidcode.ide.extensions.ExtensionHost
import com.droidcode.ide.git.GitManager
import com.droidcode.ide.lsp.LspClient
import com.droidcode.ide.terminal.TerminalSession
import com.droidcode.ide.ui.theme.Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var lspClient: LspClient
    @Inject lateinit var terminalSession: TerminalSession
    @Inject lateinit var gitManager: GitManager
    @Inject lateinit var extensionHost: ExtensionHost

    private val mainViewModel: MainViewModel by viewModel()
    private val editorViewModel: EditorViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        extensionHost.initialize(this)
        
        enableEdgeToEdge()
        setContent {
            Theme {
                MainScreen(
                    viewModel = mainViewModel,
                    editorViewModel = editorViewModel,
                    lspClient = lspClient,
                    terminalSession = terminalSession,
                    gitManager = gitManager,
                    onOpenFolder = { uri -> handleOpenFolder(uri) }
                )
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val data = intent.data
        if (Intent.ACTION_VIEW == action && data != null) {
            mainViewModel.openProjectFromUri(data)
        } else if (Intent.ACTION_OPEN_DOCUMENT_TREE == action && data != null) {
            mainViewModel.openProjectFromUri(data)
        }
    }

    private fun handleOpenFolder(uri: Uri) {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, takeFlags)
        mainViewModel.setCurrentWorkspace(uri)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 1, 0, "Command Palette").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        menu?.add(0, 2, 0, "Settings").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            1 -> { mainViewModel.showCommandPalette(); true }
            2 -> { mainViewModel.showSettings(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 