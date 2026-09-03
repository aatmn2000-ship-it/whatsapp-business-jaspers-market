package com.aatmn2000.aibuilder.core.agent

/**
 * Language agents planned for V2. They already exist in the swarm so the
 * orchestrator can branch on them; V1 routes all code generation to the
 * [PythonAgent].
 */
class JavaScriptAgent : Agent {
    override val role = AgentRole.JAVASCRIPT
    override fun run(context: AgentContext): AgentResult = AgentResult.unavailable(role)
}

class JavaAgent : Agent {
    override val role = AgentRole.JAVA
    override fun run(context: AgentContext): AgentResult = AgentResult.unavailable(role)
}

class CppAgent : Agent {
    override val role = AgentRole.CPP
    override fun run(context: AgentContext): AgentResult = AgentResult.unavailable(role)
}

class KotlinAgent : Agent {
    override val role = AgentRole.KOTLIN
    override fun run(context: AgentContext): AgentResult = AgentResult.unavailable(role)
}
