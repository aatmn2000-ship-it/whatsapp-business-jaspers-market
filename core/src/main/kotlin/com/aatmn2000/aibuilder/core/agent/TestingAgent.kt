package com.aatmn2000.aibuilder.core.agent

/**
 * Writes the test suite. V1: unittest-based tests with an in-memory fake
 * storage so the tests run anywhere Python runs — no backend, no network.
 */
class TestingAgent : FileGeneratingAgent(
    role = AgentRole.TESTING,
    description = "Unit test suite"
) {

    override fun targetPaths(context: AgentContext): List<String> =
        listOf("tests/test_${context.profile.key}.py")

    override fun extraInstructions(context: AgentContext): String =
        "Use the standard library unittest. Import the service from " +
            "modules.${context.profile.key} and test it with an in-memory " +
            "fake storage. Every test method must be named test_*."
}
