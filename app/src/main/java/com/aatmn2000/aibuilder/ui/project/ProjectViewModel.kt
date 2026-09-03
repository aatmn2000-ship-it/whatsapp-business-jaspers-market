package com.aatmn2000.aibuilder.ui.project

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aatmn2000.aibuilder.AppContainer
import com.aatmn2000.aibuilder.core.project.GeneratedProject
import com.aatmn2000.aibuilder.ui.share.ShareZip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Loads one stored project (immutable within the navigation entry). */
class ProjectViewModel(
    private val container: AppContainer,
    private val appContext: Context,
    initialProjectId: String
) : ViewModel() {

    val project: GeneratedProject? = container.projectRepository.load(initialProjectId)

    val projectId: String = project?.manifest?.id ?: initialProjectId
    val projectName: String = project?.manifest?.name.orEmpty()

    fun shareZip() {
        val bytes = container.projectRepository.exportZip(projectId) ?: return
        ShareZip.share(
            appContext,
            container.projectRepository,
            projectName.ifBlank { "project" },
            bytes
        )
    }

    fun deleteProject(onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                container.projectRepository.deleteProjectDir(projectId)
            }
            onDone()
        }
    }
}
