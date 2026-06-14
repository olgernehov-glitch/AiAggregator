package com.example.aiaggregator.data.api

import android.content.Context
import com.example.aiaggregator.data.model.AIProvider

class ApiKeyManager(context: Context) {

    private val prefs = context.getSharedPreferences("api_keys", Context.MODE_PRIVATE)

    fun saveApiKey(providerId: String, apiKey: String) {
        prefs.edit().putString("key_$providerId", apiKey).apply()
    }

    fun getApiKey(providerId: String): String {
        return prefs.getString("key_$providerId", "") ?: ""
    }

    fun getConfiguredCount(): Int {
        return getAllProviders().count { prefs.getString("key_${it.id}", "")?.isNotEmpty() == true }
    }

    fun getConfiguredProviders(): List<AIProvider> {
        return getAllProviders().filter { 
            prefs.getString("key_${it.id}", "")?.isNotEmpty() == true 
        }.map { provider ->
            provider.copy(apiKey = prefs.getString("key_${provider.id}", "") ?: "")
        }
    }

    fun getEnabledProviders(): List<AIProvider> {
        return getConfiguredProviders()
    }

    private fun getAllProviders(): List<AIProvider> {
        return listOf(
            AIProvider("gemini", "Google Gemini", "https://generativelanguage.googleapis.com"),
            AIProvider("mistral", "Mistral AI", "https://api.mistral.ai"),
            AIProvider("groq", "Groq", "https://api.groq.com/openai"),
            AIProvider("deepseek", "DeepSeek", "https://api.deepseek.com"),
            AIProvider("openrouter", "OpenRouter", "https://openrouter.ai/api"),
            AIProvider("claude", "Anthropic Claude", "https://api.anthropic.com"),
            AIProvider("chatgpt", "ChatGPT", "https://api.openai.com"),
            AIProvider("kimi", "Kimi", "https://api.moonshot.cn")
        )
    }
}
