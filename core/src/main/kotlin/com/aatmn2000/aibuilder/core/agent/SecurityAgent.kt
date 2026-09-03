package com.aatmn2000.aibuilder.core.agent

import com.aatmn2000.aibuilder.core.ai.AiGateway
import com.aatmn2000.aibuilder.core.ai.AiRequest

/**
 * Security review of the generated project: secrets, network access,
 * unsafe dependencies. (The mechanical scan — SecretScanner,
 * DependencyScanner, SandboxPolicy — runs separately in the verify stage.)
 */
class SecurityAgent(private val gateway: AiGateway) : Agent {

    override val role = AgentRole.SECURITY

    override fun run(context: AgentContext): AgentResult {
        val fileList = context.generatedSoFar.joinToString("\n") { "- ${it.path}" }
        val request = AiRequest(
            role = role,
            systemPrompt = "You are the security review agent of a software builder. " +
                "Check the project for secrets, network access and unsafe " +
                "dependencies. Reply with a short plain-text report.",
            userPrompt = """
                Project: ${context.projectName}
                Files:
                $fileList

                domain: ${context.profile.key}
                project_name: ${context.projectName}
            """.trimIndent()
        )
        val response = gateway.complete(request)
        if (response.isError) {
            return AgentResult(
                role = role,
                summary = "Security review failed",
                isError = true,
                errorDetail = response.errorDetail
            )
        }
        return AgentResult(role = role, summary = response.text.take(300))
    }
}
