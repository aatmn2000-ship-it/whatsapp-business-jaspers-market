package com.aatmn2000.aibuilder.core.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ManifestCodecTest {

    @Test
    fun `encode then decode round trips a full manifest`() {
        val manifest = ProjectManifest(
            id = "abc-123",
            name = "MyClinicApp",
            description = "Clinic appointment system",
            entryPoint = "app/main.py",
            modules = listOf(
                ModuleSpec("appointments", "modules/clinic_appointments.py", "Domain logic")
            ),
            storage = StorageSpec()
        )

        val raw = ManifestCodec.encode(manifest)
        val decoded = ManifestCodec.decode(raw)

        assertEquals(manifest, decoded)
        assertEquals(1, decoded.schemaVersion)
        assertEquals("0.1.0", decoded.version)
    }

    @Test
    fun `decode ignores unknown keys for forward compatibility`() {
        val raw = """{"id":"x","name":"X","entryPoint":"a.py","futureField":42}"""
        val decoded = ManifestCodec.decode(raw)
        assertEquals("x", decoded.id)
    }

    @Test
    fun `decode rejects a manifest without entry point`() {
        val raw = """{"id":"x","name":"X"}"""
        try {
            ManifestCodec.decode(raw)
            fail("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("entryPoint"))
        }
    }

    @Test
    fun `decode rejects malformed json`() {
        try {
            ManifestCodec.decode("{not json")
            fail("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("project.json"))
        }
    }
}
