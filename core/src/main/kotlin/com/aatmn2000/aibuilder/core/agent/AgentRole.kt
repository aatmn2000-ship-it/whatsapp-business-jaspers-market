package com.aatmn2000.aibuilder.core.agent

/**
 * The specialized agents of the swarm. The Orchestrator decides which ones
 * a task needs — no single giant prompt.
 */
enum class AgentRole {
    REQUIREMENT,
    ARCHITECTURE,
    UI,
    DATABASE,
    PYTHON,
    JAVASCRIPT,
    JAVA,
    CPP,
    KOTLIN,
    TESTING,
    SECURITY,
    DEBUGGING,
    DOCUMENTATION
}
