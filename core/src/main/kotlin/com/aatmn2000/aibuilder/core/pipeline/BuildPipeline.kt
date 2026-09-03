package com.aatmn2000.aibuilder.core.pipeline

import com.aatmn2000.aibuilder.core.agent.DebuggingAgent
import com.aatmn2000.aibuilder.core.project.GeneratedProject
import com.aatmn2000.aibuilder.core.project.ProjectStatus
import com.aatmn2000.aibuilder.core.project.ZipPackager
import com.aatmn2000.aibuilder.core.security.ProjectSecurityScanner
import com.aatmn2000.aibuilder.core.security.SecurityIssue

/**
 * The build pipeline with automatic debugging:
 *
 * ```
 * Generate ─► Compile ─► Test ─► Error?
 *                               ├── No  → Verify (security) → Package ZIP
 *                               └── Yes → Diagnose
 *                                          → Modify ONLY affected module
 *                                          → Test again
 *                                          → Repeat (max attempts)
 * ```
 *
 * The AI never rewrites the whole application for a small error: the
 * debugging agent is handed the exact issues and only the affected files
 * are regenerated. Repair attempts are bounded by [maxDebugAttempts].
 */
class BuildPipeline(
    private val validator: CodeValidator,
    private val debugger: DebuggingAgent,
    private val maxDebugAttempts: Int = 3
) {

    fun run(project: GeneratedProject, onEvent: (BuildEvent) -> Unit = {}): BuildResult {
        var current = project
        var attempts = 0

        onEvent(BuildEvent.StageStarted("compile"))
        var compileIssues = validator.compile(current)
        onEvent(BuildEvent.CompileReport(compileIssues))
        while (compileIssues.isNotEmpty() && attempts < maxDebugAttempts) {
            attempts++
            onEvent(BuildEvent.DebugAttempt(attempts, compileIssues))
            current = debugger.fix(current, compileIssues, attempts)
            compileIssues = validator.compile(current)
            onEvent(BuildEvent.CompileReport(compileIssues))
        }
        if (compileIssues.isNotEmpty()) {
            onEvent(BuildEvent.Failed("Compile issues remain after $attempts repair attempt(s)"))
            return BuildResult.Failure(
                current.withStatus(ProjectStatus.FAILED),
                compileIssues,
                attempts
            )
        }

        onEvent(BuildEvent.StageStarted("test"))
        var testIssues = validator.runTests(current)
        onEvent(BuildEvent.TestReport(testIssues))
        while (testIssues.isNotEmpty() && attempts < maxDebugAttempts) {
            attempts++
            onEvent(BuildEvent.DebugAttempt(attempts, testIssues))
            current = debugger.fix(current, testIssues, attempts)
            testIssues = validator.runTests(current)
            onEvent(BuildEvent.TestReport(testIssues))
        }
        if (testIssues.isNotEmpty()) {
            onEvent(BuildEvent.Failed("Test issues remain after $attempts repair attempt(s)"))
            return BuildResult.Failure(
                current.withStatus(ProjectStatus.FAILED),
                testIssues,
                attempts
            )
        }

        onEvent(BuildEvent.StageStarted("verify"))
        val securityIssues = ProjectSecurityScanner.scan(current)
        onEvent(BuildEvent.SecurityReport(securityIssues))
        if (securityIssues.any { it.severity == SecurityIssue.Severity.BLOCKING }) {
            onEvent(BuildEvent.Failed("Security scan blocked packaging"))
            return BuildResult.Failure(
                current.withStatus(ProjectStatus.FAILED),
                emptyList(),
                attempts
            )
        }

        onEvent(BuildEvent.StageStarted("package"))
        val zip = ZipPackager.packageProject(current)
        onEvent(BuildEvent.Packaged(zip.size.toLong()))
        return BuildResult.Success(
            current.withStatus(ProjectStatus.BUILT),
            attempts,
            securityIssues
        )
    }
}
