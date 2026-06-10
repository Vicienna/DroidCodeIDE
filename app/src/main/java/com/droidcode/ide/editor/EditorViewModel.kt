package com.droidcode.ide.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor() : ViewModel() {

    data class EditorTab(
        val id: String = java.util.UUID.randomUUID().toString(),
        val uri: String,
        val label: String,
        val language: String,
        var content: String = "",
        var isDirty: Boolean = false,
        var cursorPosition: CursorPos = CursorPos(1, 1)
    )
    data class CursorPos(val line: Int, val column: Int)

    private val _tabs = MutableStateFlow<MutableList<EditorTab>>(mutableListOf())
    val tabs = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId = _activeTabId.asStateFlow()

    val activeTab = tabs.map { tabs ->
        tabs.find { it.id == _activeTabId.value }
    }.asStateFlow()

    fun openFile(uri: String, content: String, language: String) {
        viewModelScope.launch {
            val existing = _tabs.value.find { it.uri == uri }
            if (existing != null) {
                _activeTabId.value = existing.id
            } else {
                val tab = EditorTab(uri = uri, label = uri.substringAfterLast("/"), language = language, content = content)
                _tabs.value = _tabs.value + tab
                _activeTabId.value = tab.id
            }
        }
    }

    fun closeTab(tabId: String) {
        viewModelScope.launch {
            val tabs = _tabs.value.toMutableList()
            val index = tabs.indexOfFirst { it.id == tabId }
            if (index >= 0) {
                tabs.removeAt(index)
                _tabs.value = tabs
                if (_activeTabId.value == tabId) {
                    _activeTabId.value = tabs.getOrNull(index)?.id ?: tabs.lastOrNull()?.id
                }
            }
        }
    }

    fun updateContent(tabId: String, content: String, version: Int) {
        _tabs.value.find { it.id == tabId }?.let {
            it.content = content
            it.isDirty = true
        }
    }

    fun updateCursor(tabId: String, line: Int, col: Int) {
        _tabs.value.find { it.id == tabId }?.cursorPosition = CursorPos(line, col)
    }

    fun setSaved(tabId: String) {
        _tabs.value.find { it.id == tabId }?.isDirty = false
    }
} 