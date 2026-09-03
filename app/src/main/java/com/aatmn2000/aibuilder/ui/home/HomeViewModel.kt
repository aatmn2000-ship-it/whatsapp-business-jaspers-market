package com.aatmn2000.aibuilder.ui.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aatmn2000.aibuilder.AppContainer
import com.aatmn2000.aibuilder.core.project.ZipImporter
import com.aatmn2000.aibuilder.data.ProjectSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val projects: List<ProjectSummary> = emptyList(),
    val importedProjectId: String? = null,
    val importError: String? = null
)

class HomeViewModel(
    private val container: AppContainer,
    private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = _state.value.copy(
            projects = container.projectRepository.listProjects()
        )
    }

    /** Reads the ZIP from a SAF content URI and imports it. */
    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            if (bytes == null) {
                _state.value = _state.value.copy(
                    importError = "Could not read the selected file."
                )
                return@launch
            }
            importZipBytes(bytes)
        }
    }

    fun importZipBytes(bytes: ByteArray) {
        viewModelScope.launch {
            val result: ZipImporter.Result = withContext(Dispatchers.IO) {
                container.projectRepository.importZip(bytes)
            }
            if (result.isSuccessful) {
                _state.value = _state.value.copy(
                    importedProjectId = result.project!!.manifest.id,
                    importError = null
                )
                refresh()
            } else {
                _state.value = _state.value.copy(
                    importError = result.issues.joinToString("\n") { it.message }
                )
            }
        }
    }

    fun consumeImportedProject() {
        _state.value = _state.value.copy(importedProjectId = null)
    }

    fun clearImportError() {
        _state.value = _state.value.copy(importError = null)
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                container.projectRepository.deleteProjectDir(id)
            }
            refresh()
        }
    }
}
