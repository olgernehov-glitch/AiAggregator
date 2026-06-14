package com.example.aiaggregator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aiaggregator.data.model.ChatMessage
import com.example.aiaggregator.data.model.ChatSession
import com.example.aiaggregator.data.model.JudgmentMode
import com.example.aiaggregator.data.model.KimiVerdict

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    session: ChatSession?,
    isLoading: Boolean,
    judgmentMode: JudgmentMode,
    onSendQuestion: (String) -> Unit,
    onAcceptKimi: () -> Unit,
    onSelectWinner: (String) -> Unit,
    onModeChange: (JudgmentMode) -> Unit,
    onSettingsClick: () -> Unit
) {
    var question by remember { mutableStateOf("") }
    var showModeMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🤖 AI Aggregator") },
                actions = {
                    // Выбор режима
                    Box {
                        TextButton(onClick = { showModeMenu = true }) {
                            Text(getModeEmoji(judgmentMode) + " " + getModeName(judgmentMode))
                        }
                        DropdownMenu(
                            expanded = showModeMenu,
                            onDismissRequest = { showModeMenu = false }
                        ) {
                            JudgmentMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text("${getModeEmoji(mode)} ${getModeName(mode)}") },
                                    onClick = {
                                        onModeChange(mode)
                                        showModeMenu = false
                                    }
                                )
                            }
                        }
                    }
                    
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Область сообщений
            if (session == null) {
                // Пустое состояние
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎯", style = MaterialTheme.typography.displayLarge)
                        Text(
                            "Задай вопрос 7 ИИ",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            "Kimi выберет лучший ответ",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Вопрос
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = "❓ ${session.question}",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    
                    // Индикатор загрузки
                    if (isLoading) {
                        item {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                "Ожидаем ответы...",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    
                    // Ответы от ИИ
                    items(session.messages) { message ->
                        AiResponseCard(
                            message = message,
                            isWinner = message.isWinner,
                            onSelectWinner = if (judgmentMode == JudgmentMode.MANUAL) {
                                { onSelectWinner(message.providerId) }
                            } else null
                        )
                    }
                    
                    // Вердикт Kimi (если есть)
                    session.verdict?.let { verdict ->
                        item {
                            KimiVerdictCard(
                                verdict = verdict,
                                onAccept = onAcceptKimi,
                                showAccept = judgmentMode == JudgmentMode.HYBRID
                            )
                        }
                    }
                }
            }
            
            // Поле ввода вопроса
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    placeholder = { Text("Введите вопрос...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = {
                        if (question.isNotBlank()) {
                            onSendQuestion(question)
                            question = ""
                        }
                    },
                    enabled = !isLoading && question.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Отправить")
                }
            }
        }
    }
}

@Composable
fun AiResponseCard(
    message: ChatMessage,
    isWinner: Boolean,
    onSelectWinner: (() -> Unit)?
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isWinner) 
                MaterialTheme.colorScheme.tertiaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isWinner) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary)
        } else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.providerName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (isWinner) {
                    Text("🏆 Победитель!", color = MaterialTheme.colorScheme.tertiary)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (onSelectWinner != null && !isWinner) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onSelectWinner) {
                    Text("Выбрать как лучший")
                }
            }
        }
    }
}

@Composable
fun KimiVerdictCard(
    verdict: KimiVerdict,
    onAccept: () -> Unit,
    showAccept: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "👨‍⚖️ Вердикт Kimi",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("🏆 Победитель: ${verdict.winnerProviderName}")
            Text("📊 Уверенность: ${verdict.confidence.toInt()}%")
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Обоснование:",
                style = MaterialTheme.typography.labelMedium
            )
            Text(verdict.reasoning)
            
            if (verdict.criticism.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Критика других:",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(verdict.criticism)
            }
            
            if (showAccept) {
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(onClick = onAccept) {
                        Text("✅ Согласен с Kimi")
                    }
                }
            }
        }
    }
}

private fun getModeEmoji(mode: JudgmentMode): String = when (mode) {
    JudgmentMode.AUTO -> "🤖"
    JudgmentMode.HYBRID -> "⚖️"
    JudgmentMode.MANUAL -> "👤"
    JudgmentMode.BLIND -> "🎭"
}

private fun getModeName(mode: JudgmentMode): String = when (mode) {
    JudgmentMode.AUTO -> "Авто"
    JudgmentMode.HYBRID -> "Гибрид"
    JudgmentMode.MANUAL -> "Ручной"
    JudgmentMode.BLIND -> "Blind"
}
