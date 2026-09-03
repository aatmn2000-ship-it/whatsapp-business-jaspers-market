package com.aatmn2000.aibuilder.core.agent

import com.aatmn2000.aibuilder.core.ai.AiGateway
import com.aatmn2000.aibuilder.core.pipeline.BuildAbortedException
import com.aatmn2000.aibuilder.core.project.EditRecord
import com.aatmn2000.aibuilder.core.project.GeneratedProject
import com.aatmn2000.aibuilder.core.project.ModuleSpec
import com.aatmn2000.aibuilder.core.project.ProjectFactory
import com.aatmn2000.aibuilder.core.project.ProjectFile
import com.aatmn2000.aibuilder.core.project.StorageSpec
import java.time.Instant

/** One step in an execution plan, with the reason it is needed. */
data class AgentStep(
    val role: AgentRole,
    val reason: String
)

/** The orchestrator's plan for a task. */
data class AgentPlan(
    val steps: List<AgentStep>
)

/** Progress events streamed while the orchestrator runs. */
sealed class AgentEvent {
    data class PlanReady(val plan: AgentPlan) : AgentEvent()
    data class AgentStarted(val role: AgentRole, val reason: String) : AgentEvent()
    data class AgentFinished(
        val role: AgentRole,
        val summary: String,
        val error: Boolean
    ) : AgentEvent()
}

/**
 * The AI Orchestrator: decides which specialized agents a task needs and
 * runs them in order, forwarding context between them.
 *
 * Planning rules (V1):
 *  - new project:     Requirement → Architecture → Database → UI → Python →
 *                     Testing → Security → Documentation
 *  - modification:    Requirement → Python (affected modules) → Testing →
 *                     Documentation
 */
class Orchestrator(private val agents: Map<AgentRole, Agent>) {

    fun planFor(userRequest: String, existingProject: GeneratedProject?): AgentPlan {
        return if (existingProject == null) {
            AgentPlan(
                listOf(
                    AgentStep(AgentRole.REQUIREMENT, "Understand the request"),
                    AgentStep(AgentRole.ARCHITECTURE, "Design modules, config and documentation layout"),
                    AgentStep(AgentRole.DATABASE, "Local-first SQLite storage (no backend needed)"),
                    AgentStep(AgentRole.UI, "CLI presentation layer"),
                    AgentStep(AgentRole.PYTHON, "Business logic in Python (V1 language)"),
                    AgentStep(AgentRole.TESTING, "Unit tests"),
                    AgentStep(AgentRole.SECURITY, "Security review"),
                    AgentStep(AgentRole.DOCUMENTATION, "README and docs")
                )
            )
        } else {
            AgentPlan(
                listOf(
                    AgentStep(AgentRole.REQUIREMENT, "Understand the modification request"),
                    AgentStep(AgentRole.PYTHON, "Update the business-logic modules"),
                    AgentStep(AgentRole.TESTING, "Refresh the test suite"),
                    AgentStep(AgentRole.DOCUMENTATION, "Refresh the documentation")
                )
            )
        }
    }

