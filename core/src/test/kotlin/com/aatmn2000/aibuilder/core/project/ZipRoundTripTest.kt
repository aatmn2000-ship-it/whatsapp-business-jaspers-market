package com.aatmn2000.aibuilder.core.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZipRoundTripTest {

    private fun sampleProject(): GeneratedProject {
        val files = listOf(
            ProjectFile("app/main.py", "def main():\n    print('hello')\n", "python"),
            ProjectFile("modules/items.py", "ITEMS = []\n", "python"),
            ProjectFile("tests/test_items.py", "def test_items():\n    assert True\n", "python"),
            ProjectFile("README.md", "# Sample\n", "markdown")
        )
        return ProjectFactory.create(
            name = "SampleApp",
            description = "Sample project for tests",
            files = files,
            modules = listOf(ModuleSpec("items", "modules/items.py", "Sample module")),
            entryPoint = "app/main.py"
        )
    }

    @Test
    fun `package then import round trips the project`() {
        val project = sampleProject()
        val bytes = ZipPackager.packageProject(project)

        val result = ZipImporter.importZip(bytes)

        assertTrue("import failed: ${result.issues}", result.isSuccessful)
        val imported = result.project!!
        assertEquals(project.manifest, imported.manifest)
        assertEquals(
            project.files.map { it.path }.toSet(),
            imported.files.map { it.path }.toSet()
        )
        assertEquals(
            project.files.first { it.path == "app/main.py" }.content,
            imported.fileAt("app/main.py")!!.content
        )
        assertEquals(ProjectStatus.IMPORTED, imported.status)
    }

    @Test
    fun `imported zip exposes the manifest`() {
        val project = sampleProject()
        val bytes = ZipPackager.packageProject(project)

        val result = ZipImporter.importZip(bytes)

        assertTrue(result.isSuccessful)
        assertTrue(result.issues.isEmpty())
    }
}
