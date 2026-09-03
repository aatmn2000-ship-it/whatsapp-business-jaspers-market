package com.aatmn2000.aibuilder.core.pipeline

import com.aatmn2000.aibuilder.core.project.GeneratedProject

/**
 * Validates generated code.
 *
 * V1 uses static analysis (see [PythonStaticValidator]) so that no
 * generated code is ever executed automatically. V2 replaces this with
 * real execution inside a sandbox — same interface.
 */
interface CodeValidator {
    val language: String

    fun compile(project: GeneratedProject): List<CodeIssue>

    fun runTests(project: GeneratedProject): List<CodeIssue>
}
