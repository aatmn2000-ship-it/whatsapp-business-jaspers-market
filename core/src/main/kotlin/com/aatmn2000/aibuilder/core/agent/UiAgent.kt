package com.aatmn2000.aibuilder.core.agent

/**
 * Presentation layer. V1: a clean CLI (help / add / list) in app/main.py.
 * V3 will generate full graphical UIs.
 */
class UiAgent : FileGeneratingAgent(
    role = AgentRole.UI,
    description = "CLI presentation layer"
) {

    override fun targetPaths(context: AgentContext): List<String> = listOf("app/main.py")

    override fun extraInstructions(context: AgentContext): String =
        "The CLI must parse command line arguments, construct the storage and " +
            "the domain service, and delegate to the service's run_command. " +
            "It must stay free of business logic."
}
