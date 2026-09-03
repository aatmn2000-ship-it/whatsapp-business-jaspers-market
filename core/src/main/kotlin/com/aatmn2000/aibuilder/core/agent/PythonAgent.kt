package com.aatmn2000.aibuilder.core.agent

/**
 * Business-logic modules — the V1 language agent. Produces one module per
 * concern under modules/ so the AI can later repair a single module without
 * touching the rest of the project.
 */
class PythonAgent : FileGeneratingAgent(
    role = AgentRole.PYTHON,
    description = "Python business-logic modules"
) {

    override fun targetPaths(context: AgentContext): List<String> =
        listOf("modules/${context.profile.key}.py")

    override fun extraInstructions(context: AgentContext): String =
        "Define the data class ${context.profile.entity} and the service " +
            "${context.profile.service}. The service must be IO-free (it " +
            "receives a storage object) so it can be unit tested, and must " +
            "expose create / list_all / run_command / print_help."
}
