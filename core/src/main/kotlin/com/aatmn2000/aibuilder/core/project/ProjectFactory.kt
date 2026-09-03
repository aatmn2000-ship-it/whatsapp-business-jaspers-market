package com.aatmn2000.aibuilder.core.project

import java.time.Instant
import java.util.UUID

/**
 * Builds [GeneratedProject] instances from agent artifacts.
 *
 * V1 projects are Python, stdlib-only, local-first (SQLite) and run with:
 * `python app/main.py`.
 */
object ProjectFactory {

    fun create(
        name: String,
        description: String,
        files: List<ProjectFile>,
        modules: List<ModuleSpec>,
        entryPoint: String,
        language: String = ProjectManifest.LANGUAGE_PYTHON,
        domain: String = "",
        storage: StorageSpec? = StorageSpec()
    ): GeneratedProject {
        val now = Instant.now().toString()
        val manifest = ProjectManifest(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            entryPoint = entryPoint,
            language = language,
            domain = domain,
            modules = modules,
            build = BuildSpec(
                requirements = listOf("python>=3.10"),
                commands = Commands(
                    run = "python $entryPoint",
                    test = "python -m unittest discover -s tests"
                )
            ),
            storage = storage,
            createdAt = now,
            updatedAt = now
        )
        return GeneratedProject(
            manifest = manifest,
            files = files,
            status = ProjectStatus.DRAFT,
            history = listOf(
                EditRecord(
                    timestamp = now,
                    reason = "Initial generation",
                    changedFiles = files.map { it.path }
                )
            )
        )
    }
}
