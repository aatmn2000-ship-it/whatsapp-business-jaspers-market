package com.aatmn2000.aibuilder.core.ai

import com.aatmn2000.aibuilder.core.agent.AgentRole
import com.aatmn2000.aibuilder.core.agent.DomainProfiler
import com.aatmn2000.aibuilder.core.agent.DomainProfile

/**
 * Deterministic, offline AI provider.
 *
 * It understands the token convention used by the agents (`path:`,
 * `domain:`, `entity:`, `service:`, `project_name:` appended to the user
 * prompt) and answers with full file contents from [MockPythonTemplates].
 *
 * The mock makes the entire pipeline — planning, generation, validation,
 * debugging, packaging — reproducible without any model or network, which
 * is what development, demos and unit tests rely on.
 */
class MockAiProvider : AiProvider {

    override val id: String = PROVIDER_ID

    override val displayName: String = "Mock (offline, deterministic)"

    override fun isAvailable(): Boolean = true

    override fun complete(request: AiRequest): AiResponse {
        return try {
            val tokens = parseTokens(request.userPrompt)
            val path = tokens["path"]
            val profile = resolveProfile(tokens["domain"])
            val projectName = tokens["project_name"] ?: "MyApp"

            val text = when (request.role) {
                AgentRole.REQUIREMENT -> requirementText(projectName, profile)
                AgentRole.ARCHITECTURE -> when (path) {
                    "config/config.json" -> MockPythonTemplates.configJson(projectName, profile)
                    else -> MockPythonTemplates.architectureDoc(projectName, profile)
                }
                AgentRole.UI -> MockPythonTemplates.mainPy(projectName, profile)
                AgentRole.DATABASE -> MockPythonTemplates.storagePy()
                AgentRole.PYTHON -> MockPythonTemplates.modulePy(profile)
                AgentRole.TESTING -> MockPythonTemplates.testPy(profile)
                AgentRole.DOCUMENTATION -> when (path) {
                    "docs/overview.md" -> MockPythonTemplates.overview(projectName, profile)
                    else -> MockPythonTemplates.readme(projectName, profile)
                }
                AgentRole.SECURITY -> "Security review passed: no secrets found, " +
                    "stdlib-only dependencies, local-first storage, no network access."
                AgentRole.DEBUGGING -> MockPythonTemplates.fixedFile(path.orEmpty(), profile, projectName)
                else -> "Agent ${request.role.name} is not available in the mock provider (planned for V2)."
            }
            AiResponse(text = text, providerId = id, model = MODEL)
        } catch (e: Exception) {
            AiResponse.error(id, e.message ?: "mock provider failure")
        }
    }

    private fun requirementText(projectName: String, profile: DomainProfile): String =
        """{"name":"$projectName","description":"${profile.description}","language":"python","ui":"cli","storage":"sqlite"}"""

    private fun resolveProfile(domain: String?): DomainProfile {
        if (domain == null) return DomainProfiler.defaultProfile()
        return DomainProfiler.allProfiles().firstOrNull { it.key == domain }
            ?: DomainProfiler.defaultProfile()
    }

    private fun parseTokens(prompt: String): Map<String, String> {
        val tokens = mutableMapOf<String, String>()
        prompt.lineSequence().forEach { line ->
            val match = TOKEN_REGEX.find(line.trim())
            if (match != null) {
                tokens[match.groupValues[1]] = match.groupValues[2].trim()
            }
        }
        return tokens
    }

    companion object {
        const val PROVIDER_ID = "mock"
        const val MODEL = "mock-1.0"
        private val TOKEN_REGEX = Regex("^([a-z_]+):\\s*(.+)$")
    }
}
