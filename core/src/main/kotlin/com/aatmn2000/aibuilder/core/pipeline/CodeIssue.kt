package com.aatmn2000.aibuilder.core.pipeline

/**
 * One problem found while compiling or testing a project.
 *
 * [file] names the affected file — this is what makes the repair
 * targeted: the debugging agent only touches the reported files.
 */
data class CodeIssue(
    val file: String,
    val line: Int,
    val message: String,
    val kind: Kind = Kind.COMPILE
) {
    enum class Kind {
        COMPILE,
        TEST,
        SECURITY
    }
}
