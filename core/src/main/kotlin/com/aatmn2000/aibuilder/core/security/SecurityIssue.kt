package com.aatmn2000.aibuilder.core.security

/** One problem found by the security scan (verify stage of the build). */
data class SecurityIssue(
    val severity: Severity,
    val message: String,
    val file: String? = null
) {
    enum class Severity {
        /** Surfaced to the user, does not block packaging. */
        WARNING,
        /** Blocks packaging until resolved. */
        BLOCKING
    }
}
