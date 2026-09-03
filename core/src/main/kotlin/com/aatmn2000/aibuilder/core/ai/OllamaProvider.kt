package com.aatmn2000.aibuilder.core.ai

/**
 * Local AI via Ollama, using its OpenAI-compatible endpoint (`/v1`).
 * Runs on the user's own device or LAN — fully private, no cloud.
 */
class OllamaProvider(
    private val settings: OllamaSettings,
    transport: AiTransport
) : AiProvider {

    private val delegate = OpenAiCompatibleProvider(
        OpenAiSettings(
            baseUrl = settings.baseUrl.trimEnd('/') + "/v1",
            apiKey = "ollama", // Ollama's OpenAI endpoint does not require a key
            model = settings.model
        ),
        transport
    )

    override val id: String = PROVIDER_ID

    override val displayName: String = "Ollama (${settings.model})"

    override fun isAvailable(): Boolean = settings.baseUrl.isNotBlank()

    override fun complete(request: AiRequest): AiResponse = delegate.complete(request)

    companion object {
        const val PROVIDER_ID = "ollama"
    }
}
