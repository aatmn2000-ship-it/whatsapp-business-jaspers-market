package com.aatmn2000.aibuilder.core.agent

import com.aatmn2000.aibuilder.core.ai.AiGateway
import com.aatmn2000.aibuilder.core.ai.MockAiProvider
import com.aatmn2000.aibuilder.core.pipeline.BuildPipeline
import com.aatmn2000.aibuilder.core.pipeline.BuildResult
import com.aatmn2000.aibuilder.core.pipeline.PythonStaticValidator
import com.aatmn2000.aibuilder.core.project.GeneratedProject
import com.aatmn2000.aibuilder.core.project.ProjectManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrchestratorTest {

    private fun gateway(): AiGateway = AiGateway(listOf(MockAiProvider()), "mock")

    @Test
    fun `planning a new project uses the full agent chain`() {
        val orchestrator = Orchestrator.createDefault(gateway())
        val plan = orchestrator.planFor("Create a clinic appointment system", null)
        assertEquals(
            listOf(
                AgentRole.REQUIREMENT,
                AgentRole.ARCHITECTURE,
                AgentRole.DATABASE,
                AgentRole.UI,
                AgentRole.PYTHON,
                AgentRole.TESTING,
                AgentRole.SECURITY,
                AgentRole.DOCUMENTATION
            ),
            plan.steps.map { it.role }
        )
    }

    @Test
    fun `planning a modification is shorter`() {
        val orchestrator = Orchestrator.createDefault(gateway())
        val existing = GeneratedProject(
            manifest = ProjectManifest(
                id = "p-1",
                name = "ExistingApp",
                entryPoint = "app/main.py",
                domain = "simple_tool"
            ),
            files = emptyList()
        )
        val plan = orchestrator.planFor("Add a billing module", existing)
        assertEquals(
            listOf(
                AgentRole.REQUIREMENT,
                AgentRole.PYTHON,
                AgentRole.TESTING,
                AgentRole.DOCUMENTATION
            ),
            plan.steps.map { it.role }
        )
    }

    @Test
    fun `creates a complete clinic project end to end`() {
        val g = gateway()
        val orchestrator = Orchestrator.createDefault(g)
        val events = mutableListOf<AgentEvent>()
        val project = orchestrator.createProject(
            userRequest = "Create a small clinic appointment system",
            gateway = g
        ) { events += it }

        assertEquals("ClinicApp", project.manifest.name)
        assertEquals("clinic_appointments", project.manifest.domain)
        assertEquals("python", project.manifest.language)
        assertEquals("app/main.py", project.manifest.entryPoint)
        assertNotNull(project.fileAt("app/main.py"))
        assertNotNull(project.fileAt("modules/clinic_appointments.py"))
        assertNotNull(project.fileAt("database/storage.py"))
        assertNotNull(project.fileAt("tests/test_clinic_appointments.py"))
        assertNotNull(project.fileAt("config/config.json"))
        assertNotNull(project.fileAt("docs/architecture.md"))
        assertNotNull(project.fileAt("docs/overview.md"))
        assertNotNull(project.fileAt("README.md"))

        // The generated code must pass the static validator cleanly.
        assertTrue(PythonStaticValidator.compile(project).isEmpty())
        assertTrue(PythonStaticValidator.runTests(project).isEmpty())

        // The full pipeline must then package it successfully.
        val pipeline = BuildPipeline(PythonStaticValidator, DebuggingAgent(g), maxDebugAttempts = 3)
        val result = pipeline.run(project)
        assertTrue("pipeline failed: $result", result is BuildResult.Success)

        // Every planned agent reported completion.
        val finished = events.filterIsInstance<AgentEvent.AgentFinished>()
        assertEquals(8, finished.count { !it.error })
    }

    @Test
    fun `modifies an existing project and records the change`() {
        val g = gateway()
        val orchestrator = Orchestrator.createDefault(g)
        val original = orchestrator.createProject("Create a task manager", g)

        val modified = orchestrator.modifyProject(original, "Add a billing module", g)

        assertEquals(original.manifest.id, modified.manifest.id)
        assertTrue("history did not grow", modified.history.size > original.history.size)
        assertNotNull(modified.fileAt("modules/task_manager.py"))
        assertTrue(PythonStaticValidator.compile(modified).isEmpty())
    }

    @Test
    fun `unknown requests fall back to the generic domain`() {
        val g = gateway()
        val orchestrator = Orchestrator.createDefault(g)
        val project = orchestrator.createProject("Make me a program that counts sheep", g)
        assertEquals("MyApp", project.manifest.name)
        assertNotNull(project.fileAt("modules/simple_tool.py"))
    }
}
