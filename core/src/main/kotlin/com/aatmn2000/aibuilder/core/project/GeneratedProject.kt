package com.aatmn2000.aibuilder.core.project

import kotlinx.serialization.Serializable

/** Lifecycle state of a project inside the app. */
enum class ProjectStatus {
    DRAFT,
    BUILT,
    FAILED,
    IMPORTED
}

/**
 * One recorded change to a project: initial generation, an AI repair, or an
 * imported ZIP. This is the version history the user can inspect.
 */
@Serializable
data class EditRecord(
    val timestamp: String,
    val reason: String,
    val changedFiles: List<String>,
    val attempt: Int = 0
)

/**
 * A complete, portable software project: manifest + source files + history.
 */
data class GeneratedProject(
    val manifest: ProjectManifest,
    val files: List<ProjectFile>,
    val status: ProjectStatus = ProjectStatus.DRAFT,
    val history: List<EditRecord> = emptyList()
) {

    fun fileAt(path: String): ProjectFile? = files.firstOrNull { it.path == path }

    /**
     * Returns a copy in which [newFiles] replace files with the same path
     * (new files are appended) and [record] is appended to the history.
     */
    fun withFiles(newFiles: List<ProjectFile>, record: EditRecord): GeneratedProject {
        val replaced = files.map { existing ->
            newFiles.firstOrNull { it.path == existing.path } ?: existing
        }
        val added = newFiles.filter { file -> files.none { it.path == file.path } }
        return GeneratedProject(
            manifest = manifest.copy(updatedAt = record.timestamp),
            files = replaced + added,
            status = status,
            history = history + record
        )
    }

    fun withStatus(newStatus: ProjectStatus): GeneratedProject = copy(status = newStatus)

    fun withHistoryRecord(record: EditRecord): GeneratedProject {
        return GeneratedProject(
            manifest = manifest,
            files = files,
            status = status,
            history = history + record
        )
    }
}
