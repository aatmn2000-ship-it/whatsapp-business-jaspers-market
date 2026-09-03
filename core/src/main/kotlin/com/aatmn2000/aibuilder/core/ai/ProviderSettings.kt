package com.aatmn2000.aibuilder.core.ai

/** Settings for a local Ollama instance (OpenAI-compatible endpoint). */
data class OllamaSettings(
    val baseUrl: String = "http://10.0.2.2:11434",
    val model: String = "llama3.1:8b"
)

/**
 * Settings for any OpenAI-compatible /chat/completions endpoint
 * (OpenAI, OpenRouter, vLLM, LM Studio, ...).
 */
data class OpenAiSettings(
    val baseUrl: String,
    val apiKey: String,
    val model: String
)
