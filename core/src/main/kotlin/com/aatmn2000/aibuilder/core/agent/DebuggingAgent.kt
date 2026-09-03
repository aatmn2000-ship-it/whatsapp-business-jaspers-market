package com.aatmn2000.aibuilder.core.agent

import com.aatmn2000.aibuilder.core.ai.AiGateway
import com.aatmn2000.aibuilder.core.ai.AiRequest
import com.aatmn2000.aibuilder.core.pipeline.CodeIssue
import com.aatmn2000.aibuilder.core.project.EditRecord
import com.aatmn2000.aibuilder.core.project.GeneratedProject
import com.aatmn2000.aibuilder.core.project.ProjectFile
import java.time.Instant

/**
 * Fixes reported issues. The core rule: it regenerates ONLY the affected
 * files — it never rewrites the whole application for a small error.
 *
 * Every repair is appended to the project history as an [EditRecord].
 */
class DebuggingAgent(private val gateway: AiGateway) : Agent {

    override val role = AgentRole.DEBUGGING

    override fun run(context: AgentContext): AgentResult {
        // The debugging agent runs inside the build pipeline, not the
        // orchestrator flow.
        return AgentResult(role = role, summary = "Debugging runs inside the build pipeline.")
    }

    /**
     * Repairs [issues] by asking the AI for the complete new content of each
     * affected file. Files without issues are never touched.
     */
    fun fix(project: GeneratedProject, issues: List<CodeIssue>, attempt: Int): GeneratedProject {
        if (issues.isEmpty()) return project
        val profile = DomainProfiler.profileByKey(project.manifest.domain)
            ?: DomainProfiler.defaultProfile()

        val patched = mutableListOf<ProjectFile>()
        issues.distinctBy { it.file }.forEach { issue ->
            val request = AiRequest(
                role = role,
                systemPrompt = "You are the debugging agent of a software builder. " +
                    "Fix the reported problem with the smallest possible change. " +
                    "Return ONLY the complete new content of the affected file — " +
                    "no markdown fences, no commentary.",
                userPrompt = """
                    Project: ${project.manifest.name}
                    Error in ${issue.file} (line ${issue.line}): ${issue.message}

                    path: ${issue.file}
                    domain: ${profile.key}
                    entity: ${profile.entity}
                    service: ${profile.service}
                    project_name: ${project.manifest.name}
                """.trimIndent()
            )
            val response = gateway.complete(request)
            if (!response.isError) {
                patched += ProjectFile(issue.file, response.text, ProjectFile.inferLanguage(issue.file))
            }
        }

        if (patched.isEmpty()) {
            return project.withHistoryRecord(
                EditRecord(
                    timestamp = Instant.now().toString(),
                    reason = "AI repair (attempt $attempt) produced no change",
                    changedFiles = emptyList(),
                    attempt = attempt
                )
            )
        }
        return project.withFiles(
            patched,
            EditRecord(
                timestamp = Instant.now().toString(),
                reason = "AI repair (attempt $attempt): ${issues.first().message}",
                changedFiles = patched.map { it.path },
                attempt = attempt
            )
        )
    }
}
