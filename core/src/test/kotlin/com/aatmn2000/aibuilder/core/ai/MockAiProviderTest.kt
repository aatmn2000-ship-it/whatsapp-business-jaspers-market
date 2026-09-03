package com.aatmn2000.aibuilder.core.ai

import com.aatmn2000.aibuilder.core.agent.AgentRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockAiProviderTest {

    private val provider = MockAiProvider()

    @Test
    fun `generates the main entry point from prompt tokens`() {
        val response = provider.complete(
            AiRequest(
                role = AgentRole.UI,
                systemPrompt = "system",
                userPrompt = """
                    Generate the CLI entry point.

                    path: app/main.py
                    domain: clinic_appointments
                    entity: Appointment
                    service: AppointmentService
                    project_name: ClinicApp
                """.trimIndent()
            )
        )
        assertFalse(response.isError)
        assertTrue(response.text.contains("def main("))
        assertTrue(response.text.contains("from modules.clinic_appointments import AppointmentService"))
    }

    @Test
    fun `debugging regenerates the exact module file asked for`() {
        val response = provider.complete(
            AiRequest(
                role = AgentRole.DEBUGGING,
                systemPrompt = "system",
                userPrompt = """
                    Fix the error.

                    path: modules/task_manager.py
                    domain: task_manager
                    project_name: TaskApp
                """.trimIndent()
            )
        )
        assertFalse(response.isError)
        assertTrue(response.text.contains("class TaskService"))
    }

    @Test
    fun `unknown roles produce a clear message, not an error`() {
        val response = provider.complete(
            AiRequest(
                role = AgentRole.CPP,
                systemPrompt = "system",
                userPrompt = "path: x.cpp\n"
            )
        )
        assertFalse(response.isError)
        assertTrue(response.text.contains("not available"))
    }

    @Test
    fun `provider metadata is stable`() {
        assertEquals("mock", provider.id)
        assertTrue(provider.isAvailable())
    }
}
