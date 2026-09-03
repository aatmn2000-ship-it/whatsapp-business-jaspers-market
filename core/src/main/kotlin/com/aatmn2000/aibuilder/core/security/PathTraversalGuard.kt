package com.aatmn2000.aibuilder.core.security

/**
 * Guards against malicious ZIP / file entry names (path traversal, absolute
 * paths, drive letters). Every imported or generated path must pass
 * [isSafe] before it is used for disk or ZIP operations.
 */
object PathTraversalGuard {

    fun isSafe(name: String): Boolean {
        if (name.isEmpty()) return false
        val normalized = name.replace('\\', '/')
        if (normalized.startsWith("/")) return false
        if (driveLetterRegex.containsMatchIn(normalized)) return false
        if (normalized.endsWith(":")) return false
        val segments = normalized.split('/').filter { it.isNotEmpty() }
        return segments.isNotEmpty() && segments.none { it == ".." }
    }

    /**
     * Collapses redundant separators and leading "./" segments.
     * Returns null when the name is not safe.
     */
    fun normalize(name: String): String? {
        if (!isSafe(name)) return null
        val normalized = name.replace('\\', '/')
        val segments = normalized.split('/').filter { it.isNotEmpty() && it != "." }
        return segments.joinToString("/")
    }

    private val driveLetterRegex = Regex("^[A-Za-z]:")
}
