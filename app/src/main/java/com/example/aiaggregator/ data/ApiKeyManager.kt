package com.example.aiaggregator.data.api

import com.example.aiaggregator.data.model.AIProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AIApiService {
    
    suspend fun sendMessage(provider: AIProvider, message: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = when (provider.id) {
                "gemini" -> callGemini(provider, message)
                "mistral" -> callGenericOpenAI(provider, message)
                "groq" -> callGenericOpenAI(provider, message)
                "deepseek" -> callGenericOpenAI(provider, message)
                "openrouter" -> callGenericOpenAI(provider, message)
                "claude" -> callClaude(provider, message)
                "chatgpt" -> callGenericOpenAI(provider, message)
                "kimi" -> callGenericOpenAI(provider, message)
                else -> throw IllegalArgumentException("Unknown provider: ${provider.id}")
            }
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun callGenericOpenAI(provider: AIProvider, message: String): String {
        val url = URL("${provider.baseUrl}/v1/chat/completions")
        val connection = url.openConnection() as HttpURLConnection
        
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer ${provider.apiKey}")
        connection.doOutput = true
        connection.connectTimeout = 30000
        connection.readTimeout = 60000
        
        val body = JSONObject().apply {
            put("model", provider.modelName)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", message)
                })
            })
        }
        
        connection.outputStream.use { os ->
            os.write(body.toString().toByteArray())
        }
        
        val responseCode = connection.responseCode
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        
        if (responseCode == 200) {
            val json = JSONObject(response)
            return json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } else {
            throw Exception("API error $responseCode: $response")
        }
    }
    
    private fun callGemini(provider: AIProvider, message: String): String {
        val url = URL("${provider.baseUrl}/v1beta/models/${provider.modelName}:generateContent?key=${provider.apiKey}")
        val connection = url.openConnection() as HttpURLConnection
        
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        
        val body = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", message)
                        })
                    })
                })
            })
        }
        
        connection.outputStream.use { os ->
            os.write(body.toString().toByteArray())
        }
        
        val responseCode = connection.responseCode
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        
        if (responseCode == 200) {
            val json = JSONObject(response)
            return json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        } else {
            throw Exception("Gemini API error $responseCode: $response")
        }
    }
    
    private fun callClaude(provider: AIProvider, message: String): String {
        val url = URL("${provider.baseUrl}/v1/messages")
        val connection = url.openConnection() as HttpURLConnection
        
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("x-api-key", provider.apiKey)
        connection.setRequestProperty("anthropic-version", "2023-06-01")
        connection.doOutput = true
        
        val body = JSONObject().apply {
            put("model", provider.modelName)
            put("max_tokens", 4096)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", message)
                })
            })
        }
        
        connection.outputStream.use { os ->
            os.write(body.toString().toByteArray())
        }
        
        val responseCode = connection.responseCode
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        
        if (responseCode == 200) {
            val json = JSONObject(response)
            return json.getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
        } else {
            throw Exception("Claude API error $responseCode: $response")
        }
    }
}
