package com.aatmn2000.aibuilder.core.pipeline

import com.aatmn2000.aibuilder.core.agent.DebuggingAgent
import com.aatmn2000.aibuilder.core.ai.AiGateway
import com.aatmn2000.aibuilder.core.ai.MockAiProvider
import com.aatmn2000.aibuilder.core.project.GeneratedProject
import com.aatmn2000.aibuilder.core.project.ProjectFile
import com.aatmn2000.aibuilder.core.project.ProjectManifest
import com.aatmn2000.aibuilder.core.project.ProjectStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildPipelineTest {

    private fun gateway(): AiGateway = AiGateway(listOf(MockAiProvider()), "mock")

    /**
     * A broken project: the entry point imports a module that does not
     * exist, and there is no test suite.
     */
    private fun brokenProject(): GeneratedProject {
        return GeneratedProject(
            manifest = ProjectManifest(
                id = "t-1",
                name = "BrokenApp",
                entryPoint = "app/main.py",
                domain = "simple_tool"
            ),
            files = listOf(
                ProjectFile(
                    "app/main.py",
                    "from modules.simple_tool import ItemService\n\ndef main():\n    return 0\n"
                )
            )
        )
    }

    @Test
    fun `repairs a missing module and missing tests, then packages`() {
        val g = gateway()
        val pipeline = BuildPipeline(PythonStaticValidator, DebuggingAgent(g), maxDebugAttempts = 3)
        val events = mutableListOf<BuildEvent>()
        val result = pipeline.run(brokenProject()) { events += it }

        assertTrue("expected success, got: $result", result is BuildResult.Success)
        val success = result as BuildResult.Success
        assertTrue("expected repairs, attempts=${success.repairAttempts}", success.repairAttempts >= 1)
        assertNotNull("module was not regenerated", success.project.fileAt("modules/simple_tool.py"))
        assertNotNull("tests were not regenerated", success.project.fileAt("tests/test_simple_tool.py"))
        assertEquals(ProjectStatus.BUILT, success.project.status)
        assertTrue("repair not recorded in history", success.project.history.any { it.attempt >= 1 })
        assertTrue(events.any { it is BuildEvent.Packaged })
    }

    @Test
    fun `fails when the repair budget is exhausted`() {
        val g = gateway()
        val pipeline = BuildPipeline(PythonStaticValidator, DebuggingAgent(g), maxDebugAttempts = 1)
        val result = pipeline.run(brokenProject())

        assertTrue("expected failure, got: $result", result is BuildResult.Failure)
        val failure = result as BuildResult.Failure
        assertEquals(1, failure.repairAttempts)
        assertEquals(ProjectStatus.FAILED, failure.project.status)
        assertTrue(failure.issues.isNotEmpty())
    }

    @Test
    fun `healthy project builds with zero repair attempts`() {
        val g = gateway()
        val pipeline = BuildPipeline(PythonStaticValidator, DebuggingAgent(g), maxDebugAttempts = 3)
        val project = GeneratedProject(
            manifest = ProjectManifest(
                id = "t-2",
                name = "GoodApp",
                entryPoint = "app/main.py",
                domain = "simple_tool"
            ),
            files = listOf(
                ProjectFile("app/main.py", "def main():\n    return 0\n"),
                ProjectFile("tests/test_main.py", "def test_smoke():\n    assert True\n")
            )
        )

        val result = pipeline.run(project)

        assertTrue("expected success, got: $result", result is BuildResult.Success)
        assertEquals(0, (result as BuildResult.Success).repairAttempts)
    }

    @Test
    fun `only the affected files are regenerated during a repair`() {
        val g = gateway()
        val pipeline = BuildPipeline(PythonStaticValidator, DebuggingAgent(g), maxDebugAttempts = 3)
        val result = pipeline.run(brokenProject())
        val success = result as BuildResult.Success

        // app/main.py was valid from the start and must keep its content.
        val originalMain = brokenProject().fileAt("app/main.py")!!.content
        assertEquals(originalMain, success.project.fileAt("app/main.py")!!.content)
    }
}
