package com.aatmn2000.aibuilder.core.ai

import com.aatmn2000.aibuilder.core.agent.AgentRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiGatewayTest {

    private val request = AiRequest(
        role = AgentRole.REQUIREMENT,
        systemPrompt = "system",
        userPrompt = "user"
    )

    @Test
    fun `prefers the configured provider when it is available`() {
        val gateway = AiGateway(
            providers = listOf(failingProvider(), fixedProvider("good")),
            preferredProviderId = "good"
        )
        val response = gateway.complete(request)
        assertFalse(response.isError)
        assertEquals("good", response.providerId)
    }

    @Test
    fun `falls back to the next available provider`() {
        val gateway = AiGateway(
            providers = listOf(failingProvider(), fixedProvider("good")),
            preferredProviderId = "failing"
        )
        val response = gateway.complete(request)
        assertFalse(response.isError)
        assertEquals("good", response.providerId)
    }

    @Test
    fun `skips unavailable providers`() {
        val gateway = AiGateway(
            providers = listOf(unavailableProvider(), fixedProvider("good")),
            preferredProviderId = "unavailable"
        )
        val response = gateway.complete(request)
        assertFalse(response.isError)
        assertEquals("good", response.providerId)
    }

    @Test
    fun `reports an error when no provider is available`() {
        val gateway = AiGateway(providers = emptyList(), preferredProviderId = "mock")
        val response = gateway.complete(request)
        assertTrue(response.isError)
    }

    private fun fixedProvider(id: String): AiProvider = object : AiProvider {
        override val id: String = id
        override val displayName: String = id
        override fun isAvailable(): Boolean = true
        override fun complete(r: AiRequest): AiResponse = AiResponse(text = "ok", providerId = id, model = "m")
    }

    private fun failingProvider(): AiProvider = object : AiProvider {
        override val id: String = "failing"
        override val displayName: String = "failing"
        override fun isAvailable(): Boolean = true
        override fun complete(r: AiRequest): AiResponse = AiResponse.error(id, "boom")
    }

    private fun unavailableProvider(): AiProvider = object : AiProvider {
        override val id: String = "unavailable"
        override val displayName: String = "unavailable"
        override fun isAvailable(): Boolean = false
        override fun complete(r: AiRequest): AiResponse = AiResponse.error(id, "should not be called")
    }
}
