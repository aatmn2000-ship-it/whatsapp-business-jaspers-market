package com.aatmn2000.aibuilder.core.pipeline

import com.aatmn2000.aibuilder.core.security.SecurityIssue

/** Progress events streamed by the [BuildPipeline] for the UI. */
sealed class BuildEvent {
    data class StageStarted(val stage: String) : BuildEvent()
    data class CompileReport(val issues: List<CodeIssue>) : BuildEvent()
    data class TestReport(val issues: List<CodeIssue>) : BuildEvent()
    data class DebugAttempt(val attempt: Int, val issues: List<CodeIssue>) : BuildEvent()
    data class SecurityReport(val issues: List<SecurityIssue>) : BuildEvent()
    data class Packaged(val zipSizeBytes: Long) : BuildEvent()
    data class Failed(val reason: String) : BuildEvent()
}
