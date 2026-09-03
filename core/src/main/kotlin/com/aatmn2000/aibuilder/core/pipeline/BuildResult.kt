package com.aatmn2000.aibuilder.core.pipeline

import com.aatmn2000.aibuilder.core.project.GeneratedProject
import com.aatmn2000.aibuilder.core.security.SecurityIssue

/** The outcome of a full build: compile → test → (debug loop) → verify → package. */
sealed class BuildResult {

    data class Success(
        val project: GeneratedProject,
        val repairAttempts: Int,
        val securityIssues: List<SecurityIssue>
    ) : BuildResult()

    data class Failure(
        val project: GeneratedProject,
        val issues: List<CodeIssue>,
        val repairAttempts: Int
    ) : BuildResult()
}
