package com.aatmn2000.aibuilder.core.agent

/**
 * Storage layer. V1: local SQLite via the standard library — the
 * no-backend principle in action.
 */
class DatabaseAgent : FileGeneratingAgent(
    role = AgentRole.DATABASE,
    description = "Local SQLite storage layer"
) {

    override fun targetPaths(context: AgentContext): List<String> = listOf("database/storage.py")

    override fun extraInstructions(context: AgentContext): String =
        "Use only the standard library (sqlite3). Create the schema in the " +
            "constructor and expose save/load_all/close. No network access."
}
