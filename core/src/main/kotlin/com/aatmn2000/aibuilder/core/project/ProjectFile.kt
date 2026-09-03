package com.aatmn2000.aibuilder.core.project

/**
 * A single file inside a generated project.
 *
 * [path] is always relative, slash-separated, and must pass
 * [com.aatmn2000.aibuilder.core.security.PathTraversalGuard] before it is
 * written to disk or into a ZIP.
 */
data class ProjectFile(
    val path: String,
    val content: String,
    val language: String? = null
) {
    companion object {
        fun inferLanguage(path: String): String? = when {
            path.endsWith(".py") -> "python"
            path.endsWith(".js") -> "javascript"
            path.endsWith(".ts") -> "typescript"
            path.endsWith(".java") -> "java"
            path.endsWith(".cpp") || path.endsWith(".cc") || path.endsWith(".h") -> "cpp"
            path.endsWith(".kt") -> "kotlin"
            path.endsWith(".json") -> "json"
            path.endsWith(".md") -> "markdown"
            path.endsWith(".sh") -> "bash"
            else -> null
        }
    }
}
