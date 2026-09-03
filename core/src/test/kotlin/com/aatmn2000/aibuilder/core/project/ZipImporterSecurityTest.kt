package com.aatmn2000.aibuilder.core.project

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipImporterSecurityTest {

    private val validManifest =
        """{"id":"p-1","name":"SampleApp","entryPoint":"app/main.py","version":"0.1.0","language":"python"}"""

    @Test
    fun `rejects path traversal entry names`() {
        val bytes = zipWith(
            "project.json" to validManifest,
            "app/main.py" to "print(1)",
            "../evil.py" to "x = 1"
        )
        val result = ZipImporter.importZip(bytes)
        assertFalse(result.isSuccessful)
        assertTrue(result.issues.any { it.isFatal && it.message!!.contains("Unsafe entry name") })
    }

    @Test
    fun `rejects a zip without project.json`() {
        val bytes = zipWith("app/main.py" to "print(1)")
        val result = ZipImporter.importZip(bytes)
        assertFalse(result.isSuccessful)
        assertTrue(result.issues.any { it.message!!.contains("project.json") })
    }

    @Test
    fun `rejects a zip where the entry point file is missing`() {
        val bytes = zipWith(
            "project.json" to validManifest,
            "modules/m.py" to "x = 1"
        )
        val result = ZipImporter.importZip(bytes)
        assertFalse(result.isSuccessful)
        assertTrue(result.issues.any { it.message!!.contains("Entry point file not found") })
    }

    @Test
    fun `rejects a zip with an invalid manifest`() {
        val bytes = zipWith(
            "project.json" to """{"id":"p-1"}""",
            "app/main.py" to "print(1)"
        )
        val result = ZipImporter.importZip(bytes)
        assertFalse(result.isSuccessful)
        assertTrue(result.issues.any { it.isFatal })
    }

    @Test
    fun `skips binary entries with a warning instead of failing`() {
        val bytes = zipWithMixed(
            textEntries = listOf(
                "project.json" to validManifest,
                "app/main.py" to "print(1)"
            ),
            binaryEntries = listOf("assets/blob.bin" to byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00, 0x01))
        )
        val result = ZipImporter.importZip(bytes)
        assertTrue("expected success, got ${result.issues}", result.isSuccessful)
        assertTrue(result.issues.any { !it.isFatal && it.entryName == "assets/blob.bin" })
    }

    @Test
    fun `corrupt zip bytes produce an error result`() {
        val result = ZipImporter.importZip(byteArrayOf(1, 2, 3, 4, 5))
        assertFalse(result.isSuccessful)
        assertTrue(result.issues.any { it.isFatal })
    }

    private fun zipWith(vararg entries: Pair<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out, StandardCharsets.UTF_8).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun zipWithMixed(
        textEntries: List<Pair<String, String>>,
        binaryEntries: List<Pair<String, ByteArray>>
    ): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out, StandardCharsets.UTF_8).use { zip ->
            textEntries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()
            }
            binaryEntries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }
}
