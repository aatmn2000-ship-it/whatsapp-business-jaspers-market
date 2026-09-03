package com.aatmn2000.aibuilder.core.ai

import com.aatmn2000.aibuilder.core.agent.AgentRole

/**
 * One AI completion request.
 *
 * [role] tells the provider which agent is asking, and agents append
 * machine-readable token lines (`path:`, `domain:`, `project_name:`, ...)
 * to [userPrompt] so that ANY provider — including the deterministic mock —
 * can answer file-generation requests.
 */
data class AiRequest(
    val role: AgentRole,
    val systemPrompt: String,
    val userPrompt: String,
    val temperature: Double = 0.2,
    val maxTokens: Int = 4096
)

data class AiResponse(
    val text: String,
    val providerId: String,
    val model: String = "",
    val isError: Boolean = false,
    val errorDetail: String? = null
) {
    companion object {
        fun error(providerId: String, detail: String): AiResponse =
            AiResponse(text = "", providerId = providerId, isError = true, errorDetail = detail)
    }
}

/**
 * A pluggable AI backend. The app is never locked to one vendor: providers
 * can be local (Ollama), remote (any OpenAI-compatible API) or the offline
 * mock used for development and tests.
 */
interface AiProvider {
    val id: String
    val displayName: String

    fun isAvailable(): Boolean

    fun complete(request: AiRequest): AiResponse
}

/**
 * HTTP seam for providers. The Android app provides an OkHttp
 * implementation; unit tests use a fake. The core module itself has no
 * network dependency.
 */
interface AiTransport {
    fun postJson(url: String, headers: Map<String, String>, body: String): String
}
