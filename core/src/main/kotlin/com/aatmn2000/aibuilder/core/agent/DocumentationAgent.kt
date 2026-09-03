package com.aatmn2000.aibuilder.core.agent

/**
 * Writes the human-readable documentation: README.md and docs/overview.md.
 */
class DocumentationAgent : FileGeneratingAgent(
    role = AgentRole.DOCUMENTATION,
    description = "Documentation (README + overview)"
) {

    override fun targetPaths(context: AgentContext): List<String> =
        listOf("README.md", "docs/overview.md")

    override fun extraInstructions(context: AgentContext): String =
        "Document how to run and test the project, and describe the module " +
            "layout. Keep it short and practical."
}