    /**
     * Creates a brand-new project from a natural-language request.
     *
     * @throws BuildAbortedException when an agent fails and the build cannot continue.
     */
    fun createProject(
        userRequest: String,
        gateway: AiGateway,
        onEvent: (AgentEvent) -> Unit = {}
    ): GeneratedProject {
        val profile = DomainProfiler.profileFor(userRequest)
        val projectName = ProjectNamer.nameFor(profile)
        val plan = planFor(userRequest, null)
        onEvent(AgentEvent.PlanReady(plan))

        val files = mutableListOf<ProjectFile>()
        var description = profile.description

        plan.steps.forEach { step ->
            val agent = agents[step.role] ?: return@forEach
            onEvent(AgentEvent.AgentStarted(step.role, step.reason))
            val context = AgentContext(
                gateway = gateway,
                userRequest = userRequest,
                existingProject = null,
                profile = profile,
                projectName = projectName,
                generatedSoFar = files.toList()
            )
            val result = agent.run(context)
            if (result.isError) {
                onEvent(AgentEvent.AgentFinished(step.role, result.summary, error = true))
                throw BuildAbortedException(
                    "${step.role.name} agent failed: ${result.errorDetail ?: "unknown error"}"
                )
            }
            if (step.role == AgentRole.REQUIREMENT) {
                description = extractDescription(result.summary) ?: profile.description
            }
            files += result.artifacts
            onEvent(AgentEvent.AgentFinished(step.role, result.summary, error = false))
        }

        val modules = files
            .filter { it.path.startsWith("modules/") }
            .map { file ->
                ModuleSpec(
                    name = file.path.removePrefix("modules/").removeSuffix(".py"),
                    path = file.path,
                    purpose = "Business logic module"
                )
            } + ModuleSpec(name = "storage", path = "database/storage.py", purpose = "Local storage")

        val entryPoint = files.firstOrNull { it.path == "app/main.py" }?.path
            ?: files.firstOrNull { it.path.endsWith(".py") }?.path
            ?: throw BuildAbortedException("No entry point file was generated")

        return ProjectFactory.create(
            name = projectName,
            description = description,
            files = files.toList(),
            modules = modules,
            entryPoint = entryPoint,
            domain = profile.key,
            storage = StorageSpec()
        )
    }

    /**
     * Modifies an existing (usually imported) project. The AI edits the
     * project instead of creating a new one; every change is recorded in
     * the project history.
     */
    fun modifyProject(
        project: GeneratedProject,
        userRequest: String,
        gateway: AiGateway,
        onEvent: (AgentEvent) -> Unit = {}
    ): GeneratedProject {
        val profile = DomainProfiler.profileByKey(project.manifest.domain)
            ?: DomainProfiler.defaultProfile()
        val plan = planFor(userRequest, project)
        onEvent(AgentEvent.PlanReady(plan))

        var current = project
        plan.steps.forEach { step ->
            val agent = agents[step.role] ?: return@forEach
            onEvent(AgentEvent.AgentStarted(step.role, step.reason))
            val context = AgentContext(
                gateway = gateway,
                userRequest = userRequest,
                existingProject = current,
                profile = profile,
                projectName = current.manifest.name,
                generatedSoFar = current.files
            )
            val result = agent.run(context)
            if (result.isError) {
                onEvent(AgentEvent.AgentFinished(step.role, result.summary, error = true))
                throw BuildAbortedException(
                    "${step.role.name} agent failed: ${result.errorDetail ?: "unknown error"}"
                )
            }
            if (result.artifacts.isNotEmpty()) {
                current = current.withFiles(
                    result.artifacts,
                    EditRecord(
                        timestamp = Instant.now().toString(),
                        reason = "AI modification: ${userRequest.take(80)}",
                        changedFiles = result.artifacts.map { it.path }
                    )
                )
            }
            onEvent(AgentEvent.AgentFinished(step.role, result.summary, error = false))
        }
        return current
    }

    private fun extractDescription(requirementJson: String): String? {
        val match = Regex("\"description\"\\s*:\\s*\"([^\"]*)\"").find(requirementJson)
        return match?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
    }

    companion object {
        /** Creates the V1 swarm: all agents, with Python as the language agent. */
        fun createDefault(gateway: AiGateway): Orchestrator {
            val agents: Map<AgentRole, Agent> = mapOf(
                AgentRole.REQUIREMENT to RequirementAgent(gateway),
                AgentRole.ARCHITECTURE to ArchitectureAgent(),
                AgentRole.UI to UiAgent(),
                AgentRole.DATABASE to DatabaseAgent(),
                AgentRole.PYTHON to PythonAgent(),
                AgentRole.JAVASCRIPT to JavaScriptAgent(),
                AgentRole.JAVA to JavaAgent(),
                AgentRole.CPP to CppAgent(),
                AgentRole.KOTLIN to KotlinAgent(),
                AgentRole.TESTING to TestingAgent(),
                AgentRole.SECURITY to SecurityAgent(gateway),
                AgentRole.DEBUGGING to DebuggingAgent(gateway),
                AgentRole.DOCUMENTATION to DocumentationAgent()
            )
            return Orchestrator(agents)
        }
    }
}
