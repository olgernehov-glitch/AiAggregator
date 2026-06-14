package com.example.aiaggregator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.aiaggregator.data.model.AIProvider
import com.example.aiaggregator.data.model.ProviderDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    providers: List<AIProvider>,
    configuredCount: Int,
    onApiKeyChange: (String, String) -> Unit,
    onLaunchClick: () -> Unit
) {
    var showHelp by remember { mutableStateOf(false) }
    var helpProvider by remember { mutableStateOf<AIProvider?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔑 Настройка API ключей") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Прогресс-бар
            LinearProgressIndicator(
                progress = { configuredCount / 7f },
                modifier = Modifier.fillMaxWidth(),
            )
            
            Text(
                text = "Настроено: $configuredCount / 7 провайдеров",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // Kimi отдельно — судья
            Text(
                text = "👨‍⚖️ Судья (обязательно для режимов Авто/Гибрид)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            
            ApiKeyInputField(
                provider = ProviderDefaults.KIMI.copy(
                    apiKey = providers.find { it.id == "kimi" }?.apiKey ?: ""
                ),
                onValueChange = onApiKeyChange,
                onHelpClick = { helpProvider = it; showHelp = true }
            )
            
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Остальные провайдеры
            Text(
                text = "🤖 ИИ-участники",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            ProviderDefaults.ALL_PROVIDERS.forEach { defaultProvider ->
                val provider = providers.find { it.id == defaultProvider.id } ?: defaultProvider
                ApiKeyInputField(
                    provider = provider,
                    onValueChange = onApiKeyChange,
                    onHelpClick = { helpProvider = it; showHelp = true }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Кнопка запуска
            Button(
                onClick = onLaunchClick,
                enabled = configuredCount >= 1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🚀 Запустить")
            }
            
            if (configuredCount == 0) {
                Text(
                    text = "Настройте хотя бы 1 провайдера",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
    
    // Диалог помощи
    if (showHelp && helpProvider != null) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Как получить ключ ${helpProvider!!.name}") },
            text = {
                Column {
                    Text("1. Перейдите на сайт ${helpProvider!!.name}")
                    Text("2. Зарегистрируйтесь / войдите")
                    Text("3. Найдите раздел API Keys")
                    Text("4. Создайте новый ключ")
                    Text("5. Скопируйте и вставьте сюда")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "URL: ${helpProvider!!.baseUrl}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text("Понятно")
                }
            }
        )
    }
}

@Composable
fun ApiKeyInputField(
    provider: AIProvider,
    onValueChange: (String, String) -> Unit,
    onHelpClick: (AIProvider) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val hasKey = provider.apiKey.isNotBlank()
    
    OutlinedTextField(
        value = provider.apiKey,
        onValueChange = { onValueChange(provider.id, it) },
        label = { Text(provider.name) },
        placeholder = { Text("Введите API ключ...") },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            Row {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Default.Clear else Icons.Default.Check,
                        contentDescription = if (visible) "Скрыть" else "Показать"
                    )
                }
                IconButton(onClick = { onHelpClick(provider) }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Помощь"
                    )
                }
                if (hasKey) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Настроено",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        isError = !hasKey && provider.id != "kimi",
        supportingText = {
            if (hasKey) {
                Text("✅ Настроено", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}
