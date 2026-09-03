package com.aatmn2000.aibuilder.core.agent

/**
 * Designs the project layout: runtime configuration and architecture
 * documentation. V1 layout: app/ + modules/ + database/ + tests/ + docs/.
 */
class ArchitectureAgent : FileGeneratingAgent(
    role = AgentRole.ARCHITECTURE,
    description = "Architecture design (config + architecture docs)"
) {

    override fun targetPaths(context: AgentContext): List<String> =
        listOf("config/config.json", "docs/architecture.md")

    override fun extraInstructions(context: AgentContext): String =
        "Design a local-first architecture: no backend, local SQLite storage, " +
            "one module per concern, entry point in app/. The user asked for: " +
            context.userRequest
}
