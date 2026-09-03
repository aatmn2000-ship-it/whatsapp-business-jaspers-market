package com.aatmn2000.aibuilder.core.project

/** One problem found while importing a ZIP. */
data class ImportIssue(
    val severity: Severity,
    val message: String,
    val entryName: String? = null
) {
    enum class Severity {
        WARNING,
        ERROR
    }

    val isFatal: Boolean
        get() = severity == Severity.ERROR
}
