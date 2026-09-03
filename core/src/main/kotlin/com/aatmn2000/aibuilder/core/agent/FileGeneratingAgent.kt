package com.aatmn2000.aibuilder.core.agent

import com.aatmn2000.aibuilder.core.ai.AiRequest
import com.aatmn2000.aibuilder.core.project.ProjectFile

/**
 * Base for agents whose job is to produce one or more complete files.
 *
 * The user prompt ends with machine-readable token lines so any provider —
 * including the deterministic mock — can answer file-generation requests.
 */
abstract class FileGeneratingAgent(
    override val role: AgentRole,
    private val description: String
) : Agent {

    /** The file paths this agent is responsible for. */
    protected abstract fun targetPaths(context: AgentContext): List<String>

    protected open fun extraInstructions(context: AgentContext): String = ""

    override fun run(context: AgentContext): AgentResult {
        val artifacts = mutableListOf<ProjectFile>()
        targetPaths(context).forEach { path ->
            val request = AiRequest(
                role = role,
                systemPrompt = SYSTEM_PROMPT,
                userPrompt = buildUserPrompt(context, path)
            )
            val response = context.gateway.complete(request)
            if (response.isError) {
                return AgentResult(
                    role = role,
                    summary = description,
                    isError = true,
                    errorDetail = response.errorDetail
                )
            }
            artifacts += ProjectFile(path, response.text, ProjectFile.inferLanguage(path))
        }
        return AgentResult(role = role, summary = "$description (${artifacts.size} file(s))", artifacts = artifacts)
    }

    protected fun buildUserPrompt(context: AgentContext, path: String): String {
        return """
            $description for the project "${context.projectName}" (${context.profile.description}).
            ${extraInstructions(context)}
            Return ONLY the complete file content — no markdown fences, no commentary.

            path: $path
            domain: ${context.profile.key}
            entity: ${context.profile.entity}
            service: ${context.profile.service}
            project_name: ${context.projectName}
        """.trimIndent()
    }

    companion object {
        const val SYSTEM_PROMPT =
            "You generate code for a local-first software project. " +
                "Write clean, complete, runnable code. " +
                "Never include API keys, secrets or network calls. " +
                "Never wrap the answer in markdown code fences."
    }
}
