package com.example.aiaggregator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaggregator.data.ApiKeyManager
import com.example.aiaggregator.data.api.AIApiService
import com.example.aiaggregator.data.model.AIProvider
import com.example.aiaggregator.data.model.ChatMessage
import com.example.aiaggregator.data.model.ChatSession
import com.example.aiaggregator.data.model.JudgmentMode
import com.example.aiaggregator.data.model.KimiVerdict
import com.example.aiaggregator.data.model.ProviderDefaults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    private val apiKeyManager = ApiKeyManager(application)
    private val apiService = AIApiService()
    
    // Состояние экрана настройки ключей
    private val _providers = MutableStateFlow<List<AIProvider>>(emptyList())
    val providers: StateFlow<List<AIProvider>> = _providers.asStateFlow()
    
    private val _configuredCount = MutableStateFlow(0)
    val configuredCount: StateFlow<Int> = _configuredCount.asStateFlow()
    
    // Состояние чата
    private val _currentSession = MutableStateFlow<ChatSession?>(null)
    val currentSession: StateFlow<ChatSession?> = _currentSession.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Режим судейства
    private val _judgmentMode = MutableStateFlow(JudgmentMode.HYBRID)
    val judgmentMode: StateFlow<JudgmentMode> = _judgmentMode.asStateFlow()
    
    init {
        loadProviders()
    }
    
    fun loadProviders() {
        _providers.value = apiKeyManager.getConfiguredProviders()
        _configuredCount.value = apiKeyManager.getConfiguredCount()
    }
    
    fun saveApiKey(providerId: String, apiKey: String) {
        apiKeyManager.saveApiKey(providerId, apiKey)
        loadProviders()
    }
    
    fun setJudgmentMode(mode: JudgmentMode) {
        _judgmentMode.value = mode
    }
    
    fun sendQuestion(question: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val enabledProviders = apiKeyManager.getEnabledProviders()
            
            if (enabledProviders.isEmpty()) {
                _error.value = "Настройте хотя бы один API ключ"
                _isLoading.value = false
                return@launch
            }
            
            // Создаём новую сессию
            val session = ChatSession(question = question)
            _currentSession.value = session
            
            // Собираем ответы от всех провайдеров параллельно
            val responses = mutableListOf<ChatMessage>()
            
            enabledProviders.forEach { provider ->
                launch {
                    apiService.sendMessage(provider, question)
                        .onSuccess { content ->
                            val message = ChatMessage(
                                providerId = provider.id,
                                providerName = provider.name,
                                content = content
                            )
                            responses.add(message)
                            updateSessionWithResponses(responses.toList())
                        }
                        .onFailure { e ->
                            val errorMessage = ChatMessage(
                                providerId = provider.id,
                                providerName = provider.name,
                                content = "Ошибка: ${e.message}",
                                isWinner = false
                            )
                            responses.add(errorMessage)
                            updateSessionWithResponses(responses.toList())
                        }
                }
            }
            
            _isLoading.value = false
        }
    }
    
    private fun updateSessionWithResponses(messages: List<ChatMessage>) {
        _currentSession.value = _currentSession.value?.copy(
            messages = messages
        )
    }
    
    // Kimi судит — выбирает лучший ответ
    suspend fun requestKimiVerdict(): KimiVerdict? {
        val session = _currentSession.value ?: return null
        val kimiProvider = ProviderDefaults.KIMI.copy(
            apiKey = apiKeyManager.getApiKey("kimi")
        )
        
        if (kimiProvider.apiKey.isBlank()) return null
        
        val prompt = buildKimiPrompt(session.question, session.messages)
        
        return try {
            apiService.sendMessage(kimiProvider, prompt)
                .getOrNull()
                ?.let { parseKimiVerdict(it, session.messages) }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun buildKimiPrompt(question: String, messages: List<ChatMessage>): String {
        val sb = StringBuilder()
        sb.append("Вопрос пользователя: $question\n\n")
        sb.append("Ответы от разных ИИ:\n\n")
        
        messages.forEachIndexed { index, msg ->
            sb.append("${index + 1}. ${msg.providerName}:\n${msg.content}\n\n")
        }
        
        sb.append("Твоя задача — выбрать ЛУЧШИЙ ответ. Ответь в формате:\n")
        sb.append("ПОБЕДИТЕЛЬ: [имя провайдера]\n")
        sb.append("ОБОСНОВАНИЕ: [почему этот ответ лучше]\n")
        sb.append("КРИТИКА: [что можно улучшить в других ответах]\n")
        sb.append("УВЕРЕННОСТЬ: [число от 0 до 100]")
        
        return sb.toString()
    }
    
    private fun parseKimiVerdict(response: String, messages: List<ChatMessage>): KimiVerdict {
        val lines = response.lines()
        
        var winnerName = ""
        var reasoning = ""
        var criticism = ""
        var confidence = 0f
        
        lines.forEach { line ->
            when {
                line.startsWith("ПОБЕДИТЕЛЬ:") -> winnerName = line.substringAfter(":").trim()
                line.startsWith("ОБОСНОВАНИЕ:") -> reasoning = line.substringAfter(":").trim()
                line.startsWith("КРИТИКА:") -> criticism = line.substringAfter(":").trim()
                line.startsWith("УВЕРЕННОСТЬ:") -> {
                    confidence = line.substringAfter(":").trim().toFloatOrNull() ?: 0f
                }
            }
        }
        
        // Если не нашли по русски, пробуем английский
        if (winnerName.isEmpty()) {
            lines.forEach { line ->
                when {
                    line.startsWith("WINNER:") -> winnerName = line.substringAfter(":").trim()
                    line.startsWith("REASONING:") -> reasoning = line.substringAfter(":").trim()
                    line.startsWith("CRITICISM:") -> criticism = line.substringAfter(":").trim()
                    line.startsWith("CONFIDENCE:") -> {
                        confidence = line.substringAfter(":").trim().toFloatOrNull() ?: 0f
                    }
                }
            }
        }
        
        val winnerMessage = messages.find { it.providerName.equals(winnerName, ignoreCase = true) }
            ?: messages.firstOrNull()
        
        return KimiVerdict(
            winnerProviderId = winnerMessage?.providerId ?: "",
            winnerProviderName = winnerMessage?.providerName ?: winnerName,
            reasoning = reasoning.ifEmpty { "Лучший ответ выбран на основе анализа" },
            criticism = criticism,
            confidence = confidence
        )
    }
    
    fun acceptKimiVerdict(verdict: KimiVerdict) {
        _currentSession.value = _currentSession.value?.copy(
            verdict = verdict,
            messages = _currentSession.value?.messages?.map { msg ->
                msg.copy(isWinner = msg.providerId == verdict.winnerProviderId)
            } ?: emptyList()
        )
    }
    
    fun selectManualWinner(providerId: String) {
        _currentSession.value = _currentSession.value?.copy(
            messages = _currentSession.value?.messages?.map { msg ->
                msg.copy(isWinner = msg.providerId == providerId)
            } ?: emptyList()
        )
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearSession() {
        _currentSession.value = null
    }
}
