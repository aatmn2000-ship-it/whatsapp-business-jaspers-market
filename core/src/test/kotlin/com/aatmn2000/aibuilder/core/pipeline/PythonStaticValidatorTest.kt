package com.aatmn2000.aibuilder.core.pipeline

import com.aatmn2000.aibuilder.core.project.GeneratedProject
import com.aatmn2000.aibuilder.core.project.ProjectFile
import com.aatmn2000.aibuilder.core.project.ProjectManifest
import org.junit.Assert.assertTrue
import org.junit.Test

class PythonStaticValidatorTest {

    private fun manifest(
        entryPoint: String = "app/main.py",
        domain: String = "simple_tool"
    ): ProjectManifest = ProjectManifest(
        id = "t-1",
        name = "TestApp",
        entryPoint = entryPoint,
        domain = domain
    )

    private fun project(files: List<ProjectFile>): GeneratedProject =
        GeneratedProject(manifest = manifest(), files = files)

    @Test
    fun `accepts a healthy generated project`() {
        val files = listOf(
            ProjectFile(
                "app/main.py",
                "from modules.simple_tool import ItemService\n\ndef main():\n    return 0\n"
            ),
            ProjectFile(
                "modules/simple_tool.py",
                "class ItemService:\n    def __init__(self):\n        pass\n"
            ),
            ProjectFile(
                "tests/test_main.py",
                "def test_smoke():\n    assert True\n"
            )
        )
        val project = project(files)
        assertTrue(PythonStaticValidator.compile(project).isEmpty())
        assertTrue(PythonStaticValidator.runTests(project).isEmpty())
    }

    @Test
    fun `flags leftover placeholder tokens`() {
        val project = project(
            listOf(ProjectFile("app/main.py", "def main():\n    # TODO( implement this )\n    return 0\n"))
        )
        val issues = PythonStaticValidator.compile(project)
        assertTrue(issues.any { it.message.contains("placeholder") })
    }

    @Test
    fun `flags bad indentation`() {
        val project = project(listOf(ProjectFile("app/main.py", "def main():\n  return 0\n")))
        val issues = PythonStaticValidator.compile(project)
        assertTrue(issues.any { it.message.contains("Indentation") })
    }

    @Test
    fun `flags tab indentation`() {
        val project = project(listOf(ProjectFile("app/main.py", "def main():\n\treturn 0\n")))
        val issues = PythonStaticValidator.compile(project)
        assertTrue(issues.any { it.message.contains("Tab") })
    }

    @Test
    fun `flags unresolved module imports`() {
        val project = project(
            listOf(ProjectFile("app/main.py", "from modules.missing import Thing\n\ndef main():\n    return 0\n"))
        )
        val issues = PythonStaticValidator.compile(project)
        assertTrue(issues.any { it.message.contains("modules/missing.py") })
    }

    @Test
    fun `flags unresolved names in existing modules`() {
        val project = project(
            listOf(
                ProjectFile("app/main.py", "from modules.thing import Nope\n\ndef main():\n    return 0\n"),
                ProjectFile("modules/thing.py", "class Thing:\n    pass\n")
            )
        )
        val issues = PythonStaticValidator.compile(project)
        assertTrue(issues.any { it.message.contains("'Nope'") })
    }

    @Test
    fun `ignores standard library imports`() {
        val project = project(
            listOf(
                ProjectFile("app/main.py", "import sqlite3\nfrom dataclasses import dataclass\n\ndef main():\n    return 0\n")
            )
        )
        val issues = PythonStaticValidator.compile(project)
        assertTrue(issues.none { it.message.contains("sqlite3") })
        assertTrue(issues.none { it.message.contains("dataclasses") })
    }

    @Test
    fun `flags a missing entry point`() {
        val project = GeneratedProject(
            manifest = manifest(entryPoint = "app/missing.py"),
            files = listOf(ProjectFile("modules/x.py", "VALUE = 1\n"))
        )
        val issues = PythonStaticValidator.compile(project)
        assertTrue(issues.any { it.message.contains("Entry point") })
    }

    @Test
    fun `flags malformed json files`() {
        val project = project(
            listOf(
                ProjectFile("app/main.py", "def main():\n    return 0\n"),
                ProjectFile("config/config.json", """{"a": 1""")
            )
        )
        val issues = PythonStaticValidator.compile(project)
        assertTrue(issues.any { it.message.contains("JSON") })
    }

    @Test
    fun `flags a project with no tests`() {
        val project = project(listOf(ProjectFile("app/main.py", "def main():\n    return 0\n")))
        val issues = PythonStaticValidator.runTests(project)
        assertTrue(issues.any { it.message.contains("No test files") })
    }

    @Test
    fun `flags a test file without test functions`() {
        val project = project(
            listOf(
                ProjectFile("app/main.py", "def main():\n    return 0\n"),
                ProjectFile("tests/test_main.py", "VALUE = 1\n")
            )
        )
        val issues = PythonStaticValidator.runTests(project)
        assertTrue(issues.any { it.message.contains("no test functions") })
    }
}
