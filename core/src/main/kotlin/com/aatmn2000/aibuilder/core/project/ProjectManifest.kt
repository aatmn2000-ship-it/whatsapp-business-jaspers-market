package com.aatmn2000.aibuilder.core.project

import kotlinx.serialization.Serializable

/**
 * Machine-readable description of a generated project (`project.json`).
 *
 * This manifest is the contract between the importer, the AI and the user:
 * an imported ZIP without a valid `project.json` is rejected.
 *
 * Schema: v1 (see docs/architecture.md).
 */
@Serializable
data class ProjectManifest(
    val schemaVersion: Int = SCHEMA_VERSION,
    val id: String,
    val name: String,
    val description: String = "",
    val version: String = "0.1.0",
    val language: String = LANGUAGE_PYTHON,
    val platform: List<String> = listOf("local"),
    val entryPoint: String,
    val modules: List<ModuleSpec> = emptyList(),
    val dependencies: List<DependencySpec> = emptyList(),
    val build: BuildSpec = BuildSpec(),
    val storage: StorageSpec? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    companion object {
        const val SCHEMA_VERSION = 1
        const val LANGUAGE_PYTHON = "python"
    }
}

/** One module of a generated project. */
@Serializable
data class ModuleSpec(
    val name: String,
    val path: String,
    val purpose: String = ""
)

/** A declared dependency. [scope] is "runtime" or "test". */
@Serializable
data class DependencySpec(
    val name: String,
    val version: String = "",
    val scope: String = SCOPE_RUNTIME
) {
    companion object {
        const val SCOPE_RUNTIME = "runtime"
        const val SCOPE_TEST = "test"
    }
}

@Serializable
data class BuildSpec(
    val requirements: List<String> = emptyList(),
    val commands: Commands = Commands()
)

@Serializable
data class Commands(
    val run: String = "",
    val test: String = ""
)

@Serializable
data class StorageSpec(
    val kind: String = KIND_SQLITE,
    val path: String = "data/app.db"
) {
    companion object {
        const val KIND_SQLITE = "sqlite"
    }
}
