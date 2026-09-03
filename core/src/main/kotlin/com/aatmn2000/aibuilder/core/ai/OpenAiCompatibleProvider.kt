package com.aatmn2000.aibuilder.core.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Provider for any OpenAI-compatible `/chat/completions` endpoint.
 *
 * The API key is only ever sent to the configured endpoint and is never
 * written into generated projects.
 */
class OpenAiCompatibleProvider(
    private val config: OpenAiSettings,
    private val transport: AiTransport
) : AiProvider {

    override val id: String = PROVIDER_ID

    override val displayName: String = "OpenAI-compatible (${config.model})"

    private val json = Json { ignoreUnknownKeys = true }

    override fun isAvailable(): Boolean =
        config.apiKey.isNotBlank() && config.baseUrl.isNotBlank()

    override fun complete(request: AiRequest): AiResponse {
        if (!isAvailable()) {
            return AiResponse.error(id, "Provider is not configured")
        }
        return try {
            val url = config.baseUrl.trimEnd('/') + "/chat/completions"
            val body = json.encodeToString(ChatRequest.serializer(), buildRequest(request))
            val raw = transport.postJson(
                url,
                headers = mapOf(
                    "Authorization" to "Bearer ${config.apiKey}",
                    "Content-Type" to "application/json"
                ),
                body = body
            )
            val parsed = json.decodeFromString(ChatResponse.serializer(), raw)
            val text = parsed.choices.firstOrNull()?.message?.content
            if (text.isNullOrBlank()) {
                AiResponse.error(id, "Empty completion from provider")
            } else {
                AiResponse(text = text, providerId = id, model = parsed.model ?: config.model)
            }
        } catch (e: Exception) {
            AiResponse.error(id, e.message ?: "request failed")
        }
    }

    private fun buildRequest(request: AiRequest): ChatRequest = ChatRequest(
        model = config.model,
        temperature = request.temperature,
        maxTokens = request.maxTokens,
        messages = listOf(
            ChatMessage(role = "system", content = request.systemPrompt),
            ChatMessage(role = "user", content = request.userPrompt)
        )
    )

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Double,
        @SerialName("max_tokens") val maxTokens: Int
    )

    @Serializable
    private data class ChatMessage(
        val role: String,
        val content: String
    )

    @Serializable
    private data class ChatResponse(
        val choices: List<Choice> = emptyList(),
        val model: String? = null
    )

    @Serializable
    private data class Choice(
        val message: ChatMessage = ChatMessage(role = "", content = "")
    )

    companion object {
        const val PROVIDER_ID = "openai-compatible"
    }
}
