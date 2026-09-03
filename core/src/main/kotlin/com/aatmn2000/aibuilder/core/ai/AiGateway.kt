package com.aatmn2000.aibuilder.core.ai

/**
 * The AI gateway: the single entry point all agents use to talk to AI.
 *
 * It tries the preferred provider first and falls back to the remaining
 * available providers, so a dead provider never blocks the build.
 */
class AiGateway(
    private val providers: List<AiProvider>,
    private val preferredProviderId: String
) {

    fun availableProviders(): List<AiProvider> = providers.filter { it.isAvailable() }

    fun complete(request: AiRequest): AiResponse {
        val available = availableProviders()
        if (available.isEmpty()) {
            return AiResponse.error(
                preferredProviderId,
                "No AI provider is available. Configure one in Settings."
            )
        }
        val ordered = listOfNotNull(available.firstOrNull { it.id == preferredProviderId }) +
            available.filter { it.id != preferredProviderId }
        var lastError: String? = null
        ordered.forEach { provider ->
            val response = provider.complete(request)
            if (!response.isError) {
                return response
            }
            lastError = response.errorDetail
        }
        return AiResponse.error(preferredProviderId, lastError ?: "All providers failed")
    }
}
