package com.example.aiaggregator.data.model

data class ChatMessage(
    val id: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val content: String = "",
    val isUser: Boolean = false,
    val isWinner: Boolean = false
)

data class ChatSession(
    val question: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val verdict: KimiVerdict? = null
)

data class KimiVerdict(
    val winnerProviderId: String = "",
    val winnerProviderName: String = "",
    val confidence: Float = 0f,
    val reasoning: String = "",
    val criticism: String = ""
)

object ProviderDefaults {
    val providers = listOf(
        AIProvider(id = "openai", name = "OpenAI", baseUrl = "https://api.openai.com/v1", modelName = "gpt-4"),
        AIProvider(id = "anthropic", name = "Anthropic", baseUrl = "https://api.anthropic.com", modelName = "claude-3"),
        AIProvider(id = "gemini", name = "Gemini", baseUrl = "https://generativelanguage.googleapis.com", modelName = "gemini-pro")
    )
}
