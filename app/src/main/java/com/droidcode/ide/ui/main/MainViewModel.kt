package com.droidcode.ide.ui.main

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidcode.ide.data.db.dao.ProjectDao
import com.droidcode.ide.data.db.entity.ProjectEntity
import com.droidcode.ide.git.GitManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class MainViewModel @Inject constructor(
    private val projectDao: ProjectDao
) : ViewModel() {

    private val _currentWorkspace = MutableStateFlow<Uri?>(null)
    val currentWorkspace = _currentWorkspace.asStateFlow()

    private val _recentProjects = MutableStateFlow<List<ProjectEntity>>(emptyList())
    val recentProjects = _recentProjects.asStateFlow()

    private val _selectedSideBarView = MutableStateFlow("explorer")
    val selectedSideBarView = _selectedSideBarView.asStateFlow()

    fun onSideBarViewClick(viewId: String) {
        _selectedSideBarView.value = viewId
    }

    private val _isCommandPaletteOpen = MutableStateFlow(false)
    val isCommandPaletteOpen = _isCommandPaletteOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen = _isSettingsOpen.asStateFlow()

    init {
        loadRecentProjects()
    }

    private fun loadRecentProjects() {
        viewModelScope.launch {
            projectDao.getAllProjects().collect { list ->
                _recentProjects.value = list
                if (_currentWorkspace.value == null && list.isNotEmpty()) {
                    _currentWorkspace.value = Uri.parse(list[0].uri)
                }
            }
        }
    }

    fun setCurrentWorkspace(uri: Uri) {
        _currentWorkspace.value = uri
        viewModelScope.launch {
            projectDao.insert(ProjectEntity(uri.toString(), getDisplayName(uri), System.currentTimeMillis()))
        }
    }

    fun openProjectFromUri(uri: Uri) {
        setCurrentWorkspace(uri)
    }

    fun showCommandPalette() { _isCommandPaletteOpen.value = true }
    fun hideCommandPalette() { _isCommandPaletteOpen.value = false }

    fun showSettings() { _isSettingsOpen.value = true }
    fun hideSettings() { _isSettingsOpen.value = false }

    private fun getDisplayName(uri: Uri): String {
        return uri.lastPathSegment ?: "Project"
    }
} 