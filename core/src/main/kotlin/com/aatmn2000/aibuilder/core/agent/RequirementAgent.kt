package com.aatmn2000.aibuilder.core.agent

import com.aatmn2000.aibuilder.core.ai.AiGateway
import com.aatmn2000.aibuilder.core.ai.AiRequest

/**
 * Understands what the user actually wants and turns the raw request into
 * a concise structured requirement (JSON).
 */
class RequirementAgent(private val gateway: AiGateway) : Agent {

    override val role = AgentRole.REQUIREMENT

    override fun run(context: AgentContext): AgentResult {
        val request = AiRequest(
            role = role,
            systemPrompt = "You are the requirement analysis agent of a software builder. " +
                "Extract the user's intent as a concise structured requirement. " +
                "Reply with a single JSON object and nothing else.",
            userPrompt = """
                User request:
                ${context.userRequest}

                Reply with JSON:
                {"name": ..., "description": ..., "ui": "cli", "storage": "sqlite"}

                domain: ${context.profile.key}
                project_name: ${context.projectName}
            """.trimIndent()
        )
        val response = gateway.complete(request)
        if (response.isError) {
            return AgentResult(
                role = role,
                summary = "Requirement analysis failed",
                isError = true,
                errorDetail = response.errorDetail
            )
        }
        return AgentResult(role = role, summary = response.text)
    }
}
