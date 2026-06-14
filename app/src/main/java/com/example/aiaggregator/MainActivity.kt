package com.example.aiaggregator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aiaggregator.data.model.JudgmentMode
import com.example.aiaggregator.ui.ChatScreen
import com.example.aiaggregator.ui.SetupScreen
import com.example.aiaggregator.ui.theme.AiAggregatorTheme
import com.example.aiaggregator.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiAggregatorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: ChatViewModel = viewModel()
                    val providers by viewModel.providers.collectAsState()
                    val configuredCount by viewModel.configuredCount.collectAsState()
                    val currentSession by viewModel.currentSession.collectAsState()
                    val isLoading by viewModel.isLoading.collectAsState()
                    val judgmentMode by viewModel.judgmentMode.collectAsState()
                    
                    // Состояние экрана: настройка или чат
                    var showSetup by remember { mutableStateOf(configuredCount == 0) }
                    
                    // Показываем настройку при первом запуске или если нет ключей
                    LaunchedEffect(configuredCount) {
                        if (configuredCount == 0) showSetup = true
                    }
                    
                    if (showSetup) {
                        SetupScreen(
                            providers = providers,
                            configuredCount = configuredCount,
                            onApiKeyChange = { id, key ->
                                viewModel.saveApiKey(id, key)
                            },
                            onLaunchClick = {
                                if (configuredCount > 0) {
                                    showSetup = false
                                }
                            }
                        )
                    } else {
                        ChatScreen(
                            session = currentSession,
                            isLoading = isLoading,
                            judgmentMode = judgmentMode,
                            onSendQuestion = { question ->
                                viewModel.sendQuestion(question)
                            },
                            onAcceptKimi = {
                                // В режиме HYBRID принимаем вердикт Kimi
                                currentSession?.verdict?.let { verdict ->
                                    viewModel.acceptKimiVerdict(verdict)
                                }
                            },
                            onSelectWinner = { providerId ->
                                viewModel.selectManualWinner(providerId)
                            },
                            onModeChange = { mode ->
                                viewModel.setJudgmentMode(mode)
                            },
                            onSettingsClick = {
                                showSetup = true
                            }
                        )
                    }
                }
            }
        }
    }
}
